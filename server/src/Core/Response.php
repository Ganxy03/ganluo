<?php

declare(strict_types=1);

namespace App\Core;

/**
 * 统一响应格式：
 *   { "code": 0, "message": "ok", "data": ... }   code = 0 表示成功
 */
final class Response
{
    public static function json(mixed $data = null, int $code = 0, string $message = 'ok', int $httpStatus = 200): never
    {
        if (!headers_sent()) {
            http_response_code($httpStatus);
            header('Content-Type: application/json; charset=utf-8');
        }
        echo json_encode(
            ['code' => $code, 'message' => $message, 'data' => $data],
            JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES
        );
        exit;
    }

    public static function ok(mixed $data = null, string $message = 'ok'): never
    {
        self::json($data, 0, $message);
    }

    public static function error(string $message, int $httpStatus = 400, int $code = -1): never
    {
        self::json(null, $code, $message, $httpStatus);
    }
}
