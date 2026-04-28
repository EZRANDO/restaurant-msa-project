const TOKEN_KEY = 'customer_tokens'

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
  let data = null

  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = { message: text }
    }
  }

  if (!res.ok) {
    const fallback = res.status >= 500
      ? '서버에 연결할 수 없습니다. 백엔드 실행 상태를 확인해주세요.'
      : `요청 실패 (${res.status})`
    throw new Error(data?.message || data?.detail || fallback)
  }

  return data
}
