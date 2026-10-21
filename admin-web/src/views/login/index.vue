<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { fetchCaptcha } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captcha = reactive({ captchaId: '', image: '' })
const form = reactive({ username: '', password: '', captchaCode: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function loadCaptcha() {
  try {
    const data = await fetchCaptcha()
    captcha.captchaId = data.captchaId
    captcha.image = data.image
  } catch {
    // 错误提示由响应拦截器统一处理
  }
}

async function submit() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    const result = await auth.signIn({
      captchaId: captcha.captchaId,
      captchaCode: form.captchaCode,
      username: form.username,
      password: form.password
    })
    ElMessage.success('登录成功')
    if (result.mustChangePassword) {
      router.replace({ path: '/password', query: { force: '1' } })
    } else {
      const redirect = (route.query.redirect as string) || '/'
      router.replace(redirect)
    }
  } catch {
    form.captchaCode = ''
    await loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<template>
  <div class="login-page">
    <div class="login-shell">
      <section class="brand">
        <h1>共享自行车监管平台</h1>
        <p>开放 API 管理后台</p>
        <div class="brand-shapes">
          <span class="circle"></span>
          <span class="square"></span>
        </div>
      </section>

      <section class="form-area">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-card"
          @keyup.enter="submit"
        >
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="账号" size="large" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              size="large"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>
          <el-form-item prop="captchaCode">
            <div class="captcha-row">
              <el-input v-model="form.captchaCode" placeholder="验证码" size="large" maxlength="4" />
              <img
                v-if="captcha.image"
                :src="captcha.image"
                class="captcha-image"
                alt="验证码"
                @click="loadCaptcha"
              />
              <el-button v-else text @click="loadCaptcha">加载验证码</el-button>
            </div>
          </el-form-item>
          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="submit">
            登录
          </el-button>
        </el-form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 32px;
  background: #eef1f6;
}

.login-page,
.login-page * {
  box-sizing: border-box;
}

.login-shell {
  display: flex;
  width: min(1440px, 92vw);
  height: min(820px, 84vh);
  min-height: 480px;
  overflow: hidden;
  border-radius: 12px;
  box-shadow: 0 24px 60px rgba(0, 21, 41, 0.16);
}

.brand {
  position: relative;
  display: flex;
  flex: 0 0 46%;
  flex-direction: column;
  justify-content: center;
  padding: 0 clamp(40px, 4vw, 72px);
  color: #fff;
  background: linear-gradient(135deg, #1f2d3d 0%, #304156 60%, #3a5a7a 100%);
  overflow: hidden;
}

.brand h1 {
  margin: 0 0 12px;
  font-size: clamp(24px, 2.2vw, 34px);
  letter-spacing: 2px;
}

.brand p {
  margin: 0;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.72);
}

.brand-shapes .circle,
.brand-shapes .square {
  position: absolute;
  background: rgba(64, 158, 255, 0.18);
  border-radius: 50%;
}

.brand-shapes .circle {
  right: -80px;
  bottom: -80px;
  width: 320px;
  height: 320px;
}

.brand-shapes .square {
  top: 60px;
  right: 90px;
  width: 120px;
  height: 120px;
  border-radius: 20px;
  transform: rotate(24deg);
}

.form-area {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #f5f7fa;
}

.login-card {
  width: 100%;
  max-width: 380px;
  padding: 40px 36px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 30px rgba(0, 21, 41, 0.08);
}

.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-image {
  width: 120px;
  height: 40px;
  cursor: pointer;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}

.submit-btn {
  width: 100%;
  margin-top: 6px;
}

@media (min-width: 2000px) {
  .login-card {
    max-width: 440px;
    padding: 48px 44px;
  }

  .brand h1 {
    font-size: 40px;
  }

  .brand p {
    font-size: 17px;
  }
}

@media (max-width: 900px) {
  .login-page {
    padding: 0;
  }

  .login-shell {
    width: 100%;
    height: 100vh;
    min-height: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .brand {
    display: none;
  }
}
</style>
