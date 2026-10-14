import App from './App'
import { createSSRApp } from 'vue'

// 本项目固定使用 Vue3，故只保留 Vue3 分支。
// （HBuilderX 模板默认还有一段 #ifndef VUE3 的 Vue2 分支，
//   它会 import 并不存在的 './uni.promisify.adaptor'，容易报「找不到模块」，已移除。）
export function createApp() {
	const app = createSSRApp(App)
	return {
		app
	}
}
