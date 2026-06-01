<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <PageHeader :title="$t('contact.title')" :description="$t('contact.description')" />

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--primary)]"></div>
      <span class="ml-2 text-[var(--muted-foreground)]">{{ $t('common.loading') }}</span>
    </div>
    <template v-else>
      <div class="mb-6">
        <a-input v-model="keyword" :placeholder="$t('contact.searchPlaceholder')" @press-enter="onSearch" />
      </div>

      <div v-if="contacts.length" class="space-y-3">
        <div
          v-for="contact in contacts"
          :key="contact.userId"
          class="flex items-center gap-4 rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5"
        >
          <a-avatar :style="{ background: 'var(--primary)' }" :size="48">{{ (contact.nickName || contact.userName || '?').charAt(0).toUpperCase() }}</a-avatar>
          <div class="min-w-0 flex-1">
            <div class="mb-1 flex flex-wrap items-center gap-2">
              <h3 class="font-semibold text-[var(--foreground)]">{{ contact.nickName || contact.userName }}</h3>
              <a-tag v-for="role in contact.roleNames" :key="role">{{ role }}</a-tag>
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
      <a-empty v-else :description="$t('contact.noData')" />

      <div v-if="total > pageSize" class="mt-6">
        <a-pagination v-model:current="currentPage" :total="total" :page-size="pageSize" @change="loadContacts" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Mail, Phone } from 'lucide-vue-next'
import { getContactListApi } from '@/api/enterprise'
import type { Contact } from '@/types/api'
import PageHeader from '@/components/common/PageHeader.vue'


const { t } = useI18n()
const loading = ref(true)
const contacts = ref<Contact[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const keyword = ref('')

async function loadContacts() {
  loading.value = true
  try {
    const res = await getContactListApi({
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
    })
    contacts.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (err) {
    console.error('contact list load error:', err)
  } finally {
    loading.value = false
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
