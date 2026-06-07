import { defineStore } from 'pinia'
import { pinia } from '@/stores'
import { STORAGE_KEYS } from '@/config/app'
import { readStorage, writeStorage } from '@/utils/storage'
import { useBooksStore } from '@/stores/modules/books'

function createDefaultState() {
	return {
		accessToken: '',
		refreshToken: '',
		bindToken: '',
		tokenExpireAt: '',
		refreshTokenExpireAt: '',
		currentUser: null
	}
}

export const useAuthStore = defineStore('auth', {
	state: () => {
		const cachedState = readStorage(STORAGE_KEYS.AUTH, {})
		return {
			...createDefaultState(),
			...cachedState
		}
	},
	getters: {
		authorizationToken(state) {
			return state.accessToken || state.bindToken || ''
		},
		hasAccessToken(state) {
			return Boolean(state.accessToken)
		}
	},
	actions: {
		persist() {
			writeStorage(STORAGE_KEYS.AUTH, {
				accessToken: this.accessToken,
				refreshToken: this.refreshToken,
				bindToken: this.bindToken,
				tokenExpireAt: this.tokenExpireAt,
				refreshTokenExpireAt: this.refreshTokenExpireAt,
				currentUser: this.currentUser
			})
		},
		applyBindSession(payload) {
			this.accessToken = ''
			this.refreshToken = ''
			this.bindToken = payload.bindToken || ''
			this.tokenExpireAt = payload.tokenExpireAt || ''
			this.refreshTokenExpireAt = payload.refreshTokenExpireAt || ''
			this.currentUser = {
				userId: payload.userId,
				nickname: payload.nickname,
				avatarUrl: payload.avatarUrl,
				mobileBound: payload.mobileBound
			}
			this.persist()
		},
		applyAuthorizedSession(payload) {
			this.accessToken = payload.token || this.accessToken
			this.refreshToken = payload.refreshToken || this.refreshToken
			this.bindToken = ''
			this.tokenExpireAt = payload.tokenExpireAt || ''
			this.refreshTokenExpireAt = payload.refreshTokenExpireAt || ''
			if (payload.userId || payload.nickname || payload.avatarUrl) {
				this.currentUser = {
					...(this.currentUser || {}),
					userId: payload.userId || (this.currentUser && this.currentUser.userId),
					nickname: payload.nickname || (this.currentUser && this.currentUser.nickname),
					avatarUrl: payload.avatarUrl || (this.currentUser && this.currentUser.avatarUrl),
					mobileBound: typeof payload.mobileBound === 'boolean'
						? payload.mobileBound
						: this.currentUser && this.currentUser.mobileBound
				}
			}
			this.persist()
		},
		setCurrentUser(currentUser) {
			this.currentUser = currentUser
			this.persist()
		},
		clearSession() {
			Object.assign(this, createDefaultState())
			writeStorage(STORAGE_KEYS.AUTH, createDefaultState())
			useBooksStore(pinia).clearBooks()
		}
	}
})
