import fs from 'node:fs'
import path from 'node:path'

const repoRoot = path.resolve(process.cwd(), '..', '..')
const envPath = path.join(repoRoot, '.env')

function parseEnvFile(source) {
  return source
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.length > 0 && !line.startsWith('#'))
    .reduce((acc, line) => {
      const separatorIndex = line.indexOf('=')
      if (separatorIndex === -1) {
        return acc
      }
      acc[line.slice(0, separatorIndex).trim()] = line.slice(separatorIndex + 1).trim()
      return acc
    }, {})
}

const fileEnv = fs.existsSync(envPath) ? parseEnvFile(fs.readFileSync(envPath, 'utf8')) : {}

function resolveEnv(name, fallback) {
  return process.env[name] ?? fileEnv[name] ?? fallback
}

export const dbConfig = {
  host: resolveEnv('DB_HOST', '127.0.0.1'),
  port: Number(resolveEnv('DB_PORT', '3306')),
  user: resolveEnv('DB_USERNAME', 'fundcat'),
  password: resolveEnv('DB_PASSWORD', 'change-me-app-password'),
  database: resolveEnv('DB_NAME', 'fundcat'),
}

export const e2eUser = {
  id: 'user-e2e-001',
  username: 'e2e_user',
  displayName: 'E2E Analyst',
  passwordHash: '$2a$10$O.IuFmKVza9cKNUOAhzBw.cpVFHwVsy933HftHUSiPRHfP7qd.lDy',
}
