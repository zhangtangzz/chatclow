import request from './request'

// 同步聊天
export function chatSync(data) {
  return request.post('/chat/send', data)
}

/**
 * SSE 流式聊天
 * 用 fetch + ReadableStream 逐行读取
 * @param {AbortSignal} [signal] - 可选，用于中断请求
 */
export async function chatStream({ agentId, userId, message, conversationId, memoryEnabled, fileIds }, onEvent, signal) {
  const token = localStorage.getItem('token')
  const response = await fetch('/api/chat/send-stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify({ agentId, userId, message, conversationId, memoryEnabled, fileIds }),
    signal
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })

    // 按换行拆分，处理完整的 SSE 行
    const lines = buffer.split('\n')
    buffer = lines.pop() // 最后一行可能不完整，留着下次拼

    for (const line of lines) {
      const trimmed = line.trim()
      if (!trimmed.startsWith('data:')) continue

      const jsonStr = trimmed.slice(5).trim()
      if (!jsonStr || jsonStr === '[DONE]') continue

      try {
        const event = JSON.parse(jsonStr)
        onEvent(event) // 回调：{ type, data }
      } catch (e) {
        // 解析失败跳过
      }
    }
  }
}

// 获取会话列表
export function getConversations(userId) {
  return request.get('/conversation/list', { params: { userId } })
}

// 创建新会话
export function createConversation(userId, title) {
  return request.post('/conversation/create', null, {
    params: { userId, title }
  })
}

// 获取会话历史消息
export function getConversationRecords(conversationId) {
  return request.get('/record/list', { params: { conversationId } })
}

// 删除会话
export function deleteConversation(id) {
  return request.delete(`/conversation/delete/${id}`)
}

// 清除会话记忆
export function clearConversationMemory(convId) {
  return request.delete(`/conversation/memory/${convId}`)
}

// 对话文件上传
export function uploadChatFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/chat/upload', formData)
}
