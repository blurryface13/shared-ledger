import { API_PREFIX } from '@/config/app'
import { get, post } from '@/api/request'

export function getBookMembers(bookId) {
	return get(`${API_PREFIX}/books/${bookId}/members`)
}

export function removeBookMember(bookId, memberId) {
	return post(`${API_PREFIX}/books/${bookId}/members/${memberId}/remove`)
}

export function quitBook(bookId) {
	return post(`${API_PREFIX}/books/${bookId}/quit`)
}

export function transferBookOwner(bookId, targetMemberId) {
	return post(`${API_PREFIX}/books/${bookId}/transfer-owner`, {
		targetMemberId
	})
}
