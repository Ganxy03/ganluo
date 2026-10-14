const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']

function pad(n) {
	return n < 10 ? `0${n}` : `${n}`
}

/** Date → '2026-10-13' */
function toDateStr(date) {
	const d = date instanceof Date ? date : new Date(date)
	return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** Date → '2026-10-13 20:30' */
function toDateTimeStr(date) {
	const d = date instanceof Date ? date : new Date(date)
	return `${toDateStr(d)} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 按天偏移，'2026-10-13' + 1 → '2026-10-14' */
function shiftDate(dateStr, deltaDays) {
	const [y, m, d] = dateStr.split('-').map(Number)
	const date = new Date(y, m - 1, d)
	date.setDate(date.getDate() + deltaDays)
	return toDateStr(date)
}

/** '2026-10-13' → '今天' / '昨天' / '10月13日 周一' */
function friendlyDate(dateStr) {
	const today = toDateStr(new Date())
	const yesterday = shiftDate(today, -1)
	const tomorrow = shiftDate(today, 1)

	if (dateStr === today) return '今天'
	if (dateStr === yesterday) return '昨天'
	if (dateStr === tomorrow) return '明天'

	const [y, m, d] = dateStr.split('-').map(Number)
	const weekday = WEEKDAYS[new Date(y, m - 1, d).getDay()]
	return `${m}月${d}日 周${weekday}`
}

/** '2026-10-13 20:30:00' → '20:30' */
function timeOf(datetime) {
	if (!datetime) return ''
	const match = String(datetime).match(/(\d{2}):(\d{2})/)
	return match ? `${match[1]}:${match[2]}` : ''
}

function isToday(dateStr) {
	return dateStr === toDateStr(new Date())
}

/** 取昵称首字，用作无头像时的占位 */
function initialOf(name) {
	return (name || '·').trim().slice(0, 1)
}

// 具名导出（按需引入用）
export { toDateStr, toDateTimeStr, shiftDate, friendlyDate, timeOf, isToday, initialOf, pad }

// 默认导出（页面里统一以 fmt 对象访问）
export default {
	toDateStr,
	toDateTimeStr,
	shiftDate,
	friendlyDate,
	timeOf,
	isToday,
	initialOf,
	pad
}
