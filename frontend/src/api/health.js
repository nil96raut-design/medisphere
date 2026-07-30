import { BASE_URL } from './client'

export async function getHealth() {
  const res = await fetch(`${BASE_URL}/health`)
  if (!res.ok) throw new Error(`Health check failed: ${res.status}`)
  return res.json()
}
