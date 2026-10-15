<?php

declare(strict_types=1);

/**
 * 挂到 server/routes/api.php 末尾。沿用同文件里的 Router 扁平风格。
 * 不引 group() / 中间件 / 全局包装,跟现有 22 条路由写法保持一致。
 */

use App\Controllers\AppVersionController;
use App\Controllers\AuthAppController;

/** @var \App\Core\Router $router */

// Android 端邀请码登录(替代 wx.login)。无需登录。
$router->post('/api/auth/login-app', [AuthAppController::class, 'login'], false);

// 版本元数据,启动时检查更新用。无需登录。
$router->get('/api/app/version', [AppVersionController::class, 'latest'], false);
