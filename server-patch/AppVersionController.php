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
            'latest_code'   => 10,
            'latest_name'   => '1.0.9',
            'download_url'  => 'https://y.bobay.top/downloads/ganluo-1.0.9.apk',
            'sha256'        => '',
            'changelog'     => "1.0.9 消息发图:\n• 「我的」页编辑改弹窗(头像+昵称+签名一站搞定)\n• 留言支持发图片:点「图片」按钮 → 选图 → 自动上传 → 预览 → 发送\n• 留言支持纯图、图文混排、回复时带图(被回复方也能看到)\n• 不影响小程序端:后端 messages 表加 image_url 可空字段,老数据不丢",
            'force_update'  => false,
            'min_supported' => 1,
        ]);
    }
}
