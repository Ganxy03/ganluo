<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Core\Response;

final class AppVersionController
{
    public function latest(array $params = []): void
    {
        $changelog = <<<'TXT'
1.0.22 稳定性修复(重点解决闪退):
• 彻底修复「打开就闪退」:后台位置共享改用系统定位(LocationManager),不再调用高德定位 SDK 的 native 库
• 高德只负责地图显示,并在进入地图前预检 native 库;不可用时显示降级页面而不是崩溃
• 崩溃自动上报:异常同步上报服务器(设备/版本/堆栈),下次启动还会补报「上次异常结束」的阶段面包屑
• 位置共享更省电:后台定位间隔 30s → 60s(上报节流不变:位移>100m 或 5 分钟)
• 定位坐标自动 WGS-84 → GCJ-02 转换,与高德地图瓦片对齐
• 高德 API 每天额度有限,现在定位不再消耗高德配额
TXT;

        Response::ok([
            "latest_code"   => 23,
            "latest_name"   => "1.0.22",
            "download_url"  => "https://y.bobay.top/downloads/ganluo-1.0.22.apk",
            "sha256"        => "539579837f8473db8d98975b0ee427be9d9af09d4a92f62a9347d8dd59a0e040",
            "changelog"     => $changelog,
            "force_update"  => false,
            "min_supported" => 1,
        ]);
    }
}
