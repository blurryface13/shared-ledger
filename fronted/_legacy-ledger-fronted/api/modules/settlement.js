import { API_PREFIX } from '@/config/app'
import { get, post } from '@/api/request'

export function createSettlement(bookId, payload) {
	return post(`${API_PREFIX}/books/${bookId}/settlements`, payload || {
		strategyType: 'MIN_TRANSFER_COUNT'
	})
}

export function getSettlements(bookId) {
	return get(`${API_PREFIX}/books/${bookId}/settlements`)
}

export function getSettlementDetail(bookId, settlementBatchId) {
	return get(`${API_PREFIX}/books/${bookId}/settlements/${settlementBatchId}`)
}

export function getPendingReceivePayments(bookId) {
	return get(`${API_PREFIX}/books/${bookId}/payment-confirms/pending-receive`)
}

export function getPendingPayPayments(bookId) {
	return get(`${API_PREFIX}/books/${bookId}/payment-confirms/pending-pay`)
}

export function confirmPayment(bookId, paymentConfirmId) {
	return post(`${API_PREFIX}/books/${bookId}/payment-confirms/${paymentConfirmId}/confirm`)
}

export function rejectPayment(bookId, paymentConfirmId, payload) {
	return post(`${API_PREFIX}/books/${bookId}/payment-confirms/${paymentConfirmId}/reject`, payload || {})
}
