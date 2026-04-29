/**
 * AI Chat Application
 * Entry point: index.html
 */

const $ = id => document.getElementById(id);

const userIdInput = $('userIdInput');
const randomUserIdBtn = $('randomUserIdBtn');
const agentSelect = $('agentSelect');
const agentDesc = $('agentDesc');
const refreshAgentBtn = $('refreshAgentBtn');
const newSessionBtn = $('newSessionBtn');
const clearChatBtn = $('clearChatBtn');
const sessionListEl = $('sessionList');
const chatMessages = $('chatMessages');
const welcomeScreen = $('welcomeScreen');
const welcomeHints = $('welcomeHints');
const messageInput = $('messageInput');
const attachmentInput = $('attachmentInput');
const attachmentBtn = $('attachmentBtn');
const fileUriBtn = $('fileUriBtn');
const fileUriInput = $('fileUriInput');
const fileUriMimeInput = $('fileUriMimeInput');
const addFileUriConfirmBtn = $('addFileUriConfirmBtn');
const attachmentPreview = $('attachmentPreview');
const sendBtn = $('sendBtn');
const streamToggle = $('streamToggle');
const expiredModal = $('expiredModal');
const modalNewSessionBtn = $('modalNewSessionBtn');
const modalContinueBtn = $('modalContinueBtn');
const loadingToast = $('loadingToast');

let currentSession = null;
let isLoading = false;
let isStreaming = false;
let agentList = [];
let pendingAttachments = [];
let pendingFileUris = [];

const LS_SESSIONS = 'chat_sessions';
const LS_USER_ID = 'chat_user_id';

marked.setOptions({
    highlight(code, lang) {
        if (lang && hljs.getLanguage(lang)) {
            try {
                return hljs.highlight(code, { language: lang }).value;
            } catch (err) {
                console.warn('highlight failed:', err);
            }
        }
        return hljs.highlightAuto(code).value;
    },
    breaks: true,
    gfm: true
});

function renderMarkdown(content) {
    if (!content) return '';
    return marked.parse(content);
}

function renderMarkdownStreaming(content) {
    if (!content) return '';
    const fenceCount = (content.match(/```/g) || []).length;
    const parseSource = fenceCount % 2 === 1 ? `${content}\n\`\`\`\n` : content;
    return marked.parse(parseSource);
}

function escHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

async function apiGet(path) {
    const res = await fetch(`${API_BASE}${path}`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' }
    });
    return res.json();
}

async function apiPost(path, body) {
    const res = await fetch(`${API_BASE}${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });
    return res.json();
}

function isSuccess(res) {
    return res && res.code === 'SUCCESS_0000';
}

function showLoading(text = 'AI 思考中...') {
    loadingToast.querySelector('span').textContent = text;
    loadingToast.classList.add('show');
    isLoading = true;
    updateSendBtnState();
}

function hideLoading() {
    loadingToast.classList.remove('show');
    isLoading = false;
    updateSendBtnState();
}

function formatTime(date) {
    const now = new Date();
    const current = new Date(date);
    const isToday = current.toDateString() === now.toDateString();
    const yyyy = current.getFullYear();
    const mm = String(current.getMonth() + 1).padStart(2, '0');
    const dd = String(current.getDate()).padStart(2, '0');
    const hh = String(current.getHours()).padStart(2, '0');
    const min = String(current.getMinutes()).padStart(2, '0');
    return isToday ? `今天 ${hh}:${min}` : `${yyyy}-${mm}-${dd} ${hh}:${min}`;
}

function nowStr() {
    return new Date().toISOString();
}

function generateUserId() {
    return `user_${Math.random().toString(36).substring(2, 10)}${Date.now().toString(36)}`;
}

function saveUserId(uid) {
    localStorage.setItem(LS_USER_ID, uid);
}

function loadUserId() {
    return localStorage.getItem(LS_USER_ID) || '';
}

function initUserId() {
    const saved = loadUserId();
    if (saved) {
        userIdInput.value = saved;
    }
}

function getStoredSessions() {
    try {
        return JSON.parse(localStorage.getItem(LS_SESSIONS) || '[]');
    } catch {
        return [];
    }
}

function saveSessions(sessions) {
    localStorage.setItem(LS_SESSIONS, JSON.stringify(sessions));
}

function addSession(session) {
    const sessions = getStoredSessions().filter(item => item.sessionId !== session.sessionId);
    sessions.push(session);
    saveSessions(sessions);
}

function updateCurrentSession(lastMessage) {
    if (!currentSession) return;
    currentSession.lastMessage = lastMessage;
    currentSession.lastTime = nowStr();
    addSession(currentSession);
    renderSessionList();
}

function clearChatMessages() {
    chatMessages.innerHTML = '';
    chatMessages.appendChild(welcomeScreen);
}

function clearPendingAttachments() {
    pendingAttachments = [];
    pendingFileUris = [];
    attachmentInput.value = '';
    fileUriInput.value = '';
    fileUriMimeInput.value = '';
    renderAttachmentPreview();
    updateSendBtnState();
}

function formatFileSize(size) {
    if (!Number.isFinite(size) || size <= 0) return '0 B';
    if (size < 1024) return `${size} B`;
    if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function guessMimeTypeFromUri(uri) {
    const normalized = uri.split('?')[0].toLowerCase();
    if (normalized.endsWith('.png')) return 'image/png';
    if (normalized.endsWith('.jpg') || normalized.endsWith('.jpeg')) return 'image/jpeg';
    if (normalized.endsWith('.gif')) return 'image/gif';
    if (normalized.endsWith('.webp')) return 'image/webp';
    if (normalized.endsWith('.bmp')) return 'image/bmp';
    if (normalized.endsWith('.svg')) return 'image/svg+xml';
    return '';
}

function renderAttachmentPreview() {
    attachmentPreview.innerHTML = '';

    pendingFileUris.forEach((file, index) => {
        const chip = document.createElement('div');
        chip.className = 'attachment-chip attachment-chip-uri';
        chip.innerHTML = `
            <span class="attachment-chip-name">${escHtml(file.fileUri)}</span>
            <span class="attachment-chip-meta">${escHtml(file.mimeType || 'auto mime')}</span>
            <button class="attachment-chip-remove" type="button">×</button>
        `;
        chip.querySelector('.attachment-chip-remove').addEventListener('click', () => {
            pendingFileUris.splice(index, 1);
            renderAttachmentPreview();
            updateSendBtnState();
        });
        attachmentPreview.appendChild(chip);
    });

    pendingAttachments.forEach((file, index) => {
        const chip = document.createElement('div');
        chip.className = 'attachment-chip';
        chip.innerHTML = `
            <span class="attachment-chip-name">${escHtml(file.name)}</span>
            <span class="attachment-chip-meta">${formatFileSize(file.size)}</span>
            <button class="attachment-chip-remove" type="button">×</button>
        `;
        chip.querySelector('.attachment-chip-remove').addEventListener('click', () => {
            pendingAttachments.splice(index, 1);
            renderAttachmentPreview();
            updateSendBtnState();
        });
        attachmentPreview.appendChild(chip);
    });
}

function readAttachment(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve({
            name: file.name,
            mimeType: file.type || 'application/octet-stream',
            size: file.size,
            data: reader.result
        });
        reader.onerror = () => reject(reader.error || new Error('read failed'));
        reader.readAsDataURL(file);
    });
}

function addPendingFileUri() {
    const fileUri = fileUriInput.value.trim();
    if (!fileUri) return;

    pendingFileUris.push({
        fileUri,
        mimeType: fileUriMimeInput.value.trim() || guessMimeTypeFromUri(fileUri) || 'image/png'
    });

    fileUriInput.value = '';
    fileUriMimeInput.value = '';
    renderAttachmentPreview();
    updateSendBtnState();
}

function buildChatPayload(userId, sessionId, agentId, message, attachments, fileUris) {
    return {
        agentId,
        userId,
        sessionId,
        texts: message ? [{ message }] : [],
        files: fileUris.map(item => ({
            fileUri: item.fileUri,
            mimeType: item.mimeType
        })),
        inlineData: attachments.map(item => ({
            data: item.data,
            mimeType: item.mimeType,
            fileName: item.name
        }))
    };
}

function buildUserMessageContent(message, attachments, fileUris) {
    const sections = [];
    if (message) {
        sections.push(message);
    }
    if (fileUris.length > 0) {
        const fileUriLines = fileUris
            .map(item => `- ${item.fileUri} (${item.mimeType || 'auto mime'})`)
            .join('\n');
        sections.push(`URI 附件：\n${fileUriLines}`);
    }
    if (attachments.length > 0) {
        const attachmentLines = attachments
            .map(item => `- ${item.name} (${item.mimeType}, ${formatFileSize(item.size)})`)
            .join('\n');
        sections.push(`本地附件：\n${attachmentLines}`);
    }
    return sections.join('\n\n');
}

function updateSendBtnState() {
    const hasUserId = !!userIdInput.value.trim();
    const hasAgent = !!agentSelect.value;
    const hasContent = !!messageInput.value.trim() || pendingAttachments.length > 0 || pendingFileUris.length > 0;

    sendBtn.disabled = !(hasUserId && hasAgent && hasContent && !isLoading);
    messageInput.disabled = !(hasUserId && hasAgent);
    attachmentBtn.disabled = !(hasUserId && hasAgent) || isLoading;
    fileUriBtn.disabled = !(hasUserId && hasAgent) || isLoading;
    fileUriInput.disabled = !(hasUserId && hasAgent) || isLoading;
    fileUriMimeInput.disabled = !(hasUserId && hasAgent) || isLoading;
    addFileUriConfirmBtn.disabled = !(hasUserId && hasAgent) || isLoading || !fileUriInput.value.trim();
    newSessionBtn.disabled = !(hasUserId && hasAgent);
    clearChatBtn.disabled = !currentSession;
}

function autoResize(el) {
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 120)}px`;
}

function showToast(msg) {
    const existing = document.querySelector('.app-toast');
    if (existing) existing.remove();

    const toast = document.createElement('div');
    toast.className = 'app-toast';
    toast.textContent = msg;
    document.body.appendChild(toast);

    requestAnimationFrame(() => toast.classList.add('show'));

    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 2000);
}

function appendMessage(role, content) {
    if (welcomeScreen.style.display !== 'none') {
        welcomeScreen.style.display = 'none';
    }

    const msgEl = document.createElement('div');
    msgEl.className = `message ${role}`;
    const avatarText = role === 'user' ? 'U' : 'AI';

    msgEl.innerHTML = `
        <div class="message-avatar">${avatarText}</div>
        <div class="message-body">
            <div class="message-content"></div>
            <div class="message-time">${formatTime(new Date())}</div>
        </div>
    `;

    msgEl.querySelector('.message-content').innerHTML = renderMarkdown(content);
    chatMessages.appendChild(msgEl);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function renderSessionMessages(messages) {
    chatMessages.innerHTML = '';
    chatMessages.appendChild(welcomeScreen);
    welcomeScreen.style.display = 'none';

    messages.forEach(msg => {
        const msgEl = document.createElement('div');
        msgEl.className = `message ${msg.role}`;
        const avatarText = msg.role === 'user' ? 'U' : 'AI';
        msgEl.innerHTML = `
            <div class="message-avatar">${avatarText}</div>
            <div class="message-body">
                <div class="message-content"></div>
                <div class="message-time">${formatTime(new Date(msg.timestamp))}</div>
            </div>
        `;
        msgEl.querySelector('.message-content').innerHTML = renderMarkdown(msg.content || '');
        chatMessages.appendChild(msgEl);
    });
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function renderSessionList() {
    const sessions = getStoredSessions();
    if (sessions.length === 0) {
        sessionListEl.innerHTML = '<div class="sidebar-empty">暂无会话记录<br>新建一个开始对话吧</div>';
        return;
    }

    sessionListEl.innerHTML = '';
    [...sessions].reverse().forEach(session => {
        const item = document.createElement('div');
        item.className = `session-item${currentSession && currentSession.sessionId === session.sessionId ? ' active' : ''}`;
        item.innerHTML = `
            <div class="session-item-name">${escHtml(session.agentName || '会话')}</div>
            <div class="session-item-meta">${escHtml(session.lastMessage || '新会话')}</div>
            <button class="session-item-delete" title="删除此会话">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
            </button>
        `;

        item.querySelector('.session-item-delete').addEventListener('click', event => {
            event.stopPropagation();
            deleteSession(session.sessionId);
        });

        item.addEventListener('click', () => resumeSession(session));
        sessionListEl.appendChild(item);
    });
}

function deleteSession(sessionId) {
    const sessions = getStoredSessions().filter(item => item.sessionId !== sessionId);
    saveSessions(sessions);
    if (currentSession && currentSession.sessionId === sessionId) {
        currentSession = null;
        clearChatMessages();
        welcomeScreen.style.display = '';
        clearPendingAttachments();
    }
    renderSessionList();
    updateSendBtnState();
    showToast('会话已删除');
}

function showExpiredModal(session) {
    expiredModal.classList.add('show');

    modalNewSessionBtn.onclick = () => {
        expiredModal.classList.remove('show');
        deleteSession(session.sessionId);
        agentSelect.value = '';
        agentDesc.textContent = '';
        currentSession = null;
        welcomeScreen.style.display = '';
        clearChatMessages();
        renderSessionList();
        updateSendBtnState();
    };

    modalContinueBtn.onclick = () => {
        expiredModal.classList.remove('show');
        currentSession = session;
        userIdInput.value = session.userId;
        saveUserId(session.userId);
        agentSelect.value = session.agentId;
        agentDesc.textContent = session.agentName || '';
        welcomeScreen.style.display = 'none';
        renderSessionList();
        updateSendBtnState();
        renderSessionMessages(session.messages || []);
        showToast('已恢复会话，但旧上下文可能已失效');
    };
}

async function resumeSession(session) {
    if (currentSession && currentSession.sessionId === session.sessionId) return;

    try {
        const res = await apiPost('/agent/validateSessionId', {
            agentId: session.agentId,
            userId: session.userId,
            sessionId: session.sessionId
        });

        if (isSuccess(res) && res.data === true) {
            currentSession = session;
            userIdInput.value = session.userId;
            saveUserId(session.userId);
            agentSelect.value = session.agentId;
            agentDesc.textContent = session.agentName || '';
            welcomeScreen.style.display = 'none';
            renderSessionList();
            renderSessionMessages(session.messages || []);
            updateSendBtnState();
            showToast('已恢复会话');
            return;
        }
        showExpiredModal(session);
    } catch (error) {
        console.error('Session validation failed:', error);
        showToast('会话校验失败');
    }
}

async function loadAgentList(showMsg = true) {
    try {
        refreshAgentBtn.disabled = true;
        refreshAgentBtn.classList.add('spinning');
        const res = await apiGet('/agent/query_ai_agent_config_list');
        if (isSuccess(res) && res.data) {
            agentList = res.data;
            renderAgentOptions();
            renderWelcomeCards();
            if (showMsg) showToast(`已加载 ${agentList.length} 个智能体`);
        } else {
            showToast(`加载智能体列表失败: ${res.info || '未知错误'}`);
        }
    } catch (error) {
        console.error('Failed to load agent list:', error);
        showToast('加载智能体列表失败，请检查网络');
    } finally {
        refreshAgentBtn.disabled = false;
        refreshAgentBtn.classList.remove('spinning');
    }
}

function renderAgentOptions() {
    agentSelect.innerHTML = '<option value="">-- 请选择智能体 --</option>';
    agentList.forEach(agent => {
        const opt = document.createElement('option');
        opt.value = agent.agentId;
        opt.textContent = agent.agentName;
        opt.dataset.desc = agent.agentDesc || '';
        agentSelect.appendChild(opt);
    });
}

function renderWelcomeCards() {
    welcomeHints.innerHTML = '';
    agentList.forEach(agent => {
        const card = document.createElement('div');
        card.className = 'agent-card';
        card.innerHTML = `
            <div class="agent-card-name">${escHtml(agent.agentName)}</div>
            <div class="agent-card-desc">${escHtml(agent.agentDesc || '暂无描述')}</div>
        `;
        card.addEventListener('click', () => {
            if (!userIdInput.value.trim()) {
                userIdInput.focus();
                userIdInput.style.borderColor = '#e53e3e';
                setTimeout(() => {
                    userIdInput.style.borderColor = '';
                }, 1500);
                return;
            }
            agentSelect.value = agent.agentId;
            agentDesc.textContent = agent.agentDesc || '';
            updateSendBtnState();
            messageInput.focus();
            showToast(`已选择智能体 "${agent.agentName}"`);
        });
        welcomeHints.appendChild(card);
    });
}

async function startSession() {
    if (!userIdInput.value.trim() || !agentSelect.value) return;

    try {
        const res = await apiPost('/agent/create_session', {
            agentId: agentSelect.value,
            userId: userIdInput.value.trim()
        });

        if (isSuccess(res) && res.data && res.data.sessionId) {
            const selected = agentSelect.options[agentSelect.selectedIndex];
            currentSession = {
                sessionId: res.data.sessionId,
                userId: userIdInput.value.trim(),
                agentId: agentSelect.value,
                agentName: selected.textContent,
                lastMessage: '新会话',
                lastTime: nowStr(),
                messages: []
            };
            addSession(currentSession);
            clearChatMessages();
            welcomeScreen.style.display = 'none';
            renderSessionList();
            updateSendBtnState();
            messageInput.focus();
            return;
        }

        showToast(`创建会话失败: ${res.info || '未知错误'}`);
    } catch (error) {
        console.error('Create session error:', error);
        showToast('创建会话失败，请检查网络');
    }
}

async function handleNormalChat(payload) {
    const res = await apiPost('/agent/chat', payload);
    hideLoading();

    if (isSuccess(res) && res.data && res.data.content) {
        appendMessage('ai', res.data.content);
        currentSession.messages.push({ role: 'ai', content: res.data.content, timestamp: Date.now() });
        updateCurrentSession(res.data.content.replace(/[#*`>\-\[\]]/g, '').trim().substring(0, 30));
        return;
    }

    appendMessage('ai', `请求失败: ${res.info || '未知错误'}`);
}

async function handleStreamChat(payload) {
    isStreaming = true;

    const msgEl = document.createElement('div');
    msgEl.className = 'message ai';
    msgEl.innerHTML = `
        <div class="message-avatar">AI</div>
        <div class="message-body">
            <div class="message-content streaming-content"></div>
            <div class="message-time">${formatTime(new Date())}</div>
        </div>
    `;
    const contentEl = msgEl.querySelector('.message-content');
    chatMessages.appendChild(msgEl);

    if (welcomeScreen.style.display !== 'none') {
        welcomeScreen.style.display = 'none';
    }

    let buffer = '';
    let pollTimer = null;
    const flush = () => {
        pollTimer = null;
        if (!buffer) return;
        contentEl.innerHTML = renderMarkdownStreaming(buffer);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    };
    const scheduleFlush = () => {
        if (!pollTimer) {
            pollTimer = setTimeout(flush, 100);
        }
    };

    try {
        const resp = await fetch(`${API_BASE}/agent/chat_stream`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const reader = resp.body.getReader();
        const decoder = new TextDecoder();

        const readStream = () => {
            reader.read().then(({ done, value }) => {
                if (done) {
                    if (pollTimer) clearTimeout(pollTimer);
                    contentEl.innerHTML = renderMarkdown(buffer);
                    contentEl.classList.remove('streaming-content');
                    currentSession.messages.push({ role: 'ai', content: buffer, timestamp: Date.now() });
                    updateCurrentSession(buffer.replace(/[#*`>\-\[\]]/g, '').trim().substring(0, 30));
                    isStreaming = false;
                    hideLoading();
                    return;
                }

                const chunk = decoder.decode(value, { stream: true });
                const lines = chunk.split('\n');
                for (const line of lines) {
                    if (line === 'data:' || line === 'data: [DONE]' || line === 'data:[DONE]') {
                        continue;
                    }
                    if (line.startsWith('data: ')) {
                        const raw = line.substring(6);
                        if (raw && raw !== '[DONE]') {
                            buffer += JSON.parse(raw);
                        }
                        scheduleFlush();
                    } else {
                        buffer += line;
                    }
                }
                readStream();
            }).catch(error => {
                console.error('Stream error:', error);
                if (pollTimer) clearTimeout(pollTimer);
                if (buffer) {
                    contentEl.innerHTML = renderMarkdown(buffer);
                }
                contentEl.classList.remove('streaming-content');
                isStreaming = false;
                hideLoading();
            });
        };

        readStream();
    } catch (error) {
        console.error('Chat error:', error);
        hideLoading();
        isStreaming = false;
        appendMessage('ai', '网络错误，请稍后重试');
    }
}

async function handleSend() {
    if (isLoading) return;

    const message = messageInput.value.trim();
    if (!message && pendingAttachments.length === 0 && pendingFileUris.length === 0) return;

    if (!currentSession) {
        await startSession();
        if (!currentSession) return;
    }

    const userId = userIdInput.value.trim();
    const { sessionId, agentId } = currentSession;
    const attachments = pendingAttachments.map(item => ({ ...item }));
    const fileUris = pendingFileUris.map(item => ({ ...item }));
    const payload = buildChatPayload(userId, sessionId, agentId, message, attachments, fileUris);
    const displayContent = buildUserMessageContent(message, attachments, fileUris);
    const sessionPreview = message
        || (fileUris[0] ? `[URI] ${fileUris[0].fileUri}` : '')
        || (attachments[0] ? `[附件] ${attachments[0].name}` : '多模态消息');

    appendMessage('user', displayContent);
    currentSession.messages.push({ role: 'user', content: displayContent, timestamp: Date.now() });
    updateCurrentSession(sessionPreview.length > 20 ? `${sessionPreview.substring(0, 20)}...` : sessionPreview);

    messageInput.value = '';
    autoResize(messageInput);
    clearPendingAttachments();

    showLoading(streamToggle.checked ? 'AI 思考中（流式）...' : 'AI 思考中...');

    try {
        if (streamToggle.checked) {
            await handleStreamChat(payload);
        } else {
            await handleNormalChat(payload);
        }
    } catch (error) {
        hideLoading();
        console.error('Chat error:', error);
        appendMessage('ai', '网络错误，请稍后重试');
    }
}

randomUserIdBtn.addEventListener('click', () => {
    userIdInput.value = generateUserId();
    saveUserId(userIdInput.value);
    updateSendBtnState();
});

userIdInput.addEventListener('input', () => {
    if (userIdInput.value.trim()) {
        saveUserId(userIdInput.value.trim());
    }
    updateSendBtnState();
});

refreshAgentBtn.addEventListener('click', () => loadAgentList());

agentSelect.addEventListener('change', () => {
    const selected = agentSelect.options[agentSelect.selectedIndex];
    agentDesc.textContent = selected.dataset.desc || '';
    updateSendBtnState();
});

messageInput.addEventListener('input', () => {
    autoResize(messageInput);
    updateSendBtnState();
});

messageInput.addEventListener('keydown', event => {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        if (!sendBtn.disabled) {
            handleSend();
        }
    }
});

fileUriInput.addEventListener('input', () => {
    updateSendBtnState();
});

fileUriMimeInput.addEventListener('input', () => {
    updateSendBtnState();
});

fileUriInput.addEventListener('keydown', event => {
    if (event.key === 'Enter') {
        event.preventDefault();
        if (!addFileUriConfirmBtn.disabled) {
            addPendingFileUri();
        }
    }
});

attachmentBtn.addEventListener('click', () => {
    if (!attachmentBtn.disabled) {
        attachmentInput.click();
    }
});

fileUriBtn.addEventListener('click', () => {
    if (!fileUriBtn.disabled) {
        fileUriInput.focus();
    }
});

addFileUriConfirmBtn.addEventListener('click', addPendingFileUri);

attachmentInput.addEventListener('change', async event => {
    const files = Array.from(event.target.files || []);
    if (files.length === 0) return;

    try {
        const attachments = await Promise.all(files.map(readAttachment));
        pendingAttachments = pendingAttachments.concat(attachments);
        renderAttachmentPreview();
        updateSendBtnState();
    } catch (error) {
        console.error('Failed to read attachments:', error);
        showToast('附件读取失败，请重试');
    } finally {
        attachmentInput.value = '';
    }
});

sendBtn.addEventListener('click', handleSend);

newSessionBtn.addEventListener('click', () => {
    if (!userIdInput.value.trim() || !agentSelect.value) return;
    currentSession = null;
    clearChatMessages();
    clearPendingAttachments();
    welcomeScreen.style.display = '';
    renderSessionList();
    updateSendBtnState();
    showToast('已重置，发送消息后会创建新会话');
});

clearChatBtn.addEventListener('click', () => {
    if (!currentSession) return;
    clearChatMessages();
    currentSession.messages = [];
    updateCurrentSession('新会话');
    updateSendBtnState();
});

(function injectToastStyles() {
    const style = document.createElement('style');
    style.textContent = `
        .app-toast {
            position: fixed;
            top: 80px;
            left: 50%;
            transform: translateX(-50%) translateY(-10px);
            background: rgba(30,30,30,0.88);
            color: #fff;
            padding: 8px 20px;
            border-radius: 50px;
            font-size: 13px;
            z-index: 300;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            backdrop-filter: blur(8px);
            opacity: 0;
            transition: all 0.3s ease;
            pointer-events: none;
        }
        .app-toast.show {
            opacity: 1;
            transform: translateX(-50%) translateY(0);
        }
    `;
    document.head.appendChild(style);
})();

async function init() {
    initUserId();
    renderSessionList();
    renderAttachmentPreview();
    updateSendBtnState();
    await loadAgentList(false);
    updateSendBtnState();
}

init();
