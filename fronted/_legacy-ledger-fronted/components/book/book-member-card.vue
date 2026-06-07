<template>
	<view class="member-card">
		<view class="member-card-main">
			<wd-avatar
				:src="member.avatarUrl"
				:text="avatarText"
				size="large"
				shape="round"
				bg-color="var(--app-color-primary)"
			/>

			<view class="member-card-content">
				<view class="member-card-header">
					<view class="member-card-name">{{ member.nickname || '未命名用户' }}</view>
					<view class="member-card-tags">
						<wd-tag :type="roleTagType">{{ roleLabel }}</wd-tag>
						<wd-tag v-if="statusLabel" plain :type="statusTagType">{{ statusLabel }}</wd-tag>
					</view>
				</view>
				<view class="member-card-phone">{{ member.phoneNumber || '暂未绑定手机号' }}</view>
				<view class="member-card-meta">{{ metaText }}</view>
			</view>

			<view v-if="$slots.actions" class="member-card-actions">
				<slot name="actions"></slot>
			</view>
		</view>

		<view v-if="$slots.default" class="member-card-extra">
			<slot></slot>
		</view>
	</view>
</template>

<script>
export default {
	name: 'BookMemberCard',
	props: {
		member: {
			type: Object,
			default() {
				return {}
			}
		},
		roleLabel: {
			type: String,
			default: ''
		},
		roleTagType: {
			type: String,
			default: 'primary'
		},
		statusLabel: {
			type: String,
			default: ''
		},
		statusTagType: {
			type: String,
			default: 'warning'
		}
	},
	computed: {
		avatarText() {
			if (!this.member || !this.member.nickname) {
				return '账'
			}
			return this.member.nickname.slice(0, 1)
		},
		metaText() {
			if (this.member && this.member.memberStatus === 'ACTIVE') {
				return `加入时间：${this.formatDateTime(this.member.joinedAt)}`
			}
			return `离开时间：${this.formatDateTime(this.member.leftAt)}`
		}
	},
	methods: {
		formatDateTime(value) {
			if (!value) {
				return '-'
			}
			const date = new Date(value)
			const year = date.getFullYear()
			const month = `${date.getMonth() + 1}`.padStart(2, '0')
			const day = `${date.getDate()}`.padStart(2, '0')
			const hour = `${date.getHours()}`.padStart(2, '0')
			const minute = `${date.getMinutes()}`.padStart(2, '0')
			return `${year}-${month}-${day} ${hour}:${minute}`
		}
	}
}
</script>

<style lang="scss">
.member-card {
	padding: 12px;
	border: 1px solid var(--app-border-color);
	border-radius: var(--app-card-radius);
	background: var(--app-card-bg);
	box-shadow: var(--app-card-shadow);
}

.member-card-main {
	display: flex;
	align-items: flex-start;
	gap: 12px;
}

.member-card-content {
	flex: 1;
	min-width: 0;
}

.member-card-header {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 10px;
}

.member-card-tags {
	display: flex;
	flex-wrap: wrap;
	justify-content: flex-end;
	gap: 6px;
}

.member-card-name {
	font-size: 16px;
	font-weight: 600;
	line-height: 1.4;
	color: var(--app-text-color);
}

.member-card-phone,
.member-card-meta {
	margin-top: 6px;
	font-size: 13px;
	line-height: 1.5;
	color: var(--app-text-secondary);
}

.member-card-actions {
	flex-shrink: 0;
}

.member-card-extra {
	margin-top: 12px;
}
</style>
