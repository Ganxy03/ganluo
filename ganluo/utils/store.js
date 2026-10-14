/**
 * 内存 + 本地缓存双层的登录态。
 *
 * 不依赖 getApp()，任何页面 / 工具模块都能直接用，避免 uni-app 各端
 * getApp() 行为不一致带来的坑。
 */
let currentUser = null

function getUser() {
	return currentUser
}

function setUser(user) {
	currentUser = user || null
	if (user) {
		uni.setStorageSync('user', user)
	} else {
		uni.removeStorageSync('user')
	}
}

function clearUser() {
	currentUser = null
	uni.removeStorageSync('user')
	uni.removeStorageSync('token')
}

function isLoggedIn() {
	return !!uni.getStorageSync('token')
}

/** 冷启动时把本地缓存的用户读回内存 */
function restore() {
	currentUser = uni.getStorageSync('user') || null
	return currentUser
}

/** 局部更新当前用户字段（例如绑定成功后刷新 partner） */
function patchUser(patch) {
	const merged = Object.assign({}, currentUser || {}, patch)
	setUser(merged)
	return merged
}

export default { getUser, setUser, clearUser, isLoggedIn, restore, patchUser }
