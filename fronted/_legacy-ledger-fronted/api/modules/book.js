import { API_PREFIX } from '@/config/app'
import { del, get, post, put } from '@/api/request'

export function getMyBooks() {
	return get(`${API_PREFIX}/books/my`)
}

export function getBookDetail(bookId) {
	return get(`${API_PREFIX}/books/${bookId}`)
}

export function createBook(payload) {
	return post(`${API_PREFIX}/books`, payload)
}

export function updateBook(bookId, payload) {
	return put(`${API_PREFIX}/books/${bookId}`, payload)
}

export function deleteBook(bookId) {
	return del(`${API_PREFIX}/books/${bookId}`)
}
