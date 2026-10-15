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
            'latest_code'   => 6,
            'latest_name'   => '1.0.5',
            'download_url'  => 'https://y.bobay.top/downloads/ganluo-1.0.5.apk',
            'sha256'        => '',
            'changelog'     => "修复:心情选择弹窗的图标排版错位(改为等宽单元格,固定间距,最后一行左对齐)",
            'force_update'  => false,
            'min_supported' => 1,
        ]);
    }
}
