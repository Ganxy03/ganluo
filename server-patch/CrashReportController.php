<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Core\Request;
use App\Core\Response;

/**
 * 客户端崩溃上报(1.0.22 新增)
 *
 * POST /api/crash   无需登录(崩溃时可能压根没有 token)
 *
 * 客户端在两种情况下会打这个接口:
 *  1. type=java    —— Java 未捕获异常,进程死之前同步 POST
 *  2. type=session —— 上次进程没有正常收尾(native 崩溃 / 被系统杀),
 *                     下次启动时把「面包屑」补报上来
 *
 * 目的是让「打开就闪退」这类没有堆栈的问题可以远程定位,不用再靠用户截图。
 * 落盘到 server/storage/crash/*.log,一行一个文件,方便直接 cat。
 */
final class CrashReportController
{
    public function store(array $params = []): void
    {
        $type    = $this->safe(Request::str('type', 'unknown'), 16);
        $time    = Request::str('time', date('Y-m-d H:i:s'));
        $device  = Request::str('device', '-');
        $version = Request::str('app_version', '-');
        $code    = Request::int('app_code', 0);
        $thread  = Request::str('thread', '-');
        $session = Request::str('session', '');
        $stack   = Request::str('stack', '');

        // 防滥用:单条最大 64KB
        $stack   = mb_substr($stack, 0, 65536);
        $session = mb_substr($session, 0, 32768);

        $dir = BASE_PATH . '/storage/crash';
        if (!is_dir($dir) && !@mkdir($dir, 0755, true) && !is_dir($dir)) {
            Response::error('crash 目录不可写', 500);
        }

        $name = date('Ymd_His')
            . '_' . $type
            . '_' . substr(md5(uniqid('', true) . $device), 0, 6)
            . '.log';

        $body = "time: {$time}\n"
            . "type: {$type}\n"
            . "version: {$version} ({$code})\n"
            . "device: {$device}\n"
            . "thread: {$thread}\n"
            . 'ip: ' . ($_SERVER['REMOTE_ADDR'] ?? '-') . "\n"
            . "---- session ----\n{$session}\n"
            . "---- stack ----\n{$stack}\n";

        if (@file_put_contents($dir . '/' . $name, $body) === false) {
            Response::error('写入失败', 500);
        }
        @chmod($dir . '/' . $name, 0644);

        Response::ok(['saved' => $name]);
    }

    private function safe(string $s, int $max): string
    {
        return substr(preg_replace('/[^a-zA-Z0-9_.-]/', '_', $s) ?? 'unknown', 0, $max);
    }
}
