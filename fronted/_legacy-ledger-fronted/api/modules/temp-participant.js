import { API_PREFIX } from '@/config/app'
import { del, get, post, put } from '@/api/request'

export function getTempParticipants(bookId) {
	return get(`${API_PREFIX}/books/${bookId}/temp-participants`)
}

export function createTempParticipant(bookId, payload) {
	return post(`${API_PREFIX}/books/${bookId}/temp-participants`, payload)
}

export function updateTempParticipantNickname(bookId, tempParticipantId, payload) {
	return put(`${API_PREFIX}/books/${bookId}/temp-participants/${tempParticipantId}/nickname`, payload)
}

export function updateTempParticipantAttachedMember(bookId, tempParticipantId, payload) {
	return put(`${API_PREFIX}/books/${bookId}/temp-participants/${tempParticipantId}/attached-member`, payload)
}

export function deleteTempParticipant(bookId, tempParticipantId) {
	return del(`${API_PREFIX}/books/${bookId}/temp-participants/${tempParticipantId}`)
}
