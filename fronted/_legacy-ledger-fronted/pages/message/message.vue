<template>
	<view class="page-shell">
		<view class="page-block">
			<view class="section-header">
				<view>
					<view class="page-title">消息</view>
					<view class="page-subtitle">邀请、审批和待办集中在这里处理。</view>
				</view>
				<wd-button size="small" type="primary" plain @click="loadMessages">刷新</wd-button>
			</view>
		</view>

		<view class="page-block">
			<view class="section-title">收到的邀请</view>
			<view v-if="!receivedInvitations.length" class="empty-hint">暂无收到的邀请。</view>
			<view v-else class="message-list">
				<view v-for="item in receivedInvitations" :key="`received-${item.invitationId}`" class="message-row">
					<view class="message-main">
						<view class="message-title">{{ item.bookName }}</view>
						<view class="message-meta">{{ item.inviterNickname || '成员' }} 邀请你加入 · {{ resolveStatusLabel(item.status) }}</view>
					</view>
					<view v-if="item.status === 'PENDING'" class="message-actions">
						<wd-button size="small" type="primary" @click="handleAccept(item)">接受</wd-button>
						<wd-button size="small" plain @click="handleReject(item)">拒绝</wd-button>
					</view>
				</view>
			</view>
		</view>

		<view class="page-block">
			<view class="section-title">发出的邀请</view>
			<view v-if="!sentInvitations.length" class="empty-hint">暂无发出的邀请。</view>
			<view v-else class="message-list">
				<view v-for="item in sentInvitations" :key="`sent-${item.invitationId}`" class="message-row">
					<view class="message-main">
						<view class="message-title">{{ item.bookName }}</view>
						<view class="message-meta">邀请 {{ item.inviteeNickname || '成员' }} · {{ resolveStatusLabel(item.status) }}</view>
					</view>
					<wd-button v-if="item.canRevoke" size="small" plain @click="handleRevoke(item)">撤回</wd-button>
				</view>
			</view>
		</view>

		<view class="page-block">
			<view class="section-title">待审批账单申请</view>
			<view v-if="!pendingBillRequests.length" class="empty-hint">暂无待审批申请。</view>
			<view v-else class="message-list">
				<view v-for="item in pendingBillRequests" :key="`pending-${item.requestId}`" class="message-row">
					<view class="message-main">
						<view class="message-title">{{ item.snapshot && item.snapshot.title || `申请 ${item.requestId}` }}</view>
						<view class="message-meta">{{ resolveRequestTypeLabel(item.requestType) }} · {{ resolveStatusLabel(item.status) }}</view>
					</view>
					<view class="message-actions">
						<wd-button size="small" type="primary" @click="handleApproveRequest(item)">通过</wd-button>
						<wd-button size="small" plain @click="handleRejectRequest(item)">驳回</wd-button>
					</view>
				</view>
			</view>
		</view>

		<view class="page-block">
			<view class="section-title">我的账单申请</view>
			<view v-if="!myBillRequests.length" class="empty-hint">暂无发起的账单申请。</view>
			<view v-else class="message-list">
				<view v-for="item in myBillRequests" :key="`my-${item.requestId}`" class="message-row">
					<view class="message-main">
						<view class="message-title">{{ item.snapshot && item.snapshot.title || `申请 ${item.requestId}` }}</view>
						<view class="message-meta">{{ resolveRequestTypeLabel(item.requestType) }} · {{ resolveStatusLabel(item.status) }}</view>
					</view>
					<wd-button v-if="item.currentUserCanCancel" size="small" plain @click="handleCancelRequest(item)">取消</wd-button>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { pinia } from '@/stores'
import { useBooksStore } from '@/stores/modules/books'
import { getMyBooks } from '@/api/modules/book'
import {
	acceptInvitation,
	getReceivedInvitations,
	getSentInvitations,
	rejectInvitation,
	revokeInvitation
} from '@/api/modules/book-invitation'
import {
	approveBillRequest,
	cancelBillRequest,
	getMyBillRequests,
	getPendingApproveBillRequests,
	rejectBillRequest
} from '@/api/modules/bill-request'

export default {
	data() {
		return {
			bookId: null,
			receivedInvitations: [],
			sentInvitations: [],
			pendingBillRequests: [],
			myBillRequests: []
		}
	},
	onShow() {
		this.loadMessages()
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
		async loadMessages() {
			try {
				await this.ensureBooks()
				const [received, sent] = await Promise.all([
					getReceivedInvitations(),
					getSentInvitations()
				])
				this.receivedInvitations = Array.isArray(received) ? received : []
				this.sentInvitations = Array.isArray(sent) ? sent : []
				if (this.bookId) {
					const [pending, mine] = await Promise.all([
						getPendingApproveBillRequests(this.bookId),
						getMyBillRequests(this.bookId)
					])
					this.pendingBillRequests = Array.isArray(pending) ? pending : []
					this.myBillRequests = Array.isArray(mine) ? mine : []
				}
			} catch (error) {
				uni.showToast({ title: error.message || '加载消息失败', icon: 'none' })
			}
		},
		async handleAccept(item) {
			await this.runAction(() => acceptInvitation(item.invitationId), '已接受邀请')
			const booksStore = useBooksStore(pinia)
			booksStore.setBookList(await getMyBooks())
		},
		async handleReject(item) {
			await this.runAction(() => rejectInvitation(item.invitationId), '已拒绝邀请')
		},
		async handleRevoke(item) {
			await this.runAction(() => revokeInvitation(item.invitationId), '邀请已撤回')
		},
		async handleApproveRequest(item) {
			await this.runAction(() => approveBillRequest(this.bookId, item.requestId, {}), '申请已通过')
		},
		async handleRejectRequest(item) {
			await this.runAction(() => rejectBillRequest(this.bookId, item.requestId, {}), '申请已驳回')
		},
		async handleCancelRequest(item) {
			await this.runAction(() => cancelBillRequest(this.bookId, item.requestId), '申请已取消')
		},
		async runAction(action, successText) {
			try {
				await action()
				await this.loadMessages()
				uni.showToast({ title: successText, icon: 'success' })
			} catch (error) {
				uni.showToast({ title: error.message || '操作失败', icon: 'none' })
			}
		},
		resolveStatusLabel(status) {
			const map = {
				PENDING: '待处理',
				ACCEPTED: '已接受',
				REJECTED: '已拒绝',
				EXPIRED: '已过期',
				APPROVED: '已通过',
				CANCELLED: '已取消'
			}
			return map[status] || status || '-'
		},
		resolveRequestTypeLabel(type) {
			if (type === 'DELETE') return '删除申请'
			if (type === 'MODIFY') return '修改申请'
			return '账单申请'
		}
	}
}
</script>

<style lang="scss">
.message-list {
	display: flex;
	flex-direction: column;
	gap: 10px;
}

.message-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12px;
	padding: 12px;
	background: var(--app-card-bg);
	border: 1px solid var(--app-border-color);
	border-radius: var(--app-card-radius);
}

.message-main {
	min-width: 0;
}

.message-title {
	overflow: hidden;
	font-size: 14px;
	font-weight: 600;
	color: var(--app-text-color);
	text-overflow: ellipsis;
	white-space: nowrap;
}

.message-meta {
	margin-top: 4px;
	font-size: 12px;
	color: var(--app-text-secondary);
}

.message-actions {
	display: flex;
	flex-shrink: 0;
	gap: 8px;
}
</style>
