import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import UniPluginModule from '@dcloudio/vite-plugin-uni'

const uni = UniPluginModule.default || UniPluginModule
const inputDir = fileURLToPath(new URL('.', import.meta.url))

process.env.UNI_INPUT_DIR = process.env.UNI_INPUT_DIR || inputDir

export default defineConfig({
	resolve: {
		alias: {
			'@': inputDir
		}
	},
	plugins: [uni()]
})
