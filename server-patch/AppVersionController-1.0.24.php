<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Core\Response;

final class AppVersionController
{
    public function latest(array $params = []): void
    {
        $changelog = <<<'TXT'
1.0.24 界面与体验:
• 首页可以直接留言了:留言卡底部新增输入条(发图 + 直接发送),不用再点进聊天窗
• 修复「日常」「我的」页面顶部多出一条空白(状态栏间距被叠加了两次)
• 地图 / 定位共享暂时隐藏:入口收起,后台也不再自动启动(功能还在打磨,后续放出来)
• 首页输入框支持键盘「发送」键直接发出,输入时自动避让键盘
TXT;

        Response::ok([
            "latest_code"   => 25,
            "latest_name"   => "1.0.24",
            "download_url"  => "https://y.bobay.top/downloads/ganluo-1.0.24.apk",
            "sha256"        => "1e4b413ebe898551ef7dab6d3ace09dfce8dd033aa39a0890bd7a7783620dd74",
            "changelog"     => $changelog,
            "force_update"  => false,
            "min_supported" => 1,
        ]);
    }
}
