import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({ timeout: 60000 })

http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  res => {
    const body = res.data
    if (body && typeof body.code === 'number' && body.code !== 200) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return res
  },
  err => {
    if (err.response?.status === 401) {
      localStorage.clear()
      window.location.href = '/login'
      return Promise.reject(err)
    }
    const msg = err.response?.data?.message || err.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

export default http

export const userApi = {
  login: data => http.post('/api/user/login', data),
  register: data => http.post('/api/user/register', data)
}

export const resumeApi = {
  upload: file => {
    const form = new FormData()
    form.append('file', file)
    return http.post('/api/resume/upload', form)
  },
  list: () => http.get('/api/resume/list'),
  detail: id => http.get(`/api/resume/${id}`),
  delete: id => http.delete(`/api/resume/${id}`),
  update: (id, data) => http.put(`/api/resume/${id}`, data)
}

export const aiApi = {
  analyze: resumeId => http.post('/api/ai/analyze', { resumeId }),
  analyzeStream(resumeId, onToken, onDone, onError) {
    const token = localStorage.getItem('token')
    return fetch('/api/ai/analyze/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify({ resumeId })
    }).then(async res => {
      if (!res.ok) {
        const j = await res.json().catch(() => ({}))
        throw new Error(j.message || `HTTP ${res.status}`)
      }
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) { onDone(); break }
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        for (const line of lines) {
          if (line.startsWith('data:')) onToken(line.substring(5).trim())
        }
      }
    }).catch(onError)
  }
}
