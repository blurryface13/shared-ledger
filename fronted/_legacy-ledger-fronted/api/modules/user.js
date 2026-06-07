import { API_PREFIX } from '@/config/app'
import { get } from '@/api/request'

// 获取当前用户信息
export function getCurrentUser() {
	return get(`${API_PREFIX}/users/me`)
}
