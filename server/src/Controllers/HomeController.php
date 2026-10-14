<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Core\Database;
use App\Core\Request;
use App\Core\Response;
use App\Support\Helper;

/**
 * 主页聚合接口：一次请求返回指定日期的全部数据，减少小程序首屏的请求次数。
 *
 * 返回结构对应主页「按功能分块」的布局：
 *   me / partner  → 心情日记、三餐报备 两块各取所需
 *   messages      → 留言提醒（两个人的双向留言板，与 MessageController 共用同一份逻辑）
 */
final class HomeController
{
    /** GET /api/home?date=2026-10-14 */
    public function index(array $params = []): void
    {
        $me   = Request::$user;
        $raw  = Request::str('date');
        $date = preg_match('/^\d{4}-\d{2}-\d{2}$/', $raw) ? $raw : date('Y-m-d');
        $meId = (int) $me['id'];

        $partner = Helper::partnerOf($meId);

        Response::ok([
            'date'     => $date,
            'me'       => $this->panel($me, $date),
            'partner'  => $partner === null ? null : $this->panel($partner, $date),
            'messages' => MessageController::boardFor($meId, $date),
        ]);
    }

    /** 单个人在当天的数据：用户信息 + 当日心情 + 当日三餐 */
    private function panel(array $user, string $date): array
    {
        $userId = (int) $user['id'];

        $moods = Database::select(
            'SELECT * FROM moods WHERE user_id = ? AND mood_date = ? ORDER BY id DESC',
            [$userId, $date]
        );

        $meals = Database::select(
            "SELECT * FROM meals WHERE user_id = ? AND meal_date = ?
              ORDER BY FIELD(meal_type, 'breakfast','lunch','dinner','supper')",
            [$userId, $date]
        );

        return [
            'user'  => Helper::userBrief($user),
            'moods' => array_map([MoodController::class, 'format'], $moods),
            'meals' => array_map([MealController::class, 'format'], $meals),
        ];
    }
}
