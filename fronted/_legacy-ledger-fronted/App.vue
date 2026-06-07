<template>
	<wd-config-provider :theme-vars="themeVars">
		<view class="app-root">
			<!-- #ifdef H5 -->
			<LayoutComponent />
			<!-- #endif -->
			<!-- #ifndef H5 -->
			<slot />
			<!-- #endif -->
		</view>
	</wd-config-provider>
</template>

<script>
import { WOT_THEME_VARS } from '@/config/theme'
import { bootstrapSession } from '@/services/auth'
// #ifdef H5
import { LayoutComponent } from '@dcloudio/uni-h5'
// #endif

export default {
	data() {
		return {
			themeVars: WOT_THEME_VARS
		}
	},
	onLaunch() {
		this.handleBootstrap()
	},
	onShow() {
		this.handleBootstrap()
	},
	onHide() {},
	methods: {
		async handleBootstrap() {
			try {
				await bootstrapSession()
			} catch (error) {
				console.warn('应用启动鉴权检查失败', error)
			}
		}
	}
}
</script>

<style lang="scss">
@import '@/styles/index.scss';

.app-root {
	min-height: 100vh;
}
</style>
