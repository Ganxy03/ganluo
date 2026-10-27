<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Core\Response;

final class AppVersionController
{
    public function latest(array $params = []): void
    {
        $changelog = <<<'TXT'
1.0.23 修复小米 / Android 15 启动闪退:
• 真凶:前台服务没能成功进入前台时,系统会在 10 秒内直接杀掉整个进程,没有任何崩溃堆栈
• 现在 startForeground 失败会立刻 stopSelf 自救,并把原始异常上报,不会再被系统强杀
• 进入前台分三级降级尝试(location 类型 → 无类型 → 普通两参),尽量让后台共享跑起来
• 失败过的机型会记住「本机不支持后台定位服务」,以后不再自动重试,保证 App 稳定可用
• 服务内新增细粒度阶段埋点(svc.onCreate / svc.fg.ok / svc.loop.enter / svc.first.fix)
• 后台共享不可用时,打开地图页依然能实时定位自己并同步给对方
TXT;

        Response::ok([
            "latest_code"   => 24,
            "latest_name"   => "1.0.23",
            "download_url"  => "https://y.bobay.top/downloads/ganluo-1.0.23.apk",
            "sha256"        => "19bef2f94c214d10ea02e6d0af678b913c99a12a1bbf9e0d978e822b88127038",
            "changelog"     => $changelog,
            "force_update"  => false,
            "min_supported" => 1,
        ]);
    }
}
