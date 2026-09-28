export interface CurrentUser {
  id: string
  username: string
  nickname: string
  permissions: string[]
}

export interface LoginResult {
  token: string
  tokenType: string
  expiresIn: number
  user: CurrentUser
}
