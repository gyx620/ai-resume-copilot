<template>
  <div class="dashboard">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-logo">Resume Copilot</div>
      <nav class="sidebar-menu">
        <div class="menu-item" :class="{active:activeSection==='upload'}" @click="activeSection='upload'">
          <el-icon><Upload /></el-icon><span>上传简历</span>
        </div>
        <div class="menu-item" :class="{active:activeSection==='list'}" @click="activeSection='list';loadResumes()">
          <el-icon><Document /></el-icon><span>简历列表</span>
        </div>
        <div class="menu-item" :class="{active:activeSection==='analyze'}" @click="activeSection='analyze'">
          <el-icon><MagicStick /></el-icon><span>AI 分析</span>
        </div>
        <div class="menu-item" :class="{active:activeSection==='history'}" @click="activeSection='history'">
          <el-icon><Clock /></el-icon><span>分析记录</span>
        </div>
      </nav>
      <div class="sidebar-footer">
        <div class="user-info">
          <el-icon><User /></el-icon>
          <span>{{ auth.username }}</span>
          <el-button text size="small" style="margin-left:auto;color:rgba(255,255,255,.45)" @click="handleLogout">退出</el-button>
        </div>
      </div>
    </aside>

    <!-- 主区域 -->
    <main class="main-area">
      <header class="topbar"><span class="topbar-title">{{ sectionTitle }}</span></header>
      <div class="content">

        <!-- ====== 上传简历 ====== -->
        <section v-if="activeSection==='upload'">
          <div class="stat-grid">
            <div class="stat-card primary"><div class="stat-label">简历总数</div><div class="stat-value">{{ resumes.length }}</div></div>
            <div class="stat-card success"><div class="stat-label">本周新增</div><div class="stat-value">{{ weekCount }}</div></div>
            <div class="stat-card warning"><div class="stat-label">待分析</div><div class="stat-value">{{ resumes.length }}</div></div>
            <div class="stat-card"><div class="stat-label">最高评分</div><div class="stat-value">{{ bestScore > 0 ? bestScore : '--' }}</div></div>
          </div>
          <div class="panel">
            <div class="panel-header"><span>上传 PDF 简历</span></div>
            <div class="upload-zone">
              <el-upload class="upload-dragger" drag :http-request="handleUpload" :show-file-list="true" accept=".pdf" :disabled="uploading" :limit="5">
                <el-icon :size="48" color="#c0c4cc"><UploadFilled /></el-icon>
                <div style="margin-top:12px;font-size:15px;color:#606266">拖拽 PDF 到此处，或<em style="color:#667eea;font-style:normal">点击上传</em></div>
                <div style="margin-top:4px;font-size:12px;color:#c0c4cc">仅支持 10 MB 以内、含可提取文字的 PDF（PDFBox）</div>
              </el-upload>
              <div v-if="uploading" style="margin-top:16px"><el-progress :percentage="uploadPercent" :stroke-width="6" color="#667eea" /></div>
            </div>
          </div>
        </section>

        <!-- ====== 简历列表 ====== -->
        <section v-if="activeSection==='list'">
          <div class="panel">
            <div class="panel-header">
              <span>我的简历</span>
              <el-button type="primary" text :icon="Refresh" @click="loadResumes">刷新</el-button>
            </div>
            <el-table :data="resumes" stripe v-loading="resumesLoading" empty-text="暂无简历" style="width:100%">
              <el-table-column prop="id" label="ID" width="70" align="center" />
              <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
              <el-table-column label="上传时间" width="170" align="center">
                <template #default="{row}">{{ formatTime(row.createTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="280" align="center" fixed="right">
                <template #default="{row}">
                  <el-button size="small" type="primary" link @click="openDetail(row)">查看</el-button>
                  <el-button size="small" type="success" link @click="openEdit(row)">编辑</el-button>
                  <el-button size="small" type="warning" link @click="startAnalyze(row)">分析</el-button>
                  <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </section>

        <!-- ====== AI 分析 ====== -->
        <section v-if="activeSection==='analyze'">
          <div class="panel">
            <div class="panel-header"><span>选择简历进行分析</span></div>
            <div style="padding:20px 24px">
              <el-select v-model="selectedResumeId" placeholder="请选择一份简历" size="large" style="width:100%" filterable>
                <el-option v-for="r in resumes" :key="r.id" :label="r.fileName" :value="r.id" />
              </el-select>
              <el-button type="primary" size="large" style="margin-top:16px;width:100%"
                @click="startAnalyzeById" :loading="analyzing" :disabled="!selectedResumeId">
                <el-icon v-if="!analyzing"><MagicStick /></el-icon>
                {{ analyzing ? 'AI 正在分析中...' : '开始 AI 分析' }}
              </el-button>
              <div v-if="analyzeError" style="margin-top:12px">
                <el-alert :title="analyzeError" type="error" show-icon :closable="false" />
              </div>
            </div>
          </div>

          <!-- 流式输出 -->
          <div class="panel" v-if="analyzing">
            <div class="panel-header">
              <span>实时分析</span>
              <el-tag type="warning" size="small">流式输出中</el-tag>
            </div>
            <div class="stream-box">
              <div class="stream-text" ref="streamEl">{{ streamText }}<span class="stream-cursor" v-if="analyzing">|</span></div>
            </div>
          </div>

          <!-- 分析结果 -->
          <div class="panel" v-if="analyzeResult && !analyzing">
            <div class="panel-header"><span>分析结果</span></div>
            <div class="result-score">
              <div class="score-num">{{ analyzeResult.score }}</div>
              <div>
                <div class="score-unit">/ 100</div>
                <el-rate :model-value="scoreStars" disabled show-score :max="10" style="margin-top:4px" />
              </div>
            </div>
            <div style="padding:0 24px 20px">
              <div style="margin-bottom:16px">
                <div class="section-label"><el-icon color="#67C23A"><CircleCheck /></el-icon> 优势</div>
                <div class="section-text" style="border-left:3px solid #67C23A">{{ analyzeResult.advantage || '无' }}</div>
              </div>
              <div style="margin-bottom:16px">
                <div class="section-label"><el-icon color="#F56C6C"><WarningFilled /></el-icon> 问题</div>
                <div class="section-text" style="border-left:3px solid #F56C6C">{{ analyzeResult.problem || '无' }}</div>
              </div>
              <div>
                <div class="section-label"><el-icon color="#409EFF"><TrendCharts /></el-icon> 优化建议</div>
                <div class="section-text" style="border-left:3px solid #409EFF">{{ analyzeResult.suggestion || '无' }}</div>
              </div>
            </div>
          </div>
        </section>

        <!-- ====== 分析记录 ====== -->
        <section v-if="activeSection==='history'">
          <div class="panel">
            <div class="panel-header"><span>分析记录</span></div>
            <el-table :data="resumes.filter(r=>r.aiScore)" stripe empty-text="暂无分析记录" style="width:100%">
              <el-table-column prop="id" label="ID" width="70" align="center" />
              <el-table-column prop="fileName" label="简历" min-width="200" show-overflow-tooltip />
              <el-table-column label="上传时间" width="170" align="center">
                <template #default="{row}">{{ formatTime(row.createTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="150" align="center">
                <template #default="{row}">
                  <el-button size="small" type="warning" link @click="startAnalyze(row)">重新分析</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </section>

      </div>
    </main>

    <!-- 查看对话框 -->
    <el-dialog v-model="detailDialog.visible" title="简历内容" width="720px" top="5vh">
      <pre class="section-text" style="max-height:460px;overflow-y:auto">{{ detailDialog.text }}</pre>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialog.visible" title="编辑简历" width="640px" top="8vh" @close="editDialog.visible=false">
      <el-form :model="editDialog.form" label-position="top">
        <el-form-item label="文件名">
          <el-input v-model="editDialog.form.fileName" />
        </el-form-item>
        <el-form-item label="简历内容">
          <el-input v-model="editDialog.form.content" type="textarea" :rows="14" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog.visible=false">取消</el-button>
        <el-button type="primary" @click="submitEdit" :loading="editDialog.loading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { resumeApi, aiApi } from '../api'
import { useAuthStore } from '../stores/auth'

import { Upload, UploadFilled, Document, MagicStick, Clock, User, Refresh, CircleCheck, WarningFilled, TrendCharts, Edit } from '@element-plus/icons-vue'

const router = useRouter()
const auth = useAuthStore()

const activeSection = ref('upload')
const sectionTitle = computed(() => ({
  upload: '上传简历', list: '简历列表', analyze: 'AI 分析', history: '分析记录'
}[activeSection.value] || ''))

const resumes = ref([])
const resumesLoading = ref(false)
const uploading = ref(false)
const uploadPercent = ref(0)
const streamEl = ref(null)

const selectedResumeId = ref(null)
const analyzing = ref(false)
const streamText = ref('')
const analyzeResult = ref(null)
const analyzeError = ref('')
const scoreStars = computed(() => analyzeResult.value ? Math.round(analyzeResult.value.score / 10) : 0)

const detailDialog = reactive({ visible: false, text: '' })
const editDialog = reactive({
  visible: false, loading: false,
  form: { id: null, fileName: '', content: '' }
})

const weekCount = computed(() => {
  const t = Date.now() - 7*24*3600*1000
  return resumes.value.filter(r => r.createTime && new Date(r.createTime).getTime() > t).length
})
const bestScore = computed(() => resumes.value.reduce((max, r) => (r.aiScore||0) > max ? r.aiScore : max, 0))

// ===== 上传 =====
async function handleUpload(options) {
  uploading.value = true
  uploadPercent.value = 30
  try {
    const { data: res } = await resumeApi.upload(options.file)
    uploadPercent.value = 100
    if (res.code === 200) { ElMessage.success('上传并解析成功'); loadResumes() }
  } catch { /* interceptor handles */ }
  finally { setTimeout(() => { uploading.value = false; uploadPercent.value = 0 }, 600) }
}

// ===== 列表 =====
async function loadResumes() {
  resumesLoading.value = true
  try {
    const { data: res } = await resumeApi.list()
    if (res.code === 200) resumes.value = res.data || []
  } catch { /* interceptor handles */ }
  finally { resumesLoading.value = false }
}

// ===== 查看 =====
async function openDetail(row) {
  try {
    const { data: res } = await resumeApi.detail(row.id)
    if (res.code === 200) {
      detailDialog.text = res.data?.content || '(简历内容为空)'
      detailDialog.visible = true
    }
  } catch { /* interceptor handles */ }
}

// ===== 编辑 =====
function openEdit(row) {
  editDialog.form.id = row.id
  editDialog.form.fileName = row.fileName || ''
  editDialog.form.content = row.content || ''
  editDialog.visible = true
}

async function submitEdit() {
  editDialog.loading = true
  try {
    const { data: res } = await resumeApi.update(editDialog.form.id, {
      fileName: editDialog.form.fileName,
      content: editDialog.form.content
    })
    if (res.code === 200) {
      ElMessage.success('已更新')
      editDialog.visible = false
      loadResumes()
    }
  } catch { /* interceptor handles */ }
  finally { editDialog.loading = false }
}

// ===== 删除 =====
async function handleDelete(row) {
  try { await ElMessageBox.confirm(`确定删除「${row.fileName}」？`, '确认删除', { type: 'warning' }) } catch { return }

  console.log('[删除] 请求: DELETE /api/resume/' + row.id)

  try {
    const resp = await resumeApi.delete(row.id)
    console.log('[删除] 完整响应:', JSON.stringify(resp))
    const res = resp.data
    if (res && res.code === 200) {
      ElMessage.success('已删除')
      await loadResumes()
    }
  } catch (err) {
    console.error('[删除] 失败: id=' + row.id, err.message || err)
  }
}

// ===== AI 分析 =====
function startAnalyze(row) {
  selectedResumeId.value = row.id
  activeSection.value = 'analyze'
  nextTick(() => startAnalyzeById())
}

async function startAnalyzeById() {
  if (!selectedResumeId.value) return ElMessage.warning('请选择简历')
  const resumeId = selectedResumeId.value
  analyzeResult.value = null
  analyzeError.value = ''
  analyzing.value = true
  streamText.value = ''

  let streamDone = false

  aiApi.analyzeStream(
    resumeId,
    token => {
      streamText.value += token
      nextTick(() => { if (streamEl.value) streamEl.value.scrollTop = streamEl.value.scrollHeight })
    },
    async () => {
      streamDone = true
      await finishAnalyze(resumeId)
    },
    async (err) => {
      if (streamDone) return
      console.warn('SSE流式失败，降级为非流式:', err.message)
      await fallbackAnalyze(resumeId)
    }
  )
}

async function finishAnalyze(resumeId) {
  analyzing.value = false
  try {
    const cleaned = streamText.value.replace(/```json|```/g, '').trim()
    analyzeResult.value = JSON.parse(cleaned)
    ElMessage.success('分析完成')
  } catch {
    analyzeResult.value = { score: 0, advantage: streamText.value, problem: '', suggestion: '' }
  }

  // 确保结果入库：调非流式接口（有 Redis 缓存，不会重复调用 AI）
  try {
    await aiApi.analyze(resumeId)
  } catch { /* ignore, streaming already showed the result */ }

  await loadResumes()
}

async function fallbackAnalyze(resumeId) {
  try {
    const { data: res } = await aiApi.analyze(resumeId)
    if (res.code === 200 && res.data) {
      analyzeResult.value = res.data
      ElMessage.success('分析完成')
    }
  } catch (e) {
    analyzeError.value = e.message || 'AI 分析失败'
    ElMessage.error(analyzeError.value)
  } finally {
    analyzing.value = false
    await loadResumes()
  }
}

// ===== 其他 =====
function handleLogout() { auth.logout(); router.push('/login') }
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '-' }

onMounted(() => { loadResumes() })
</script>
