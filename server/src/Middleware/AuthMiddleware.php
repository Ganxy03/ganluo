<?php

declare(strict_types=1);

namespace App\Middleware;

use App\Core\Database;
use App\Core\Jwt;
use App\Core\Request;
use App\Core\Response;

/**
 * 校验 Authorization: Bearer <token>，成功后返回当前用户记录。
 */
final class AuthMiddleware
{
    public static function handle(): array
    {
        $token = Request::bearerToken();
        if ($token === null || $token === '') {
            Response::error('未登录', 401);
        }

        $payload = Jwt::decode($token);
        if ($payload === null || empty($payload['uid'])) {
            Response::error('登录已失效，请重新登录', 401);
        }

        $user = Database::first('SELECT * FROM users WHERE id = ? AND status = 1', [(int) $payload['uid']]);
        if ($user === null) {
            Response::error('用户不存在或已被禁用', 401);
        }

        return $user;
    }
}
