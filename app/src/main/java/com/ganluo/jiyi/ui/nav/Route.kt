package com.ganluo.jiyi.ui.nav

/** 路由常量,集中管理,避免拼写错误 */
object Route {
    // 顶层 tab
    const val LOGIN = "login"
    const val HOME = "home"
    const val PROFILE = "profile"

    // Profile 内部子页(嵌套 NavHost)
    const val PROFILE_ROOT = "profile_root"
    const val PROFILE_LOCATION = "profile/location"
    const val PROFILE_MAP = "profile/map"
    const val PROFILE_BLUETOOTH = "profile/bluetooth"
    const val PROFILE_ABOUT = "profile/about"

    @Deprecated("地图已下沉到 我的 / 位置查看")
    const val MAP = "map"
    const val BLUETOOTH = "bluetooth"

    object Profile {
        const val ROOT = PROFILE_ROOT
        const val LOCATION = PROFILE_LOCATION
        const val MAP = PROFILE_MAP
        const val BLUETOOTH = PROFILE_BLUETOOTH
        const val ABOUT = PROFILE_ABOUT
    }
}
