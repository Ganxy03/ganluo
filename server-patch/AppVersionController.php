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
            'latest_code'   => 7,
            'latest_name'   => '1.0.6',
            'download_url'  => 'https://y.bobay.top/downloads/ganluo-1.0.6.apk',
            'sha256'        => '',
            'changelog'     => "修复:心情选择弹窗只显示 3 个图标(1.0.5 改 cols 自适应时,小屏选 3 列但弹窗被 maxHeight 裁掉剩余行)。改为固定 4 列 + 内容 verticalScroll,8 个永远都在。",
            'force_update'  => false,
            'min_supported' => 1,
        ]);
    }
}
