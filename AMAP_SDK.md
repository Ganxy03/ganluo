# 高德 SDK 接入说明

## 文件清单

```
app/libs/
  amap-3dmap-11.3.100.jar   # 12 MB — Java 代码(3DMap + Search + Location 三合一)

app/src/main/jniLibs/
  arm64-v8a/
    libAMapSDK_MAP_v11_3_100.so   # 33 MB — 3D 地图 native
    libapssdk.so                   # 0.8 MB — 定位 native
```

合计 ~46 MB,几乎全是 .so。

## 为什么用本地文件,不用 maven

AMap 官方 maven 仓库(`https://maven.amap.com/repository/public`)TLS 握手在
部分网络下会被中断(curl 报 "schannel: failed to receive handshake")。
Aliyun 镜像和 Maven Central 都没有这个包。

所以直接放本地,用 `implementation(files("libs/amap-3dmap-11.3.100.jar"))` 引用。

## 升级到新版 SDK

1. 打开 https://lbs.amap.com/api/android-sdk/download
2. 下载 "3DMap+Search+Location" 三合一 zip
3. 解压取 `AMap3DMap_X.Y.Z_AMapSearch_A.B.C_AMapLocation_D.E.F_<日期>.jar`
4. 复制到 `app/libs/amap-3dmap-X.Y.Z.jar`
5. 复制两个 arm64-v8a 的 .so 到 `app/src/main/jniLibs/arm64-v8a/`
6. 改 `app/build.gradle.kts` 的 `implementation(files(...))` 路径
7. 改 key 时同步更新 `app/src/main/AndroidManifest.xml` 和 `~/.gradle/gradle.properties`

## 只支持 arm64-v8a(2026-10 起)

armeabi-v7a 已砍掉,32-bit 手机市场 < 1%,省 23MB 体积。
如果需要 32-bit 设备,恢复 abiFilters + 复制 .so 到 `jniLibs/armeabi-v7a/`。
