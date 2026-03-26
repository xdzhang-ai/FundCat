/** 后台鉴权本地存储层，负责访问令牌与刷新令牌的读写和清理。 */
const ACCESS_TOKEN_KEY = 'fundcat.access-token'
const REFRESH_TOKEN_KEY = 'fundcat.refresh-token'

export function getAccessToken() {
  return window.localStorage.getItem(ACCESS_TOKEN_KEY)
}

export const authStorage = {
  clear() {
    window.localStorage.removeItem(ACCESS_TOKEN_KEY)
    window.localStorage.removeItem(REFRESH_TOKEN_KEY)
  },
  hasToken() {
    return Boolean(getAccessToken())
  },
  save(accessToken: string, refreshToken: string) {
    window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
    window.localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  },
}

