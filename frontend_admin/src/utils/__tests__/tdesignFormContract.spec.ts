import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h, nextTick, reactive, ref } from 'vue'
import { Dialog as TDialog, Form as TForm, FormItem as TFormItem, Input as TInput, Button as TButton } from 'tdesign-vue-next'

/**
 * 锁定本项目表单实现所依赖的 TDesign 契约。
 *
 * 这三条都是"看起来正常、实际静默失效"的坑，且都曾在全仓范围内踩到：
 * 1. t-form 的数据 prop 是 data，写成 :model= 时 rules 永不求值；
 * 2. t-form-item 的字段锚点是 name，写成 field= 时校验结果挂在空键上、红字显示不出来；
 * 3. validate() 通过时返回 true；t-dialog 确认不会自动改 visible（关闭须由调用方决定）。
 * 升级 tdesign-vue-next 若改动其中任一语义，本文件会先失败，而不是让线上表单静默失灵。
 */
const rules = [{ required: true, message: 'A 不能为空' }]

function buildForm(itemAttr: 'name' | 'field') {
  return defineComponent({
    components: { TForm, TFormItem, TInput },
    setup() {
      const data = reactive<Record<string, string>>({ a: '' })
      const formRef = ref()
      return { data, rules, formRef }
    },
    template: `<t-form ref="formRef" :data="data"><t-form-item ${itemAttr}="a" :rules="rules"><t-input v-model="data.a" /></t-form-item></t-form>`,
  })
}

describe('TDesign 表单契约', () => {
  it('t-form-item 用 name 锚定时，校验结果按字段归位', async () => {
    const w = mount(buildForm('name'))
    const res = await (w.vm as any).formRef.validate()
    expect(res).toBeTruthy()
    expect(res).not.toBe(true)
    expect(Object.keys(res)).toContain('a')
    // 红字要挂在字段行上，而不是无处安放
    await nextTick()
    expect(w.text()).toContain('A 不能为空')
  })

  it('误用 field 锚点时校验结果落在空键上（这正是回归的形态，据此守住写法）', async () => {
    const w = mount(buildForm('field'))
    const res = await (w.vm as any).formRef.validate()
    expect(Object.keys(res)[0]).toBe('')
  })

  it('字段合法时 validate() 返回 true', async () => {
    const Comp = defineComponent({
      components: { TForm, TFormItem, TInput },
      setup() {
        const data = reactive({ a: 'x' })
        const formRef = ref()
        return { data, rules, formRef }
      },
      template: `<t-form ref="formRef" :data="data"><t-form-item name="a" :rules="rules"><t-input v-model="data.a" /></t-form-item></t-form>`,
    })
    const w = mount(Comp)
    expect(await (w.vm as any).formRef.validate()).toBe(true)
  })

  it('t-dialog 点击确认不会自行关闭，关闭时机由调用方控制', async () => {
    const Comp = defineComponent({
      components: { TDialog, TButton },
      setup() {
        const visible = ref(false)
        const confirmCalls = ref(0)
        const onConfirm = () => { confirmCalls.value++ }
        return { visible, confirmCalls, onConfirm }
      },
      template: `<div><t-button class="opener" @click="visible = true">open</t-button>
        <t-dialog v-model="visible" header="H" confirm-btn="确认" @confirm="onConfirm" /></div>`,
    })
    const w = mount(Comp, { attachTo: document.body })
    w.find('.opener').trigger('click')
    await nextTick()
    const ok = [...document.querySelectorAll('.t-dialog__footer button')].find(b => b.textContent?.trim() === '确认')
    expect(ok).toBeTruthy()
    ok!.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await nextTick()
    const vm = w.vm as any
    expect(vm.confirmCalls).toBe(1)
    // 校验不过时提前 return 即可留住对话框：TDesign 不会替你把 visible 置 false
    expect(vm.visible).toBe(true)
    w.unmount()
  })
})
