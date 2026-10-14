<?php

declare(strict_types=1);

namespace App\Core;

/**
 * 请求上下文。Request::$user 由 Router 在鉴权通过后注入当前登录用户。
 */
final class Request
{
    /** @var array<string, mixed>|null */
    public static ?array $user = null;

    /** @var array<string, mixed>|null */
    private static ?array $body = null;

    public static function method(): string
    {
        return strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');
    }

    /** 归一化后的路径，如 /api/moods */
    public static function path(): string
    {
        $path = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
        return '/' . trim($path, '/');
    }

    public static function header(string $name): ?string
    {
        $key = 'HTTP_' . strtoupper(str_replace('-', '_', $name));
        return $_SERVER[$key] ?? null;
    }

    /** 解析 JSON 请求体，回退到表单数据 */
    public static function body(): array
    {
        if (self::$body === null) {
            $raw  = file_get_contents('php://input') ?: '';
            $json = json_decode($raw, true);
            self::$body = is_array($json) ? $json : $_POST;
        }
        return self::$body;
    }

    public static function input(string $key, mixed $default = null): mixed
    {
        $body = self::body();
        if (array_key_exists($key, $body)) {
            return $body[$key];
        }
        return $_GET[$key] ?? $default;
    }

    public static function query(string $key, mixed $default = null): mixed
    {
        return $_GET[$key] ?? $default;
    }

    public static function str(string $key, string $default = ''): string
    {
        $value = self::input($key, $default);
        return is_scalar($value) ? trim((string) $value) : $default;
    }

    public static function int(string $key, int $default = 0): int
    {
        $value = self::input($key, $default);
        return is_numeric($value) ? (int) $value : $default;
    }

    /** 从 Authorization: Bearer xxx 中取出 token */
    public static function bearerToken(): ?string
    {
        $auth = self::header('Authorization') ?? '';
        if (preg_match('/^Bearer\s+(.+)$/i', $auth, $m)) {
            return trim($m[1]);
        }
        return null;
    }
}
