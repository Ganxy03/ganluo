<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Core\Request;
use App\Core\Response;

/**
 * 图片上传。按 年-月 分目录存放于 public/uploads 下。
 *
 * 注意：正式环境请改用云存储（COS / OSS / 七牛），或把 uploads 目录挂到 CDN。
 */
final class UploadController
{
    private const ALLOWED = [
        'image/jpeg' => 'jpg',
        'image/png'  => 'png',
        'image/webp' => 'webp',
        'image/gif'  => 'gif',
    ];

    private const MAX_BYTES = 5242880; // 5MB

    /** POST /api/upload/image  (multipart/form-data, 字段名 file) */
    public function image(array $params = []): void
    {
        $file = $_FILES['file'] ?? null;
        if ($file === null || ($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK) {
            Response::error('请选择要上传的图片');
        }
        if ((int) $file['size'] > self::MAX_BYTES) {
            Response::error('图片不能超过 5MB');
        }

        $mime = mime_content_type($file['tmp_name']) ?: '';
        if (!isset(self::ALLOWED[$mime])) {
            Response::error('只支持 jpg / png / webp / gif 格式');
        }

        $subDir = 'uploads/' . date('Ym');
        $dir    = PUBLIC_PATH . '/' . $subDir;
        if (!is_dir($dir) && !mkdir($dir, 0755, true) && !is_dir($dir)) {
            Response::error('上传目录创建失败', 500);
        }

        $filename = bin2hex(random_bytes(16)) . '.' . self::ALLOWED[$mime];
        $target   = $dir . '/' . $filename;

        if (!move_uploaded_file($file['tmp_name'], $target)) {
            Response::error('图片保存失败', 500);
        }

        // 存相对路径，输出时再由 Helper::toAbsoluteUrl 拼完整域名
        Response::ok(['url' => $subDir . '/' . $filename]);
    }

    /** GET /api/upload/ping —— 前端自检上传接口是否连通 */
    public function ping(array $params = []): void
    {
        Response::ok([
            'max_size' => self::MAX_BYTES,
            'accept'   => array_keys(self::ALLOWED),
            'uid'      => (int) Request::$user['id'],
        ]);
    }
}
