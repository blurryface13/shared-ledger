<template>
	<view class="page-shell">
		<wd-notify
			:visible="notify.visible"
			:type="notify.type"
			:message="notify.message"
			:duration="2200"
			root-portal
			@update:visible="handleNotifyVisibleChange"
		/>

		<view class="page-block">
			<view class="page-title">创建{{ bookTypeLabel }}</view>
			<view class="page-subtitle">个人账本和共享账本共用同一套表单，后续更新账本信息也会复用这里的表单组件。</view>
		</view>

		<book-form-panel
			:form-data="formData"
			:book-type-label="bookTypeLabel"
			@update:formData="handleFormChange"
		/>

		<view class="page-block action-block">
			<wd-button type="primary" block :loading="submitting" @click="handleSubmit">
				确认创建
			</wd-button>
			<wd-button block custom-style="margin-top: 12px;" @click="handleCancel">
				返回账本列表
			</wd-button>
		</view>
	</view>
</template>

<script>
import { pinia } from '@/stores'
import { useBooksStore } from '@/stores/modules/books'
import { createBook, getMyBooks } from '@/api/modules/book'
import BookFormPanel from '@/components/book/book-form-panel.vue'

function createDefaultForm(bookType) {
	return {
		name: '',
		description: '',
		coverUrl: '',
		bookType: bookType || 'PERSONAL'
	}
}

export default {
	components: {
		BookFormPanel
	},
	data() {
		return {
			bookTypeLabel: '个人账本',
			formData: createDefaultForm('PERSONAL'),
			submitting: false,
			notify: {
				visible: false,
				type: 'primary',
				message: ''
			}
		}
	},
	onLoad(options) {
		const bookType = options && options.bookType === 'SHARED' ? 'SHARED' : 'PERSONAL'
		this.formData = createDefaultForm(bookType)
		this.bookTypeLabel = bookType === 'SHARED' ? '共享账本' : '个人账本'
		uni.setNavigationBarTitle({
			title: `创建${this.bookTypeLabel}`
		})
	},
	methods: {
		handleFormChange(nextFormData) {
			this.formData = {
				...this.formData,
				...nextFormData
			}
		},
		showNotify(message, type) {
			this.notify = {
				visible: true,
				type: type || 'primary',
				message
			}
		},
		handleNotifyVisibleChange(visible) {
			this.notify.visible = visible
		},
		async handleSubmit() {
			if (!this.formData.name.trim()) {
				this.showNotify('请先填写账本名称', 'warning')
				return
			}

			this.submitting = true
			try {
				const createResult = await createBook({
					name: this.formData.name.trim(),
					bookType: this.formData.bookType,
					description: this.formData.description.trim(),
					coverUrl: null
				})
				console.log('trip-ledger create book result:', createResult)
				const bookList = await getMyBooks()
				const booksStore = useBooksStore(pinia)
				booksStore.setBookList(bookList)
				if (createResult && createResult.bookId) {
					booksStore.setCurrentBookId(createResult.bookId)
				}
				this.showNotify('创建账本成功', 'success')
				setTimeout(() => {
					uni.navigateBack()
				}, 500)
			} catch (error) {
				this.showNotify(error.message || '创建账本失败', 'danger')
			} finally {
				this.submitting = false
			}
		},
		handleCancel() {
			uni.navigateBack()
		}
	}
}
</script>

<style lang="scss">
.action-block {
	margin-top: 14px;
}
</style>
