import { API_PREFIX } from '@/config/app'
import { get, post } from '@/api/request'

export function getMyBillRequests(bookId) {
	return get(`${API_PREFIX}/books/${bookId}/bill-requests/my`)
}

export function getPendingApproveBillRequests(bookId) {
	return get(`${API_PREFIX}/books/${bookId}/bill-requests/pending-approve`)
}

export function approveBillRequest(bookId, requestId, payload) {
	return post(`${API_PREFIX}/books/${bookId}/bill-requests/${requestId}/approve`, payload || {})
}

export function rejectBillRequest(bookId, requestId, payload) {
	return post(`${API_PREFIX}/books/${bookId}/bill-requests/${requestId}/reject`, payload || {})
}

export function cancelBillRequest(bookId, requestId) {
	return post(`${API_PREFIX}/books/${bookId}/bill-requests/${requestId}/cancel`)
}
