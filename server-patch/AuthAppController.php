<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Core\Database;
use App\Core\Jwt;
use App\Core\Request;
use App\Core\Response;
use App\Support\Helper;

/**
 * POST /api/auth/login-app
 *
 * Android 端登录端点(替代小程序 wx.login)。
 * 输入 8 位邀请码,命中已有用户 → 直接签 JWT;
 * 未命中 → 自动建号(openid 占位为 'dev_app_<invite_code>'),邀请码沿用用户输入。
 *
 * ⚠️ dev 模式,不校验密码(等价于小程序"知道 openid 就能进"的威胁模型)。
 *    正式接入微信开放平台 SDK 后,这里加 PKCE / code2Session 流程。
 */
final class AuthAppController
{
    /** POST /api/auth/login-app */
    public function login(array $params = []): void
    {
        $invite = strtoupper(Request::str('invite_code'));
        if ($invite === '') {
            Response::error('缺少 invite_code 参数');
        }

        // 跟原小程序一致的 8 位规则:字母数字,去 I O 0 1
        if (!preg_match('/^[A-HJ-KLMNP-QR-Z2-9]{8}$/', $invite)) {
            Response::error('邀请码必须是 8 位字母数字(已去掉 I O 0 1)');
        }

        $isNew = false;
        $user  = Database::first('SELECT * FROM users WHERE invite_code = ?', [$invite]);

        if ($user === null) {
            // 自动建号 — openid 用 dev_app_<invite_code> 占位(微信 openid 不会有这前缀)
            $fakeOpenid = 'dev_app_' . $invite;
            try {
                $id = Database::insert(
                    'INSERT INTO users (openid, nickname, avatar, invite_code, created_at, updated_at)
                     VALUES (?, ?, ?, ?, NOW(), NOW())',
                    [
                        $fakeOpenid,
                        Helper::randomNickname(),
                        '',
                        $invite,    // 用户输入的码直接绑上
                    ]
                );
            } catch (\Throwable $e) {
                Response::error('建号失败:' . $e->getMessage(), 500);
            }
            $user  = Database::first('SELECT * FROM users WHERE id = ?', [$id]);
            $isNew = true;
        }

        if ($user === null) {
            Response::error('建号后查不到用户', 500);
        }

        Response::ok([
            'token'  => Jwt::encode(['uid' => (int) $user['id']]),
            'is_new' => $isNew,
            'user'   => Helper::userProfile($user, Helper::partnerOf((int) $user['id'])),
        ]);
    }
}
