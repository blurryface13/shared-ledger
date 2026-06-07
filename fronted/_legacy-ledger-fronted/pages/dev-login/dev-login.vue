<template>
	<view class="page-shell">
		<view class="page-block">
			<view class="page-title">开发调试登录</view>
			<view class="page-subtitle">
				当前页面仅服务开发联调。你可以直接输入任意 mock code 登录，也可以点击下面已有 mock openid 对应的快捷入口。
			</view>
		</view>

		<view class="page-block">
			<wd-input
				v-model="form.apiBaseUrl"
				label="后端地址"
				placeholder="请输入后端地址，例如 http://127.0.0.1:8080"
				clearable
			/>
			<wd-button type="primary" block custom-style="margin-top: 12px;" @click="handleSaveBaseUrl">
				保存后端地址
			</wd-button>
		</view>

		<view class="page-block">
			<wd-input
				v-model="form.code"
				label="Mock Code"
				placeholder="请输入任意 mock code"
				clearable
			/>
			<wd-button type="primary" block :loading="loginLoading" custom-style="margin-top: 12px;" @click="handleLogin">
				使用 Code 登录
			</wd-button>
		</view>

		<view v-if="showBindPanel" class="page-block">
			<view class="page-title bind-title">首次登录需要绑定手机号</view>
			<view class="page-subtitle">
				正式上线后这里会替换为微信无感登录和手机号能力。当前开发阶段先支持手动输入手机号补全绑定流程。
			</view>
			<wd-input
				v-model="form.mobile"
				label="手机号"
				placeholder="请输入 11 位手机号"
				clearable
			/>
			<wd-button type="primary" block :loading="bindLoading" custom-style="margin-top: 12px;" @click="handleBindMobile">
				绑定手机号并进入系统
			</wd-button>
		</view>

		<view class="page-block">
			<view class="mock-header">
				<view class="page-title mock-title">已有 Mock OpenID</view>
				<wd-button size="small" type="primary" plain @click="handleRefreshMockUsers">
					刷新
				</wd-button>
			</view>

			<view v-if="mockUsers.length">
				<view v-for="item in mockUsers" :key="item.openId" class="mock-user-card">
					<view class="mock-user-top">
						<text class="mock-user-name">{{ item.nickname || item.openId }}</text>
						<wd-tag :type="item.mobileBound ? 'success' : 'warning'">
							{{ item.mobileBound ? '已绑手机' : '待绑手机' }}
						</wd-tag>
					</view>
					<view class="mock-user-meta">OpenID：{{ item.openId }}</view>
					<view class="mock-user-meta">建议 Code：{{ item.suggestedCode }}</view>
					<view class="mock-user-meta" v-if="item.mobileMasked">手机号：{{ item.mobileMasked }}</view>
					<wd-button
						type="primary"
						size="small"
						custom-style="margin-top: 10px;"
						@click="handleQuickLogin(item.suggestedCode)"
					>
						使用此账号登录
					</wd-button>
				</view>
			</view>

			<view v-else class="empty-tip">
				当前还没有已落库的 mock 用户。你也可以先输入任意 mock code 登录，系统会自动生成对应 mock openid。
			</view>
		</view>
	</view>
</template>

<script>
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/modules/auth'
import { useRuntimeStore } from '@/stores/modules/runtime'
import { bindWechatMobile, fetchMockUsers, loginByWechatCode } from '@/api/modules/auth'
import { DEFAULT_API_BASE_URL } from '@/config/app'
import { hydrateSessionData } from '@/services/auth'
import { redirectToBill } from '@/utils/navigation'

export default {
	data() {
		return {
			form: {
				apiBaseUrl: DEFAULT_API_BASE_URL,
				code: '',
				mobile: '13800138000'
			},
			loginLoading: false,
			bindLoading: false,
			showBindPanel: false,
			mockUsers: []
		}
	},
	onLoad() {
		this.syncStoreState()
		this.handleRefreshMockUsers()
	},
	methods: {
		syncStoreState() {
			const runtimeStore = useRuntimeStore(pinia)
			const authStore = useAuthStore(pinia)
			this.form.apiBaseUrl = runtimeStore.normalizedApiBaseUrl
			this.showBindPanel = Boolean(authStore.bindToken)
		},
		handleSaveBaseUrl(showToast) {
			const runtimeStore = useRuntimeStore(pinia)
			runtimeStore.setApiBaseUrl(this.form.apiBaseUrl)
			this.form.apiBaseUrl = runtimeStore.normalizedApiBaseUrl
			if (showToast !== false) {
				uni.showToast({
					title: '已保存',
					icon: 'success'
				})
			}
		},
		async finishAuthorizedLogin(payload) {
			const authStore = useAuthStore(pinia)
			authStore.applyAuthorizedSession(payload)
			await hydrateSessionData()
			this.showBindPanel = false
			redirectToBill()
		},
		async handleLogin(confirmReRegister) {
			const authStore = useAuthStore(pinia)
			const code = (this.form.code || '').trim()

			if (!code) {
				uni.showToast({
					title: '请先输入 mock code',
					icon: 'none'
				})
				return
			}

			this.handleSaveBaseUrl(false)
			this.loginLoading = true
			try {
				const result = await loginByWechatCode({
					code,
					confirmReRegister: Boolean(confirmReRegister)
				})

				if (result.needConfirmReRegister) {
					const modalResult = await uni.showModal({
						title: '恢复账号确认',
						content: '当前 mock 账号已注销，是否恢复该账号并继续登录？'
					})
					if (modalResult.confirm) {
						await this.handleLogin(true)
					}
					return
				}

				if (result.needBindMobile) {
					authStore.applyBindSession(result)
					this.showBindPanel = true
					uni.showToast({
						title: '请先绑定手机号',
						icon: 'none'
					})
					return
				}

				await this.finishAuthorizedLogin(result)
			} catch (error) {
				uni.showToast({
					title: error.message || '登录失败',
					icon: 'none'
				})
			} finally {
				this.loginLoading = false
			}
		},
		async handleBindMobile() {
			const mobile = (this.form.mobile || '').trim()

			if (!/^1\d{10}$/.test(mobile)) {
				uni.showToast({
					title: '请输入正确的 11 位手机号',
					icon: 'none'
				})
				return
			}

			this.bindLoading = true
			try {
				const result = await bindWechatMobile({
					manualMobile: mobile
				})
				await this.finishAuthorizedLogin(result)
			} catch (error) {
				uni.showToast({
					title: error.message || '绑定失败',
					icon: 'none'
				})
			} finally {
				this.bindLoading = false
			}
		},
		async handleRefreshMockUsers() {
			try {
				this.handleSaveBaseUrl(false)
				const mockUsers = await fetchMockUsers()
				this.mockUsers = Array.isArray(mockUsers) ? mockUsers : []
			} catch (error) {
				this.mockUsers = []
				uni.showToast({
					title: error.message || '获取 mock 用户失败',
					icon: 'none'
				})
			}
		},
		handleQuickLogin(code) {
			this.form.code = code
			this.handleLogin(false)
		}
	}
}
</script>

<style lang="scss">
.bind-title,
.mock-title {
	margin-bottom: 4px;
}

.mock-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 12px;
}

.mock-user-card {
	padding: 12px 0;
	border-top: 1px solid var(--app-border-color);
}

.mock-user-card:first-child {
	border-top: 0;
	padding-top: 0;
}

.mock-user-top {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12px;
	margin-bottom: 8px;
}

.mock-user-name {
	flex: 1;
	font-size: 15px;
	font-weight: 600;
	color: var(--app-text-color);
	word-break: break-all;
}

.mock-user-meta {
	margin-top: 4px;
	font-size: 13px;
	line-height: 1.5;
	color: var(--app-text-secondary);
	word-break: break-all;
}

.empty-tip {
	font-size: 14px;
	line-height: 1.7;
	color: var(--app-text-secondary);
}
</style>
