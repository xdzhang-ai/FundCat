/** 认证模块契约，仅描述前端目前真正消费的登录结果和登录入参。 */
import type { UserProfile } from './shared'

/** 访问令牌对，前端当前使用 Bearer Token 方案。 */
export type AuthTokens = {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

/** 登录成功后返回令牌和用户资料。 */
export type AuthResponse = AuthTokens & {
  profile: UserProfile
}

/** 登录入参保持最小化，仅包含用户名和密码。 */
export type LoginPayload = {
  username: string
  password: string
}

/** 注册契约仍保留，用于后续开放注册或后台代建账号场景。 */
export type RegisterPayload = LoginPayload & {
  displayName: string
}
