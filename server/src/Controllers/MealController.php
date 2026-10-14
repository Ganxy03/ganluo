<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Core\Database;
use App\Core\Request;
use App\Core\Response;
use App\Support\Helper;

/**
 * 三餐报备：早餐 / 午餐 / 晚餐 / 宵夜，可带多张图片。
 */
final class MealController
{
    public const TYPES = [
        'breakfast' => '早餐',
        'lunch'     => '午餐',
        'dinner'    => '晚餐',
        'supper'    => '宵夜',
    ];

    /** GET /api/meals?user_id=&date=2026-10-13 */
    public function index(array $params = []): void
    {
        $me     = Request::$user;
        $userId = Helper::resolveVisibleUserId((int) $me['id'], Request::int('user_id', (int) $me['id']));
        $date   = $this->normalizeDate(Request::str('date'));

        $rows = Database::select(
            'SELECT * FROM meals WHERE user_id = ? AND meal_date = ? ORDER BY FIELD(meal_type, \'breakfast\',\'lunch\',\'dinner\',\'supper\')',
            [$userId, $date]
        );

        Response::ok(array_map([$this, 'format'], $rows));
    }

    /** POST /api/meals  { meal_type, content, images: [] } */
    public function store(array $params = []): void
    {
        $me       = Request::$user;
        $mealType = Request::str('meal_type');

        if (!array_key_exists($mealType, self::TYPES)) {
            Response::error('餐次不正确');
        }

        $content = mb_substr(Request::str('content'), 0, 500);
        $images  = $this->normalizeImages(Request::input('images', []));
        $date    = $this->normalizeDate(Request::str('meal_date'));

        if ($content === '' && $images === []) {
            Response::error('写点什么或者拍张照吧');
        }

        // 同一天同一餐次只保留一条，重复提交视为覆盖更新
        $existing = Database::first(
            'SELECT id FROM meals WHERE user_id = ? AND meal_type = ? AND meal_date = ?',
            [$me['id'], $mealType, $date]
        );

        if ($existing !== null) {
            Database::execute(
                'UPDATE meals SET content = ?, images = ? WHERE id = ?',
                [$content, json_encode($images, JSON_UNESCAPED_UNICODE), $existing['id']]
            );
            $id = (int) $existing['id'];
        } else {
            $id = Database::insert(
                'INSERT INTO meals (user_id, meal_type, content, images, meal_date, created_at)
                 VALUES (?, ?, ?, ?, ?, NOW())',
                [$me['id'], $mealType, $content, json_encode($images, JSON_UNESCAPED_UNICODE), $date]
            );
        }

        Response::ok($this->format(Database::first('SELECT * FROM meals WHERE id = ?', [$id])), '已报备');
    }

    /** DELETE /api/meals/{id} */
    public function destroy(array $params = []): void
    {
        $me = Request::$user;
        $id = (int) ($params['id'] ?? 0);

        $affected = Database::execute('DELETE FROM meals WHERE id = ? AND user_id = ?', [$id, $me['id']]);
        if ($affected === 0) {
            Response::error('记录不存在', 404);
        }

        Response::ok(null, '已删除');
    }

    private function normalizeDate(string $date): string
    {
        return preg_match('/^\d{4}-\d{2}-\d{2}$/', $date) ? $date : date('Y-m-d');
    }

    /** @return array<int, string> */
    private function normalizeImages(mixed $images): array
    {
        if (!is_array($images)) {
            return [];
        }
        $result = [];
        foreach ($images as $url) {
            if (is_string($url) && $url !== '') {
                $result[] = mb_substr($url, 0, 255);
            }
        }
        return array_slice($result, 0, 9);
    }

    public static function format(array $row): array
    {
        $images = json_decode((string) $row['images'], true);

        return [
            'id'         => (int) $row['id'],
            'user_id'    => (int) $row['user_id'],
            'meal_type'  => (string) $row['meal_type'],
            'meal_label' => self::TYPES[$row['meal_type']] ?? (string) $row['meal_type'],
            'content'    => (string) $row['content'],
            'images'     => is_array($images) ? array_map([Helper::class, 'toAbsoluteUrl'], $images) : [],
            'meal_date'  => (string) $row['meal_date'],
            'created_at' => (string) $row['created_at'],
        ];
    }
}
