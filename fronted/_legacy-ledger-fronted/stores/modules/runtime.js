import { defineStore } from 'pinia'
import { DEFAULT_API_BASE_URL, STORAGE_KEYS } from '@/config/app'
import { readStorage, writeStorage } from '@/utils/storage'

function createDefaultState() {
	return {
		apiBaseUrl: DEFAULT_API_BASE_URL
	}
}

export const useRuntimeStore = defineStore('runtime', {
	state: () => {
		const cachedState = readStorage(STORAGE_KEYS.RUNTIME, {})
		return {
			...createDefaultState(),
			...cachedState
		}
	},
	getters: {
		normalizedApiBaseUrl(state) {
			const baseUrl = (state.apiBaseUrl || DEFAULT_API_BASE_URL).trim()
			return baseUrl.replace(/\/+$/, '')
		}
	},
	actions: {
		persist() {
			writeStorage(STORAGE_KEYS.RUNTIME, {
				apiBaseUrl: this.apiBaseUrl
			})
		},
		setApiBaseUrl(baseUrl) {
			this.apiBaseUrl = (baseUrl || DEFAULT_API_BASE_URL).trim() || DEFAULT_API_BASE_URL
			this.persist()
		}
	}
})
