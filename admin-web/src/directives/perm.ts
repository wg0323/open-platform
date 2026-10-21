import type { Directive } from 'vue'
import { useAuthStore } from '@/stores/auth'

const perm: Directive<HTMLElement, string> = {
  mounted(el, binding) {
    const auth = useAuthStore()
    if (binding.value && !auth.hasPerm(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  }
}

export default perm
