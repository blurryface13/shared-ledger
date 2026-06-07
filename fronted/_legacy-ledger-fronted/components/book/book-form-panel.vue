<template>
	<view class="book-form-panel">
		<wd-cell-group v-if="showBookTypeCell" border>
			<wd-cell title="账本类型" :value="bookTypeLabel"></wd-cell>
		</wd-cell-group>

		<view class="book-form-section">
			<view class="book-form-section-title">账本封面</view>
			<wd-img
				:src="resolvedCoverPreviewUrl"
				width="104"
				height="104"
				radius="4"
				mode="aspectFill"
			/>
			<wd-button
				v-if="editable"
				type="primary"
				plain
				size="small"
				:loading="uploading"
				custom-style="margin-top: 12px;"
				@click="handleChooseCover"
			>
				更换封面
			</wd-button>
			<view class="book-form-help">支持上传本地图片，保存账本后封面会同步到列表与详情。</view>
		</view>

		<view class="book-form-section">
			<wd-input
				:model-value="formData.name"
				label="账本名称"
				placeholder="请输入账本名称"
				:maxlength="15"
				:show-word-limit="editable"
				:clearable="editable"
				:readonly="!editable"
				@update:modelValue="handleFieldChange('name', $event)"
			/>
		</view>

		<view class="book-form-section">
			<wd-textarea
				:model-value="formData.description"
				label="账本描述"
				placeholder="请输入账本描述"
				:maxlength="100"
				:show-word-limit="editable"
				:readonly="!editable"
				@update:modelValue="handleFieldChange('description', $event)"
			/>
		</view>
	</view>
</template>

<script>
import { uploadImage } from '@/api/modules/file'

export default {
	name: 'BookFormPanel',
	props: {
		formData: {
			type: Object,
			default() {
				return {
					name: '',
					description: '',
					coverUrl: '',
					bookType: 'PERSONAL'
				}
			}
		},
		bookTypeLabel: {
			type: String,
			default: ''
		},
		editable: {
			type: Boolean,
			default: true
		},
		showBookTypeCell: {
			type: Boolean,
			default: true
		},
		coverPreviewUrl: {
			type: String,
			default: ''
		}
	},
	data() {
		return {
			uploading: false
		}
	},
	computed: {
		resolvedCoverPreviewUrl() {
			return this.coverPreviewUrl || this.formData.coverUrl || '/static/logo.png'
		}
	},
	methods: {
		handleFieldChange(field, value) {
			if (!this.editable) {
				return
			}
			this.$emit('update:formData', {
				...this.formData,
				[field]: value
			})
		},
		async handleChooseCover() {
			if (!this.editable || this.uploading) {
				return
			}
			try {
				const chooseResult = await uni.chooseImage({
					count: 1,
					sizeType: ['compressed'],
					sourceType: ['album', 'camera']
				})
				const filePath = chooseResult.tempFilePaths && chooseResult.tempFilePaths[0]
				if (!filePath) {
					return
				}
				this.uploading = true
				const result = await uploadImage(filePath, 'cover')
				this.handleFieldChange('coverUrl', result.fileUrl)
				uni.showToast({
					title: '封面已上传',
					icon: 'success'
				})
			} catch (error) {
				uni.showToast({
					title: error.message || '上传失败',
					icon: 'none'
				})
			} finally {
				this.uploading = false
			}
		}
	}
}
</script>

<style lang="scss">
.book-form-section {
	margin-top: 14px;
	padding: 12px;
	background: var(--app-card-bg);
	border: 1px solid var(--app-border-color);
	border-radius: var(--app-card-radius);
	box-shadow: var(--app-card-shadow);
}

.book-form-section-title {
	margin-bottom: 10px;
	font-size: 15px;
	font-weight: 600;
	color: var(--app-text-color);
}

.book-form-help {
	margin-top: 8px;
	font-size: 12px;
	line-height: 1.6;
	color: var(--app-text-secondary);
}
</style>
