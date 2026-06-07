import { ROUTES } from '@/config/app'

export function getCurrentRoute() {
	const pages = getCurrentPages()
	if (!pages.length) {
		return ''
	}
	const currentPage = pages[pages.length - 1]
	return currentPage.route ? `/${currentPage.route}` : ''
}

export function redirectToLogin() {
	if (getCurrentRoute() === ROUTES.LOGIN) {
		return
	}
	uni.reLaunch({
		url: ROUTES.LOGIN
	})
}

export function redirectToHome() {
	if (getCurrentRoute() === ROUTES.HOME) {
		return
	}
	uni.switchTab({
		url: ROUTES.HOME
	})
}

export function redirectToBill() {
	if (getCurrentRoute() === ROUTES.BILL) {
		return
	}
	uni.switchTab({
		url: ROUTES.BILL
	})
}
