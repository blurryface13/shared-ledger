import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/modules/auth'
import { useBooksStore } from '@/stores/modules/books'
import { getCurrentUser } from '@/api/modules/user'
import { getMyBooks } from '@/api/modules/book'
import { ROUTES } from '@/config/app'
import { getCurrentRoute, redirectToBill } from '@/utils/navigation'

let bootstrapPromise = null

export async function hydrateSessionData() {
	const authStore = useAuthStore(pinia)
	const booksStore = useBooksStore(pinia)
	const currentUser = await getCurrentUser()
	authStore.setCurrentUser(currentUser)
	const bookList = await getMyBooks()
	booksStore.setBookList(bookList)
	return {
		currentUser,
		bookList
	}
}

export async function bootstrapSession() {
	if (bootstrapPromise) {
		return bootstrapPromise
	}

	const authStore = useAuthStore(pinia)

	if (!authStore.hasAccessToken) {
		return false
	}

	bootstrapPromise = hydrateSessionData().then(() => {
		if (getCurrentRoute() === ROUTES.LOGIN) {
			redirectToBill()
		}
		return true
	}).catch((error) => {
		console.warn('启动期刷新当前用户失败', error)
		return false
	}).finally(() => {
		bootstrapPromise = null
	})

	return bootstrapPromise
}
