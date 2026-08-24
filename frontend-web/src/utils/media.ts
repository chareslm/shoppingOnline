export function mediaUrl(path: string | null | undefined) {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  const base = (import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080').replace(/\/$/, '')
  return path.startsWith('/') ? `${base}${path}` : `${base}/${path}`
}
