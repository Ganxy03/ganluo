# deploy-1.0.17.ps1
# Push v1.0.17 to 38.22.95.152 (release apk + 修改服务器版本检查控制器)
#
# 参数 (ENV 或者脚本头部改):
#   $SERVER   - SSH 目标 (默认 38.22.95.152)
#   $USER     - SSH 用户 (默认 root)
#   $PASSWORD - SSH 密码 (默认 @Inc123456.)
#   $VERSION  - 默认 1.0.17
#   $CODE     - 默认 18

$SERVER   = if ($env:SERVER)   { $env:SERVER }   else { "38.22.95.152" }
$USER     = if ($env:USER)     { $env:USER }     else { "root" }
$PW       = if ($env:PASSWORD) { $env:PASSWORD } else { "@Inc123456." }
$VERSION  = "1.0.17"
$CODE     = "18"
$DL_DIR   = "/www/wwwroot/y.bobay.top/server/public/downloads"
$APK_NAME = "ganluo-$VERSION.apk"
$LOCAL_APK = "D:\Work\ganluo-android\app\build\outputs\apk\release\app-release.apk"

# 待生成的 APK sha256 (sha256 一并写入控制器)
$LOCAL_SHA = (Get-FileHash -Algorithm SHA256 $LOCAL_APK).Hash.ToLower()
Write-Host "[INFO] sha256(local apk) = $LOCAL_SHA"
Write-Host "[INFO] size  = $((Get-Item $LOCAL_APK).Length) bytes"

$pscpArgs = @(
    "-P", "22",
    "-pw", $PW,
    "-C",
    $LOCAL_APK,
    "${USER}@${SERVER}:/tmp/$APK_NAME"
)
Write-Host "[STEP] scp 本地 release apk → /tmp/$APK_NAME"
& "C:\Windows\System32\OpenSSH\scp.exe" @pscpArgs
if ($LASTEXITCODE -ne 0) { Write-Error "scp failed: $LASTEXITCODE"; exit 1 }

# 构建在远端执行的 Bash 脚本 (一次性传输 /tmp + one-shot)
$remote = @"
set -e
mkdir -p '$DL_DIR'
mv -f /tmp/$APK_NAME '$DL_DIR/$APK_NAME'
chmod 644 '$DL_DIR/$APK_NAME'
echo '[INFO] uploaded:' && ls -lh '$DL_DIR/$APK_NAME'

# 自动定位控制器 (生产路径可能不是 src/...)
CTRL=`find /www/wwwroot/y.bobay.top -type f -name AppVersionController.php 2>/dev/null | head -n 1`
if [ -z "$CTRL" ]; then
  echo '[WARN] 没找到 AppVersionController.php,需要手动改 PHP'
  exit 0
fi
echo "[INFO] controller = $CTRL"
cp -n "$CTRL" "$CTRL.bak.$(date +%Y%m%d%H%M%S)"

# 替换 latest_code / latest_name / download_url / sha256 (PHP 单引号字符串内替换)
sed -i "s|'latest_code'   => [0-9]\+|'latest_code'   => ${CODE}|" "$CTRL"
sed -i "s|'latest_name'   => '[^']*'|'latest_name'   => '$VERSION'|" "$CTRL"
sed -i "s|'download_url'  => '[^']*'|'download_url'  => 'https://y.bobay.top/downloads/$APK_NAME'|" "$CTRL"
sed -i "s|'sha256'        => '[^']*'|'sha256'        => '$LOCAL_SHA'|" "$CTRL"

echo '---- diff (改动行) ----'
diff "$CTRL.bak."* "$CTRL" 2>/dev/null | head -n 20 || true

# 如使用 opcache,强制刷新
if [ -d /run/php ]; then
  pkill -USR2 -f php-fpm 2>/dev/null || true
  echo "[INFO] php-fpm reload signal sent"
fi
echo "[DONE] controller updated, $DL_DIR/$APK_NAME ready"
"@

$remoteFile = "$env:TEMP\deploy-1.0.17.sh"
[System.IO.File]::WriteAllText($remoteFile, $remote, $utf8)

# scp script to /tmp
& "C:\Windows\System32\OpenSSH\scp.exe" "-P","22","-pw",$PW,$remoteFile,"${USER}@${SERVER}:/tmp/deploy-1.0.17.sh"

# exec
Write-Host "[STEP] ssh ${USER}@${SERVER} bash /tmp/deploy-1.0.17.sh"
& "C:\Windows\System32\OpenSSH\ssh.exe" "-p","22","-o","StrictHostKeyChecking=accept-new","${USER}@${SERVER}","bash /tmp/deploy-1.0.17.sh"
Write-Host "[EXIT] $LASTEXITCODE"
