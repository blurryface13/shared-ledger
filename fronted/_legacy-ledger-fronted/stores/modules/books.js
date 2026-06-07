import { defineStore } from 'pinia'
import { readStorage, writeStorage } from '@/utils/storage'

const BOOKS_STORAGE_KEY = 'trip-ledger-books-storage'

function createDefaultState() {
	return {
		bookList: [],
		currentBookId: null,
		manualSelected: false,
		lastFetchedAt: ''
	}
}

function resolveLatestCreatedBookId(bookList) {
	if (!Array.isArray(bookList) || !bookList.length) {
		return null
	}
	const latestBook = bookList.slice().sort((left, right) => {
		return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()
	})[0]
	return latestBook ? latestBook.bookId : null
}

export const useBooksStore = defineStore('books', {
	state: () => {
		const cachedState = readStorage(BOOKS_STORAGE_KEY, {})
		return {
			...createDefaultState(),
			...cachedState
		}
	},
	getters: {
		currentBook(state) {
			if (!state.currentBookId) {
				return state.bookList.length ? state.bookList[0] : null
			}
			return state.bookList.find((item) => item.bookId === state.currentBookId) || null
		}
	},
	actions: {
		persist() {
			writeStorage(BOOKS_STORAGE_KEY, {
				bookList: this.bookList,
				currentBookId: this.currentBookId,
				manualSelected: this.manualSelected,
				lastFetchedAt: this.lastFetchedAt
			})
		},
		setBookList(bookList) {
			this.bookList = Array.isArray(bookList) ? bookList : []
			if (!this.manualSelected) {
				this.currentBookId = resolveLatestCreatedBookId(this.bookList)
			} else if (!this.currentBookId || !this.bookList.some((item) => item.bookId === this.currentBookId)) {
				this.currentBookId = resolveLatestCreatedBookId(this.bookList)
				this.manualSelected = false
			}
			this.lastFetchedAt = new Date().toISOString()
			this.persist()
		},
		setCurrentBookId(bookId) {
			this.currentBookId = bookId || null
			this.manualSelected = Boolean(bookId)
			this.persist()
		},
		appendBook(book) {
			const nextList = [book].concat(this.bookList.filter((item) => item.bookId !== book.bookId))
			this.bookList = nextList
			if (!this.manualSelected) {
				this.currentBookId = book.bookId
			}
			this.lastFetchedAt = new Date().toISOString()
			this.persist()
		},
		clearBooks() {
			Object.assign(this, createDefaultState())
			writeStorage(BOOKS_STORAGE_KEY, createDefaultState())
		}
	}
})
