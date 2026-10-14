<?php

declare(strict_types=1);

namespace App\Core;

use App\Middleware\AuthMiddleware;

/**
 * 极简路由：支持 {id} 形式的路径参数。
 * 注册时第 3 个参数为 false 表示该接口无需登录。
 */
final class Router
{
    /** @var array<int, array{0:string,1:string,2:mixed,3:bool}> */
    private array $routes = [];

    public function add(string $method, string $pattern, callable|array $handler, bool $auth = true): void
    {
        $this->routes[] = [strtoupper($method), $pattern, $handler, $auth];
    }

    public function get(string $pattern, callable|array $handler, bool $auth = true): void
    {
        $this->add('GET', $pattern, $handler, $auth);
    }

    public function post(string $pattern, callable|array $handler, bool $auth = true): void
    {
        $this->add('POST', $pattern, $handler, $auth);
    }

    public function put(string $pattern, callable|array $handler, bool $auth = true): void
    {
        $this->add('PUT', $pattern, $handler, $auth);
    }

    public function delete(string $pattern, callable|array $handler, bool $auth = true): void
    {
        $this->add('DELETE', $pattern, $handler, $auth);
    }

    public function dispatch(): void
    {
        $method = Request::method();
        $path   = Request::path();
        $allowed = [];
        $matchedPath = false;

        foreach ($this->routes as [$routeMethod, $pattern, $handler, $auth]) {
            $regex = '#^' . preg_replace('#\{(\w+)\}#', '(?P<$1>[^/]+)', $pattern) . '$#';
            if (!preg_match($regex, $path, $matches)) {
                continue;
            }

            $matchedPath = true;
            if ($routeMethod !== $method) {
                $allowed[] = $routeMethod;
                continue;
            }

            /** @var array<string, string> $params */
            $params = array_filter($matches, 'is_string', ARRAY_FILTER_USE_KEY);

            if ($auth) {
                Request::$user = AuthMiddleware::handle();
            }

            if (is_array($handler)) {
                [$class, $action] = $handler;
                (new $class())->{$action}($params);
            } else {
                $handler($params);
            }
            return;
        }

        if ($matchedPath && $allowed !== []) {
            header('Allow: ' . implode(', ', array_unique($allowed)));
            Response::error('请求方法不被允许', 405);
        }

        Response::error('接口不存在：' . $path, 404);
    }
}
