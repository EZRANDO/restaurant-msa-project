const TOKEN_KEY = 'admin_tokens'

export function getSession() {
  try {
    return JSON.parse(localStorage.getItem(TOKEN_KEY) || 'null')
  } catch {
    return null
  }
}

export function saveSession(session) {
  localStorage.setItem(TOKEN_KEY, JSON.stringify(session))
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
}

export async function api(path, options = {}) {
  const session = getSession()
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }

  if (session?.accessToken) {
    headers.Authorization = `Bearer ${session.accessToken}`
  }

  const res = await fetch(`/api${path}`, {
    ...options,
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  })

  if (res.status === 204) return null
  const text = await res.text()
  const data = text ? JSON.parse(text) : null

  if (!res.ok) {
    throw new Error(data?.message || data?.detail || `요청 실패 (${res.status})`)
  }

  return data
}
