import { execFileSync } from 'node:child_process'
import path from 'node:path'
import { getRepoRoot } from './env'

export function resetE2EData() {
  execFileSync('node', ['./e2e/scripts/seed-db.mjs'], {
    cwd: path.join(getRepoRoot(), 'frontend', 'web'),
    stdio: 'inherit',
    env: process.env,
  })
}
