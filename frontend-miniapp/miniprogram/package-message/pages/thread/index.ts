import { redirectToLogin } from '../../../core/auth/access'
import { ApiError, errorMessage } from '../../../core/models/api'
import { chatMessageApi } from '../../../features/message/data/message-api'
import type { ChatMessage } from '../../../features/message/domain/message-models'

Page({
  data: {
    sessionId: '',
    messages: [] as ChatMessage[],
    content: '',
    loading: false,
    sending: false,
    error: '',
  },

  async onLoad(options: Record<string, string | undefined>) {
    const sessionId = options.id ?? ''
    this.setData({ sessionId })
    if (!sessionId) {
      this.setData({ error: '缺少会话编号' })
      return
    }
    await this.load()
  },

  onContentInput(event: { detail: { value: string } }) {
    this.setData({ content: event.detail.value })
  },

  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const messages = await chatMessageApi.list(this.data.sessionId)
      this.setData({ messages })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    } finally {
      this.setData({ loading: false })
    }
  },

  async send() {
    const content = this.data.content.trim()
    if (!content || !this.data.sessionId) return
    this.setData({ sending: true, error: '' })
    try {
      const message = await chatMessageApi.send(this.data.sessionId, { content })
      this.setData({ content: '', messages: [...this.data.messages, message] })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    } finally {
      this.setData({ sending: false })
    }
  },
})
