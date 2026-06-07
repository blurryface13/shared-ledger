<template>
	<view class="page-shell">
		<wd-notify :visible="notify.visible" :type="notify.type" :message="notify.message" :duration="2200" root-portal
			@update:visible="handleNotifyVisibleChange" />

		<view class="page-block detail-header">
			<view class="detail-header-main">
				<view class="page-title">{{ detail.name || '账本详情' }}（{{ bookTypeLabel }}）</view>
				<view class="page-subtitle">{{ headerSubtitle }}</view>
			</view>
			<view class="detail-header-actions">
				<wd-button size="small" type="primary" plain @click="handleRefreshDetail">
					刷新详情
				</wd-button>
				<wd-button size="small" type="primary" :plain="isCurrentBook" custom-style="margin-top: 8px;"
					@click="handleSetCurrentBook">
					{{ isCurrentBook ? '当前选中的账本' : '设为当前账本' }}
				</wd-button>
			</view>
		</view>

		<book-form-panel :form-data="formData" :book-type-label="bookTypeLabel" :editable="detail.canEditBook"
			:show-book-type-cell="false" :cover-preview-url="resolvedCoverUrl" @update:formData="handleFormChange" />

		<view class="page-block">
			<view class="section-title">所有者</view>
			<view class="member-list">
				<book-member-card v-for="member in ownerMembers" :key="`owner-${member.memberId}`" :member="member"
					:role-label="resolveRoleLabel(member.memberRole)"
					:role-tag-type="resolveRoleTagType(member.memberRole)"
					:status-label="resolveStatusLabel(member.memberStatus)"
					:status-tag-type="resolveStatusTagType(member.memberStatus)">
					<view v-if="getTempParticipantsForMember(member.memberId).length" class="temp-section">
						<wd-collapse :model-value="getTempCollapseValue(member.memberId)"
							@change="handleTempCollapseChange(member.memberId, $event)">
							<wd-collapse-item name="temp"
								:title="`挂靠临时成员（${getTempParticipantsForMember(member.memberId).length}）`">
								<view class="temp-list">
									<view v-for="item in getTempParticipantsForMember(member.memberId)"
										:key="item.tempParticipantId" class="temp-item">
										<wd-avatar :text="resolveTempAvatarText(item.nickname)" size="normal"
											shape="round" bg-color="var(--app-color-primary)" />
										<view class="temp-item-content">
											<view class="temp-item-name">{{ item.nickname }}</view>
											<view class="temp-item-meta">临时成员</view>
										</view>
									</view>
								</view>
							</wd-collapse-item>
						</wd-collapse>
					</view>
				</book-member-card>
			</view>
		</view>

		<view v-if="isSharedBook && adminMembers.length" class="page-block">
			<view class="section-title">管理员</view>
			<view class="member-list">
				<book-member-card v-for="member in adminMembers" :key="`admin-${member.memberId}`" :member="member"
					:role-label="resolveRoleLabel(member.memberRole)"
					:role-tag-type="resolveRoleTagType(member.memberRole)"
					:status-label="resolveStatusLabel(member.memberStatus)"
					:status-tag-type="resolveStatusTagType(member.memberStatus)">
					<view v-if="getTempParticipantsForMember(member.memberId).length" class="temp-section">
						<wd-collapse :model-value="getTempCollapseValue(member.memberId)"
							@change="handleTempCollapseChange(member.memberId, $event)">
							<wd-collapse-item name="temp"
								:title="`挂靠临时成员（${getTempParticipantsForMember(member.memberId).length}）`">
								<view class="temp-list">
									<view v-for="item in getTempParticipantsForMember(member.memberId)"
										:key="item.tempParticipantId" class="temp-item">
										<wd-avatar :text="resolveTempAvatarText(item.nickname)" size="normal"
											shape="round" bg-color="var(--app-color-primary)" />
										<view class="temp-item-content">
											<view class="temp-item-name">{{ item.nickname }}</view>
											<view class="temp-item-meta">临时成员</view>
										</view>
									</view>
								</view>
							</wd-collapse-item>
						</wd-collapse>
					</view>
				</book-member-card>
			</view>
		</view>

		<view v-if="isSharedBook" class="page-block">
			<view class="section-header">
				<view>
					<view class="section-title">账本成员</view>
					<view class="section-subtitle">正式成员会按权限和状态分组展示，挂靠临时成员默认展开。</view>
				</view>
				<view class="section-actions">
					<wd-button v-if="detail.canInviteMember" size="small" type="primary" plain
						custom-style="margin-bottom: 8px;" @click="handleOpenInvitePopup">
						邀请成员
					</wd-button>
					<wd-button v-if="detail.canRemoveMember" size="small" type="primary" plain
						@click="handleOpenRemovePopup">
						移除成员
					</wd-button>
				</view>
			</view>

			<view v-if="activeMembers.length" class="member-list">
				<book-member-card v-for="member in activeMembers" :key="`member-${member.memberId}`" :member="member"
					:role-label="resolveRoleLabel(member.memberRole)"
					:role-tag-type="resolveRoleTagType(member.memberRole)"
					:status-label="resolveStatusLabel(member.memberStatus)"
					:status-tag-type="resolveStatusTagType(member.memberStatus)">
					<view v-if="getTempParticipantsForMember(member.memberId).length" class="temp-section">
						<wd-collapse :model-value="getTempCollapseValue(member.memberId)"
							@change="handleTempCollapseChange(member.memberId, $event)">
							<wd-collapse-item name="temp"
								:title="`挂靠临时成员（${getTempParticipantsForMember(member.memberId).length}）`">
								<view class="temp-list">
									<view v-for="item in getTempParticipantsForMember(member.memberId)"
										:key="item.tempParticipantId" class="temp-item">
										<wd-avatar :text="resolveTempAvatarText(item.nickname)" size="normal"
											shape="round" bg-color="var(--app-color-primary)" />
										<view class="temp-item-content">
											<view class="temp-item-name">{{ item.nickname }}</view>
											<view class="temp-item-meta">可见范围：{{ item.visibleScope }}</view>
										</view>
									</view>
								</view>
							</wd-collapse-item>
						</wd-collapse>
					</view>
				</book-member-card>
			</view>
			<view v-else class="empty-hint">当前共享账本还没有普通成员。</view>
		</view>

		<view v-if="isSharedBook && historyMembers.length" class="page-block">
			<view class="section-title">已退出 / 已移除成员</view>
			<view class="member-list">
				<book-member-card v-for="member in historyMembers" :key="`history-${member.memberId}`" :member="member"
					:role-label="resolveRoleLabel(member.memberRole)"
					:role-tag-type="resolveRoleTagType(member.memberRole)"
					:status-label="resolveStatusLabel(member.memberStatus)"
					:status-tag-type="resolveStatusTagType(member.memberStatus)" />
			</view>
		</view>

		<view v-if="isSharedBook" class="page-block">
			<view class="section-title">添加临时成员</view>
			<view class="section-subtitle">适合还没有微信账号或不想加入账本的同行人，费用会挂靠到一位正式成员名下。</view>
			<wd-input v-model="tempForm.nickname" label="昵称" placeholder="例如 小朋友、司机、同事A" maxlength="20" clearable />
			<view class="picker-line">
				<text class="picker-label">挂靠成员</text>
				<picker :range="tempAttachMemberNames" :value="tempAttachMemberIndex" @change="handleTempAttachMemberChange">
					<view class="picker-value">{{ selectedTempAttachMemberName }}</view>
				</picker>
			</view>
			<wd-button type="primary" block :loading="creatingTemp" custom-style="margin-top: 12px;" @click="handleCreateTempParticipant">
				添加临时成员
			</wd-button>
		</view>

		<view class="page-block action-block">
			<wd-button v-if="detail.canEditBook" type="primary" block :loading="saving" @click="handleSaveBook">
				保存
			</wd-button>
			<wd-button v-if="detail.canQuitBook" block custom-style="margin-top: 12px;" :loading="quitting"
				@click="handlePrepareQuitBook">
				退出账本
			</wd-button>
			<wd-button v-if="detail.canTransferOwner" block custom-style="margin-top: 12px;"
				@click="handleOpenTransferPopup">
				转让账本
			</wd-button>
			<wd-button v-if="detail.canDeleteBook" block custom-style="margin-top: 12px;" :loading="deleting"
				@click="handlePrepareDeleteBook">
				删除账本
			</wd-button>
		</view>

		<wd-popup v-model="invitePopupVisible" position="bottom" root-portal safe-area-inset-bottom
			:close-on-click-modal="true" custom-style="background: var(--app-card-bg); border-radius: 4px 4px 0 0;"
			@update:modelValue="handleInvitePopupVisibleChange">
			<view class="popup-panel">
				<view class="popup-title">邀请成员</view>
				<view class="popup-subtitle">支持按手机号或昵称模糊搜索，命中后可直接发起邀请。</view>

				<wd-search v-if="!activeInviteSearchType || activeInviteSearchType === 'MOBILE'"
					:model-value="inviteSearch.mobile" placeholder="根据手机号模糊搜索" hide-cancel focus-when-clear
					custom-style="margin-top: 12px;" @update:modelValue="handleInviteSearchInput('MOBILE', $event)" />
				<wd-search v-if="!activeInviteSearchType || activeInviteSearchType === 'NICKNAME'"
					:model-value="inviteSearch.nickname" placeholder="根据昵称模糊搜索" hide-cancel focus-when-clear
					custom-style="margin-top: 12px;" @update:modelValue="handleInviteSearchInput('NICKNAME', $event)" />

				<view v-if="inviteSearch.loading" class="popup-state">正在搜索候选用户...</view>
				<view v-else-if="inviteCandidates.length" class="candidate-list">
					<view v-for="item in inviteCandidates" :key="item.userId" class="candidate-row">
						<view class="candidate-main">
							<wd-avatar :src="item.avatarUrl" :text="resolveTempAvatarText(item.nickname)" size="large"
								shape="round" bg-color="var(--app-color-primary)" />
							<view class="candidate-content">
								<view class="candidate-name">{{ item.nickname || '未命名用户' }}</view>
								<view class="candidate-phone">{{ item.phoneNumber || '暂未绑定手机号' }}</view>
							</view>
						</view>
						<wd-button size="small" type="primary" :plain="item.pendingInvitation"
							:disabled="item.pendingInvitation" :loading="inviteSubmittingUserId === item.userId"
							@click="handlePrepareInvite(item)">
							{{ item.pendingInvitation ? '已邀请' : '邀请' }}
						</wd-button>
					</view>
				</view>
				<view v-else class="popup-state">{{ inviteEmptyText }}</view>
			</view>
		</wd-popup>

		<wd-popup v-model="removePopupVisible" position="bottom" root-portal safe-area-inset-bottom
			:close-on-click-modal="true" custom-style="background: var(--app-card-bg); border-radius: 4px 4px 0 0;">
			<view class="popup-panel">
				<view class="popup-title">移除成员</view>
				<view class="popup-subtitle">仅展示当前可被移除的正式成员，确认后会保留历史关系并标记为已移除。</view>

				<view v-if="removableMembers.length" class="candidate-list remove-list">
					<view v-for="member in removableMembers" :key="member.memberId" class="candidate-row">
						<view class="candidate-main">
							<wd-avatar :src="member.avatarUrl" :text="resolveTempAvatarText(member.nickname)"
								size="large" shape="round" bg-color="var(--app-color-primary)" />
							<view class="candidate-content">
								<view class="candidate-name">{{ member.nickname || '未命名用户' }}</view>
								<view class="candidate-phone">{{ member.phoneNumber || '暂未绑定手机号' }}</view>
							</view>
						</view>
						<wd-button size="small" type="primary" :loading="removeSubmittingMemberId === member.memberId"
							@click="handlePrepareRemove(member)">
							删除
						</wd-button>
					</view>
				</view>
				<view v-else class="popup-state">当前没有可移除的正式成员。</view>
			</view>
		</wd-popup>

		<wd-popup v-model="transferPopupVisible" position="bottom" root-portal safe-area-inset-bottom
			:close-on-click-modal="true" custom-style="background: var(--app-card-bg); border-radius: 4px 4px 0 0;">
			<view class="popup-panel">
				<view class="popup-title">转让账本</view>
				<view class="popup-subtitle">请选择一位正式成员接收账本所有权，确认后当前所有者会自动降为普通成员。</view>

				<view v-if="transferCandidates.length" class="candidate-list remove-list">
					<view v-for="member in transferCandidates" :key="member.memberId" class="candidate-row">
						<view class="candidate-main">
							<wd-avatar :src="member.avatarUrl" :text="resolveTempAvatarText(member.nickname)"
								size="large" shape="round" bg-color="var(--app-color-primary)" />
							<view class="candidate-content">
								<view class="candidate-name">{{ member.nickname || '未命名用户' }}</view>
								<view class="candidate-phone">{{ member.phoneNumber || '暂未绑定手机号' }}</view>
							</view>
						</view>
						<wd-button size="small" type="primary" :loading="transferSubmittingMemberId === member.memberId"
							@click="handlePrepareTransfer(member)">
							转让
						</wd-button>
					</view>
				</view>
				<view v-else class="popup-state">当前没有可接收账本的成员。</view>
			</view>
		</wd-popup>

		<wd-popup v-model="confirmDialog.visible" position="center" root-portal :close-on-click-modal="false"
			custom-style="background: var(--app-card-bg); border-radius: 4px;">
			<view class="confirm-dialog">
				<view class="confirm-title">{{ confirmDialog.title }}</view>
				<view class="confirm-message">{{ confirmDialog.message }}</view>
				<view class="confirm-actions">
					<wd-button plain @click="handleCancelConfirm">取消</wd-button>
					<wd-button type="primary" :loading="confirmDialog.loading" custom-style="margin-top: 12px;"
						@click="handleConfirmAction">
						{{ confirmDialog.confirmText }}
					</wd-button>
				</view>
			</view>
		</wd-popup>
	</view>
</template>

<script>
	import {
		pinia
	} from '@/stores'
	import {
		useBooksStore
	} from '@/stores/modules/books'
	import {
		createInvitation,
		searchInvitationCandidates
	} from '@/api/modules/book-invitation'
	import {
		quitBook,
		removeBookMember,
		transferBookOwner
	} from '@/api/modules/book-member'
	import {
		deleteBook,
		getBookDetail,
		getMyBooks,
		updateBook
	} from '@/api/modules/book'
	import {
		createTempParticipant
	} from '@/api/modules/temp-participant'
	import {
		ROUTES
	} from '@/config/app'
	import BookFormPanel from '@/components/book/book-form-panel.vue'
	import BookMemberCard from '@/components/book/book-member-card.vue'

	const DEFAULT_COVER_URL = '/static/logo.png'

	function createDefaultDetail() {
		return {
			bookId: null,
			name: '',
			bookType: 'PERSONAL',
			description: '',
			coverUrl: '',
			currentMemberId: null,
			currentMemberRole: '',
			currentMemberStatus: '',
			canEditBook: false,
			canInviteMember: false,
			canRemoveMember: false,
			canTransferOwner: false,
			canQuitBook: false,
			canDeleteBook: false,
			members: [],
			tempParticipants: []
		}
	}

	function createDefaultForm() {
		return {
			name: '',
			description: '',
			coverUrl: '',
			bookType: 'PERSONAL'
		}
	}

	export default {
		components: {
			BookFormPanel,
			BookMemberCard
		},
		data() {
			return {
				bookId: null,
				detail: createDefaultDetail(),
				formData: createDefaultForm(),
				saving: false,
				deleting: false,
				quitting: false,
				invitePopupVisible: false,
				removePopupVisible: false,
				transferPopupVisible: false,
				inviteCandidates: [],
				inviteSubmittingUserId: null,
				removeSubmittingMemberId: null,
				transferSubmittingMemberId: null,
				creatingTemp: false,
				tempForm: {
					nickname: '',
					attachedMemberId: null
				},
				inviteSearch: {
					activeType: '',
					mobile: '',
					nickname: '',
					loading: false
				},
				inviteSearchTimer: null,
				expandedTempMemberIds: [],
				notify: {
					visible: false,
					type: 'primary',
					message: ''
				},
				confirmDialog: {
					visible: false,
					title: '',
					message: '',
					confirmText: '确认',
					loading: false
				},
				pendingConfirmAction: null
			}
		},
		computed: {
			bookTypeLabel() {
				return this.detail.bookType === 'SHARED' ? '共享账本' : '个人账本'
			},
			isSharedBook() {
				return this.detail.bookType === 'SHARED'
			},
			isCurrentBook() {
				return useBooksStore(pinia).currentBookId === this.bookId
			},
			resolvedCoverUrl() {
				return this.formData.coverUrl || this.detail.coverUrl || DEFAULT_COVER_URL
			},
			headerSubtitle() {
				if (this.detail.canEditBook) {
					return '当前账号具备账本资料编辑权限，保存后会同步刷新账本列表。'
				}
				return '当前账号仅具备查看权限，账本资料字段已自动切换为只读。'
			},
			ownerMembers() {
				return this.detail.members.filter((member) => member.memberRole === 'OWNER')
			},
			adminMembers() {
				return this.detail.members.filter((member) => member.memberRole === 'ADMIN' && member.memberStatus ===
					'ACTIVE')
			},
			activeMembers() {
				return this.detail.members.filter((member) => member.memberRole === 'MEMBER' && member.memberStatus ===
					'ACTIVE')
			},
			historyMembers() {
				return this.detail.members.filter((member) => member.memberStatus !== 'ACTIVE')
			},
			removableMembers() {
				return this.detail.members.filter((member) => {
					return member.memberStatus === 'ACTIVE' &&
						member.memberRole !== 'OWNER' &&
						member.memberId !== this.detail.currentMemberId
				})
			},
			transferCandidates() {
				return this.detail.members.filter((member) => {
					return member.memberStatus === 'ACTIVE' &&
						member.memberRole !== 'OWNER'
				})
			},
			tempAttachMembers() {
				return this.detail.members.filter((member) => member.memberStatus === 'ACTIVE')
			},
			tempAttachMemberNames() {
				return this.tempAttachMembers.map((member) => member.nickname || `成员 ${member.memberId}`)
			},
			tempAttachMemberIndex() {
				return Math.max(0, this.tempAttachMembers.findIndex((member) => member.memberId === this.tempForm.attachedMemberId))
			},
			selectedTempAttachMemberName() {
				const member = this.tempAttachMembers[this.tempAttachMemberIndex]
				return member ? (member.nickname || `成员 ${member.memberId}`) : '请选择成员'
			},
			activeInviteSearchType() {
				return this.inviteSearch.activeType
			},
			inviteEmptyText() {
				if (this.inviteSearch.loading) {
					return '正在搜索候选用户...'
				}
				if (!this.activeInviteSearchType) {
					return '请输入手机号或昵称开始搜索。'
				}
				return '没有找到可邀请的候选用户。'
			}
		},
		onLoad(options) {
			const booksStore = useBooksStore(pinia)
			const bookId = options && options.bookId ? Number(options.bookId) : Number(booksStore.currentBookId)
			this.bookId = Number.isFinite(bookId) && bookId > 0 ? bookId : null
		},
		onShow() {
			this.loadBookDetail().catch(() => {})
		},
		onUnload() {
			if (this.inviteSearchTimer) {
				clearTimeout(this.inviteSearchTimer)
				this.inviteSearchTimer = null
			}
		},
		methods: {
			// 账本详情页只信任后端权限字段，前端只负责渲染和交互串联。
			async loadBookDetail() {
				if (!this.bookId) {
					this.showNotify('当前没有可查看的账本', 'warning')
					return
				}

				try {
					const detail = await getBookDetail(this.bookId)
					this.applyBookDetail(detail)
				} catch (error) {
					this.showNotify(error.message || '加载账本详情失败', 'danger')
					throw error
				}
			},
			applyBookDetail(detail) {
				this.detail = {
					...createDefaultDetail(),
					...(detail || {})
				}
				this.formData = {
					name: this.detail.name || '',
					description: this.detail.description || '',
					coverUrl: this.detail.coverUrl || '',
					bookType: this.detail.bookType || 'PERSONAL'
				}
				this.expandedTempMemberIds = this.resolveDefaultExpandedTempIds()
				if (!this.tempForm.attachedMemberId && this.detail.currentMemberId) {
					this.tempForm.attachedMemberId = this.detail.currentMemberId
				}
				uni.setNavigationBarTitle({
					title: `${this.detail.name || '账本详情'}`
				})
			},
			resolveDefaultExpandedTempIds() {
				const attachedIds = new Set(
					(this.detail.tempParticipants || [])
					.map((item) => `${item.attachedMemberId}`)
				)
				return Array.from(attachedIds)
			},
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
			formatDateTime(value) {
				if (!value) {
					return '-'
				}
				const date = new Date(value)
				const year = date.getFullYear()
				const month = `${date.getMonth() + 1}`.padStart(2, '0')
				const day = `${date.getDate()}`.padStart(2, '0')
				const hour = `${date.getHours()}`.padStart(2, '0')
				const minute = `${date.getMinutes()}`.padStart(2, '0')
				return `${year}-${month}-${day} ${hour}:${minute}`
			},
			resolveRoleLabel(role) {
				if (role === 'OWNER') {
					return '所有者'
				}
				if (role === 'ADMIN') {
					return '管理员'
				}
				return '成员'
			},
			resolveRoleTagType(role) {
				if (role === 'OWNER') {
					return 'danger'
				}
				if (role === 'ADMIN') {
					return 'warning'
				}
				return 'primary'
			},
			resolveStatusLabel(status) {
				if (status === 'ACTIVE') {
					return '有效成员'
				}
				if (status === 'QUIT') {
					return '已退出'
				}
				if (status === 'REMOVED') {
					return '已移除'
				}
				return status || ''
			},
			resolveStatusTagType(status) {
				if (status === 'ACTIVE') {
					return 'success'
				}
				return 'warning'
			},
			resolveTempAvatarText(nickname) {
				return nickname ? nickname.slice(0, 1) : '临'
			},
			getTempParticipantsForMember(memberId) {
				return (this.detail.tempParticipants || []).filter((item) => item.attachedMemberId === memberId)
			},
			getTempCollapseValue(memberId) {
				return this.expandedTempMemberIds.includes(`${memberId}`) ? ['temp'] : []
			},
			handleTempCollapseChange(memberId, event) {
				const expanded = Array.isArray(event && event.value) && event.value.includes('temp')
				const memberKey = `${memberId}`
				if (expanded && !this.expandedTempMemberIds.includes(memberKey)) {
					this.expandedTempMemberIds = this.expandedTempMemberIds.concat(memberKey)
					return
				}
				if (!expanded) {
					this.expandedTempMemberIds = this.expandedTempMemberIds.filter((item) => item !== memberKey)
				}
			},
			handleTempAttachMemberChange(event) {
				const member = this.tempAttachMembers[Number(event.detail.value)]
				this.tempForm.attachedMemberId = member && member.memberId
			},
			async handleCreateTempParticipant() {
				const nickname = (this.tempForm.nickname || '').trim()
				const attachedMemberId = this.tempForm.attachedMemberId || (this.tempAttachMembers[0] && this.tempAttachMembers[0].memberId)
				if (!nickname || !attachedMemberId) {
					this.showNotify('请填写昵称并选择挂靠成员', 'warning')
					return
				}
				this.creatingTemp = true
				try {
					await createTempParticipant(this.bookId, {
						nickname,
						tempType: 'GLOBAL',
						attachedMemberId
					})
					this.tempForm.nickname = ''
					await this.loadBookDetail()
					this.showNotify('临时成员已添加', 'success')
				} catch (error) {
					this.showNotify(error.message || '添加失败', 'danger')
				} finally {
					this.creatingTemp = false
				}
			},
			async refreshBookList() {
				const bookList = await getMyBooks()
				useBooksStore(pinia).setBookList(bookList)
			},
			handleSetCurrentBook() {
				useBooksStore(pinia).setCurrentBookId(this.bookId)
				this.showNotify('当前账本已切换', 'success')
			},
			async handleRefreshDetail() {
				try {
					await this.loadBookDetail()
					await this.refreshBookList()
					this.showNotify('账本详情已刷新', 'success')
				} catch (error) {
					this.showNotify(error.message || '刷新失败', 'danger')
				}
			},
			async handleSaveBook() {
				if (!this.detail.canEditBook) {
					return
				}
				if (!this.formData.name.trim()) {
					this.showNotify('请先填写账本名称', 'warning')
					return
				}

				this.saving = true
				try {
					await updateBook(this.bookId, {
						name: this.formData.name.trim(),
						description: this.formData.description.trim(),
						coverUrl: this.formData.coverUrl || null
					})
					await this.refreshBookList()
					await this.loadBookDetail()
					this.showNotify('账本信息已保存', 'success')
				} catch (error) {
					this.showNotify(error.message || '保存失败', 'danger')
				} finally {
					this.saving = false
				}
			},
			handlePrepareDeleteBook() {
				this.openConfirmDialog({
					title: '删除账本',
					message: '删除后当前个人账本将从账本列表中移除，是否继续？',
					confirmText: '确认删除',
					action: async () => {
						this.deleting = true
						try {
							await deleteBook(this.bookId)
							await this.refreshBookList()
							this.showNotify('账本已删除', 'success')
							setTimeout(() => {
								uni.switchTab({
									url: ROUTES.HOME
								})
							}, 400)
						} finally {
							this.deleting = false
						}
					}
				})
			},
			handlePrepareQuitBook() {
				this.openConfirmDialog({
					title: '退出账本',
					message: '退出后该共享账本将不再出现在你的账本列表中，是否确认退出？',
					confirmText: '确认退出',
					action: async () => {
						this.quitting = true
						try {
							await quitBook(this.bookId)
							await this.refreshBookList()
							this.showNotify('你已退出当前账本', 'success')
							setTimeout(() => {
								uni.switchTab({
									url: ROUTES.HOME
								})
							}, 400)
						} finally {
							this.quitting = false
						}
					}
				})
			},
			handleOpenInvitePopup() {
				this.invitePopupVisible = true
				this.inviteCandidates = []
			},
			handleInvitePopupVisibleChange(visible) {
				this.invitePopupVisible = visible
				if (!visible) {
					this.resetInvitePopupState()
				}
			},
			handleOpenRemovePopup() {
				this.removePopupVisible = true
			},
			handleOpenTransferPopup() {
				this.transferPopupVisible = true
			},
			// 搜索输入变化后通过轻量防抖更新候选列表，避免每个字符都立刻打满请求。
			handleInviteSearchInput(searchType, value) {
				if (searchType === 'MOBILE') {
					this.inviteSearch.mobile = value || ''
					if (value) {
						this.inviteSearch.activeType = 'MOBILE'
						this.inviteSearch.nickname = ''
					} else if (!this.inviteSearch.nickname) {
						this.inviteSearch.activeType = ''
					}
				}
				if (searchType === 'NICKNAME') {
					this.inviteSearch.nickname = value || ''
					if (value) {
						this.inviteSearch.activeType = 'NICKNAME'
						this.inviteSearch.mobile = ''
					} else if (!this.inviteSearch.mobile) {
						this.inviteSearch.activeType = ''
					}
				}

				if (this.inviteSearchTimer) {
					clearTimeout(this.inviteSearchTimer)
				}

				const keyword = searchType === 'MOBILE' ? this.inviteSearch.mobile : this.inviteSearch.nickname
				if (!this.inviteSearch.activeType || !keyword.trim()) {
					this.inviteCandidates = []
					this.inviteSearch.loading = false
					return
				}

				this.inviteSearch.loading = true
				this.inviteSearchTimer = setTimeout(() => {
					this.fetchInviteCandidates(this.inviteSearch.activeType, keyword.trim())
				}, 280)
			},
			async fetchInviteCandidates(searchType, keyword) {
				try {
					const candidates = await searchInvitationCandidates(this.bookId, {
						searchType,
						keyword
					})
					this.inviteCandidates = Array.isArray(candidates) ? candidates : []
				} catch (error) {
					this.inviteCandidates = []
					this.showNotify(error.message || '搜索候选用户失败', 'danger')
				} finally {
					this.inviteSearch.loading = false
				}
			},
			handlePrepareInvite(candidate) {
				this.openConfirmDialog({
					title: '确认邀请成员',
					message: `确定邀请 ${candidate.nickname || '该用户'} 加入当前账本吗？`,
					confirmText: '确认邀请',
					action: async () => {
						this.inviteSubmittingUserId = candidate.userId
						try {
							await createInvitation(this.bookId, {
								inviteeUserId: candidate.userId,
								remark: ''
							})
							this.invitePopupVisible = false
							this.resetInvitePopupState()
							await this.loadBookDetail()
							this.showNotify('邀请已发送', 'success')
						} finally {
							this.inviteSubmittingUserId = null
						}
					}
				})
			},
			handlePrepareRemove(member) {
				this.openConfirmDialog({
					title: '确认移除成员',
					message: `确定将 ${member.nickname || '该成员'} 移出当前账本吗？`,
					confirmText: '确认删除',
					action: async () => {
						this.removeSubmittingMemberId = member.memberId
						try {
							await removeBookMember(this.bookId, member.memberId)
							this.removePopupVisible = false
							await this.loadBookDetail()
							await this.refreshBookList()
							this.showNotify('成员已移除', 'success')
						} finally {
							this.removeSubmittingMemberId = null
						}
					}
				})
			},
			handlePrepareTransfer(member) {
				this.openConfirmDialog({
					title: '确认转让账本',
					message: `确定将账本所有权转让给 ${member.nickname || '该成员'} 吗？`,
					confirmText: '确认转让',
					action: async () => {
						this.transferSubmittingMemberId = member.memberId
						try {
							await transferBookOwner(this.bookId, member.memberId)
							this.transferPopupVisible = false
							await this.loadBookDetail()
							await this.refreshBookList()
							this.showNotify('账本所有权已转让', 'success')
						} finally {
							this.transferSubmittingMemberId = null
						}
					}
				})
			},
			resetInvitePopupState() {
				this.inviteSearch = {
					activeType: '',
					mobile: '',
					nickname: '',
					loading: false
				}
				this.inviteCandidates = []
				if (this.inviteSearchTimer) {
					clearTimeout(this.inviteSearchTimer)
					this.inviteSearchTimer = null
				}
			},
			openConfirmDialog(options) {
				this.pendingConfirmAction = options.action
				this.confirmDialog = {
					visible: true,
					title: options.title,
					message: options.message,
					confirmText: options.confirmText || '确认',
					loading: false
				}
			},
			handleCancelConfirm() {
				this.pendingConfirmAction = null
				this.confirmDialog.visible = false
				this.confirmDialog.loading = false
			},
			async handleConfirmAction() {
				if (!this.pendingConfirmAction) {
					this.handleCancelConfirm()
					return
				}
				this.confirmDialog.loading = true
				try {
					await this.pendingConfirmAction()
					this.handleCancelConfirm()
				} catch (error) {
					this.showNotify(error.message || '操作失败', 'danger')
					this.confirmDialog.loading = false
				}
			}
		}
	}
</script>

<style lang="scss">
	.detail-header {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 12px;
	}

	.detail-header-main {
		flex: 1;
		min-width: 0;
	}

	.detail-header-actions,
	.section-actions {
		display: flex;
		flex-direction: column;
		align-items: flex-end;
	}

	.section-header {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 12px;
	}

	.section-title {
		font-size: 17px;
		font-weight: 600;
		color: var(--app-text-color);
	}

	.section-subtitle {
		margin-top: 6px;
		font-size: 13px;
		line-height: 1.5;
		color: var(--app-text-secondary);
	}

	.member-list {
		display: flex;
		flex-direction: column;
		gap: 12px;
		margin-top: 12px;
	}

	.empty-hint,
	.popup-state {
		margin-top: 14px;
		padding: 12px;
		border-radius: var(--app-card-radius);
		background: var(--app-section-muted-bg);
		font-size: 14px;
		line-height: 1.6;
		color: var(--app-text-secondary);
		box-shadow: var(--app-card-shadow);
	}

	.temp-section :deep(.wd-collapse-item__body) {
		padding: 0;
	}

	.temp-list {
		display: flex;
		flex-direction: column;
		gap: 10px;
		padding-top: 12px;
	}

	.temp-item {
		display: flex;
		align-items: center;
		gap: 10px;
		padding: 10px 12px;
		border-radius: var(--app-card-radius);
		background: var(--app-section-soft-bg);
		box-shadow: var(--app-card-shadow);
	}

	.temp-item-content {
		min-width: 0;
	}

	.temp-item-name {
		font-size: 14px;
		font-weight: 600;
		color: var(--app-text-color);
	}

	.temp-item-meta {
		margin-top: 4px;
		font-size: 12px;
		color: var(--app-text-secondary);
	}

	.action-block {
		margin-top: 12px;
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
		min-width: 140px;
		text-align: right;
		color: var(--app-text-color);
	}

	.popup-panel {
		padding: 18px 14px 22px;
	}

	.popup-title {
		font-size: 18px;
		font-weight: 600;
		color: var(--app-text-color);
	}

	.popup-subtitle {
		margin-top: 8px;
		font-size: 13px;
		line-height: 1.6;
		color: var(--app-text-secondary);
	}

	.candidate-list {
		display: flex;
		flex-direction: column;
		gap: 12px;
		margin-top: 14px;
	}

	.candidate-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
		padding: 12px;
		border-radius: var(--app-card-radius);
		border: 1px solid var(--app-border-color);
		background: var(--app-card-bg);
		box-shadow: var(--app-card-shadow);
	}

	.candidate-main {
		display: flex;
		align-items: center;
		gap: 12px;
		flex: 1;
		min-width: 0;
	}

	.candidate-content {
		min-width: 0;
	}

	.candidate-name {
		font-size: 15px;
		font-weight: 600;
		color: var(--app-text-color);
	}

	.candidate-phone {
		margin-top: 6px;
		font-size: 13px;
		line-height: 1.5;
		color: var(--app-text-secondary);
	}

	.confirm-dialog {
		width: 280px;
		padding: 18px 16px 16px;
		box-shadow: var(--app-card-shadow-strong);
	}

	.confirm-title {
		font-size: 18px;
		font-weight: 600;
		text-align: center;
		color: var(--app-text-color);
	}

	.confirm-message {
		margin-top: 12px;
		font-size: 14px;
		line-height: 1.7;
		text-align: center;
		color: var(--app-text-secondary);
	}

	.confirm-actions {
		margin-top: 16px;
	}
</style>
