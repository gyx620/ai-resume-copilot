const { createApp, ref, reactive, computed, watch, onMounted, nextTick } = Vue;
const { ElMessage, ElMessageBox } = ElementPlus;

const TOKEN_KEY = 'ai_resume_token';
const USER_KEY = 'ai_resume_user';

const api = {
    _headers() {
        const token = localStorage.getItem(TOKEN_KEY);
        return token ? { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' } : { 'Content-Type': 'application/json' };
    },
    async _post(url, data) {
        const res = await fetch(url, { method: 'POST', headers: this._headers(), body: JSON.stringify(data) });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
    },
    async _get(url) {
        const res = await fetch(url, { method: 'GET', headers: this._headers() });
        if (!res.ok && res.status === 401) { localStorage.clear(); location.reload(); return; }
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
    },
    async _delete(url) {
        const res = await fetch(url, { method: 'DELETE', headers: this._headers() });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
    },
    async _put(url, data) {
        const res = await fetch(url, { method: 'PUT', headers: this._headers(), body: JSON.stringify(data) });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
    },

    register(data) { return this._post('/api/user/register', data); },
    login(data) { return this._post('/api/user/login', data); },
    upload(file) {
        return new Promise((resolve, reject) => {
            const form = new FormData();
            form.append('file', file);
            const token = localStorage.getItem(TOKEN_KEY);
            fetch('/api/resume/upload', { method: 'POST', headers: token ? { 'Authorization': `Bearer ${token}` } : {}, body: form })
                .then(r => r.ok ? r.json() : r.json().then(j => { throw new Error(j.message || 'Upload failed'); }))
                .then(resolve).catch(reject);
        });
    },
    saveResume(data) { return this._post('/api/resume/save', data); },
    async getResumes() { return this._get('/api/resume/list'); },
    async getResume(id) { return this._get(`/api/resume/${id}`); },
    async deleteResume(id) { return this._delete(`/api/resume/${id}`); },
    async updateResume(id, data) { return this._put(`/api/resume/${id}`, data); },
    async analyze(resumeId) { return this._post('/api/ai/analyze', { resumeId }); },
    analyzeStream(resumeId, onToken, onDone, onError) {
        const token = localStorage.getItem(TOKEN_KEY);
        fetch('/api/ai/analyze/stream', {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
            body: JSON.stringify({ resumeId })
        }).then(async response => {
            if (!response.ok) { const j = await response.json(); throw new Error(j.message || 'Stream error'); }
            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';
            while (true) {
                const { done, value } = await reader.read();
                if (done) { onDone(); break; }
                buffer += decoder.decode(value, { stream: true });
                const lines = buffer.split('\n');
                buffer = lines.pop() || '';
                for (const line of lines) {
                    if (line.startsWith('data:')) onToken(line.substring(5).trim());
                }
            }
        }).catch(onError);
    }
};

const app = createApp({
    setup() {
        // ========== 状态 ==========
        const isLoggedIn = ref(!!localStorage.getItem(TOKEN_KEY));
        const currentUser = ref(localStorage.getItem(USER_KEY) || '');
        const authTab = ref('login');
        const authLoading = ref(false);

        const loginForm = reactive({ username: '', password: '' });
        const loginRules = { username: [{ required: true, message: '请输入用户名', trigger: 'blur' }], password: [{ required: true, message: '请输入密码', trigger: 'blur' }] };
        const loginFormRef = ref(null);

        const registerForm = reactive({ username: '', password: '', email: '' });
        const registerRules = {
            username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 3, max: 20, message: '长度 3-20 位', trigger: 'blur' }],
            password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 32, message: '长度 6-32 位', trigger: 'blur' }],
        };
        const registerFormRef = ref(null);

        const uploading = ref(false);
        const uploadPercent = ref(0);
        const resumes = ref([]);
        const resumesLoading = ref(false);

        const analyzing = ref(false);
        const streamText = ref('');
        const analyzeResult = ref(null);
        const scoreStars = computed(() => analyzeResult.value ? Math.round(analyzeResult.value.score / 10) : 0);

        const contentDialog = reactive({ visible: false, text: '' });

        // ========== 认证 ==========
        async function handleLogin() {
            if (!loginFormRef.value) return;
            try {
                await loginFormRef.value.validate();
            } catch { return; }
            authLoading.value = true;
            try {
                const res = await api.login({ username: loginForm.username, password: loginForm.password });
                if (res.code === 200 && res.data) {
                    localStorage.setItem(TOKEN_KEY, res.data);
                    localStorage.setItem(USER_KEY, loginForm.username);
                    isLoggedIn.value = true;
                    currentUser.value = loginForm.username;
                    ElMessage.success('登录成功');
                    loadResumes();
                } else {
                    ElMessage.error(res.message || '登录失败');
                }
            } catch (e) {
                ElMessage.error('登录失败: ' + e.message);
            } finally { authLoading.value = false; }
        }

        async function handleRegister() {
            if (!registerFormRef.value) return;
            try {
                await registerFormRef.value.validate();
            } catch { return; }
            authLoading.value = true;
            try {
                const res = await api.register({ username: registerForm.username, password: registerForm.password, email: registerForm.email });
                if (res.code === 200) {
                    ElMessage.success('注册成功，请登录');
                    authTab.value = 'login';
                    loginForm.username = registerForm.username;
                    loginForm.password = '';
                } else {
                    ElMessage.error(res.message || '注册失败');
                }
            } catch (e) {
                ElMessage.error('注册失败: ' + e.message);
            } finally { authLoading.value = false; }
        }

        function handleLogout() {
            localStorage.clear();
            isLoggedIn.value = false;
            resumes.value = [];
            analyzeResult.value = null;
        }

        // ========== 上传 ==========
        async function handleUpload(options) {
            uploading.value = true;
            uploadPercent.value = 30;
            try {
                const res = await api.upload(options.file);
                uploadPercent.value = 100;
                if (res.code === 200) {
                    ElMessage.success('简历上传并解析成功');
                    loadResumes();
                } else {
                    ElMessage.error(res.message || '上传失败');
                }
            } catch (e) {
                ElMessage.error('上传失败: ' + e.message);
            } finally {
                setTimeout(() => { uploading.value = false; uploadPercent.value = 0; }, 800);
            }
        }

        // ========== 简历管理 ==========
        async function loadResumes() {
            resumesLoading.value = true;
            try {
                const res = await api.getResumes();
                if (res.code === 200) resumes.value = res.data || [];
            } catch (e) {
                ElMessage.error('加载简历列表失败');
            } finally { resumesLoading.value = false; }
        }

        function viewResume(row) {
            api.getResume(row.id).then(res => {
                if (res.code === 200 && res.data) {
                    contentDialog.text = res.data.content || '(简历内容为空)';
                    contentDialog.visible = true;
                }
            }).catch(() => ElMessage.error('获取简历内容失败'));
        }

        async function deleteResume(row) {
            try {
                await ElMessageBox.confirm(`确定删除「${row.fileName}」？`, '确认删除', { type: 'warning' });
            } catch { return; }
            try {
                const res = await api.deleteResume(row.id);
                if (res.code === 200) { ElMessage.success('删除成功'); loadResumes(); }
                else ElMessage.error(res.message || '删除失败');
            } catch (e) { ElMessage.error('删除失败: ' + e.message); }
        }

        // ========== AI 分析 ==========
        async function analyzeResume(row) {
            analyzeResult.value = null;
            analyzing.value = true;
            streamText.value = '';

            api.analyzeStream(
                row.id,
                token => { streamText.value += token; nextTick(() => { const el = document.querySelector('.stream-content'); if (el) el.scrollTop = el.scrollHeight; }); },
                () => {
                    analyzing.value = false;
                    try {
                        const cleaned = streamText.value.replace(/```json|```/g, '').trim();
                        analyzeResult.value = JSON.parse(cleaned);
                        ElMessage.success('AI 分析完成');
                    } catch {
                        analyzeResult.value = { score: 0, advantage: streamText.value, problem: '', suggestion: '' };
                    }
                },
                err => {
                    analyzing.value = false;
                    ElMessage.error('分析失败: ' + err.message);
                }
            );
        }

        // ========== 工具 ==========
        function formatTime(t) { if (!t) return '-'; return new Date(t).toLocaleString('zh-CN'); }

        // ========== 生命周期 ==========
        onMounted(() => { if (isLoggedIn.value) loadResumes(); });

        return {
            isLoggedIn, currentUser, authTab, authLoading,
            loginForm, loginRules, loginFormRef,
            registerForm, registerRules, registerFormRef,
            uploading, uploadPercent, resumes, resumesLoading,
            analyzing, streamText, analyzeResult, scoreStars,
            contentDialog,
            handleLogin, handleRegister, handleLogout,
            handleUpload, loadResumes, viewResume, deleteResume, analyzeResume,
            formatTime
        };
    }
});

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
}
app.use(ElementPlus, { locale: ElementPlusLocaleZhCn });
app.mount('#app');
