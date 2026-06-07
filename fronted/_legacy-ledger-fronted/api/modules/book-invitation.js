import { API_PREFIX } from '@/config/app'
import { get, post } from '@/api/request'

export function searchInvitationCandidates(bookId, params) {
	return get(`${API_PREFIX}/books/${bookId}/invitation-candidates`, {
		data: params
	})
}

export function createInvitation(bookId, payload) {
	return post(`${API_PREFIX}/books/${bookId}/invitations`, payload)
}

export function getPendingInvitations() {
	return get(`${API_PREFIX}/invitations/pending`)
}

export function getReceivedInvitations() {
	return get(`${API_PREFIX}/invitations/received`)
}

export function getSentInvitations() {
	return get(`${API_PREFIX}/invitations/sent`)
}

export function acceptInvitation(invitationId) {
	return post(`${API_PREFIX}/invitations/${invitationId}/accept`)
}

export function rejectInvitation(invitationId) {
	return post(`${API_PREFIX}/invitations/${invitationId}/reject`)
}

export function revokeInvitation(invitationId) {
	return post(`${API_PREFIX}/invitations/${invitationId}/revoke`)
}
