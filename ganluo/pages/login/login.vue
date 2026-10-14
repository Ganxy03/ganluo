<template>
	<view class="login-page">
		<view class="hero">
			<view class="hero-logo">吉壹</view>
			<view class="hero-slogan">只属于你们两个人的日常</view>
		</view>

		<!-- 第一步：微信授权登录 -->
		<view v-if="step === 'login'" class="login-body">
			<button
				class="btn btn-primary login-btn"
				:loading="loading"
				:disabled="loading"
				@tap="onLogin"
			>
				微信一键登录
			</button>
			<view class="login-tip">登录仅用于识别你的身份，不会获取你的手机号</view>
		</view>

		<!-- 第二步：输入对方用户ID -->
		<view v-else class="login-body">
			<view class="bind-card">
				<view class="bind-title">绑定你的另一半</view>
				<view class="bind-tip">让对方在「我的」页面把 TA 的用户ID 发给你，填写后即可发起绑定请求</view>

				<input
					class="input bind-input"
					:value="inviteCode"
					maxlength="16"
					placeholder="请输入对方的用户ID"
					placeholder-class="input-placeholder"
					@input="onCodeInput"
				/>

				<button
					class="btn btn-primary bind-submit"
					:loading="submitting"
					:disabled="submitting"
					@tap="onApply"
				>
					发送绑定请求
				</button>

				<view class="bind-skip" @tap="onSkip">先跳过，我自己逛逛</view>
			</view>
		</view>
	</view>
</template>

<script>
	import api from '../../utils/api.js'
	import store from '../../utils/store.js'

	export default {
		data() {
			return {
				step: 'login', // login = 授权登录 | bind = 输入对方用户ID
				loading: false,
				submitting: false,
				inviteCode: ''
			}
		},

		onLoad() {
			if (store.isLoggedIn()) {
				this.afterLogin(store.getUser())
			}
		},

		methods: {
			/** 微信一键登录 */
			onLogin() {
				if (this.loading) return
				this.loading = true

				uni.login({
					provider: 'weixin',
					success: (res) => {
						if (!res.code) {
							this.loading = false
							uni.showToast({ title: '获取微信凭证失败', icon: 'none' })
							return
						}

						api
							.login({ code: res.code })
							.then((data) => {
								uni.setStorageSync('token', data.token)
								store.setUser(data.user)
								this.afterLogin(data.user)
							})
							.catch(() => {
								// 错误提示已在 request 中统一处理
							})
							.finally(() => {
								this.loading = false
							})
					},
					fail: () => {
						this.loading = false
						uni.showToast({ title: '微信登录失败', icon: 'none' })
					}
				})
			},

			afterLogin(user) {
				if (user && user.bound) {
					uni.switchTab({ url: '/pages/index/index' })
					return
				}
				this.step = 'bind'
			},

			onCodeInput(e) {
				this.inviteCode = (e.detail.value || '').toUpperCase()
			},

			/** 发送绑定请求 */
			onApply() {
				const code = (this.inviteCode || '').trim()
				if (!code) {
					uni.showToast({ title: '请输入对方的用户ID', icon: 'none' })
					return
				}
				if (this.submitting) return
				this.submitting = true

				api
					.applyBind(code)
					.then((res) => {
						uni.showToast({
							title: res.status === 'bound' ? '绑定成功' : '请求已发送，等 TA 同意',
							icon: 'none'
						})
						return api.authMe()
					})
					.then((me) => {
						store.setUser(me)
						if (me.bound) {
							setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 800)
						}
					})
					.catch(() => {
						// 已提示
					})
					.finally(() => {
						this.submitting = false
					})
			},

			/** 先跳过，稍后从「我的」页面绑定 */
			onSkip() {
				uni.switchTab({ url: '/pages/index/index' })
			}
		}
	}
</script>

<style>
	.login-page {
		min-height: 100vh;
		background: linear-gradient(180deg, #ffe6eb 0%, #fdf6f7 46%);
		padding: 0 56rpx;
		display: flex;
		flex-direction: column;
	}

	.hero {
		padding-top: 260rpx;
		text-align: center;
	}

	.hero-logo {
		font-size: 72rpx;
		font-weight: 700;
		letter-spacing: 12rpx;
		color: #f2697f;
	}

	.hero-slogan {
		margin-top: 24rpx;
		font-size: 28rpx;
		color: #b58f97;
		letter-spacing: 2rpx;
	}

	.login-body {
		margin-top: 140rpx;
		padding-bottom: 80rpx;
	}

	.login-btn {
		width: 100%;
	}

	.login-tip {
		margin-top: 32rpx;
		font-size: 22rpx;
		line-height: 1.7;
		color: #b9a9ad;
		text-align: center;
	}

	.bind-card {
		background: #fff;
		border-radius: 28rpx;
		padding: 48rpx 40rpx;
		box-shadow: 0 8rpx 32rpx rgba(255, 143, 163, 0.12);
	}

	.bind-title {
		font-size: 34rpx;
		font-weight: 600;
		text-align: center;
	}

	.bind-tip {
		margin: 18rpx 0 36rpx;
		font-size: 24rpx;
		line-height: 1.7;
		color: #9b9b9b;
		text-align: center;
	}

	.bind-input {
		text-align: center;
		letter-spacing: 6rpx;
		font-weight: 600;
		color: #f2697f;
	}

	.bind-submit {
		width: 100%;
		margin-top: 28rpx;
	}

	.bind-skip {
		margin-top: 32rpx;
		text-align: center;
		font-size: 24rpx;
		color: #b9a9ad;
	}
</style>
