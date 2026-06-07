<template>
	<view class="page-shell book-page">
		<wd-notify
			:visible="notify.visible"
			:type="notify.type"
			:message="notify.message"
			:duration="2200"
			root-portal
			@update:visible="handleNotifyVisibleChange"
		/>

		<view v-if="books.length" class="book-list">
			<view
				v-for="book in books"
				:key="book.bookId"
				:class="['ledger-card', isCurrentBook(book) ? 'is-current' : '']"
				@click="handleOpenBookDetail(book)"
			>
				<image class="ledger-cover" :src="resolveCoverUrl(book)" mode="aspectFill"></image>
				<view class="ledger-body">
					<view class="ledger-name">{{ book.name }}</view>
					<view class="ledger-desc">{{ book.description || '暂无描述' }}</view>
					<view class="ledger-meta">创建时间：{{ formatDate(book.createdAt) }}</view>
				</view>
				<view class="ledger-side">
					<view :class="['select-mark', isCurrentBook(book) ? 'selected' : '']" @click.stop="handleSelectBook(book)">
						<text v-if="isCurrentBook(book)">✓</text>
					</view>
					<view :class="['type-pill', book.bookType === 'SHARED' ? 'shared' : 'personal']">
						{{ book.bookType === 'SHARED' ? '共享账本' : '个人账本' }}
					</view>
				</view>
			</view>
		</view>

		<view v-else class="page-block empty-block">
			<view class="page-title">还没有账本</view>
			<view class="page-subtitle">先创建一个账本，旅行里的每一笔垫付和分摊都会有地方安放。</view>
		</view>

		<view class="floating-add" @click="handleGoCreatePage('SHARED')">+</view>
	</view>
</template>

<script>
import { pinia } from '@/stores'
import { useBooksStore } from '@/stores/modules/books'
import { getMyBooks } from '@/api/modules/book'
import { ROUTES } from '@/config/app'

const DEFAULT_COVER_URL = '/static/logo.png'

export default {
	data() {
		return {
			books: [],
			notify: {
				visible: false,
				type: 'primary',
				message: ''
			}
		}
	},
	onShow() {
		this.loadBooks()
	},
	methods: {
		async loadBooks() {
			try {
				const bookList = await getMyBooks()
				useBooksStore(pinia).setBookList(bookList)
				this.books = (bookList || []).slice().sort((left, right) => {
					return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()
				})
			} catch (error) {
				this.showNotify(error.message || '账本加载失败', 'danger')
			}
		},
		resolveCoverUrl(book) {
			return book.coverUrl || DEFAULT_COVER_URL
		},
		isCurrentBook(book) {
			return useBooksStore(pinia).currentBookId === book.bookId
		},
		formatDate(value) {
			if (!value) {
				return '-'
			}
			const date = new Date(value)
			return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
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
		handleSelectBook(book) {
			useBooksStore(pinia).setCurrentBookId(book.bookId)
			this.showNotify(`已切换到 ${book.name}`, 'success')
		},
		handleOpenBookDetail(book) {
			uni.navigateTo({
				url: `${ROUTES.BOOK_DETAIL}?bookId=${book.bookId}`
			})
		},
		handleGoCreatePage(bookType) {
			uni.navigateTo({
				url: `${ROUTES.BOOK_EDITOR}?bookType=${bookType}`
			})
		}
	}
}
</script>

<style lang="scss">
.book-page {
	padding-top: 14px;
}

.book-list {
	display: flex;
	flex-direction: column;
	gap: 14px;
}

.ledger-card {
	position: relative;
	display: flex;
	gap: 14px;
	min-height: 144px;
	padding: 16px 14px;
	background: var(--app-card-bg);
	border: 1px solid transparent;
	border-radius: 8px;
	box-shadow: var(--app-card-shadow);
}

.ledger-card.is-current {
	border-color: var(--app-color-primary);
	box-shadow: 0 12px 30px rgba(197, 22, 29, 0.12);
}

.ledger-cover {
	width: 104px;
	height: 104px;
	flex-shrink: 0;
	border-radius: 6px;
	background: var(--app-section-muted-bg);
}

.ledger-body {
	flex: 1;
	min-width: 0;
	padding-top: 2px;
}

.ledger-name {
	font-size: 22px;
	font-weight: 800;
	line-height: 1.25;
	color: var(--app-text-color);
}

.ledger-desc {
	margin-top: 14px;
	font-size: 17px;
	color: #98a1b2;
}

.ledger-meta {
	margin-top: 14px;
	font-size: 14px;
	color: #a0a8b6;
}

.ledger-side {
	display: flex;
	flex-direction: column;
	align-items: flex-end;
	justify-content: space-between;
	min-width: 72px;
}

.select-mark {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 30px;
	height: 30px;
	border-radius: 50%;
	border: 2px solid #cfd5df;
	background: #e5e9f0;
	color: #ffffff;
	font-size: 20px;
	font-weight: 700;
}

.select-mark.selected {
	border-color: var(--app-color-primary);
	background: var(--app-color-primary);
}

.type-pill {
	padding: 3px 7px;
	border-radius: 3px;
	font-size: 13px;
	color: #ffffff;
}

.type-pill.shared {
	background: var(--app-color-primary);
}

.type-pill.personal {
	background: #55c7ef;
}

.floating-add {
	position: fixed;
	right: 26px;
	bottom: 112px;
	z-index: 10;
	display: flex;
	align-items: center;
	justify-content: center;
	width: 68px;
	height: 68px;
	border-radius: 50%;
	background: var(--app-color-primary);
	color: #ffffff;
	font-size: 44px;
	line-height: 1;
	box-shadow: 0 16px 36px rgba(197, 22, 29, 0.28);
}

.empty-block {
	text-align: center;
}
</style>
