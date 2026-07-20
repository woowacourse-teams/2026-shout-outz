import { createClient, type User } from '@supabase/supabase-js'

export interface AuthUser {
  id: string
  name: string
  email: string | null
  avatarUrl: string | null
}

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY

export const isAuthConfigured = Boolean(supabaseUrl && supabaseAnonKey)
export const supabase = supabaseUrl && supabaseAnonKey ? createClient(supabaseUrl, supabaseAnonKey) : null

export function toAuthUser(user: User): AuthUser {
  const metadata = user.user_metadata ?? {}
  const name = typeof metadata.full_name === 'string' && metadata.full_name.trim()
    ? metadata.full_name
    : typeof metadata.user_name === 'string' && metadata.user_name.trim()
      ? metadata.user_name
      : user.email?.split('@')[0] ?? 'GitHub 사용자'

  return {
    id: user.id,
    name,
    email: user.email ?? null,
    avatarUrl: typeof metadata.avatar_url === 'string' ? metadata.avatar_url : null,
  }
}
