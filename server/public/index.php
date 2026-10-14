<?php

declare(strict_types=1);

use App\Core\Config;
use App\Core\Request;
use App\Core\Response;
use App\Core\Router;

define('BASE_PATH', dirname(__DIR__));
define('PUBLIC_PATH', __DIR__);

// ---- 极简 PSR-4 自动加载：App\Foo\Bar → src/Foo/Bar.php ----
spl_autoload_register(static function (string $class): void {
    $prefix = 'App\\';
    if (!str_starts_with($class, $prefix)) {
        return;
    }
    $relative = substr($class, strlen($prefix));
    $file     = BASE_PATH . '/src/' . str_replace('\\', '/', $relative) . '.php';
    if (is_file($file)) {
        require $file;
    }
});

Config::load(BASE_PATH . '/.env');

// ---- 跨域（本地调试 & 开发者工具用；正式环境请收紧到具体域名）----
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type, Authorization');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Max-Age: 86400');
if (Request::method() === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// ---- 全局异常兜底，保证任何情况下都返回 JSON ----
set_exception_handler(static function (Throwable $e): void {
    error_log('[ganluo] ' . $e->getMessage() . ' @ ' . $e->getFile() . ':' . $e->getLine());
    Response::error(Config::bool('APP_DEBUG') ? $e->getMessage() : '服务器内部错误', 500);
});

// ---- 静态资源（PHP 内置服务器直接跑时用得上）----
$path = Request::path();
if (str_starts_with($path, '/uploads/')) {
    $file = PUBLIC_PATH . $path;
    if (is_file($file)) {
        header('Content-Type: ' . (mime_content_type($file) ?: 'application/octet-stream'));
        readfile($file);
        exit;
    }
    Response::error('文件不存在', 404);
}

$router = new Router();
require BASE_PATH . '/routes/api.php';
$router->dispatch();
