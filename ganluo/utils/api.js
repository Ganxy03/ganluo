import { request, uploadFile } from './request.js'

export default {
	// ===== 登录 =====
	login: (data) => request({ url: '/api/auth/login', method: 'POST', data, auth: false }),
	authMe: () => request({ url: '/api/auth/me' }),

	// ===== 用户 =====
	updateProfile: (data) => request({ url: '/api/user/me', method: 'PUT', data }),
	searchUser: (inviteCode) => request({ url: '/api/user/search', data: { invite_code: inviteCode } }),

	// ===== 绑定 =====
	applyBind: (inviteCode) =>
		request({ url: '/api/bind/apply', method: 'POST', data: { invite_code: inviteCode } }),
	bindRequests: () => request({ url: '/api/bind/requests' }),
	handleBind: (requestId, action) =>
		request({ url: '/api/bind/handle', method: 'POST', data: { request_id: requestId, action } }),
	cancelBind: (requestId) =>
		request({ url: '/api/bind/cancel', method: 'POST', data: { request_id: requestId } }),
	unbind: () => request({ url: '/api/bind/unbind', method: 'POST' }),

	// ===== 主页聚合 =====
	home: (date) => request({ url: '/api/home', data: date ? { date } : {} }),

	// ===== 心情 =====
	moods: (params) => request({ url: '/api/moods', data: params }),
	addMood: (data) => request({ url: '/api/moods', method: 'POST', data }),
	deleteMood: (id) => request({ url: `/api/moods/${id}`, method: 'DELETE' }),

	// ===== 三餐 =====
	meals: (params) => request({ url: '/api/meals', data: params }),
	addMeal: (data) => request({ url: '/api/meals', method: 'POST', data }),
	deleteMeal: (id) => request({ url: `/api/meals/${id}`, method: 'DELETE' }),

	// ===== 留言（双向留言板，按天查询）=====
	// 返回 { date, partner, list, unread_in_day, unread_total, latest_unread_date }
	messages: (date) => request({ url: '/api/messages', data: date ? { date } : {} }),
	// data: { content, reply_to_id? }
	sendMessage: (data) => request({ url: '/api/messages', method: 'POST', data }),
	readMessage: (id) => request({ url: `/api/messages/${id}/read`, method: 'POST' }),
	// 传 date 只标记那天的，不传标记全部
	readAllMessages: (date) =>
		request({ url: '/api/messages/read-all', method: 'POST', data: date ? { date } : {} }),
	deleteMessage: (id) => request({ url: `/api/messages/${id}`, method: 'DELETE' }),

	// ===== 上传 =====
	uploadImage: (filePath) => uploadFile(filePath)
}
