import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/modules/auth'
import { useRuntimeStore } from '@/stores/modules/runtime'
import { API_PREFIX, REQUEST_TIMEOUT } from '@/config/app'
import { redirectToLogin } from '@/utils/navigation'

let refreshingPromise = null

function printRequestLog(stage, payload) {
	console.log(`[trip-ledger][request:${stage}]`, payload)
}

function joinUrl(baseUrl, path) {
	if (/^https?:\/\//.test(path)) {
		return path
	}
	return `${baseUrl}${path.startsWith('/') ? path : `/${path}`}`
}

function createBusinessError(response, statusCode) {
	const error = new Error((response && response.message) || '请求失败')
	error.code = response && response.code ? response.code : statusCode
	error.response = response
	error.statusCode = statusCode
	return error
}

async function rawRequest(options) {
	printRequestLog('send', {
		url: options.url,
		method: options.method || 'GET',
		header: options.header || {},
		data: options.data || {}
	})
	return uni.request(options)
}

async function refreshAccessToken() {
	if (refreshingPromise) {
		return refreshingPromise
	}

	const authStore = useAuthStore(pinia)
	const runtimeStore = useRuntimeStore(pinia)

	if (!authStore.refreshToken) {
		authStore.clearSession()
		redirectToLogin()
		throw new Error('登录已失效，请重新登录')
	}

	refreshingPromise = rawRequest({
		url: joinUrl(runtimeStore.normalizedApiBaseUrl, `${API_PREFIX}/auth/refresh-token`),
		method: 'POST',
		timeout: REQUEST_TIMEOUT,
		header: {
			'Content-Type': 'application/json'
		},
		data: {
			refreshToken: authStore.refreshToken
		}
	}).then((response) => {
		printRequestLog('refresh-response', response)
		const payload = response.data || {}
		if (response.statusCode < 200 || response.statusCode >= 300 || payload.code !== 0) {
			throw createBusinessError(payload, response.statusCode)
		}
		authStore.applyAuthorizedSession(payload.data || {})
		return payload.data
	}).catch((error) => {
		printRequestLog('refresh-error', error)
		authStore.clearSession()
		redirectToLogin()
		throw error
	}).finally(() => {
		refreshingPromise = null
	})

	return refreshingPromise
}

export async function request(options) {
	const authStore = useAuthStore(pinia)
	const runtimeStore = useRuntimeStore(pinia)
	const shouldAttachAuth = options.auth !== false
	const token = authStore.authorizationToken

	if (shouldAttachAuth && !token) {
		redirectToLogin()
		throw new Error('未登录或登录已失效')
	}

	const header = {
		'Content-Type': 'application/json',
		...(options.header || {})
	}

	if (shouldAttachAuth && token) {
		header.Authorization = `Bearer ${token}`
	}

	const response = await rawRequest({
		url: joinUrl(runtimeStore.normalizedApiBaseUrl, options.url),
		method: options.method || 'GET',
		data: options.data || {},
		timeout: options.timeout || REQUEST_TIMEOUT,
		header
	})
	printRequestLog('response', response)

	const payload = response.data || {}

	if (response.statusCode >= 200 && response.statusCode < 300 && payload.code === 0) {
		return payload.data
	}

	if (
		shouldAttachAuth &&
		!options._retried &&
		payload.code === 4002 &&
		options.url !== `${API_PREFIX}/auth/refresh-token`
	) {
		await refreshAccessToken()
		return request({
			...options,
			_retried: true
		})
	}

	throw createBusinessError(payload, response.statusCode)
}

export function get(url, options) {
	return request({
		...(options || {}),
		url,
		method: 'GET'
	})
}

export function post(url, data, options) {
	return request({
		...(options || {}),
		url,
		data,
		method: 'POST'
	})
}

export function put(url, data, options) {
	return request({
		...(options || {}),
		url,
		data,
		method: 'PUT'
	})
}

export function del(url, options) {
	return request({
		...(options || {}),
		url,
		method: 'DELETE'
	})
}
