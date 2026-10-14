<template>
	<view class="page">
		<!-- 日期栏：‹ 日期 ›，点中间的日期弹出日历可以跳到任意一天 -->
		<view class="datebar">
			<view class="datebar-arrow" @tap="onPrevDay">‹</view>
			<picker mode="date" :value="date" :end="today" @change="onPickDate">
				<view class="datebar-center">
					<view class="datebar-label">{{ dateLabel }}</view>
					<view class="datebar-sub">{{ date }}<text class="cal-icon">📅</text></view>
				</view>
			</picker>
			<view class="datebar-arrow" :class="{ 'is-disabled': isToday }" @tap="onNextDay">›</view>
		</view>

		<!-- 未绑定提示 -->
		<view v-if="!loading && !bound" class="card bind-hint">
			<view class="bind-hint-title">还没有绑定另一半</view>
			<view class="bind-hint-text">去「我的」页面输入 TA 的用户ID，就能看到彼此的日常啦</view>
			<button class="btn btn-primary bind-hint-btn" @tap="goProfile">去绑定</button>
		</view>

		<view v-if="loading" class="empty">加载中…</view>

		<block v-else>
			<!-- ① 心情日记 -->
			<view class="card">
				<view class="section-title">
					<view class="label">心情日记</view>
					<view v-if="isToday" class="action" @tap="onAddMood">＋ 记录</view>
				</view>

				<!-- TA 的 -->
				<view v-if="partner" class="person">
					<view class="person-head">
						<image v-if="partner.user.avatar" class="avatar-sm" :src="partner.user.avatar" mode="aspectFill" />
						<view v-else class="avatar-sm avatar-fallback">{{ partnerChar }}</view>
						<text class="person-name">TA</text>
					</view>
					<view v-if="partnerMoods.length === 0" class="person-empty">这天没有记录</view>
					<view v-for="mood in partnerMoods" :key="mood.id" class="mood-item">
						<view class="mood-emoji">{{ mood.emoji }}</view>
						<view class="mood-body">
							<view class="mood-label">{{ mood.mood_label }}</view>
							<view v-if="mood.content" class="mood-content">{{ mood.content }}</view>
						</view>
						<view class="mood-time">{{ mood.time }}</view>
					</view>
				</view>

				<!-- 我的 -->
				<view class="person">
					<view class="person-head">
						<image v-if="me.user.avatar" class="avatar-sm" :src="me.user.avatar" mode="aspectFill" />
						<view v-else class="avatar-sm avatar-fallback">{{ meChar }}</view>
						<text class="person-name">我</text>
					</view>
					<view v-if="myMoods.length === 0" class="person-empty">这天还没有记录</view>
					<view
						v-for="mood in myMoods"
						:key="mood.id"
						class="mood-item"
						:data-id="mood.id"
						@longpress="onLongPressMood"
					>
						<view class="mood-emoji">{{ mood.emoji }}</view>
						<view class="mood-body">
							<view class="mood-label">{{ mood.mood_label }}</view>
							<view v-if="mood.content" class="mood-content">{{ mood.content }}</view>
						</view>
						<view class="mood-time">{{ mood.time }}</view>
					</view>
					<view v-if="myMoods.length > 0" class="list-hint">长按一条可以删除</view>
				</view>
			</view>

			<!-- ② 三餐报备 —— 暂时停用，以后要恢复：把下面这个 v-if="false" 删掉即可。
			     （没有直接写成 HTML 注释，是因为块内本身已有注释，而 HTML 注释不支持嵌套） -->
			<view v-if="false" class="card">
				<view class="section-title">
					<view class="label">三餐报备</view>
				</view>

				<!-- TA 的 -->
				<view v-if="partner" class="person">
					<view class="person-head">
						<image v-if="partner.user.avatar" class="avatar-sm" :src="partner.user.avatar" mode="aspectFill" />
						<view v-else class="avatar-sm avatar-fallback">{{ partnerChar }}</view>
						<text class="person-name">TA</text>
					</view>
					<view class="meal-grid">
						<view
							v-for="meal in partnerMeals"
							:key="meal.type"
							class="meal-slot"
							:class="{ 'is-filled': meal.has }"
						>
							<view class="meal-thumb">
								<image v-if="meal.cover" class="meal-img" :src="meal.cover" mode="aspectFill" />
								<view v-else class="meal-plus">＋</view>
							</view>
							<view class="meal-label">{{ meal.label }}</view>
							<view v-if="meal.content" class="meal-desc">{{ meal.content }}</view>
						</view>
					</view>
				</view>

				<!-- 我的 -->
				<view class="person">
					<view class="person-head">
						<image v-if="me.user.avatar" class="avatar-sm" :src="me.user.avatar" mode="aspectFill" />
						<view v-else class="avatar-sm avatar-fallback">{{ meChar }}</view>
						<text class="person-name">我</text>
					</view>
					<view class="meal-grid">
						<view
							v-for="meal in myMeals"
							:key="meal.type"
							class="meal-slot"
							:class="{ 'is-filled': meal.has }"
							:data-type="meal.type"
							:data-id="meal.id"
							@tap="onTapMeal"
							@longpress="onLongPressMeal"
						>
							<view class="meal-thumb">
								<image v-if="meal.cover" class="meal-img" :src="meal.cover" mode="aspectFill" />
								<view v-else class="meal-plus">＋</view>
							</view>
							<view class="meal-label">{{ meal.label }}</view>
							<view v-if="meal.content" class="meal-desc">{{ meal.content }}</view>
						</view>
					</view>
					<view class="list-hint">点格子报备　长按已报备的格子可删除</view>
				</view>
			</view>

			<!-- ③ 留言提醒（两个人的双向留言） -->
			<view class="card">
				<view class="section-title">
					<view class="label">留言提醒</view>
					<view v-if="unreadTotal > 0" class="badge">{{ unreadTotal }}</view>
				</view>

				<view v-if="!bound" class="empty">绑定后就能看到彼此的留言</view>

				<block v-else>
					<!-- 未读在别的日子时，给个跳转提示，否则很容易永远找不到 -->
					<view
						v-if="latestUnreadDate && latestUnreadDate !== date"
						class="unread-jump"
						@tap="onJumpToUnread"
					>
						还有 {{ unreadTotal }} 条未读在 {{ latestUnreadDate }}，点这里查看
					</view>

					<view v-if="messages.length === 0" class="empty">这一天还没有留言</view>

					<view
						v-for="msg in messages"
						:key="msg.id"
						class="chat-row"
						:class="{ 'is-mine': msg.is_mine }"
						:data-id="msg.id"
						@tap="onTapMessage"
						@longpress="onLongPressMessage"
					>
						<view class="chat-bubble">
							<view v-if="msg.reply_to" class="chat-quote">
								<text class="chat-quote-mark">↳</text>
								<text class="chat-quote-text">{{ msg.reply_to.content }}</text>
							</view>
							<view class="chat-text">{{ msg.content }}</view>
						</view>
						<view class="chat-meta">
							<text v-if="!msg.is_mine && !msg.is_read" class="chat-unread">未读</text>
							<text class="chat-time">{{ msg.time }}</text>
						</view>
					</view>

					<button class="btn btn-ghost write-btn" @tap="onComposeMessage">＋ 写留言</button>
				</block>
			</view>
		</block>
	</view>

	<!-- 心情记录弹层。
	     ⚠️ 不能用 showActionSheet：它的 itemList 最多只支持 6 项，而我们有 8 种心情，
	        超限会直接报错（而且不弹任何界面），所以自己画一个。 -->
	<view v-if="moodSheet.visible">
		<view class="sheet-mask" @tap="onCloseMoodSheet"></view>
		<view class="sheet">
			<view class="sheet-title">今天心情怎么样</view>

			<view class="mood-grid">
				<view
					v-for="m in moodOptions"
					:key="m.type"
					class="mood-option"
					:class="{ 'is-active': moodSheet.selected === m.type }"
					:data-type="m.type"
					@tap="onPickMood"
				>
					<view class="mood-option-emoji">{{ m.emoji }}</view>
					<view class="mood-option-label">{{ m.label }}</view>
				</view>
			</view>

			<textarea
				class="sheet-textarea"
				:value="moodSheet.content"
				placeholder="想说点什么…（可留空）"
				placeholder-class="input-placeholder"
				maxlength="500"
				@input="onMoodContentInput"
			/>

			<view class="sheet-actions">
				<button class="btn btn-plain" @tap="onCloseMoodSheet">取消</button>
				<button class="btn btn-primary" :disabled="!moodSheet.selected" @tap="onSaveMood">记录</button>
			</view>
		</view>
	</view>

	<!-- 写留言 / 回复弹层 -->
	<view v-if="msgSheet.visible">
		<view class="sheet-mask" @tap="onCloseMsgSheet"></view>
		<view class="sheet">
			<view class="sheet-title">{{ msgSheet.replyTo ? '回复留言' : '给 TA 留言' }}</view>

			<view v-if="msgSheet.replyTo" class="reply-quote">
				<view class="reply-quote-author">{{ msgSheet.replyTo.author.nickname }}</view>
				<view class="reply-quote-text">{{ msgSheet.replyTo.content }}</view>
			</view>

			<textarea
				class="sheet-textarea"
				:value="msgSheet.content"
				:placeholder="msgSheet.replyTo ? '回复…' : '想对 TA 说的话…'"
				placeholder-class="input-placeholder"
				maxlength="500"
				@input="onMsgInput"
			/>

			<view class="sheet-actions">
				<button class="btn btn-plain" @tap="onCloseMsgSheet">取消</button>
				<button class="btn btn-primary" @tap="onSendMessage">发送</button>
			</view>
		</view>
	</view>

	<!-- 三餐文字输入弹层 -->
	<InputSheet
		:visible="inputSheet.visible"
		:title="inputSheet.title"
		:hint="inputSheet.hint"
		:placeholder="inputSheet.placeholder"
		@confirm="onSheetConfirm"
		@cancel="onSheetCancel"
	/>
</template>

<script>
	import api from '../../utils/api.js'
	import store from '../../utils/store.js'
	import fmt from '../../utils/format.js'
	import InputSheet from '../../components/InputSheet.vue'

	const MEAL_SLOTS = [
		{ type: 'breakfast', label: '早餐' },
		{ type: 'lunch', label: '午餐' },
		{ type: 'dinner', label: '晚餐' },
		{ type: 'supper', label: '宵夜' }
	]

	const MOODS = [
		{ type: 'happy', label: '开心', emoji: '😊' },
		{ type: 'sweet', label: '幸福', emoji: '🥰' },
		{ type: 'calm', label: '平静', emoji: '😌' },
		{ type: 'excited', label: '兴奋', emoji: '🤩' },
		{ type: 'tired', label: '疲惫', emoji: '😪' },
		{ type: 'sad', label: '难过', emoji: '😢' },
		{ type: 'angry', label: '生气', emoji: '😠' },
		{ type: 'anxious', label: '焦虑', emoji: '😰' }
	]

	const MOOD_EMOJI = MOODS.reduce((acc, m) => {
		acc[m.type] = m.emoji
		return acc
	}, {})

	export default {
		components: { InputSheet },

		data() {
			return {
				date: '',
				dateLabel: '',
				today: '',
				isToday: true,
				loading: true,
				bound: true,

				// 我 / TA 在当天的数据
				me: { user: {}, moods: [], meals: [] },
				partner: null,
				meChar: '·',
				partnerChar: '·',

				// 整理成模板好渲染的结构
				myMoods: [],
				partnerMoods: [],
				myMeals: [],
				partnerMeals: [],

				// 留言板
				messages: [],
				unreadTotal: 0,
				unreadInDay: 0,
				latestUnreadDate: '',

				// 心情记录弹层（8 种心情超过 showActionSheet 的 6 项上限，所以自己做）
				moodOptions: MOODS,
				moodSheet: { visible: false, selected: '', content: '' },

				// 写留言 / 回复弹层
				msgSheet: { visible: false, content: '', replyTo: null },

				// 三餐的文字输入弹层
				inputSheet: {
					visible: false,
					title: '',
					hint: '',
					placeholder: '',
					mealType: '',
					images: []
				},

				// 长按后微信仍可能补一个 tap，用时间戳挡掉
				lastLongPressAt: 0,

				// onLoad 里已经加载过一次，首次 onShow 不重复请求
				firstShow: true
			}
		},

		onLoad() {
			if (!store.isLoggedIn()) {
				uni.reLaunch({ url: '/pages/login/login' })
				return
			}

			const today = fmt.toDateStr(new Date())
			this.today = today
			this.date = today
			this.dateLabel = fmt.friendlyDate(today)
			this.isToday = true
			this.load()
		},

		onShow() {
			if (!store.isLoggedIn() || !this.date) return

			// onLoad 已经拉过一次，首次 onShow 跳过 —— 否则每次进主页都会连发两次 /api/home
			if (this.firstShow) {
				this.firstShow = false
				return
			}

			this.load()
		},

		async onPullDownRefresh() {
			await this.load()
			uni.stopPullDownRefresh()
		},

		methods: {
			// ===== 数据 =====
			async load() {
				try {
					const home = await api.home(this.date)
					const msg = home.messages || {}

					this.me = home.me
					this.partner = home.partner
					this.bound = !!home.partner
					this.meChar = fmt.initialOf(home.me.user.nickname)
					this.partnerChar = home.partner ? fmt.initialOf(home.partner.user.nickname) : '·'

					this.myMoods = this.buildMoods(home.me.moods)
					this.myMeals = this.buildMeals(home.me.meals)
					this.partnerMoods = home.partner ? this.buildMoods(home.partner.moods) : []
					this.partnerMeals = home.partner ? this.buildMeals(home.partner.meals) : []

					this.messages = msg.list || []
					this.unreadTotal = msg.unread_total || 0
					this.unreadInDay = msg.unread_in_day || 0
					this.latestUnreadDate = msg.latest_unread_date || ''

					this.loading = false

					store.patchUser(
						Object.assign({}, home.me.user, {
							bound: !!home.partner,
							partner: home.partner ? home.partner.user : null
						})
					)

					// 看完这一天的留言就标已读。延迟一下，让"未读"高亮先被看见
					if (this.unreadInDay > 0) {
						setTimeout(() => this.markDayRead(), 800)
					}
				} catch (e) {
					this.loading = false
				}
			},

			buildMoods(list) {
				return (list || []).map((m) => ({
					id: m.id,
					emoji: MOOD_EMOJI[m.mood_type] || '🙂',
					mood_label: m.mood_label,
					content: m.content,
					time: fmt.timeOf(m.created_at)
				}))
			},

			buildMeals(list) {
				const map = {}
				;(list || []).forEach((m) => {
					map[m.meal_type] = m
				})

				return MEAL_SLOTS.map((slot) => {
					const item = map[slot.type]
					return {
						type: slot.type,
						label: slot.label,
						id: item ? item.id : 0, // 长按删除时要用的记录 id
						has: !!item,
						content: item ? item.content : '',
						cover: item && item.images.length ? item.images[0] : ''
					}
				})
			},

			/** 把当前这天的留言标为已读（本地同步更新，不重新拉接口） */
			async markDayRead() {
				try {
					await api.readAllMessages(this.date)
					this.messages = this.messages.map((m) => Object.assign({}, m, { is_read: true }))
					this.unreadTotal = Math.max(0, this.unreadTotal - this.unreadInDay)
					this.unreadInDay = 0
				} catch (e) {
					// 已提示
				}
			},

			// ===== 日期 =====
			onPrevDay() {
				this.changeDate(-1)
			},

			onNextDay() {
				if (this.isToday) return
				this.changeDate(1)
			},

			onPickDate(e) {
				this.goToDate(e.detail.value)
			},

			changeDate(delta) {
				this.goToDate(fmt.shiftDate(this.date, delta))
			},

			goToDate(date) {
				if (!date || date === this.date) return
				this.date = date
				this.dateLabel = fmt.friendlyDate(date)
				this.isToday = fmt.isToday(date)
				this.load()
			},

			/** 跳到最近一条未读所在的那天 */
			onJumpToUnread() {
				if (this.latestUnreadDate) this.goToDate(this.latestUnreadDate)
			},

			goProfile() {
				uni.switchTab({ url: '/pages/profile/profile' })
			},

			// ===== 心情日记 =====
			onAddMood() {
				this.moodSheet = { visible: true, selected: '', content: '' }
			},

			onCloseMoodSheet() {
				this.moodSheet.visible = false
			},

			onPickMood(e) {
				this.moodSheet.selected = e.currentTarget.dataset.type
			},

			onMoodContentInput(e) {
				this.moodSheet.content = e.detail.value
			},

			async onSaveMood() {
				if (!this.moodSheet.selected) {
					uni.showToast({ title: '先选一个心情', icon: 'none' })
					return
				}

				try {
					await api.addMood({
						mood_type: this.moodSheet.selected,
						content: this.moodSheet.content || '',
						mood_date: this.date
					})
					uni.showToast({ title: '已记录', icon: 'success' })
					this.moodSheet.visible = false
					this.load()
				} catch (e) {
					// 已提示
				}
			},

			// ===== 三餐报备 =====
			onTapMeal(e) {
				const { type } = e.currentTarget.dataset

				// 长按删除后微信可能还会补一个 tap，这里挡掉，否则会顺带弹出报备框
				if (Date.now() - this.lastLongPressAt < 600) return

				if (!this.isToday) {
					uni.showToast({ title: '只能报备今天的', icon: 'none' })
					return
				}

				uni.showActionSheet({
					itemList: ['拍照 / 从相册选择', '只写文字'],
					success: ({ tapIndex }) => {
						if (tapIndex === 0) {
							this.pickMealImage(type)
						} else {
							this.openMealSheet(type, [])
						}
					}
				})
			},

			pickMealImage(type) {
				uni.chooseImage({
					count: 1,
					sizeType: ['compressed'],
					sourceType: ['album', 'camera'],
					success: (res) => {
						const filePath = res.tempFilePaths[0]
						uni.showLoading({ title: '上传中', mask: true })

						api
							.uploadImage(filePath)
							.then((data) => {
								uni.hideLoading()
								this.openMealSheet(type, [data.url])
							})
							.catch(() => {
								uni.hideLoading()
							})
					}
				})
			},

			openMealSheet(type, images) {
				const slot = MEAL_SLOTS.find((s) => s.type === type) || {}
				this.inputSheet = {
					visible: true,
					title: `${slot.label || '这一餐'}吃什么啦`,
					hint: images.length ? '已选好 1 张照片' : '',
					placeholder: '描述一下（可留空）',
					mealType: type,
					images
				}
			},

			async onSheetConfirm(text) {
				const sheet = this.inputSheet
				sheet.visible = false

				try {
					await api.addMeal({
						meal_type: sheet.mealType,
						content: text || '',
						images: sheet.images,
						meal_date: this.date
					})
					uni.showToast({ title: '已报备', icon: 'success' })
					this.load()
				} catch (e) {
					// 已提示
				}
			},

			onSheetCancel() {
				this.inputSheet.visible = false
			},

			// ===== 长按删除 =====
			onLongPressMood(e) {
				const { id } = e.currentTarget.dataset
				this.lastLongPressAt = Date.now()
				this.confirmDelete('删除这条心情', () => api.deleteMood(id))
			},

			onLongPressMeal(e) {
				const { id } = e.currentTarget.dataset
				this.lastLongPressAt = Date.now()

				if (!id) {
					uni.showToast({ title: '这一餐还没有记录', icon: 'none' })
					return
				}
				this.confirmDelete('删除这餐的记录', () => api.deleteMeal(id))
			},

			onLongPressMessage(e) {
				const { id } = e.currentTarget.dataset
				this.lastLongPressAt = Date.now()
				this.confirmDelete('删除这条留言', () => api.deleteMessage(id))
			},

			confirmDelete(title, action) {
				uni.showModal({
					title,
					content: '删除后无法恢复，确定吗？',
					confirmText: '删除',
					confirmColor: '#f2697f',
					success: async (res) => {
						if (!res.confirm) return
						try {
							await action()
							uni.showToast({ title: '已删除', icon: 'none' })
							this.load()
						} catch (e) {
							// 已提示
						}
					}
				})
			},

			// ===== 留言 =====
			onComposeMessage() {
				this.msgSheet = { visible: true, content: '', replyTo: null }
			},

			/** 点某条留言 = 回复它 */
			onTapMessage(e) {
				if (Date.now() - this.lastLongPressAt < 600) return

				const { id } = e.currentTarget.dataset
				const target = this.messages.find((m) => m.id === id)
				if (!target) return

				this.msgSheet = { visible: true, content: '', replyTo: target }
			},

			onCloseMsgSheet() {
				this.msgSheet.visible = false
			},

			onMsgInput(e) {
				this.msgSheet.content = e.detail.value
			},

			async onSendMessage() {
				const sheet = this.msgSheet
				const content = (sheet.content || '').trim()

				if (!content) {
					uni.showToast({ title: '写点什么吧', icon: 'none' })
					return
				}

				const payload = { content }
				if (sheet.replyTo) {
					payload.reply_to_id = sheet.replyTo.id
				}

				try {
					await api.sendMessage(payload)
					uni.showToast({ title: '已送达', icon: 'success' })
					this.msgSheet.visible = false
					this.load()
				} catch (e) {
					// 已提示
				}
			}
		}
	}
</script>

<style>
	/* ===== 日期栏 ===== */
	.datebar {
		display: flex;
		flex-direction: row;
		align-items: center;
		justify-content: space-between;
		padding: 8rpx 24rpx 28rpx;
	}

	.datebar-arrow {
		width: 64rpx;
		height: 64rpx;
		border-radius: 50%;
		background: #fff;
		color: #ff8fa3;
		font-size: 40rpx;
		line-height: 60rpx;
		text-align: center;
		box-shadow: 0 4rpx 16rpx rgba(255, 143, 163, 0.12);
	}

	.datebar-arrow.is-disabled {
		color: #ddd6d8;
	}

	.datebar-center {
		padding: 4rpx 30rpx;
		text-align: center;
	}

	.datebar-label {
		font-size: 34rpx;
		font-weight: 600;
	}

	.datebar-sub {
		font-size: 22rpx;
		color: #b9a9ad;
	}

	.cal-icon {
		margin-left: 8rpx;
		font-size: 22rpx;
	}

	/* ===== 未绑定提示 ===== */
	.bind-hint {
		text-align: center;
		padding: 44rpx 40rpx;
	}

	.bind-hint-title {
		font-size: 30rpx;
		font-weight: 600;
	}

	.bind-hint-text {
		margin: 16rpx 0 32rpx;
		font-size: 24rpx;
		line-height: 1.7;
		color: #9b9b9b;
	}

	.bind-hint-btn {
		width: 100%;
	}

	/* ===== 板块内的人员分组 ===== */
	.person {
		padding: 8rpx 0 20rpx;
		border-bottom: 1rpx solid #f6edef;
	}

	.person:last-child {
		border-bottom: none;
		padding-bottom: 0;
	}

	.person-head {
		display: flex;
		flex-direction: row;
		align-items: center;
		margin-bottom: 14rpx;
	}

	.avatar-sm {
		width: 52rpx;
		height: 52rpx;
		border-radius: 50%;
		background: #ffe3e9;
		flex-shrink: 0;
	}

	.avatar-sm.avatar-fallback {
		font-size: 24rpx;
	}

	.person-name {
		margin-left: 14rpx;
		font-size: 26rpx;
		font-weight: 600;
		color: #6f6a6c;
	}

	.person-empty {
		padding: 12rpx 0 8rpx;
		font-size: 24rpx;
		color: #c4c0c2;
	}

	/* ===== 心情 ===== */
	.mood-item {
		display: flex;
		flex-direction: row;
		align-items: flex-start;
		padding: 14rpx 0;
	}

	.mood-emoji {
		width: 52rpx;
		height: 52rpx;
		border-radius: 50%;
		background: #fff2f5;
		font-size: 30rpx;
		line-height: 52rpx;
		text-align: center;
		flex-shrink: 0;
	}

	.mood-body {
		flex: 1;
		min-width: 0;
		margin: 0 16rpx;
	}

	.mood-label {
		font-size: 28rpx;
		font-weight: 600;
	}

	.mood-content {
		margin-top: 4rpx;
		font-size: 26rpx;
		color: #6f6a6c;
		word-break: break-all;
	}

	.mood-time {
		font-size: 22rpx;
		color: #c4c0c2;
		flex-shrink: 0;
	}

	/* ===== 三餐 ===== */
	.meal-grid {
		display: flex;
		flex-direction: row;
	}

	.meal-slot {
		width: 25%;
		padding: 0 8rpx;
		text-align: center;
	}

	.meal-thumb {
		width: 100%;
		height: 140rpx;
		position: relative;
		border-radius: 20rpx;
		background: #f8f4f5;
		overflow: hidden;
	}

	.meal-slot.is-filled .meal-thumb {
		background: #fff2f5;
	}

	.meal-img {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
	}

	.meal-plus {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		display: flex;
		align-items: center;
		justify-content: center;
		color: #e3ccd2;
		font-size: 40rpx;
	}

	.meal-label {
		margin-top: 12rpx;
		font-size: 24rpx;
		color: #6f6a6c;
	}

	.meal-slot.is-filled .meal-label {
		color: #f2697f;
		font-weight: 600;
	}

	.meal-desc {
		margin-top: 4rpx;
		font-size: 20rpx;
		color: #b9a9ad;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.list-hint {
		margin-top: 14rpx;
		text-align: center;
		font-size: 22rpx;
		color: #c4c0c2;
	}

	/* ===== 留言板 ===== */
	.badge {
		min-width: 40rpx;
		height: 40rpx;
		padding: 0 12rpx;
		border-radius: 20rpx;
		background: #ff8fa3;
		color: #fff;
		font-size: 22rpx;
		line-height: 40rpx;
		text-align: center;
	}

	.unread-jump {
		margin-bottom: 20rpx;
		padding: 16rpx 24rpx;
		border-radius: 16rpx;
		background: #fff2f5;
		color: #f2697f;
		font-size: 24rpx;
	}

	.chat-row {
		display: flex;
		flex-direction: column;
		align-items: flex-start;
		margin-bottom: 22rpx;
	}

	.chat-row.is-mine {
		align-items: flex-end;
	}

	.chat-bubble {
		max-width: 78%;
		padding: 18rpx 24rpx;
		border-radius: 20rpx;
		background: #f6f2f3;
	}

	.chat-row.is-mine .chat-bubble {
		background: #ffe3e9;
	}

	.chat-quote {
		display: flex;
		flex-direction: row;
		margin-bottom: 10rpx;
		padding-left: 12rpx;
		border-left: 4rpx solid #d8cccf;
	}

	.chat-quote-mark {
		margin-right: 8rpx;
		font-size: 22rpx;
		color: #b9a9ad;
	}

	.chat-quote-text {
		flex: 1;
		min-width: 0;
		font-size: 22rpx;
		color: #9b9b9b;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.chat-text {
		font-size: 28rpx;
		word-break: break-all;
	}

	.chat-meta {
		margin-top: 6rpx;
		font-size: 22rpx;
		color: #c4c0c2;
	}

	.chat-unread {
		margin-right: 12rpx;
		color: #ff8fa3;
		font-weight: 600;
	}

	.write-btn {
		width: 100%;
		margin-top: 8rpx;
	}

	/* ===== 心情选择弹层 ===== */
	.mood-grid {
		display: flex;
		flex-direction: row;
		flex-wrap: wrap;
	}

	.mood-option {
		width: 25%;
		padding: 16rpx 8rpx;
		text-align: center;
	}

	.mood-option-emoji {
		width: 88rpx;
		height: 88rpx;
		margin: 0 auto;
		border-radius: 50%;
		background: #f8f4f5;
		font-size: 44rpx;
		line-height: 88rpx;
		text-align: center;
	}

	.mood-option.is-active .mood-option-emoji {
		background: #ffe3e9;
		box-shadow: 0 0 0 4rpx #ff8fa3;
	}

	.mood-option-label {
		margin-top: 10rpx;
		font-size: 24rpx;
		color: #6f6a6c;
	}

	.mood-option.is-active .mood-option-label {
		color: #f2697f;
		font-weight: 600;
	}

	/* ===== 回复引用 ===== */
	.reply-quote {
		margin-bottom: 20rpx;
		padding: 18rpx 24rpx;
		border-radius: 16rpx;
		background: #f8f4f5;
		border-left: 6rpx solid #ff8fa3;
	}

	.reply-quote-author {
		font-size: 22rpx;
		color: #b9a9ad;
	}

	.reply-quote-text {
		margin-top: 6rpx;
		font-size: 26rpx;
		color: #6f6a6c;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
</style>
