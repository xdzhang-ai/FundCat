import fs from 'node:fs'
import path from 'node:path'

const repoRoot = path.resolve(process.cwd(), '..', '..')
const envPath = path.join(repoRoot, '.env')

let cachedEnv: Record<string, string> | null = null

function parseEnvFile(source: string) {
  return source
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.length > 0 && !line.startsWith('#'))
    .reduce<Record<string, string>>((acc, line) => {
      const separatorIndex = line.indexOf('=')
      if (separatorIndex === -1) return acc
      const key = line.slice(0, separatorIndex).trim()
      const value = line.slice(separatorIndex + 1).trim()
      acc[key] = value
      return acc
    }, {})
}

export function loadRepoEnv() {
  if (cachedEnv) {
    return cachedEnv
  }

  cachedEnv = fs.existsSync(envPath) ? parseEnvFile(fs.readFileSync(envPath, 'utf8')) : {}
  return cachedEnv
}

export function resolveEnv(name: string, fallback: string) {
  return process.env[name] ?? loadRepoEnv()[name] ?? fallback
}

export function getRepoRoot() {
  return repoRoot
}
