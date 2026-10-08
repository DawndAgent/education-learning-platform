export type AppEnv = 'development' | 'production'

/**
 * 本地联调使用 development（电脑局域网 IP）。
 * 当前指向腾讯云轻量服务器公网 IP；有正式域名 + HTTPS 后，把 production 改成 https://你的域名。
 * 开发者工具预览请勾选「不校验合法域名」。
 */
const currentEnv: AppEnv = 'production'

const apiBaseUrlByEnv: Record<AppEnv, string> = {
  development: 'http://192.168.71.122:8080',
  production: 'http://124.223.208.4'
}

export const appEnv = currentEnv
export const apiBaseUrl = apiBaseUrlByEnv[currentEnv]
export const requestTimeoutMs = 10000
