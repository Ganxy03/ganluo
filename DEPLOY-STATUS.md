# 现状:

## 网络
- 38.22.95.152 TCP 22 通了 (TCP connect OK)
- SSH 协议在 kex_exchange_identification 阶段被 server 主动关闭 (kex_exchange_identification: Connection closed by remote host)
- 这很常见是:fail2ban 已 ban 我这边的 egress IP / sshd MaxStartups 太紧 / sshd 在 banner 之前断
- TCP 80/443/21/2222/8080 全部 OPEN (ftp/备用 ssh 可用,以及 web 端口)

## 我需要你回答这 3 个问题才能继续
1. ssh 是不是禁止 root 密码登录? (`/etc/ssh/sshd_config` 里 `PasswordAuthentication` + `PermitRootLogin` 是怎样的?)
2. fail2ban / iptables 里我这边的 IP 是否在白名单? (你之前有没有别的机器用 ssh 上去过?)
3. 如果 root+ssh 不行,我可以走下面两条路之一:
   (a) 给我一个普通用户(能 sudo 写 PHP 那个目录的)
   (b) 我用 HTTP 上传 — 在服务器加一个 PHP upload 端点,你帮我把端点文件扔上去(只一次,以后上传都走 HTTP)
   (c) 给我一个密钥(.pem) — 你 cat 出来贴给我,我直接 ssh -i

## 我不能在线尝试密码登录多少次
- ssh 的 retry 上限会被 server 设小,Try 多了 fail2ban 会再 ban 一遍。
- 所以我先用密钥 / 用户名 / 端口,你给我哪个都行。
