<?php

declare(strict_types=1);

namespace App\Core;

/**
 * 极简 HS256 JWT 实现，不依赖第三方库。
 */
final class Jwt
{
    public static function encode(array $payload, ?int $ttl = null): string
    {
        $now = time();
        $ttl ??= Config::int('JWT_TTL', 604800);

        $payload = array_merge($payload, ['iat' => $now, 'exp' => $now + $ttl]);

        $header = self::b64url(json_encode(['alg' => 'HS256', 'typ' => 'JWT'], JSON_UNESCAPED_UNICODE));
        $body   = self::b64url(json_encode($payload, JSON_UNESCAPED_UNICODE));

        return $header . '.' . $body . '.' . self::sign($header . '.' . $body);
    }

    /** 校验并返回 payload；token 非法或过期返回 null */
    public static function decode(string $token): ?array
    {
        $parts = explode('.', $token);
        if (count($parts) !== 3) {
            return null;
        }
        [$header, $body, $signature] = $parts;

        if (!hash_equals(self::sign($header . '.' . $body), $signature)) {
            return null;
        }

        $payload = json_decode(self::unb64url($body), true);
        if (!is_array($payload)) {
            return null;
        }
        if (isset($payload['exp']) && (int) $payload['exp'] < time()) {
            return null;
        }

        return $payload;
    }

    private static function sign(string $data): string
    {
        return self::b64url(hash_hmac('sha256', $data, (string) Config::get('JWT_SECRET', 'ganluo-dev-secret'), true));
    }

    private static function b64url(string $raw): string
    {
        return rtrim(strtr(base64_encode($raw), '+/', '-_'), '=');
    }

    private static function unb64url(string $data): string
    {
        $remainder = strlen($data) % 4;
        if ($remainder > 0) {
            $data .= str_repeat('=', 4 - $remainder);
        }
        return (string) base64_decode(strtr($data, '-_', '+/'));
    }
}
