/**
 * 请求封装：统一 baseURL、token 注入、错误提示、401 自动跳登录。
 *
 * ⚠️ 开发时把 BASE_URL 指向本机 PHP 服务；
 *    正式发布必须是 HTTPS 域名，并在「小程序后台 → 开发设置 → 服务器域名」中配置。
 */
// 后端地址。线上地址在模拟器 / 真机 / 体验版里都能用，优先用它。
//   线上（推荐）        https://y.bobay.top
//   本机后端            http://127.0.0.1:8000          ← 只有模拟器能用
//   局域网真机调试      http://192.168.13.30:8000      ← 手机要和电脑同一 WiFi
const BASE_URL = 'https://y.bobay.top'

// ---- 防止"连续失败 → 疯狂弹 toast"造成的页面闪烁 ----
let lastToast = { message: '', at: 0 }
function toast(message) {
	const now = Date.now()
	// 1.5 秒内重复的同一条提示直接吞掉
	if (message === lastToast.message && now - lastToast.at < 1500) return
	lastToast = { message, at: now }
	uni.showToast({ title: message, icon: 'none', duration: 2000 })
}

// ---- 防止重复 reLaunch 造成的页面闪烁 ----
let redirecting = false
function redirectToLogin() {
	uni.removeStorageSync('token')
	uni.removeStorageSync('user')

	if (redirecting) return

	// 各端 getCurrentPages() 返回的 route 有的带前导斜杠、有的不带，统一归一化再比
	const pages = getCurrentPages()
	const current = pages.length
		? String(pages[pages.length - 1].route || '').replace(/^\//, '')
		: ''

	// 已经在登录页就什么都不做 —— 否则会无限 reLaunch，页面一直闪
	if (current === 'pages/login/login') return

	redirecting = true
	uni.reLaunch({
		url: '/pages/login/login',
		complete() {
			// 留一点缓冲，避免短时间内连续触发
			setTimeout(() => {
				redirecting = false
			}, 800)
		}
	})
}

function request(options) {
	const { url, method = 'GET', data = {}, auth = true, loading = false } = options

	return new Promise((resolve, reject) => {
		const header = { 'Content-Type': 'application/json' }
		const token = uni.getStorageSync('token')
		if (auth && token) {
			header.Authorization = `Bearer ${token}`
		}

		if (loading) {
			uni.showLoading({ title: '加载中', mask: true })
		}

		uni.request({
			url: BASE_URL + url,
			method,
			data,
			header,
			success(res) {
				const body = res.data || {}

				if (res.statusCode === 401) {
					// 只有"需要登录"的接口才跳登录页。
					// 登录接口自己返回 401 时如果也跳，会清掉刚拿到的登录态并反复跳转。
					if (auth) {
						redirectToLogin()
					}
					reject(new Error(body.message || '登录已失效'))
					return
				}

				if (res.statusCode >= 200 && res.statusCode < 300 && body.code === 0) {
					return resolve(body.data)
				}

				const message = body.message || `请求失败（${res.statusCode}）`
				toast(message)
				reject(new Error(message))
			},
			fail(err) {
				// 微信把"域名没配"和"服务器连不上"都归到 fail 里，
				// 靠 errMsg 区分，否则提示会把人带偏
				const detail = (err && err.errMsg) || ''

				let hint = '网络异常，请确认后端服务已启动'
				if (detail.includes('domain list') || detail.includes('not in domain')) {
					hint = '域名未配置：正式版/体验版必须用 HTTPS 域名，IP 和 http 都会被拦'
				} else if (detail.includes('ERR_CONNECTION') || detail.includes('CONNECTION_REFUSED')) {
					hint = '连不上后端：确认服务已启动、IP 正确、手机和电脑在同一网络'
				} else if (detail.toLowerCase().includes('timeout')) {
					hint = '请求超时：检查网络，或后端是否卡住'
				}

				toast(hint)
				console.error('[request] 请求失败 ' + BASE_URL + url, detail, err)
				reject(err)
			},
			complete() {
				if (loading) {
					uni.hideLoading()
				}
			}
		})
	})
}

/**
 * 上传失败时把原始信息完整弹出来。
 * 用 showModal 而不是 toast —— toast 两秒就没了，而且长度受限，
 * 在真机上根本来不及看清 errMsg。
 */
function showUploadError(reason, detail) {
	console.error('[upload] ' + reason, detail)
	uni.showModal({
		title: '上传失败',
		content: reason + (detail ? '\n\n' + detail : ''),
		showCancel: false,
		confirmText: '知道了'
	})
}

/** 上传单张图片，返回 { url } */
function uploadFile(filePath) {
	return new Promise((resolve, reject) => {
		const token = uni.getStorageSync('token')

		uni.uploadFile({
			url: `${BASE_URL}/api/upload/image`,
			filePath,
			name: 'file',
			header: token ? { Authorization: `Bearer ${token}` } : {},
			success(res) {
				let body
				try {
					body = JSON.parse(res.data)
				} catch (e) {
					showUploadError('返回内容无法解析', res.data)
					return reject(e)
				}

				// HTTP 层就失败（比如 404 / 500），body 不是我们的格式
				if (res.statusCode !== 200) {
					showUploadError('HTTP ' + res.statusCode, res.data)
					return reject(new Error('HTTP ' + res.statusCode))
				}

				if (body.code === 0) {
					resolve(body.data)
				} else {
					showUploadError(body.message || '上传失败', '')
					reject(new Error(body.message || '上传失败'))
				}
			},
			// 关键：把微信的原始 errMsg 弹出来，不要吞掉。
			// 体验版里没法看 console，弹窗是唯一能读到原因的地方。
			fail(err) {
				showUploadError('请求没有被发出去', (err && err.errMsg) || String(err))
				reject(err)
			}
		})
	})
}

export { request, uploadFile, BASE_URL }
