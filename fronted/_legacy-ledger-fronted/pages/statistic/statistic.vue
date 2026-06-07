<template>
	<view class="page-shell">
		<view class="page-block">
			<view class="section-header">
				<view>
					<view class="page-title">实时统计</view>
					<view class="page-subtitle">{{ currentBookName }}</view>
				</view>
				<wd-button size="small" type="primary" plain @click="loadDashboard">刷新</wd-button>
			</view>
			<view v-if="!bookId" class="empty-hint">请先创建或选择账本。</view>
		</view>

		<view v-if="bookId" class="metric-grid">
			<view v-for="item in metrics" :key="item.label" class="metric-cell">
				<view class="metric-label">{{ item.label }}</view>
				<view class="metric-value">{{ item.value }}</view>
			</view>
		</view>

		<view v-if="bookId" class="page-block">
			<view class="section-title">分类消费</view>
			<view v-if="!categoryRows.length" class="empty-hint">暂无分类消费数据。</view>
			<view v-else class="data-list">
				<view v-for="item in categoryRows" :key="item.categoryId || item.categoryName" class="data-row">
					<view>
						<view class="row-title">{{ item.categoryName || '未分类' }}</view>
						<view class="row-meta">{{ item.categoryType === 'INCOME' ? '收入' : '支出' }}</view>
					</view>
					<view class="row-amount">{{ formatMoney(item.consumptionAmountCent) }}</view>
				</view>
			</view>
		</view>

		<view v-if="bookId" class="page-block">
			<view class="section-title">成员关系</view>
			<view v-if="!relations.length" class="empty-hint">暂无成员关系数据。</view>
			<view v-else class="data-list">
				<view v-for="item in relations" :key="`${item.targetParticipantType}-${item.targetParticipantId}`" class="data-row">
					<view>
						<view class="row-title">{{ item.targetMemberName || '成员' }}</view>
						<view class="row-meta">{{ resolveNetText(item) }}</view>
					</view>
					<view class="row-amount">{{ formatMoney(item.netAmountCent) }}</view>
				</view>
			</view>
		</view>

		<view v-if="bookId" class="page-block">
			<view class="section-header">
				<view>
					<view class="section-title">结算建议</view>
					<view class="section-subtitle">按后端结算策略生成当前账本的建议转账。</view>
				</view>
				<wd-button size="small" type="primary" :loading="settling" @click="handleCreateSettlement">生成</wd-button>
			</view>
			<view v-if="!settlementTransfers.length" class="empty-hint">还没有结算建议。</view>
			<view v-else class="data-list">
				<view v-for="item in settlementTransfers" :key="item.transferId || `${item.fromMemberId}-${item.toMemberId}`" class="data-row">
					<view>
						<view class="row-title">{{ item.fromMemberName }} → {{ item.toMemberName }}</view>
						<view class="row-meta">建议转账</view>
					</view>
					<view class="row-amount">{{ formatMoney(item.transferAmountCent) }}</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { pinia } from '@/stores'
import { useBooksStore } from '@/stores/modules/books'
import { getMyBooks } from '@/api/modules/book'
import { getCategoryConsumption, getMemberRelations, getStatisticsOverview } from '@/api/modules/statistics'
import { createSettlement } from '@/api/modules/settlement'

export default {
	data() {
		return {
			bookId: null,
			overview: {},
			categoryRows: [],
			relations: [],
			settlementTransfers: [],
			settling: false
		}
	},
	computed: {
		currentBookName() {
			const book = useBooksStore(pinia).currentBook
			return book ? `当前账本：${book.name}` : '当前还没有选中的账本'
		},
		metrics() {
			return [
				{ label: '付款流水', value: this.formatMoney(this.overview.payFlowAmountCent) },
				{ label: '应计消费', value: this.formatMoney(this.overview.accruedConsumptionAmountCent) },
				{ label: '待贡献', value: this.formatMoney(this.overview.pendingContributionAmountCent) },
				{ label: '可收回', value: this.formatMoney(this.overview.receivableAmountCent) },
				{ label: '个人收入', value: this.formatMoney(this.overview.personalIncomeAmountCent) },
				{ label: '预算使用', value: `${Number(this.overview.budgetUsagePercent || 0).toFixed(1)}%` }
			]
		}
	},
	onShow() {
		this.loadDashboard()
	},
	methods: {
		resolveCurrentBook() {
			const booksStore = useBooksStore(pinia)
			this.bookId = booksStore.currentBookId || (booksStore.currentBook && booksStore.currentBook.bookId) || null
		},
		async ensureBooks() {
			const booksStore = useBooksStore(pinia)
			if (!booksStore.bookList.length) {
				booksStore.setBookList(await getMyBooks())
			}
			this.resolveCurrentBook()
		},
		async loadDashboard() {
			try {
				await this.ensureBooks()
				if (!this.bookId) {
					return
				}
				const [overview, categoryRows, relations] = await Promise.all([
					getStatisticsOverview(this.bookId),
					getCategoryConsumption(this.bookId),
					getMemberRelations(this.bookId)
				])
				this.overview = overview || {}
				this.categoryRows = Array.isArray(categoryRows) ? categoryRows : []
				this.relations = Array.isArray(relations) ? relations : []
			} catch (error) {
				uni.showToast({ title: error.message || '加载统计失败', icon: 'none' })
			}
		},
		async handleCreateSettlement() {
			this.settling = true
			try {
				const result = await createSettlement(this.bookId, {
					strategyType: 'MIN_TRANSFER_COUNT'
				})
				this.settlementTransfers = (result && result.transferList) || []
				uni.showToast({ title: '结算已生成', icon: 'success' })
			} catch (error) {
				uni.showToast({ title: error.message || '生成失败', icon: 'none' })
			} finally {
				this.settling = false
			}
		},
		resolveNetText(item) {
			if (item.netDirection === 'I_OWE_TARGET') {
				return '我需要支付给对方'
			}
			if (item.netDirection === 'TARGET_OWES_ME') {
				return '对方需要支付给我'
			}
			return '已结清'
		},
		formatMoney(value) {
			return `¥${(Number(value || 0) / 100).toFixed(2)}`
		}
	}
}
</script>

<style lang="scss">
.metric-grid {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 10px;
	margin: 0 12px 12px;
}

.metric-cell {
	padding: 12px;
	background: var(--app-card-bg);
	border: 1px solid var(--app-border-color);
	border-radius: var(--app-card-radius);
}

.metric-label,
.row-meta {
	font-size: 12px;
	color: var(--app-text-secondary);
}

.metric-value {
	margin-top: 6px;
	font-size: 18px;
	font-weight: 700;
	color: var(--app-text-color);
}

.data-list {
	display: flex;
	flex-direction: column;
	gap: 10px;
}

.data-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12px;
	padding: 12px;
	background: var(--app-card-bg);
	border: 1px solid var(--app-border-color);
	border-radius: var(--app-card-radius);
}

.row-title {
	font-size: 14px;
	font-weight: 600;
	color: var(--app-text-color);
}

.row-amount {
	font-size: 15px;
	font-weight: 700;
	color: var(--app-color-primary);
}
</style>
