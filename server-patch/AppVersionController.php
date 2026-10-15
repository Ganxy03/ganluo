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
            'latest_code'   => 4,
            'latest_name'   => '1.0.3',
            'download_url'  => 'https://y.bobay.top/downloads/ganluo-1.0.3.apk',
            'sha256'        => '',
            'changelog'     => "1. 缩短顶部导航栏至 44dp,贴近主流 App\n2. 暂时隐藏首页蓝牙入口\n3. 正式版本号命名(去掉 -debug 后缀)",
            'force_update'  => false,
            'min_supported' => 1,
        ]);
    }
}
