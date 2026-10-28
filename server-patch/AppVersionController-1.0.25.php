<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Core\Response;

final class AppVersionController
{
    public function latest(array $params = []): void
    {
        $changelog = <<<'TXT'
1.0.25 首页留言卡片改版:
• 留言卡撑满首页剩余高度,变成一个小聊天窗
• 我的留言靠右(主题色气泡),TA 的留言靠左(浅色气泡),和聊天页一致
• 消息区内部滚动,进入页面自动贴到最新一条;标题栏和输入框固定,不会被顶走
• 首页整页不再纵向滚动,日历/心情收起时留言区自动变高
• 首页直接留言、发图仍然可用
TXT;

        Response::ok([
            "latest_code"   => 26,
            "latest_name"   => "1.0.25",
            "download_url"  => "https://y.bobay.top/downloads/ganluo-1.0.25.apk",
            "sha256"        => "25bb01fb16e69ff4c22e87638fa4b8cb9c52d0db14bdc29d3b4c91fc43ec47f1",
            "changelog"     => $changelog,
            "force_update"  => false,
            "min_supported" => 1,
        ]);
    }
}
