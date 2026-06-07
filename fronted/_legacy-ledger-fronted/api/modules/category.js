import { API_PREFIX } from '@/config/app'
import { get } from '@/api/request'

export function getBookCategories(bookId, params) {
	return get(`${API_PREFIX}/books/${bookId}/categories`, {
		data: params || {}
	})
}
