<script>
	import store from './utils/store.js'

	export default {
		onLaunch() {
			// 冷启动时把本地缓存的登录态读进内存
			store.restore()
		},

		onShow() {
			// 每次回到前台校验一次登录态
			if (uni.getStorageSync('token') && !store.getUser()) {
				store.restore()
			}
		},

		onHide() {}
	}
</script>

<style>
	/* ===== 全局主题 ===== */
	page {
		--primary: #ff8fa3;
		--primary-soft: #ffe3e9;
		--primary-deep: #f2697f;
		--bg: #fdf6f7;
		--card: #ffffff;
		--text: #3d3a3c;
		--text-sub: #9b9b9b;
		--text-light: #c4c0c2;
		--line: #f2e6e9;
		--radius: 24rpx;

		background: var(--bg);
		color: var(--text);
		font-size: 28rpx;
		font-family: -apple-system, "PingFang SC", "Helvetica Neue", sans-serif;
		line-height: 1.6;
	}

	view,
	text,
	image,
	scroll-view {
		box-sizing: border-box;
	}

	/* ===== 通用容器 ===== */
	.page {
		min-height: 100vh;
		padding: 24rpx 28rpx 60rpx;
	}

	.card {
		background: var(--card);
		border-radius: var(--radius);
		padding: 28rpx;
		margin-bottom: 24rpx;
		box-shadow: 0 4rpx 20rpx rgba(255, 143, 163, 0.08);
	}

	.section-title {
		display: flex;
		align-items: center;
		justify-content: space-between;
		font-size: 28rpx;
		font-weight: 600;
		margin-bottom: 20rpx;
	}

	.section-title .label {
		display: flex;
		align-items: center;
	}

	.section-title .label::before {
		content: '';
		width: 8rpx;
		height: 28rpx;
		border-radius: 4rpx;
		background: var(--primary);
		margin-right: 10rpx;
	}

	.section-title .action {
		font-size: 24rpx;
		font-weight: 400;
		color: var(--primary-deep);
	}

	/* ===== 头像 ===== */
	.avatar {
		width: 88rpx;
		height: 88rpx;
		border-radius: 50%;
		background: var(--primary-soft);
		flex-shrink: 0;
	}

	.avatar-fallback {
		display: flex;
		align-items: center;
		justify-content: center;
		color: var(--primary-deep);
		font-size: 34rpx;
		font-weight: 600;
	}

	/* ===== 按钮 ===== */
	.btn {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 88rpx;
		border-radius: 44rpx;
		font-size: 30rpx;
		border: none;
		line-height: 1;
	}

	.btn::after {
		border: none;
	}

	.btn-primary {
		background: linear-gradient(135deg, #ffa3b5, #ff8fa3);
		color: #fff;
	}

	.btn-ghost {
		background: var(--primary-soft);
		color: var(--primary-deep);
	}

	.btn-plain {
		background: #f6f2f3;
		color: var(--text-sub);
	}

	/* ===== 文本 ===== */
	.muted {
		color: var(--text-sub);
		font-size: 24rpx;
	}

	.empty {
		padding: 36rpx 0;
		text-align: center;
		color: var(--text-light);
		font-size: 26rpx;
	}

	/* ===== 输入框 ===== */
	.input {
		height: 88rpx;
		padding: 0 28rpx;
		background: #f8f4f5;
		border-radius: 44rpx;
		font-size: 28rpx;
	}

	.input-placeholder {
		color: var(--text-light);
	}

	/* ===== 底部弹层 ===== */
	.sheet-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		background: rgba(0, 0, 0, 0.35);
		z-index: 90;
	}

	.sheet {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		z-index: 100;
		background: #fff;
		border-radius: 32rpx 32rpx 0 0;
		padding: 40rpx 40rpx calc(40rpx + env(safe-area-inset-bottom));
	}

	.sheet-title {
		font-size: 32rpx;
		font-weight: 600;
		text-align: center;
		margin-bottom: 28rpx;
	}

	.sheet-textarea {
		width: 100%;
		height: 200rpx;
		padding: 24rpx;
		background: #f8f4f5;
		border-radius: 20rpx;
		font-size: 28rpx;
	}

	.sheet-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 24rpx 8rpx;
		border-bottom: 1rpx solid var(--line);
	}

	.sheet-row-label {
		font-size: 28rpx;
	}

	.sheet-actions {
		display: flex;
		margin-top: 32rpx;
	}

	.sheet-actions .btn {
		flex: 1;
		margin: 0 12rpx;
	}

	.picker {
		padding: 12rpx 28rpx;
		background: #fff2f5;
		border-radius: 32rpx;
		color: var(--primary-deep);
		font-size: 26rpx;
		margin-left: 16rpx;
	}
</style>
