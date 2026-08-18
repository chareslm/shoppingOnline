import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'

const STARTUP_RETRY_LIMIT = 45
const STARTUP_RETRY_DELAY_MS = 2000

type RetryConfig = InternalAxiosRequestConfig & { _startupRetries?: number; _retried?: boolean }

export function isBackendUnavailable(error: AxiosError) {
  if (error.response) {
    return false
  }
  const code = error.code ?? ''
  return (
    code === 'ERR_NETWORK' ||
    code === 'ECONNREFUSED' ||
    code === 'ECONNRESET' ||
    code === 'ERR_CONNECTION_REFUSED' ||
    code === 'ECONNABORTED'
  )
}

export async function retryWhileBackendStarts<T>(
  error: AxiosError,
  replay: (config: RetryConfig) => Promise<T>,
): Promise<T> {
  const request = error.config as RetryConfig | undefined
  if (!request || !isBackendUnavailable(error)) {
    return Promise.reject(error)
  }
  request._startupRetries = (request._startupRetries ?? 0) + 1
  if (request._startupRetries > STARTUP_RETRY_LIMIT) {
    return Promise.reject(error)
  }
  await new Promise((resolve) => setTimeout(resolve, STARTUP_RETRY_DELAY_MS))
  return replay(request)
}
