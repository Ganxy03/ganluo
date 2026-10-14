<?php

declare(strict_types=1);

namespace App\Core;

/**
 * 极简配置容器：从 .env 读取键值对，同时兼容真实环境变量（线上优先）。
 */
final class Config
{
    /** @var array<string, string> */
    private static array $items = [];

    private static bool $loaded = false;

    public static function load(string $envFile): void
    {
        if (self::$loaded) {
            return;
        }
        self::$loaded = true;

        if (is_file($envFile)) {
            foreach (file($envFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) ?: [] as $line) {
                $line = trim($line);
                if ($line === '' || str_starts_with($line, '#')) {
                    continue;
                }
                [$key, $value] = array_pad(explode('=', $line, 2), 2, '');
                self::$items[trim($key)] = trim(trim($value), "\"'");
            }
        }

        // 真实环境变量优先级更高
        foreach (array_keys(self::$items) as $key) {
            $env = getenv($key);
            if ($env !== false && $env !== '') {
                self::$items[$key] = $env;
            }
        }
    }

    public static function get(string $key, mixed $default = null): mixed
    {
        return self::$items[$key] ?? $default;
    }

    public static function bool(string $key, bool $default = false): bool
    {
        $value = self::get($key);
        if ($value === null) {
            return $default;
        }
        return in_array(strtolower((string) $value), ['1', 'true', 'yes', 'on'], true);
    }

    public static function int(string $key, int $default = 0): int
    {
        $value = self::get($key);
        return $value === null ? $default : (int) $value;
    }
}
