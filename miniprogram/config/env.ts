export type AppEnv = 'development' | 'production'

/**
 * 本地联调使用 development。
 * production 只是占位地址，发布前替换，不要写入真实密钥。
 */
const currentEnv: AppEnv = 'development'

const apiBaseUrlByEnv: Record<AppEnv, string> = {
  development: 'http://localhost:8080',
  production: 'https://api.example.com'
}

export const appEnv = currentEnv
export const apiBaseUrl = apiBaseUrlByEnv[currentEnv]
export const requestTimeoutMs = 10000
