<?php

declare(strict_types=1);

namespace App\Core;

use PDO;
use PDOException;
use Throwable;

/**
 * PDO 单例 + 常用查询快捷方法。全部使用预处理语句，避免 SQL 注入。
 */
final class Database
{
    private static ?PDO $pdo = null;

    public static function pdo(): PDO
    {
        if (self::$pdo instanceof PDO) {
            return self::$pdo;
        }

        $dsn = sprintf(
            'mysql:host=%s;port=%s;dbname=%s;charset=utf8mb4',
            (string) Config::get('DB_HOST', '127.0.0.1'),
            (string) Config::get('DB_PORT', '3306'),
            (string) Config::get('DB_DATABASE', 'ganluo')
        );

        try {
            self::$pdo = new PDO(
                $dsn,
                (string) Config::get('DB_USERNAME', 'root'),
                (string) Config::get('DB_PASSWORD', ''),
                [
                    PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                    PDO::ATTR_EMULATE_PREPARES   => false,
                ]
            );
        } catch (PDOException $e) {
            Response::error('数据库连接失败：' . $e->getMessage(), 500);
        }

        return self::$pdo;
    }

    /** @return array<int, array<string, mixed>> */
    public static function select(string $sql, array $bind = []): array
    {
        $stmt = self::pdo()->prepare($sql);
        $stmt->execute($bind);
        return $stmt->fetchAll();
    }

    /** @return array<string, mixed>|null */
    public static function first(string $sql, array $bind = []): ?array
    {
        $stmt = self::pdo()->prepare($sql);
        $stmt->execute($bind);
        $row = $stmt->fetch();
        return $row === false ? null : $row;
    }

    /** 返回受影响行数 */
    public static function execute(string $sql, array $bind = []): int
    {
        $stmt = self::pdo()->prepare($sql);
        $stmt->execute($bind);
        return $stmt->rowCount();
    }

    /** 返回自增主键 */
    public static function insert(string $sql, array $bind = []): int
    {
        self::execute($sql, $bind);
        return (int) self::pdo()->lastInsertId();
    }

    /** 在事务中执行回调，异常自动回滚 */
    public static function transaction(callable $callback): mixed
    {
        $pdo = self::pdo();
        $pdo->beginTransaction();
        try {
            $result = $callback($pdo);
            $pdo->commit();
            return $result;
        } catch (Throwable $e) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            throw $e;
        }
    }
}
