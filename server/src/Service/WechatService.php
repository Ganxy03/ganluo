<?php

declare(strict_types=1);

namespace App\Service;

use App\Core\Config;
use App\Core\Response;

/**
 * 微信小程序服务端接口。
 */
final class WechatService
{
    /**
     * 用 wx.login 拿到的 code 换取 openid / session_key / unionid。
     *
     * @return array{openid:string, session_key?:string, unionid?:string}
     */
    public static function code2Session(string $code): array
    {
        $appid  = (string) Config::get('WECHAT_APPID', '');
        $secret = (string) Config::get('WECHAT_SECRET', '');

        if ($appid === '' || $secret === '') {
            Response::error('服务端未配置 WECHAT_APPID / WECHAT_SECRET', 500);
        }

        $url = 'https://api.weixin.qq.com/sns/jscode2session?' . http_build_query([
            'appid'      => $appid,
            'secret'     => $secret,
            'js_code'    => $code,
            'grant_type' => 'authorization_code',
        ]);

        $raw  = self::httpGet($url);
        $data = json_decode($raw, true);

        if (!is_array($data)) {
            Response::error('微信登录失败：返回内容无法解析', 502);
        }
        if (!empty($data['errcode'])) {
            $errcode = (int) $data['errcode'];

            // 常见错误码翻译成人话，其余原样返回便于排查
            $friendly = [
                40029 => '登录凭证已失效，请重试',
                45011 => '操作太频繁，请稍后再试',
                40226 => '该账号存在风险，已被限制登录',
                40013 => 'AppID 配置有误，请检查 server/.env',
                40125 => 'AppSecret 配置有误，请检查 server/.env',
            ][$errcode] ?? null;

            error_log('[ganluo] 微信 code2Session 失败: ' . $errcode . ' ' . ($data['errmsg'] ?? ''));

            Response::error($friendly ?? '微信登录失败：' . ($data['errmsg'] ?? '未知错误'), 400);
        }
        if (empty($data['openid'])) {
            Response::error('微信登录失败：未获取到 openid', 400);
        }

        return $data;
    }

    private static function httpGet(string $url): string
    {
        if (!function_exists('curl_init')) {
            return (string) @file_get_contents($url);
        }

        $result = self::curlGet($url);

        // 现场网络的首个 DNS 解析经常超时，抖动一下就好了 —— 重试一次
        if ($result === false) {
            usleep(300000);
            $result = self::curlGet($url);
        }

        if ($result === false) {
            Response::error('无法连接微信服务器，请检查服务器网络', 502);
        }

        return $result;
    }

    /** @return string|false 失败返回 false */
    private static function curlGet(string $url): string|false
    {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            // 首次解析 api.weixin.qq.com 可能要好几秒，超时给宽一点
            CURLOPT_TIMEOUT        => 15,
            CURLOPT_CONNECTTIMEOUT => 10,
            // Windows 上不指定的话，curl 会先试 IPv6，机器没有 IPv6 路由时
            // 要干等十几秒才回落 IPv4 —— 表现就是"第一次登录特别慢"。
            CURLOPT_IPRESOLVE      => CURL_IPRESOLVE_V4,
            // 进程内的 DNS 缓存留久一点，减少重复解析
            CURLOPT_DNS_CACHE_TIMEOUT => 300,
        ]);
        $result = curl_exec($ch);
        $errno  = curl_errno($ch);
        $error  = curl_error($ch);
        curl_close($ch);

        if ($result === false) {
            error_log('[ganluo] 调微信接口失败: ' . $errno . ' ' . $error);
            return false;
        }

        return (string) $result;
    }
}
