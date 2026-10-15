<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Core\Response;

/**
 * 返回 App 最新版本号。Android 客户端启动时调一次,
 * 大于本地 versionCode 就弹更新。
 */
final class AppVersionController
{
    public function latest(array $params = []): void
    {
        Response::ok([
            'latest_code'   => 8,
            'latest_name'   => '1.0.7',
            'download_url'  => 'https://y.bobay.top/downloads/ganluo-1.0.7.apk',
            'sha256'        => '',
            'changelog'     => "新增:头像上传(「我的」页点头像或右下角「换」角标 → 系统相册选图 → 自动上传到 https://y.bobay.top/uploads/,旧后端 /api/upload/image 端点,无需新后端代码)。",
            'force_update'  => false,
            'min_supported' => 1,
        ]);
    }
}
