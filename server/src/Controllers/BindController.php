<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Core\Database;
use App\Core\Request;
use App\Core\Response;
use App\Support\Helper;

/**
 * 绑定流程：
 *   A 输入 B 的用户ID → 发起绑定请求(status=0)
 *   B 在个人页看到请求 → 同意(status=1，双方互为伴侣) / 拒绝(status=2)
 *   A 可撤销(status=3)
 * 已绑定的双方只有通过"解除绑定"才能重新绑定他人。
 */
final class BindController
{
    private const PENDING  = 0;
    private const ACCEPTED = 1;
    private const REJECTED = 2;
    private const CANCELED = 3;

    /** POST /api/bind/apply  { invite_code } */
    public function apply(array $params = []): void
    {
        $me   = Request::$user;
        $code = strtoupper(Request::str('invite_code'));

        if ($code === '') {
            Response::error('请输入对方的用户ID');
        }
        if (strcasecmp($code, (string) $me['invite_code']) === 0) {
            Response::error('不能绑定自己哦');
        }
        if (!empty($me['partner_id'])) {
            Response::error('你已经绑定了伴侣，请先解除绑定');
        }

        $target = Database::first('SELECT * FROM users WHERE invite_code = ? AND status = 1', [$code]);
        if ($target === null) {
            Response::error('没有找到这个用户ID，请确认后重试');
        }
        if (!empty($target['partner_id'])) {
            Response::error('对方已经绑定了伴侣');
        }

        // 对方已向我发起过请求 → 直接互相绑定
        $incoming = Database::first(
            'SELECT * FROM bind_requests WHERE from_user_id = ? AND to_user_id = ? AND status = 0',
            [$target['id'], $me['id']]
        );
        if ($incoming !== null) {
            $this->bind((int) $me['id'], (int) $target['id']);
            Database::execute('UPDATE bind_requests SET status = ?, updated_at = NOW() WHERE id = ?', [self::ACCEPTED, $incoming['id']]);
            Response::ok(['status' => 'bound'], '绑定成功，开始记录你们的日子吧');
        }

        $exists = Database::first(
            'SELECT id FROM bind_requests WHERE from_user_id = ? AND to_user_id = ? AND status = 0',
            [$me['id'], $target['id']]
        );
        if ($exists !== null) {
            Response::ok(['status' => 'pending'], '已经发过请求了，等对方同意吧');
        }

        Database::insert(
            'INSERT INTO bind_requests (from_user_id, to_user_id, status, created_at, updated_at)
             VALUES (?, ?, ?, NOW(), NOW())',
            [$me['id'], $target['id'], self::PENDING]
        );

        Response::ok([
            'status' => 'pending',
            'user'   => Helper::userBrief($target),
        ], '绑定请求已发送');
    }

    /** GET /api/bind/requests —— 收到的 + 我发出的待处理请求 */
    public function requests(array $params = []): void
    {
        $me = Request::$user;

        $incoming = Database::select(
            'SELECT r.id, r.status, r.created_at, u.id AS user_id, u.nickname, u.avatar
               FROM bind_requests r
               JOIN users u ON u.id = r.from_user_id
              WHERE r.to_user_id = ? AND r.status = 0
              ORDER BY r.id DESC',
            [$me['id']]
        );

        $outgoing = Database::select(
            'SELECT r.id, r.status, r.created_at, u.id AS user_id, u.nickname, u.avatar
               FROM bind_requests r
               JOIN users u ON u.id = r.to_user_id
              WHERE r.from_user_id = ? AND r.status = 0
              ORDER BY r.id DESC',
            [$me['id']]
        );

        Response::ok([
            'incoming' => array_map([$this, 'formatRequest'], $incoming),
            'outgoing' => array_map([$this, 'formatRequest'], $outgoing),
        ]);
    }

    /** POST /api/bind/handle  { request_id, action: accept|reject } */
    public function handle(array $params = []): void
    {
        $me     = Request::$user;
        $id     = Request::int('request_id');
        $action = Request::str('action');

        $req = Database::first('SELECT * FROM bind_requests WHERE id = ?', [$id]);
        if ($req === null || (int) $req['to_user_id'] !== (int) $me['id']) {
            Response::error('请求不存在');
        }
        if ((int) $req['status'] !== self::PENDING) {
            Response::error('该请求已处理');
        }

        if ($action === 'accept') {
            $from = Database::first('SELECT * FROM users WHERE id = ?', [(int) $req['from_user_id']]);
            if ($from === null || !empty($from['partner_id'])) {
                Database::execute('UPDATE bind_requests SET status = ?, updated_at = NOW() WHERE id = ?', [self::CANCELED, $id]);
                Response::error('对方已经绑定了其他人');
            }
            if (!empty($me['partner_id'])) {
                Response::error('你已经绑定了伴侣，请先解除绑定');
            }

            $this->bind((int) $me['id'], (int) $from['id']);
            Database::execute('UPDATE bind_requests SET status = ?, updated_at = NOW() WHERE id = ?', [self::ACCEPTED, $id]);

            Response::ok(['status' => 'bound'], '绑定成功，开始记录你们的日子吧');
        }

        Database::execute('UPDATE bind_requests SET status = ?, updated_at = NOW() WHERE id = ?', [self::REJECTED, $id]);
        Response::ok(['status' => 'rejected'], '已拒绝');
    }

    /** POST /api/bind/cancel  { request_id } —— 撤销我发出的请求 */
    public function cancel(array $params = []): void
    {
        $me = Request::$user;
        $id = Request::int('request_id');

        $affected = Database::execute(
            'UPDATE bind_requests SET status = ?, updated_at = NOW()
              WHERE id = ? AND from_user_id = ? AND status = 0',
            [self::CANCELED, $id, $me['id']]
        );

        if ($affected === 0) {
            Response::error('请求不存在或已处理');
        }
        Response::ok(null, '已撤销');
    }

    /** POST /api/bind/unbind —— 解除绑定关系（双方数据保留，仅断开关系） */
    public function unbind(array $params = []): void
    {
        $me      = Request::$user;
        $partner = Helper::partnerOf((int) $me['id']);

        if ($partner === null) {
            Response::error('你还没有绑定伴侣');
        }

        Database::transaction(function () use ($me, $partner): void {
            Database::execute('UPDATE users SET partner_id = NULL, bind_at = NULL, updated_at = NOW() WHERE id IN (?, ?)', [$me['id'], $partner['id']]);
        });

        Response::ok(null, '已解除绑定');
    }

    /** 建立双向绑定关系 */
    private function bind(int $a, int $b): void
    {
        Database::transaction(function () use ($a, $b): void {
            $now = date('Y-m-d H:i:s');
            Database::execute('UPDATE users SET partner_id = ?, bind_at = ?, updated_at = ? WHERE id = ?', [$b, $now, $now, $a]);
            Database::execute('UPDATE users SET partner_id = ?, bind_at = ?, updated_at = ? WHERE id = ?', [$a, $now, $now, $b]);
        });
    }

    private function formatRequest(array $row): array
    {
        return [
            'request_id' => (int) $row['id'],
            'user'       => [
                'id'       => (int) $row['user_id'],
                'nickname' => (string) $row['nickname'],
                'avatar'   => (string) $row['avatar'],
            ],
            'created_at' => (string) $row['created_at'],
        ];
    }
}
