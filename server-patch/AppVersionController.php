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
            'latest_code'   => 5,
            'latest_name'   => '1.0.4',
            'download_url'  => 'https://y.bobay.top/downloads/ganluo-1.0.4.apk',
            'sha256'        => '',
            'changelog'     => "紧急修复:心情日记的「+ 记录」按钮无法点击(缺失 clickable 修饰符)",
            'force_update'  => false,
            'min_supported' => 1,
        ]);
    }
}
