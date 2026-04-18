<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <PageHeader title="通讯录" description="查看企业员工联系信息" />

    <div class="mb-6">
      <Input v-model="keyword" placeholder="搜索姓名、用户名、邮箱或手机号" @keyup.enter="onSearch" />
    </div>

    <div v-if="contacts.length" class="space-y-3">
      <div
        v-for="contact in contacts"
        :key="contact.userId"
        class="flex items-center gap-4 rounded-lg border border-[var(--border)] bg-[var(--card)] p-5"
      >
        <Avatar :fallback="(contact.nickName || contact.userName || '?').charAt(0).toUpperCase()" size="lg" />
        <div class="min-w-0 flex-1">
          <div class="mb-1 flex flex-wrap items-center gap-2">
            <h3 class="font-semibold text-[var(--foreground)]">{{ contact.nickName || contact.userName }}</h3>
            <Badge v-for="role in contact.roleNames" :key="role" variant="default">{{ role }}</Badge>
          </div>
          <p class="text-sm text-[var(--muted-foreground)]">@{{ contact.userName }}</p>
        </div>
        <div class="hidden items-center gap-6 text-sm text-[var(--foreground)] md:flex">
          <div v-if="contact.email" class="flex items-center gap-1.5">
            <Mail :size="16" class="text-[var(--muted-foreground)]" />
            <span>{{ contact.email }}</span>
          </div>
          <div v-if="contact.phone" class="flex items-center gap-1.5">
            <Phone :size="16" class="text-[var(--muted-foreground)]" />
            <span>{{ contact.phone }}</span>
          </div>
        </div>
      </div>
    </div>
    <Empty v-else description="暂无员工信息" />

    <div v-if="total > pageSize" class="mt-6">
      <Pagination v-model="currentPage" :total="total" :page-size="pageSize" @change="loadContacts" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Mail, Phone } from 'lucide-vue-next'
import { getContactListApi } from '@/api/enterprise'
import type { Contact } from '@/types/api'
import PageHeader from '@/components/common/PageHeader.vue'
import Input from '@/components/ui/input/Input.vue'
import Avatar from '@/components/ui/avatar/Avatar.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import Pagination from '@/components/ui/pagination/Pagination.vue'
import Empty from '@/components/ui/empty/Empty.vue'

const contacts = ref<Contact[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const keyword = ref('')

async function loadContacts() {
  try {
    const res = await getContactListApi({
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
    })
    contacts.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    // 静默
  }
}

function onSearch() {
  currentPage.value = 1
  loadContacts()
}

onMounted(() => {
  loadContacts()
})
</script>
