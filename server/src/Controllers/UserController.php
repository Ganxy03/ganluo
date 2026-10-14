<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Core\Database;
use App\Core\Request;
use App\Core\Response;
use App\Support\Helper;

final class UserController
{
    /** GET /api/user/me —— 自己的资料 + 伴侣信息 */
    public function me(array $params = []): void
    {
        $me      = Request::$user;
        $partner = Helper::partnerOf((int) $me['id']);

        Response::ok(Helper::userProfile($me, $partner));
    }

    /** PUT /api/user/me —— 修改昵称 / 头像 / 个性签名 */
    public function update(array $params = []): void
    {
        $me = Request::$user;

        $nickname  = mb_substr(Request::str('nickname'), 0, 32);
        $avatar    = mb_substr(Request::str('avatar'), 0, 255);
        $signature = mb_substr(Request::str('signature'), 0, 100);

        Database::execute(
            'UPDATE users
                SET nickname  = IF(? = \'\', nickname,  ?),
                    avatar    = IF(? = \'\', avatar,    ?),
                    signature = IF(? = \'\', signature, ?),
                    updated_at = NOW()
              WHERE id = ?',
            [$nickname, $nickname, $avatar, $avatar, $signature, $signature, $me['id']]
        );

        $fresh = Database::first('SELECT * FROM users WHERE id = ?', [$me['id']]);
        Response::ok(Helper::userProfile($fresh, Helper::partnerOf((int) $me['id'])), '保存成功');
    }

    /** GET /api/user/search?invite_code=XXXX —— 按用户ID查找对方，用于发起绑定 */
    public function search(array $params = []): void
    {
        $me   = Request::$user;
        $code = strtoupper(Request::str('invite_code'));

        if ($code === '') {
            Response::error('请输入对方的用户ID');
        }
        if (strcasecmp($code, (string) $me['invite_code']) === 0) {
            Response::error('不能绑定自己哦');
        }

        $target = Database::first('SELECT * FROM users WHERE invite_code = ? AND status = 1', [$code]);
        if ($target === null) {
            Response::error('没有找到这个用户ID，请确认后重试');
        }
        if (!empty($target['partner_id'])) {
            Response::error('对方已经绑定了伴侣');
        }
        if (!empty($me['partner_id'])) {
            Response::error('你已经绑定了伴侣，请先解除绑定');
        }

        Response::ok([
            'user'          => Helper::userBrief($target),
            'already_sent'  => Database::first(
                'SELECT id FROM bind_requests WHERE from_user_id = ? AND to_user_id = ? AND status = 0',
                [$me['id'], $target['id']]
            ) !== null,
        ]);
    }
}
