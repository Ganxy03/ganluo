<?php

declare(strict_types=1);

namespace App\Support;

use App\Core\Config;
use App\Core\Database;

/**
 * 通用工具：邀请码、昵称、对外数据结构。
 */
final class Helper
{
    /** 生成全局唯一的 8 位邀请码（对用户即"我的用户 ID"） */
    public static function uniqueInviteCode(): string
    {
        // 去掉容易混淆的 I O 0 1
        $alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

        for ($attempt = 0; $attempt < 20; $attempt++) {
            $code = '';
            for ($i = 0; $i < 8; $i++) {
                $code .= $alphabet[random_int(0, strlen($alphabet) - 1)];
            }
            if (Database::first('SELECT id FROM users WHERE invite_code = ?', [$code]) === null) {
                return $code;
            }
        }

        // 极端兜底
        return strtoupper(bin2hex(random_bytes(6)));
    }

    public static function randomNickname(): string
    {
        $adjectives = ['甜甜的', '软软的', '安静的', '闪亮的', '温柔的', '暖暖的'];
        $nouns      = ['小汤圆', '小芋圆', '小星球', '小月亮', '小云朵', '小橘子'];

        return $adjectives[random_int(0, count($adjectives) - 1)]
            . $nouns[random_int(0, count($nouns) - 1)];
    }

    /** 取某用户的伴侣（未绑定返回 null） */
    public static function partnerOf(int $userId): ?array
    {
        $row = Database::first('SELECT partner_id FROM users WHERE id = ?', [$userId]);
        if ($row === null || empty($row['partner_id'])) {
            return null;
        }
        return Database::first('SELECT * FROM users WHERE id = ?', [(int) $row['partner_id']]) ?: null;
    }

    /** 当前用户可见的目标用户：只能是自己或自己的伴侣 */
    public static function resolveVisibleUserId(int $meId, int $targetId): int
    {
        $targetId = $targetId > 0 ? $targetId : $meId;
        if ($targetId === $meId) {
            return $meId;
        }
        $partner = self::partnerOf($meId);
        if ($partner !== null && (int) $partner['id'] === $targetId) {
            return $targetId;
        }
        \App\Core\Response::error('无权访问对方的数据', 403);
    }

    /** 拼接上传文件的完整可访问 URL */
    public static function toAbsoluteUrl(string $path): string
    {
        if ($path === '' || preg_match('#^https?://#i', $path)) {
            return $path;
        }
        return self::baseUrl() . '/' . ltrim($path, '/');
    }

    /**
     * 当前请求的来源地址（协议 + 域名），用于拼接资源 URL。
     *
     * 这里刻意优先用「请求自带的 Host」而不是 .env 里的 APP_URL：
     * 客户端从哪个地址进来的，返回的图片 URL 就该是哪个地址。
     * 部署时忘了改 APP_URL 会导致图片全部指向内网 IP —— 这类坑从根上避免。
     */
    private static function baseUrl(): string
    {
        // Nginx / 宝塔等反向代理会把真实协议和域名放在这两个头里
        $host = $_SERVER['HTTP_X_FORWARDED_HOST'] ?? $_SERVER['HTTP_HOST'] ?? '';

        if ($host === '') {
            // 命令行或取不到 Host 时，才回退到配置
            return rtrim((string) Config::get('APP_URL', ''), '/');
        }

        $proto = $_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '';
        if ($proto === '') {
            $https = $_SERVER['HTTPS'] ?? '';
            $proto = ($https !== '' && $https !== 'off') ? 'https' : 'http';
        }

        return $proto . '://' . $host;
    }

    /** 对外输出的用户信息 */
    public static function userProfile(array $user, ?array $partner = null): array
    {
        return [
            'id'          => (int) $user['id'],
            'nickname'    => (string) $user['nickname'],
            'avatar'      => self::toAbsoluteUrl((string) $user['avatar']),
            'invite_code' => (string) $user['invite_code'],
            'signature'   => (string) ($user['signature'] ?? ''),
            'bind_at'     => $user['bind_at'] ?? null,
            'bound'       => !empty($user['partner_id']),
            'partner'     => $partner === null ? null : self::userBrief($partner),
        ];
    }

    /** 精简用户信息（用在列表、头像行） */
    public static function userBrief(array $user): array
    {
        return [
            'id'       => (int) $user['id'],
            'nickname' => (string) $user['nickname'],
            'avatar'   => self::toAbsoluteUrl((string) $user['avatar']),
        ];
    }
}
