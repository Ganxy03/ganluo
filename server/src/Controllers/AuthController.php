<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Core\Database;
use App\Core\Jwt;
use App\Core\Request;
use App\Core\Response;
use App\Service\WechatService;
use App\Support\Helper;

/**
 * 登录：wx.login 拿 code → code2Session 换 openid → 首次自动注册 → 签发 token。
 */
final class AuthController
{
    /** POST /api/auth/login */
    public function login(array $params = []): void
    {
        $code = Request::str('code');
        if ($code === '') {
            Response::error('缺少 code 参数');
        }

        $session = WechatService::code2Session($code);
        $openid  = (string) $session['openid'];

        $user  = Database::first('SELECT * FROM users WHERE openid = ?', [$openid]);
        $isNew = false;

        if ($user === null) {
            $id = Database::insert(
                'INSERT INTO users (openid, unionid, nickname, avatar, invite_code, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, NOW(), NOW())',
                [
                    $openid,
                    $session['unionid'] ?? null,
                    Request::str('nickname') !== '' ? Request::str('nickname') : Helper::randomNickname(),
                    Request::str('avatar'),
                    Helper::uniqueInviteCode(),
                ]
            );
            $user  = Database::first('SELECT * FROM users WHERE id = ?', [$id]);
            $isNew = true;
        } else {
            // 用小程序侧最新的头像 / 昵称覆盖
            $nickname = Request::str('nickname');
            $avatar   = Request::str('avatar');
            if ($nickname !== '' || $avatar !== '') {
                Database::execute(
                    'UPDATE users
                        SET nickname = IF(? = \'\', nickname, ?),
                            avatar   = IF(? = \'\', avatar, ?),
                            updated_at = NOW()
                      WHERE id = ?',
                    [$nickname, $nickname, $avatar, $avatar, $user['id']]
                );
                $user = Database::first('SELECT * FROM users WHERE id = ?', [$user['id']]);
            }
        }

        Response::ok([
            'token'  => Jwt::encode(['uid' => (int) $user['id']]),
            'is_new' => $isNew,
            'user'   => Helper::userProfile($user, Helper::partnerOf((int) $user['id'])),
        ]);
    }

    /** GET /api/auth/me */
    public function me(array $params = []): void
    {
        $me = Request::$user;
        Response::ok(Helper::userProfile($me, Helper::partnerOf((int) $me['id'])));
    }
}
