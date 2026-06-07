<template>
	<view class="page-shell bill-page">
		<view v-if="bookId" class="day-summary">
			<view class="day-title">{{ latestDateLabel }}</view>
			<view class="day-line">
				<text class="income">收入：{{ formatPlainMoney(summary.income) }}</text>
				<text class="expense">支出：{{ formatPlainMoney(summary.expense) }}</text>
				<text class="balance">结余：{{ formatPlainMoney(summary.income - summary.expense) }}</text>
			</view>
		</view>

		<view v-if="!bookId" class="page-block empty-hint">请先在账本页创建或选择一个账本。</view>
		<view v-else-if="loading" class="page-block empty-hint">正在加载账单...</view>
		<view v-else-if="!bills.length" class="page-block empty-hint">还没有账单，点右下角新增第一笔。</view>
		<view v-else class="bill-list">
			<view v-for="bill in bills" :key="bill.billId" class="bill-card">
				<view class="bill-icon">{{ resolveCategoryIcon(bill) }}</view>
				<view class="bill-main">
					<view class="bill-title">{{ bill.title }}</view>
					<view class="bill-meta">{{ bill.categoryName || '未分类' }}</view>
					<view class="bill-time">{{ formatTime(bill.billTime) }}</view>
				</view>
				<view class="bill-side">
					<view :class="['bill-amount', bill.billType === 'PERSONAL_INCOME' ? 'positive' : 'negative']">
						{{ bill.billType === 'PERSONAL_INCOME' ? '+' : '-' }}{{ formatPlainMoney(bill.billAmountCent) }}元
					</view>
					<view v-if="bill.billType === 'SHARED_EXPENSE'" class="status-pill">共享垫付未结</view>
					<view class="bill-small">个人应计：{{ formatPlainMoney(bill.viewerOwnShareAmountCent || bill.billAmountCent) }}元</view>
					<view v-if="bill.viewerReceivableAmountCent" class="bill-small">待收回：{{ formatPlainMoney(bill.viewerReceivableAmountCent) }}元</view>
				</view>
			</view>
		</view>

		<view class="floating-add" @click="createVisible = true">+</view>

		<wd-popup v-model="createVisible" position="bottom" root-portal safe-area-inset-bottom custom-style="background: #fff; border-radius: 8px 8px 0 0;">
			<view class="create-panel">
				<view class="panel-title">新增账单</view>
				<view class="mode-row">
					<wd-button size="small" :type="form.billType === 'PERSONAL_EXPENSE' ? 'primary' : 'info'" @click="setBillType('PERSONAL_EXPENSE')">支出</wd-button>
					<wd-button size="small" :type="form.billType === 'PERSONAL_INCOME' ? 'primary' : 'info'" @click="setBillType('PERSONAL_INCOME')">收入</wd-button>
					<wd-button size="small" :type="form.billType === 'SHARED_EXPENSE' ? 'primary' : 'info'" @click="setBillType('SHARED_EXPENSE')">共享</wd-button>
				</view>
				<wd-input v-model="form.title" label="标题" placeholder="例如 午餐、酒店、门票" maxlength="30" clearable />
				<wd-input v-model="form.amount" label="金额" placeholder="0.00" type="digit" clearable />
				<view class="picker-line">
					<text class="picker-label">分类</text>
					<picker :range="categoryNames" :value="categoryIndex" @change="handleCategoryChange">
						<view class="picker-value">{{ selectedCategoryName }}</view>
					</picker>
				</view>
				<view class="picker-line">
					<text class="picker-label">付款人</text>
					<picker :range="memberNames" :value="payerIndex" @change="handlePayerChange">
						<view class="picker-value">{{ selectedPayerName }}</view>
					</picker>
				</view>
				<wd-textarea v-model="form.remark" label="备注" placeholder="可选" maxlength="100" />
				<wd-button type="primary" block :loading="creating" custom-style="margin-top: 12px;" @click="handleCreateBill">
					保存账单
				</wd-button>
			</view>
		</wd-popup>
	</view>
</template>

<script>
import { pinia } from '@/stores'
import { useBooksStore } from '@/stores/modules/books'
import { getBookDetail, getMyBooks } from '@/api/modules/book'
import { getBookCategories } from '@/api/modules/category'
import { createPersonalBill, createSharedExpenseBill, getBills } from '@/api/modules/bill'

function createForm() {
	return {
		billType: 'PERSONAL_EXPENSE',
		title: '',
		amount: '',
		categoryId: null,
		payerMemberId: null,
		remark: ''
	}
}

export default {
	data() {
		return {
			bookId: null,
			bookDetail: null,
			categories: [],
			bills: [],
			loading: false,
			creating: false,
			createVisible: false,
			form: createForm()
		}
	},
	computed: {
		activeMembers() {
			return ((this.bookDetail && this.bookDetail.members) || []).filter((item) => item.memberStatus === 'ACTIVE')
		},
		memberNames() {
			return this.activeMembers.map((item) => item.nickname || `成员 ${item.memberId}`)
		},
		categoryOptions() {
			const expectedType = this.form.billType === 'PERSONAL_INCOME' ? 'INCOME' : 'EXPENSE'
			return this.categories.filter((item) => item.categoryType === expectedType && item.status === 'ACTIVE')
		},
		categoryNames() {
			return this.categoryOptions.map((item) => item.name)
		},
		categoryIndex() {
			return Math.max(0, this.categoryOptions.findIndex((item) => item.categoryId === this.form.categoryId))
		},
		payerIndex() {
			return Math.max(0, this.activeMembers.findIndex((item) => item.memberId === this.form.payerMemberId))
		},
		selectedCategoryName() {
			const item = this.categoryOptions[this.categoryIndex]
			return item ? item.name : '请选择分类'
		},
		selectedPayerName() {
			const item = this.activeMembers[this.payerIndex]
			return item ? (item.nickname || `成员 ${item.memberId}`) : '请选择付款人'
		},
		latestDateLabel() {
			const first = this.bills[0]
			if (!first || !first.billTime) {
				return '今日'
			}
			const date = new Date(first.billTime)
			return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
		},
		summary() {
			return this.bills.reduce((acc, bill) => {
				if (bill.billType === 'PERSONAL_INCOME') {
					acc.income += Number(bill.billAmountCent || 0)
				} else {
					acc.expense += Number(bill.billAmountCent || 0)
				}
				return acc
			}, { income: 0, expense: 0 })
		}
	},
	onShow() {
		this.refreshAll()
	},
	methods: {
		resolveCurrentBook() {
			const booksStore = useBooksStore(pinia)
			this.bookId = booksStore.currentBookId || (booksStore.currentBook && booksStore.currentBook.bookId) || null
		},
		async refreshAll() {
			try {
				const booksStore = useBooksStore(pinia)
				if (!booksStore.bookList.length) {
					booksStore.setBookList(await getMyBooks())
				}
				this.resolveCurrentBook()
				if (!this.bookId) return
				this.loading = true
				const [detail, categories, page] = await Promise.all([
					getBookDetail(this.bookId),
					getBookCategories(this.bookId),
					getBills(this.bookId, { pageNo: 1, pageSize: 30 })
				])
				this.bookDetail = detail
				this.categories = Array.isArray(categories) ? categories : []
				this.bills = (page && page.list) || []
				this.applyDefaultSelections()
				uni.setNavigationBarTitle({ title: detail.name || '账单' })
			} catch (error) {
				uni.showToast({ title: error.message || '加载失败', icon: 'none' })
			} finally {
				this.loading = false
			}
		},
		applyDefaultSelections() {
			if (!this.form.payerMemberId) {
				this.form.payerMemberId = (this.bookDetail && this.bookDetail.currentMemberId) || (this.activeMembers[0] && this.activeMembers[0].memberId)
			}
			const category = this.categoryOptions[0]
			if (!this.form.categoryId || !this.categoryOptions.some((item) => item.categoryId === this.form.categoryId)) {
				this.form.categoryId = category && category.categoryId
			}
		},
		setBillType(type) {
			this.form.billType = type
			this.form.categoryId = null
			this.applyDefaultSelections()
		},
		handleCategoryChange(event) {
			const category = this.categoryOptions[Number(event.detail.value)]
			this.form.categoryId = category && category.categoryId
		},
		handlePayerChange(event) {
			const member = this.activeMembers[Number(event.detail.value)]
			this.form.payerMemberId = member && member.memberId
		},
		async handleCreateBill() {
			const amountCent = Math.round(Number(this.form.amount) * 100)
			if (!this.form.title.trim() || !amountCent || amountCent <= 0) {
				uni.showToast({ title: '请填写标题和金额', icon: 'none' })
				return
			}
			this.creating = true
			try {
				const payload = {
					title: this.form.title.trim(),
					billAmountCent: amountCent,
					categoryId: this.form.categoryId,
					payerMemberId: this.form.payerMemberId,
					recorderMemberId: this.bookDetail.currentMemberId,
					billTime: new Date().toISOString().slice(0, 19),
					remark: this.form.remark || '',
					attachmentUrls: []
				}
				if (this.form.billType === 'SHARED_EXPENSE') {
					await createSharedExpenseBill(this.bookId, {
						...payload,
						shareItems: this.activeMembers.map((member) => ({
							participantType: 'MEMBER',
							participantRefId: member.memberId,
							shareMethod: 'AVERAGE',
							shareAmountCent: null,
							shareRatio: null
						}))
					})
				} else {
					await createPersonalBill(this.bookId, { ...payload, billType: this.form.billType })
				}
				this.form = createForm()
				this.createVisible = false
				await this.refreshAll()
				uni.showToast({ title: '账单已保存', icon: 'success' })
			} catch (error) {
				uni.showToast({ title: error.message || '保存失败', icon: 'none' })
			} finally {
				this.creating = false
			}
		},
		resolveCategoryIcon() {
			return '▣'
		},
		formatPlainMoney(value) {
			return (Number(value || 0) / 100).toFixed(2)
		},
		formatTime(value) {
			if (!value) return '-'
			return String(value).replace('T', ' ').slice(11, 16)
		}
	}
}
</script>

<style lang="scss">
.bill-page {
	padding-top: 18px;
}

.day-summary {
	margin-bottom: 16px;
}

.day-title {
	font-size: 20px;
	font-weight: 800;
	color: var(--app-text-color);
}

.day-line {
	display: flex;
	gap: 12px;
	margin-top: 8px;
	font-size: 14px;
}

.income { color: #21b96b; }
.expense { color: var(--app-color-primary); }
.balance { color: var(--app-text-secondary); }

.bill-list {
	display: flex;
	flex-direction: column;
	gap: 12px;
}

.bill-card {
	display: flex;
	gap: 12px;
	padding: 16px 14px;
	background: #fbf7ff;
	border: 1px solid #f0e9fb;
	border-radius: 8px;
	box-shadow: var(--app-card-shadow);
}

.bill-icon {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 54px;
	height: 54px;
	flex-shrink: 0;
	border-radius: 50%;
	background: #eef1f6;
	color: #4b5565;
	font-size: 22px;
}

.bill-main {
	flex: 1;
	min-width: 0;
}

.bill-title {
	font-size: 18px;
	font-weight: 800;
	color: var(--app-text-color);
}

.bill-meta,
.bill-time,
.bill-small {
	margin-top: 6px;
	font-size: 14px;
	color: var(--app-text-secondary);
}

.bill-side {
	min-width: 126px;
	text-align: right;
}

.bill-amount {
	font-size: 22px;
	font-weight: 800;
	color: var(--app-color-primary);
}

.bill-amount.positive {
	color: #21b96b;
}

.status-pill {
	display: inline-block;
	margin-top: 8px;
	padding: 3px 6px;
	border-radius: 3px;
	background: #7c3aed;
	color: #ffffff;
	font-size: 12px;
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

.create-panel {
	padding: 18px 16px 24px;
}

.panel-title {
	font-size: 18px;
	font-weight: 800;
	color: var(--app-text-color);
}

.mode-row {
	display: flex;
	gap: 8px;
	margin: 12px 0;
}

.picker-line {
	display: flex;
	align-items: center;
	justify-content: space-between;
	min-height: 46px;
	padding: 0 12px;
	margin-top: 8px;
	background: var(--app-card-bg);
	border: 1px solid var(--app-border-color);
	border-radius: var(--app-card-radius);
}

.picker-label {
	color: var(--app-text-secondary);
	font-size: 14px;
}

.picker-value {
	min-width: 160px;
	color: var(--app-text-color);
	text-align: right;
}
</style>
