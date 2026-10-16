# 1.0.9 消息发图 — 后端 PHP 控制器改动

> **目的**:让 `POST /api/messages` 接受可选 `image_url` 字段,响应里多返 `image_url` 字段。
> **不破坏性**:老小程序调用(只传 `content` + `reply_to_id`)继续工作,响应里多一字段 JSON 解析不报错。

---

## 1. 数据库先跑 SQL(必须,先于代码上线)

```bash
mysql -u root -p your_db < migration_add_message_image.sql
```

或者登录 phpmyadmin / navicat 跑那一条 `ALTER TABLE` 即可。

---

## 2. 现有 MessagesController 改动(3 处,共 ~10 行)

打开 `server/src/Controllers/MessagesController.php`,做下面三处微改:

### 改动 A:create() / store() 方法里读 `image_url`

找到形如这样的代码(具体函数名看你项目):

```php
// 原代码
public function create(array $params = []): void
{
    $user = Auth::user();
    $content = trim((string) Request::input('content', ''));
    $replyToId = Request::input('reply_to_id');

    if ($content === '' && !$replyToId) {
        Response::fail('内容不能为空');
        return;
    }
    // ... INSERT INTO messages (user_id, partner_id, content, reply_to_id, ...) ...
}
```

**改成** (在读取参数那段加 2 行,在 INSERT 里加 1 个字段):

```php
public function create(array $params = []): void
{
    $user = Auth::user();
    $content = trim((string) Request::input('content', ''));
    $replyToId = Request::input('reply_to_id');
    $imageUrl = Request::input('image_url');           // ← 新增(可选,null = 纯文本)
    $imageUrl = is_string($imageUrl) && $imageUrl !== '' ? $imageUrl : null;  // ← 新增(归一化)

    if ($content === '' && !$imageUrl && !$replyToId) {  // ← 改:三选一非空才允许
        Response::fail('内容不能为空');
        return;
    }
    // ... INSERT INTO messages (user_id, partner_id, content, image_url, reply_to_id, ...) ...
    //                                                   ^^^^^^^^^^^ 新增列
}
```

### 改动 B:SELECT 消息列表时把 `image_url` 一起查出来

找到查 messages 的 SQL(应该在 `index()` / `list()` 之类),把:

```sql
SELECT id, user_id, content, reply_to_id, is_read, created_at
FROM messages
WHERE ...
```

**改成**:

```sql
SELECT id, user_id, content, image_url, reply_to_id, is_read, created_at
FROM messages
WHERE ...
```

### 改动 C:把 `image_url` 塞进响应 JSON

找到组装响应的地方(应该是把每行 map 成数组的位置),加一行:

```php
// 原代码
$row = [
    'id'        => (int) $r['id'],
    'content'   => $r['content'],
    'is_mine'   => ((int) $r['user_id']) === $user['id'],
    'is_read'   => (bool) $r['is_read'],
    'time'      => date('H:i', strtotime($r['created_at'])),
    'reply_to'  => $replyToMap[(int) $r['reply_to_id']] ?? null,
];

// 改成
$row = [
    'id'        => (int) $r['id'],
    'content'   => $r['content'],
    'image_url' => $r['image_url'] ?: null,    // ← 新增(没图 = null)
    'is_mine'   => ((int) $r['user_id']) === $user['id'],
    'is_read'   => (bool) $r['is_read'],
    'time'      => date('H:i', strtotime($r['created_at'])),
    'reply_to'  => $replyToMap[(int) $r['reply_to_id']] ?? null,
];
```

### 改动 D(可选,推荐):`/api/messages/{id}/read` 和 `delete` 不需要改

读/删接口跟 image_url 无关,保持不动。

---

## 3. 不需要动的其他端点

| 端点 | 是否改 | 原因 |
|---|---|---|
| `POST /api/auth/login-app` | 不改 | 跟消息无关 |
| `GET /api/auth/me` | 不改 | 同上 |
| `GET /api/messages?date=` | 改(见上 B+C) | 要多查/多返 image_url |
| `POST /api/messages` | 改(见上 A) | 要接收 image_url |
| `POST /api/upload/image` | **不改**(已存在) | 1.0.7 头像上传已用,Android 1.0.9 直接复用 |
| 其它 17 个端点 | 不改 | 跟消息图片无关 |

---

## 4. 验证清单(部署后必跑)

```bash
# 4.1 数据库列存在
mysql -e "DESCRIBE messages" | grep image_url
# 期望:image_url varchar(255) NULL

# 4.2 纯文本老请求继续工作(小程序不变)
curl -X POST https://y.bobay.top/api/messages \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"content": "测试纯文本"}'
# 期望:返回 200,响应里有 image_url: null

# 4.3 Android 新请求(带图)工作
curl -X POST https://y.bobay.top/api/messages \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"content": "看这张图", "image_url": "uploads/test.jpg"}'
# 期望:200,响应里 image_url = "uploads/test.jpg"

# 4.4 列表查询带图
curl "https://y.bobay.top/api/messages?date=2026-10-16" \
  -H "Authorization: Bearer $JWT"
# 期望:list[].image_url 有值或 null,小程序解析这个字段不报错
```

---

## 5. 回滚(万一出问题)

```sql
-- 撤掉字段
ALTER TABLE messages DROP COLUMN image_url;
-- PHP 代码 git revert 改动 A/B/C 的 commit 即可
```
