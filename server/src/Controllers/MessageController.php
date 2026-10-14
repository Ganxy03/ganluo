<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Core\Database;
use App\Core\Request;
use App\Core\Response;
use App\Support\Helper;

/**
 * 留言提醒：两个人之间的双向留言板。
 *
 * 设计要点：
 *  - 收发合并：不再分"收件箱 / 发件箱"，一次查询返回两个方向的留言
 *  - 按天查看：跟主页的日期切换保持一致
 *  - 支持回复：reply_to_id 记录"回的是哪条"，列表里带出引用原文
 *  - 解绑后不可见：所有查询都以"当前伴侣"为前提，解绑即查不到
 */
final class MessageController
{
    /** GET /api/messages?date=2026-10-14 —— 某天两个人之间的全部留言 */
    public function index(array $params = []): void
    {
        $me = Request::$user;
        Response::ok(self::boardFor((int) $me['id'], Request::str('date')));
    }

    /**
     * 留言板数据。做成 static 是为了让 /api/home 也能复用同一份逻辑，
     * 避免主页和留言页两处算法不一致。
     */
    public static function boardFor(int $meId, string $dateRaw): array
    {
        $date = self::normalizeDate($dateRaw);

        $partner = Helper::partnerOf($meId);

        // 未绑定：留言板是空的，但不算错误（主页可以正常渲染）
        if ($partner === null) {
            return self::emptyBoard($date);
        }

        $pid  = (int) $partner['id'];
        $from = $date . ' 00:00:00';
        $to   = self::nextDate($date) . ' 00:00:00';

        // 用 created_at 的半开区间而不是 DATE(created_at)=?，这样能吃到索引
        $rows = Database::select(
            "SELECT m.*,
                    fu.nickname AS from_nickname, fu.avatar AS from_avatar,
                    r.content   AS reply_content, r.from_user_id AS reply_from_user_id
               FROM messages m
               JOIN users fu ON fu.id = m.from_user_id
          LEFT JOIN messages r ON r.id = m.reply_to_id
              WHERE ((m.from_user_id = ? AND m.to_user_id = ?)
                  OR (m.from_user_id = ? AND m.to_user_id = ?))
                AND m.created_at >= ? AND m.created_at < ?
           ORDER BY m.id ASC",
            [$meId, $pid, $pid, $meId, $from, $to]
        );

        $unreadInDay = (int) (Database::first(
            'SELECT COUNT(*) AS c FROM messages
              WHERE to_user_id = ? AND from_user_id = ? AND is_read = 0
                AND created_at >= ? AND created_at < ?',
            [$meId, $pid, $from, $to]
        )['c'] ?? 0);

        $unreadTotal = (int) (Database::first(
            'SELECT COUNT(*) AS c FROM messages
              WHERE to_user_id = ? AND from_user_id = ? AND is_read = 0',
            [$meId, $pid]
        )['c'] ?? 0);

        // 最近一条未读所在的日期：主页用它提示"还有更早的未读"，点击可跳过去
        $latestUnread = Database::first(
            'SELECT DATE(created_at) AS d FROM messages
              WHERE to_user_id = ? AND from_user_id = ? AND is_read = 0
              ORDER BY id DESC LIMIT 1',
            [$meId, $pid]
        );

        return [
            'date'               => $date,
            'partner'            => Helper::userBrief($partner),
            'list'               => array_map(fn (array $r) => self::format($r, $meId), $rows),
            'unread_in_day'      => $unreadInDay,
            'unread_total'       => $unreadTotal,
            'latest_unread_date' => $latestUnread['d'] ?? null,
        ];
    }

    /** POST /api/messages  { content, reply_to_id? } —— 只能发给伴侣 */
    public function store(array $params = []): void
    {
        $me      = Request::$user;
        $content = mb_substr(Request::str('content'), 0, 500);

        if ($content === '') {
            Response::error('留言内容不能为空');
        }

        $partner = Helper::partnerOf((int) $me['id']);
        if ($partner === null) {
            Response::error('还没有绑定伴侣，先去绑定吧');
        }

        $pid     = (int) $partner['id'];
        $replyTo = Request::int('reply_to_id', 0);

        // 只能回复"我和当前伴侣之间"的留言，防止越权引用别人的内容
        if ($replyTo > 0) {
            $exists = Database::first(
                'SELECT id FROM messages
                  WHERE id = ?
                    AND ((from_user_id = ? AND to_user_id = ?)
                      OR (from_user_id = ? AND to_user_id = ?))',
                [$replyTo, $me['id'], $pid, $pid, $me['id']]
            );
            if ($exists === null) {
                Response::error('要回复的留言不存在');
            }
        } else {
            $replyTo = 0;
        }

        $id = Database::insert(
            'INSERT INTO messages (from_user_id, to_user_id, reply_to_id, content, created_at)
             VALUES (?, ?, ?, ?, NOW())',
            [$me['id'], $pid, $replyTo > 0 ? $replyTo : null, $content]
        );

        $row = Database::first(
            "SELECT m.*,
                    fu.nickname AS from_nickname, fu.avatar AS from_avatar,
                    r.content   AS reply_content, r.from_user_id AS reply_from_user_id
               FROM messages m
               JOIN users fu ON fu.id = m.from_user_id
          LEFT JOIN messages r ON r.id = m.reply_to_id
              WHERE m.id = ?",
            [$id]
        );

        Response::ok(self::format($row, (int) $me['id']), '已送达');
    }

    /** POST /api/messages/{id}/read */
    public function read(array $params = []): void
    {
        $me = Request::$user;
        $id = (int) ($params['id'] ?? 0);

        $affected = Database::execute(
            'UPDATE messages SET is_read = 1, read_at = NOW() WHERE id = ? AND to_user_id = ? AND is_read = 0',
            [$id, $me['id']]
        );

        if ($affected === 0) {
            Response::error('留言不存在或已读', 404);
        }
        Response::ok(null, '已读');
    }

    /** POST /api/messages/read-all  { date? } —— 传 date 只标那天，不传标全部 */
    public function readAll(array $params = []): void
    {
        $me = Request::$user;

        $partner = Helper::partnerOf((int) $me['id']);
        if ($partner === null) {
            Response::ok(null, '没有留言');
        }
        $pid = (int) $partner['id'];

        $date = Request::str('date');

        if ($date !== '') {
            $date = self::normalizeDate($date);
            Database::execute(
                'UPDATE messages SET is_read = 1, read_at = NOW()
                  WHERE to_user_id = ? AND from_user_id = ? AND is_read = 0
                    AND created_at >= ? AND created_at < ?',
                [$me['id'], $pid, $date . ' 00:00:00', self::nextDate($date) . ' 00:00:00']
            );
        } else {
            Database::execute(
                'UPDATE messages SET is_read = 1, read_at = NOW()
                  WHERE to_user_id = ? AND from_user_id = ? AND is_read = 0',
                [$me['id'], $pid]
            );
        }

        Response::ok(null, '已读');
    }

    /** DELETE /api/messages/{id} */
    public function destroy(array $params = []): void
    {
        $me = Request::$user;
        $id = (int) ($params['id'] ?? 0);

        // 发送者可以撤回，接收者可以删除
        $affected = Database::execute(
            'DELETE FROM messages WHERE id = ? AND (from_user_id = ? OR to_user_id = ?)',
            [$id, $me['id'], $me['id']]
        );

        if ($affected === 0) {
            Response::error('留言不存在', 404);
        }
        Response::ok(null, '已删除');
    }

    private static function normalizeDate(string $date): string
    {
        return preg_match('/^\d{4}-\d{2}-\d{2}$/', $date) ? $date : date('Y-m-d');
    }

    /** 半开区间用：日期 + 1 天 */
    private static function nextDate(string $date): string
    {
        $ts = strtotime($date . ' 00:00:00');
        return date('Y-m-d', ($ts === false ? time() : $ts) + 86400);
    }

    private static function emptyBoard(string $date): array
    {
        return [
            'date'               => $date,
            'partner'            => null,
            'list'               => [],
            'unread_in_day'      => 0,
            'unread_total'       => 0,
            'latest_unread_date' => null,
        ];
    }

    /** 输出给前端的单条留言 */
    private static function format(array $row, int $viewerId): array
    {
        return [
            'id'         => (int) $row['id'],
            'content'    => (string) $row['content'],
            'is_mine'    => (int) $row['from_user_id'] === $viewerId,
            'is_read'    => (int) $row['is_read'] === 1,
            'created_at' => (string) $row['created_at'],
            'time'       => substr((string) $row['created_at'], 11, 5),
            'author'     => [
                'id'       => (int) $row['from_user_id'],
                'nickname' => (string) $row['from_nickname'],
                'avatar'   => Helper::toAbsoluteUrl((string) $row['from_avatar']),
            ],
            'reply_to'   => $row['reply_to_id'] === null ? null : [
                'id'      => (int) $row['reply_to_id'],
                'content' => (string) $row['reply_content'],
                'is_mine' => (int) $row['reply_from_user_id'] === $viewerId,
            ],
        ];
    }
}
