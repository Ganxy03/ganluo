<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Core\Database;
use App\Core\Request;
use App\Core\Response;
use App\Support\Helper;

/**
 * 心情日记。支持和伴侣互相查看，但只能增删自己的记录。
 */
final class MoodController
{
    /** 心情类型 → 中文名（前端也保留一份，用于配色和图标） */
    public const TYPES = [
        'happy'   => '开心',
        'sweet'   => '幸福',
        'calm'    => '平静',
        'excited' => '兴奋',
        'tired'   => '疲惫',
        'sad'     => '难过',
        'angry'   => '生气',
        'anxious' => '焦虑',
    ];

    /** GET /api/moods?user_id=&date=2026-10-13   或   ?user_id=&month=2026-10 */
    public function index(array $params = []): void
    {
        $me     = Request::$user;
        $userId = Helper::resolveVisibleUserId((int) $me['id'], Request::int('user_id', (int) $me['id']));

        $month = Request::str('month');
        if ($month !== '') {
            $rows = Database::select(
                // 用单引号包字符串：MySQL 若开了 ANSI_QUOTES，双引号会被当成标识符而报错
                "SELECT * FROM moods WHERE user_id = ? AND DATE_FORMAT(mood_date, '%Y-%m') = ?
                  ORDER BY mood_date DESC, id DESC",
                [$userId, $month]
            );
        } else {
            $date = Request::str('date') !== '' ? Request::str('date') : date('Y-m-d');
            $rows = Database::select(
                'SELECT * FROM moods WHERE user_id = ? AND mood_date = ? ORDER BY id DESC',
                [$userId, $date]
            );
        }

        Response::ok(array_map([$this, 'format'], $rows));
    }

    /** POST /api/moods  { mood_type, content, mood_date } */
    public function store(array $params = []): void
    {
        $me       = Request::$user;
        $moodType = Request::str('mood_type');

        if (!array_key_exists($moodType, self::TYPES)) {
            Response::error('心情类型不正确');
        }

        $content  = mb_substr(Request::str('content'), 0, 500);
        $moodDate = $this->normalizeDate(Request::str('mood_date'));

        $id = Database::insert(
            'INSERT INTO moods (user_id, mood_type, content, mood_date, created_at) VALUES (?, ?, ?, ?, NOW())',
            [$me['id'], $moodType, $content, $moodDate]
        );

        Response::ok($this->format(Database::first('SELECT * FROM moods WHERE id = ?', [$id])), '已记录');
    }

    /** PUT /api/moods/{id} */
    public function update(array $params = []): void
    {
        $me = Request::$user;
        $id = (int) ($params['id'] ?? 0);

        $row = Database::first('SELECT * FROM moods WHERE id = ?', [$id]);
        if ($row === null || (int) $row['user_id'] !== (int) $me['id']) {
            Response::error('记录不存在', 404);
        }

        $moodType = Request::str('mood_type');
        if ($moodType !== '' && !array_key_exists($moodType, self::TYPES)) {
            Response::error('心情类型不正确');
        }
        $content = mb_substr(Request::str('content'), 0, 500);

        Database::execute(
            'UPDATE moods
                SET mood_type = IF(? = \'\', mood_type, ?),
                    content   = ?
              WHERE id = ?',
            [$moodType, $moodType, $content, $id]
        );

        Response::ok($this->format(Database::first('SELECT * FROM moods WHERE id = ?', [$id])), '已更新');
    }

    /** DELETE /api/moods/{id} */
    public function destroy(array $params = []): void
    {
        $me = Request::$user;
        $id = (int) ($params['id'] ?? 0);

        $affected = Database::execute('DELETE FROM moods WHERE id = ? AND user_id = ?', [$id, $me['id']]);
        if ($affected === 0) {
            Response::error('记录不存在', 404);
        }

        Response::ok(null, '已删除');
    }

    private function normalizeDate(string $date): string
    {
        return preg_match('/^\d{4}-\d{2}-\d{2}$/', $date) ? $date : date('Y-m-d');
    }

    public static function format(array $row): array
    {
        return [
            'id'         => (int) $row['id'],
            'user_id'    => (int) $row['user_id'],
            'mood_type'  => (string) $row['mood_type'],
            'mood_label' => self::TYPES[$row['mood_type']] ?? (string) $row['mood_type'],
            'content'    => (string) $row['content'],
            'mood_date'  => (string) $row['mood_date'],
            'created_at' => (string) $row['created_at'],
        ];
    }
}
