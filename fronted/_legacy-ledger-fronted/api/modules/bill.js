import { API_PREFIX } from '@/config/app'
import { del, get, post, put } from '@/api/request'

export function getBills(bookId, params) {
	return get(`${API_PREFIX}/books/${bookId}/bills`, {
		data: params || {}
	})
}

export function getBillDetail(bookId, billId) {
	return get(`${API_PREFIX}/books/${bookId}/bills/${billId}`)
}

export function createPersonalBill(bookId, payload) {
	return post(`${API_PREFIX}/books/${bookId}/bills/personal-bill`, payload)
}

export function createSharedExpenseBill(bookId, payload) {
	return post(`${API_PREFIX}/books/${bookId}/bills/shared-expense`, payload)
}

export function updateBill(bookId, billId, payload) {
	return put(`${API_PREFIX}/books/${bookId}/bills/${billId}`, payload)
}

export function deleteBill(bookId, billId) {
	return del(`${API_PREFIX}/books/${bookId}/bills/${billId}`)
}

export function settleBillParticipant(bookId, billId, participantMemberId, payload) {
	return post(`${API_PREFIX}/books/${bookId}/bills/${billId}/participants/${participantMemberId}/settle`, payload || {})
}
