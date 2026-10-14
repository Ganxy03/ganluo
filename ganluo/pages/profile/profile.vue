<template>
	<view class="page">
		<!-- 我的资料 -->
		<view class="card profile-card">
			<view class="avatar-wrap" @tap="onPickAvatar">
				<image v-if="user.avatar" class="avatar-lg" :src="user.avatar" mode="aspectFill" />
				<view v-else class="avatar-lg avatar-fallback">{{ avatarChar }}</view>
				<view class="avatar-badge">换</view>
			</view>

			<view class="profile-info">
				<input
					class="profile-nickname"
					:value="nickname"
					maxlength="32"
					placeholder="设置昵称"
					placeholder-class="input-placeholder"
					@input="onNicknameInput"
				/>
				<input
					class="profile-signature"
					:value="signature"
					maxlength="100"
					placeholder="写一句话介绍自己"
					placeholder-class="input-placeholder"
					@input="onSignatureInput"
				/>
			</view>

			<button class="btn btn-ghost save-btn" :loading="saving" :disabled="saving" @tap="onSaveProfile">
				保存
			</button>
		</view>

		<!-- 我的用户ID -->
		<view class="card">
			<view class="section-title">
				<view class="label">我的用户ID</view>
			</view>
			<view class="code-row">
				<view class="code-value">{{ inviteCode }}</view>
				<view class="code-copy" @tap="onCopyCode">复制</view>
			</view>
			<view class="muted code-tip">把这串 ID 发给 TA，TA 就能向你发起绑定</view>
		</view>

		<!-- 已绑定 -->
		<view v-if="partner" class="card">
			<view class="section-title">
				<view class="label">我的另一半</view>
			</view>
			<view class="partner-row">
				<image v-if="partner.avatar" class="avatar" :src="partner.avatar" mode="aspectFill" />
				<view v-else class="avatar avatar-fallback">{{ partnerChar }}</view>
				<view class="partner-info">
					<view class="partner-name">{{ partner.nickname }}</view>
					<view v-if="bindAtLabel" class="muted">绑定于 {{ bindAtLabel }}</view>
				</view>
				<view class="partner-unbind" @tap="onUnbind">解除</view>
			</view>
		</view>

		<!-- 未绑定：发起绑定 -->
		<view v-else class="card">
			<view class="section-title">
				<view class="label">绑定另一半</view>
			</view>
			<input
				class="input"
				:value="inviteInput"
				maxlength="16"
				placeholder="请输入对方的用户ID"
				placeholder-class="input-placeholder"
				@input="onInviteInput"
			/>
			<button class="btn btn-primary apply-btn" :loading="applying" :disabled="applying" @tap="onApplyBind">
				发送绑定请求
			</button>
		</view>

		<!-- 收到的绑定请求 -->
		<view v-if="incoming.length > 0" class="card">
			<view class="section-title">
				<view class="label">待处理的请求</view>
			</view>
			<view v-for="req in incoming" :key="req.request_id" class="req-row">
				<image v-if="req.user.avatar" class="avatar-sm" :src="req.user.avatar" mode="aspectFill" />
				<view v-else class="avatar-sm avatar-fallback">{{ initialOf(req.user.nickname) }}</view>
				<view class="req-name">{{ req.user.nickname }}</view>
				<view class="req-btn req-accept" :data-id="req.request_id" @tap="onAccept">同意</view>
				<view class="req-btn req-reject" :data-id="req.request_id" @tap="onReject">拒绝</view>
			</view>
		</view>

		<!-- 我发出的请求 -->
		<view v-if="outgoing.length > 0" class="card">
			<view class="section-title">
				<view class="label">我发出的请求</view>
			</view>
			<view v-for="req in outgoing" :key="req.request_id" class="req-row">
				<image v-if="req.user.avatar" class="avatar-sm" :src="req.user.avatar" mode="aspectFill" />
				<view v-else class="avatar-sm avatar-fallback">{{ initialOf(req.user.nickname) }}</view>
				<view class="req-name">{{ req.user.nickname }}</view>
				<view class="muted req-pending">等待同意</view>
				<view class="req-btn req-reject" :data-id="req.request_id" @tap="onCancelRequest">撤销</view>
			</view>
		</view>

		<!-- 退出登录 -->
		<button class="btn btn-plain logout-btn" @tap="onLogout">退出登录</button>
	</view>
</template>

<script>
	import api from '../../utils/api.js'
	import store from '../../utils/store.js'
	import fmt from '../../utils/format.js'

	export default {
		data() {
			return {
				user: {},
				partner: null,
				inviteCode: '',
				bindAtLabel: '',
				avatarChar: '·',
				partnerChar: '·',
				incoming: [],
				outgoing: [],
				inviteInput: '',
				nickname: '',
				signature: '',
				saving: false,
				applying: false
			}
		},

		onShow() {
			if (!store.isLoggedIn()) {
				uni.reLaunch({ url: '/pages/login/login' })
				return
			}
			this.load()
		},

		async onPullDownRefresh() {
			await this.load()
			uni.stopPullDownRefresh()
		},

		methods: {
			initialOf: fmt.initialOf,

			async load() {
				try {
					const me = await api.authMe()
					const req = await api.bindRequests()

					store.setUser(me)

					this.user = me
					this.partner = me.partner
					this.inviteCode = me.invite_code
					this.bindAtLabel = me.bind_at ? fmt.toDateStr(me.bind_at) : ''
					this.avatarChar = fmt.initialOf(me.nickname)
					this.partnerChar = me.partner ? fmt.initialOf(me.partner.nickname) : '·'
					this.nickname = me.nickname
					this.signature = me.signature || ''
					this.incoming = req.incoming || []
					this.outgoing = req.outgoing || []
				} catch (e) {
					// 已提示
				}
			},

			// ===== 我的用户ID =====
			onCopyCode() {
				uni.setClipboardData({
					data: this.inviteCode,
					success: () => uni.showToast({ title: '用户ID已复制', icon: 'none' })
				})
			},

			// ===== 发起绑定 =====
			onInviteInput(e) {
				this.inviteInput = (e.detail.value || '').toUpperCase()
			},

			async onApplyBind() {
				const code = (this.inviteInput || '').trim()
				if (!code) {
					uni.showToast({ title: '请输入对方的用户ID', icon: 'none' })
					return
				}
				if (this.applying) return
				this.applying = true

				try {
					const res = await api.applyBind(code)
					uni.showToast({
						title: res.status === 'bound' ? '绑定成功' : '请求已发送',
						icon: 'none'
					})
					this.inviteInput = ''
					await this.load()
				} catch (e) {
					// 已提示
				} finally {
					this.applying = false
				}
			},

			// ===== 处理收到的请求 =====
			onAccept(e) {
				this.handleRequest(e.currentTarget.dataset.id, 'accept')
			},

			onReject(e) {
				this.handleRequest(e.currentTarget.dataset.id, 'reject')
			},

			async handleRequest(requestId, action) {
				try {
					await api.handleBind(requestId, action)
					uni.showToast({
						title: action === 'accept' ? '绑定成功' : '已拒绝',
						icon: 'none'
					})
					await this.load()
				} catch (e) {
					// 已提示
				}
			},

			onCancelRequest(e) {
				const id = e.currentTarget.dataset.id
				uni.showModal({
					title: '撤销申请',
					content: '确定要撤销这条绑定请求吗？',
					success: async (res) => {
						if (!res.confirm) return
						try {
							await api.cancelBind(id)
							uni.showToast({ title: '已撤销', icon: 'none' })
							this.load()
						} catch (err) {
							// 已提示
						}
					}
				})
			},

			// ===== 解除绑定 =====
			onUnbind() {
				uni.showModal({
					title: '解除绑定',
					content: '解除后将无法查看彼此的日常记录，之前的内容会保留。确定要解除吗？',
					confirmColor: '#f2697f',
					success: async (res) => {
						if (!res.confirm) return
						try {
							await api.unbind()
							uni.showToast({ title: '已解除绑定', icon: 'none' })
							this.load()
						} catch (e) {
							// 已提示
						}
					}
				})
			},

			// ===== 头像 =====
			onPickAvatar() {
				uni.chooseImage({
					count: 1,
					sizeType: ['compressed'],
					sourceType: ['album', 'camera'],
					success: async (res) => {
						const filePath = res.tempFilePaths[0]
						uni.showLoading({ title: '上传中', mask: true })

						try {
							// 先上传拿到相对路径，再存进用户资料
							const { url } = await api.uploadImage(filePath)

							const me = await api.updateProfile({
								nickname: this.nickname,
								signature: this.signature,
								avatar: url
							})

							store.setUser(me)
							this.user = me
							this.avatarChar = fmt.initialOf(me.nickname)
							uni.hideLoading()
							uni.showToast({ title: '头像已更新', icon: 'success' })
						} catch (e) {
							uni.hideLoading()
							// 错误提示已在 request 里统一处理
						}
					}
				})
			},

			// ===== 编辑资料 =====
			onNicknameInput(e) {
				this.nickname = e.detail.value
			},

			onSignatureInput(e) {
				this.signature = e.detail.value
			},

			async onSaveProfile() {
				if (this.saving) return
				this.saving = true

				try {
					const me = await api.updateProfile({
						nickname: this.nickname,
						signature: this.signature
					})
					store.setUser(me)
					this.user = me
					this.nickname = me.nickname
					this.signature = me.signature || ''
					this.avatarChar = fmt.initialOf(me.nickname)
					uni.showToast({ title: '已保存', icon: 'success' })
				} catch (e) {
					// 已提示
				} finally {
					this.saving = false
				}
			},

			// ===== 退出登录 =====
			onLogout() {
				uni.showModal({
					title: '退出登录',
					content: '退出后需要重新用微信登录，记录不会丢失。',
					confirmColor: '#f2697f',
					success: (res) => {
						if (!res.confirm) return
						store.clearUser()
						uni.reLaunch({ url: '/pages/login/login' })
					}
				})
			}
		}
	}
</script>

<style>
	/* ===== 资料卡 ===== */
	.profile-card {
		display: flex;
		flex-direction: row;
		align-items: center;
		padding: 36rpx 28rpx;
	}

	.avatar-lg {
		width: 128rpx;
		height: 128rpx;
		border-radius: 50%;
		background: #ffe3e9;
		flex-shrink: 0;
	}

	/* 头像 + 右下角的"换"角标 */
	.avatar-wrap {
		position: relative;
		flex-shrink: 0;
	}

	.avatar-badge {
		position: absolute;
		right: -2rpx;
		bottom: -2rpx;
		width: 48rpx;
		height: 48rpx;
		border-radius: 50%;
		background: #ff8fa3;
		color: #fff;
		font-size: 22rpx;
		border: 4rpx solid #fff;
		box-sizing: border-box;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.avatar-sm {
		width: 72rpx;
		height: 72rpx;
		border-radius: 50%;
		background: #ffe3e9;
		flex-shrink: 0;
	}

	.avatar-lg.avatar-fallback {
		font-size: 48rpx;
	}

	.avatar-sm.avatar-fallback {
		font-size: 28rpx;
	}

	.profile-info {
		flex: 1;
		min-width: 0;
		margin: 0 24rpx;
	}

	.profile-nickname {
		font-size: 32rpx;
		font-weight: 600;
		height: 52rpx;
	}

	.profile-signature {
		margin-top: 12rpx;
		font-size: 24rpx;
		color: #9b9b9b;
		height: 44rpx;
	}

	.save-btn {
		width: 140rpx;
		height: 64rpx;
		font-size: 26rpx;
		flex-shrink: 0;
	}

	/* ===== 用户ID ===== */
	.code-row {
		display: flex;
		flex-direction: row;
		align-items: center;
		padding: 24rpx 28rpx;
		background: #fff8f9;
		border-radius: 20rpx;
	}

	.code-value {
		flex: 1;
		font-size: 40rpx;
		font-weight: 700;
		letter-spacing: 8rpx;
		color: #f2697f;
	}

	.code-copy {
		padding: 12rpx 32rpx;
		border-radius: 32rpx;
		background: #ffe3e9;
		color: #f2697f;
		font-size: 26rpx;
	}

	.code-tip {
		margin-top: 16rpx;
	}

	/* ===== 伴侣 ===== */
	.partner-row {
		display: flex;
		flex-direction: row;
		align-items: center;
	}

	.partner-info {
		flex: 1;
		min-width: 0;
		margin-left: 20rpx;
	}

	.partner-name {
		font-size: 30rpx;
		font-weight: 600;
	}

	.partner-unbind {
		padding: 10rpx 28rpx;
		border-radius: 32rpx;
		background: #f6f2f3;
		color: #9b9b9b;
		font-size: 24rpx;
	}

	/* ===== 绑定请求 ===== */
	.apply-btn {
		width: 100%;
		margin-top: 24rpx;
	}

	.req-row {
		display: flex;
		flex-direction: row;
		align-items: center;
		padding: 20rpx 0;
		border-bottom: 1rpx solid #f6edef;
	}

	.req-row:last-child {
		border-bottom: none;
	}

	.req-name {
		flex: 1;
		min-width: 0;
		margin-left: 20rpx;
		font-size: 28rpx;
	}

	.req-pending {
		margin-right: 16rpx;
	}

	.req-btn {
		padding: 10rpx 28rpx;
		border-radius: 32rpx;
		font-size: 24rpx;
		margin-left: 12rpx;
	}

	.req-accept {
		background: #ff8fa3;
		color: #fff;
	}

	.req-reject {
		background: #f6f2f3;
		color: #9b9b9b;
	}

	/* ===== 退出 ===== */
	.logout-btn {
		width: 100%;
		margin-top: 40rpx;
	}
</style>
