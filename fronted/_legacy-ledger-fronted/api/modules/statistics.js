import { API_PREFIX } from '@/config/app'
import { get, put } from '@/api/request'

export function getStatisticsOverview(bookId) {
	return get(`${API_PREFIX}/books/${bookId}/statistics/overview`)
}

export function updateBudget(bookId, payload) {
	return put(`${API_PREFIX}/books/${bookId}/statistics/budget`, payload)
}

export function getCategoryConsumption(bookId, params) {
	return get(`${API_PREFIX}/books/${bookId}/statistics/category-consumption`, {
		data: params || {}
	})
}

export function getMemberRelations(bookId) {
	return get(`${API_PREFIX}/books/${bookId}/statistics/member-relations`)
}

export function getAttachedTempDetails(bookId) {
	return get(`${API_PREFIX}/books/${bookId}/statistics/attached-temp-details`)
}
