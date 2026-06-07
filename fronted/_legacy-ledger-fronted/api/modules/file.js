import { pinia } from '@/stores'
import { API_PREFIX, REQUEST_TIMEOUT } from '@/config/app'
import { useAuthStore } from '@/stores/modules/auth'
import { useRuntimeStore } from '@/stores/modules/runtime'

function joinUrl(baseUrl, path) {
	if (/^https?:\/\//.test(path)) {
		return path
	}
	return `${baseUrl}${path.startsWith('/') ? path : `/${path}`}`
}

export async function uploadImage(filePath, type, extraFormData) {
	const authStore = useAuthStore(pinia)
	const runtimeStore = useRuntimeStore(pinia)
	const token = authStore.authorizationToken
	if (!token) {
		throw new Error('请先登录')
	}

	const response = await uni.uploadFile({
		url: joinUrl(runtimeStore.normalizedApiBaseUrl, `${API_PREFIX}/files/upload`),
		filePath,
		name: 'file',
		timeout: REQUEST_TIMEOUT,
		formData: {
			type,
			...(extraFormData || {})
		},
		header: {
			Authorization: `Bearer ${token}`
		}
	})

	const payload = typeof response.data === 'string' ? JSON.parse(response.data || '{}') : response.data
	if (response.statusCode >= 200 && response.statusCode < 300 && payload && payload.code === 0) {
		return payload.data
	}

	throw new Error((payload && payload.message) || '上传失败')
}
