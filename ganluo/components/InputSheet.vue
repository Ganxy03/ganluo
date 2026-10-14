<template>
	<view v-if="visible">
		<view class="sheet-mask" @tap="onCancel"></view>
		<view class="sheet">
			<view class="sheet-title">{{ title }}</view>

			<view v-if="hint" class="sheet-hint">{{ hint }}</view>

			<textarea
				class="sheet-textarea"
				:value="text"
				:placeholder="placeholder"
				placeholder-class="input-placeholder"
				maxlength="500"
				@input="onInput"
			/>

			<view class="sheet-actions">
				<button class="btn btn-plain" @tap="onCancel">取消</button>
				<button class="btn btn-primary" @tap="onConfirm">确定</button>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		name: 'InputSheet',
		props: {
			visible: { type: Boolean, default: false },
			title: { type: String, default: '' },
			hint: { type: String, default: '' },
			placeholder: { type: String, default: '' },
			defaultValue: { type: String, default: '' }
		},
		emits: ['confirm', 'cancel'],
		data() {
			return {
				text: ''
			}
		},
		watch: {
			// 每次打开时重置成传入的初始值
			visible(val) {
				if (val) {
					this.text = this.defaultValue
				}
			}
		},
		methods: {
			onInput(e) {
				this.text = e.detail.value
			},
			onConfirm() {
				this.$emit('confirm', this.text)
			},
			onCancel() {
				this.$emit('cancel')
			}
		}
	}
</script>

<style>
	.sheet-hint {
		margin: -16rpx 0 20rpx;
		text-align: center;
		font-size: 24rpx;
		color: #b9a9ad;
	}
</style>
