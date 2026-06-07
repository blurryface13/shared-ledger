<template>
	<view class="page-shell profile-page">
		<view class="profile-card">
			<view class="profile-top">
				<image class="profile-avatar" :src="avatarUrl" mode="aspectFill"></image>
				<wd-button size="small" plain type="primary" @click="handleGoLogin">开发登录</wd-button>
			</view>
			<view class="profile-row">
				<text class="profile-label">用户名</text>
				<text class="profile-value">{{ nicknameText }}</text>
			</view>
			<view class="profile-row">
				<text class="profile-label">手机号</text>
				<text class="profile-value">{{ mobileText }}</text>
			</view>
			<view class="profile-row">
				<text class="profile-label">后端</text>
				<text class="profile-value api">{{ apiBaseUrl }}</text>
			</view>
		</view>

		<view class="profile-card action-card">
			<wd-button type="primary" block @click="handleGoLogin">修改用户与登录态</wd-button>
			<wd-button custom-style="margin-top: 12px;" block plain type="warning" @click="handleClearSession">
				退出登录
			</wd-button>
		</view>
	</view>
</template>

<script>
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/modules/auth'
import { useRuntimeStore } from '@/stores/modules/runtime'
import { ROUTES } from '@/config/app'
import { redirectToLogin } from '@/utils/navigation'

export default {
	data() {
		return {
			nicknameText: '未登录',
			mobileText: '-',
			avatarUrl: '/static/logo.png',
			apiBaseUrl: ''
		}
	},
	onShow() {
		this.syncViewState()
	},
	methods: {
		syncViewState() {
			const authStore = useAuthStore(pinia)
			const runtimeStore = useRuntimeStore(pinia)
			const currentUser = authStore.currentUser || {}
			this.nicknameText = currentUser.nickname || '未登录'
			this.mobileText = currentUser.mobile || currentUser.mobileMasked || (currentUser.mobileBound ? '已绑定' : '-')
			this.avatarUrl = currentUser.avatarUrl || '/static/logo.png'
			this.apiBaseUrl = runtimeStore.normalizedApiBaseUrl
		},
		handleGoLogin() {
			uni.navigateTo({
				url: ROUTES.LOGIN
			})
		},
		handleClearSession() {
			useAuthStore(pinia).clearSession()
			redirectToLogin()
		}
	}
}
</script>

<style lang="scss">
.profile-page {
	padding-top: 14px;
}

.profile-card {
	margin-bottom: 14px;
	padding: 18px 16px;
	background: var(--app-card-bg);
	border: 1px solid var(--app-border-color);
	border-radius: 8px;
	box-shadow: var(--app-card-shadow);
}

.profile-top {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	margin-bottom: 24px;
}

.profile-avatar {
	width: 88px;
	height: 88px;
	border-radius: 50%;
	background: var(--app-section-muted-bg);
}

.profile-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	min-height: 44px;
	border-bottom: 1px solid var(--app-border-color);
}

.profile-row:last-child {
	border-bottom: 0;
}

.profile-label {
	font-size: 16px;
	color: #9aa1ad;
}

.profile-value {
	max-width: 230px;
	font-size: 16px;
	font-weight: 600;
	color: var(--app-text-color);
	text-align: right;
	word-break: break-all;
}

.profile-value.api {
	font-size: 12px;
	color: var(--app-text-secondary);
}

.action-card {
	padding-top: 16px;
	padding-bottom: 16px;
}
</style>
