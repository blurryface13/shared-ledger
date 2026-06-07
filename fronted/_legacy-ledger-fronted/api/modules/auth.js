import { API_PREFIX } from '@/config/app'
import { get, post } from '@/api/request'

// 通过WeChat Code登陆
export function loginByWechatCode(payload) {
	return post(`${API_PREFIX}/auth/wechat-login`, payload, {
		auth: false
	})
}

// 绑定微信手机号
export function bindWechatMobile(payload) {
	return post(`${API_PREFIX}/auth/bind-wechat-mobile`, payload)
}

// 获取模拟的用户
export function fetchMockUsers() {
	return get(`${API_PREFIX}/auth/dev/mock-users`, {
		auth: false
	})
}
