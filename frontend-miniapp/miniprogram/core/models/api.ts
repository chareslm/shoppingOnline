export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly statusCode: number,
    readonly code?: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}

