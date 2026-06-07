export function readStorage(key, fallbackValue) {
	try {
		const value = uni.getStorageSync(key)
		return value === '' || typeof value === 'undefined' || value === null ? fallbackValue : value
	} catch (error) {
		console.warn('读取本地存储失败', key, error)
		return fallbackValue
	}
}

export function writeStorage(key, value) {
	try {
		if (value === null || typeof value === 'undefined') {
			uni.removeStorageSync(key)
			return
		}
		uni.setStorageSync(key, value)
	} catch (error) {
		console.warn('写入本地存储失败', key, error)
	}
}
