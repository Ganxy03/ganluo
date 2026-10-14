<?php

declare(strict_types=1);

use App\Controllers\AuthController;
use App\Controllers\BindController;
use App\Controllers\HomeController;
use App\Controllers\MealController;
use App\Controllers\MessageController;
use App\Controllers\MoodController;
use App\Controllers\UploadController;
use App\Controllers\UserController;
use App\Core\Router;

/** @var Router $router */

// ===== 认证（无需登录）=====
$router->post('/api/auth/login', [AuthController::class, 'login'], false);

// ===== 当前用户 =====
$router->get('/api/auth/me', [AuthController::class, 'me']);
$router->get('/api/user/me', [UserController::class, 'me']);
$router->put('/api/user/me', [UserController::class, 'update']);
$router->get('/api/user/search', [UserController::class, 'search']);

// ===== 绑定 =====
$router->post('/api/bind/apply', [BindController::class, 'apply']);
$router->get('/api/bind/requests', [BindController::class, 'requests']);
$router->post('/api/bind/handle', [BindController::class, 'handle']);
$router->post('/api/bind/cancel', [BindController::class, 'cancel']);
$router->post('/api/bind/unbind', [BindController::class, 'unbind']);

// ===== 主页聚合 =====
$router->get('/api/home', [HomeController::class, 'index']);

// ===== 心情日记 =====
$router->get('/api/moods', [MoodController::class, 'index']);
$router->post('/api/moods', [MoodController::class, 'store']);
$router->put('/api/moods/{id}', [MoodController::class, 'update']);
$router->delete('/api/moods/{id}', [MoodController::class, 'destroy']);

// ===== 三餐报备 =====
$router->get('/api/meals', [MealController::class, 'index']);
$router->post('/api/meals', [MealController::class, 'store']);
$router->delete('/api/meals/{id}', [MealController::class, 'destroy']);

// ===== 留言提醒 =====
$router->get('/api/messages', [MessageController::class, 'index']);
$router->post('/api/messages', [MessageController::class, 'store']);
$router->post('/api/messages/read-all', [MessageController::class, 'readAll']);
$router->post('/api/messages/{id}/read', [MessageController::class, 'read']);
$router->delete('/api/messages/{id}', [MessageController::class, 'destroy']);

// ===== 上传 =====
$router->post('/api/upload/image', [UploadController::class, 'image']);
$router->get('/api/upload/ping', [UploadController::class, 'ping']);
