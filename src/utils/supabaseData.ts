import type { AppItem, Category, Maker, ThumbnailVariant } from '../types'
import { CATEGORIES } from '../types'
import { supabase } from './auth'

export interface RemoteState {
  apps: AppItem[]
  profile: Maker | null
  bookmarkedIds: string[]
  likedIds: string[]
}

interface RemoteAppRow {
  id: string
  name: string
  tagline: string
  description: string
  category: string
  thumbnail_variant: string
  thumbnail_url: string | null
  app_url: string
  github_url: string | null
  maker: unknown
  tech_tags: string[] | null
  plays: number | null
  likes: number | null
  created_at: string
  owner_id: string
  source: string | null
}

interface RemoteMakerRow {
  id: string
  name: string
  initials: string
  avatar_url?: string | null
  role: string
  bio: string
  tone: string
}

const APP_COLUMNS = 'id,name,tagline,description,category,thumbnail_variant,thumbnail_url,app_url,github_url,maker,tech_tags,plays,likes,created_at,owner_id,source'
const THUMBNAIL_VARIANTS: ThumbnailVariant[] = ['retro', 'food', 'code', 'roulette', 'css', 'temperature', 'garden', 'dungeon', 'naming', 'http', 'timer', 'museum', 'new']

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value && typeof value === 'object' && !Array.isArray(value))
}

function isCategory(value: unknown): value is AppItem['category'] {
  return typeof value === 'string' && value !== '전체' && CATEGORIES.includes(value as Category)
}

function isThumbnailVariant(value: unknown): value is ThumbnailVariant {
  return typeof value === 'string' && THUMBNAIL_VARIANTS.includes(value as ThumbnailVariant)
}

function toMaker(value: unknown): Maker | null {
  if (!isRecord(value)) return null
  if (typeof value.id !== 'string' || typeof value.name !== 'string' || typeof value.initials !== 'string' || typeof value.role !== 'string' || typeof value.bio !== 'string' || typeof value.tone !== 'string') return null
  if (value.avatarUrl !== undefined && value.avatarUrl !== null && typeof value.avatarUrl !== 'string') return null
  return {
    id: value.id,
    name: value.name,
    initials: value.initials,
    avatarUrl: typeof value.avatarUrl === 'string' ? value.avatarUrl : null,
    role: value.role,
    bio: value.bio,
    tone: value.tone,
  }
}

function toRemoteApp(row: RemoteAppRow): AppItem | null {
  const maker = toMaker(row.maker)
  if (!maker || !isCategory(row.category) || !isThumbnailVariant(row.thumbnail_variant)) return null
  return {
    id: row.id,
    name: row.name,
    tagline: row.tagline,
    description: row.description,
    category: row.category,
    thumbnailVariant: row.thumbnail_variant,
    thumbnailUrl: row.thumbnail_url ?? undefined,
    appUrl: row.app_url,
    githubUrl: row.github_url ?? undefined,
    maker,
    techTags: Array.isArray(row.tech_tags) ? row.tech_tags.filter((tag): tag is string => typeof tag === 'string') : [],
    plays: typeof row.plays === 'number' ? row.plays : 0,
    likes: typeof row.likes === 'number' ? row.likes : 0,
    createdAt: row.created_at,
    ownerId: row.owner_id,
    source: row.source === 'seed' ? 'seed' : 'submitted',
  }
}

function toRemoteMaker(row: RemoteMakerRow | null): Maker | null {
  if (!row) return null
  return {
    id: row.id,
    name: row.name,
    initials: row.initials,
    avatarUrl: row.avatar_url,
    role: row.role,
    bio: row.bio,
    tone: row.tone,
  }
}

async function fetchRemoteMaker(userId: string) {
  if (!supabase) return { data: null, error: null }
  const currentRequest = await supabase.from('makers').select('id,name,initials,avatar_url,role,bio,tone').eq('id', userId).maybeSingle()
  if (!currentRequest.error || !currentRequest.error.message.includes('avatar_url')) return currentRequest
  return supabase.from('makers').select('id,name,initials,role,bio,tone').eq('id', userId).maybeSingle()
}

function appToRow(app: AppItem) {
  return {
    id: app.id,
    owner_id: app.ownerId ?? app.maker.id,
    name: app.name,
    tagline: app.tagline,
    description: app.description,
    category: app.category,
    thumbnail_variant: app.thumbnailVariant,
    thumbnail_url: app.thumbnailUrl ?? null,
    app_url: app.appUrl,
    github_url: app.githubUrl ?? null,
    maker: app.maker,
    tech_tags: app.techTags,
    plays: app.plays,
    likes: app.likes,
    created_at: app.createdAt,
    source: app.source ?? 'submitted',
  }
}

function throwIfError(error: { message: string } | null) {
  if (error) throw error
}

export async function fetchRemoteState(userId: string | null): Promise<RemoteState> {
  if (!supabase) return { apps: [], profile: null, bookmarkedIds: [], likedIds: [] }

  const appsRequest = supabase.from('apps').select(APP_COLUMNS).order('created_at', { ascending: false })
  const profileRequest = userId
    ? fetchRemoteMaker(userId)
    : Promise.resolve({ data: null, error: null })
  const bookmarksRequest = userId
    ? supabase.from('app_bookmarks').select('app_id').eq('user_id', userId)
    : Promise.resolve({ data: [], error: null })
  const likesRequest = userId
    ? supabase.from('app_likes').select('app_id').eq('user_id', userId)
    : Promise.resolve({ data: [], error: null })

  const [{ data: appRows, error: appsError }, { data: profileRow, error: profileError }, { data: bookmarkRows, error: bookmarksError }, { data: likeRows, error: likesError }] = await Promise.all([appsRequest, profileRequest, bookmarksRequest, likesRequest])
  throwIfError(appsError)
  throwIfError(profileError)
  throwIfError(bookmarksError)
  throwIfError(likesError)

  return {
    apps: (Array.isArray(appRows) ? appRows : []).map((row) => toRemoteApp(row as RemoteAppRow)).filter((app): app is AppItem => Boolean(app)),
    profile: toRemoteMaker(profileRow as RemoteMakerRow | null),
    bookmarkedIds: Array.isArray(bookmarkRows) ? bookmarkRows.map((row) => row.app_id).filter((id): id is string => typeof id === 'string') : [],
    likedIds: Array.isArray(likeRows) ? likeRows.map((row) => row.app_id).filter((id): id is string => typeof id === 'string') : [],
  }
}

export async function createRemoteApp(app: AppItem) {
  if (!supabase) return
  const { error } = await supabase.from('apps').insert(appToRow(app))
  throwIfError(error)
}

export async function updateRemoteApp(app: AppItem) {
  if (!supabase) return
  const { error } = await supabase.from('apps').update(appToRow(app)).eq('id', app.id).eq('owner_id', app.ownerId ?? app.maker.id)
  throwIfError(error)
}

export async function upsertRemoteProfile(maker: Maker) {
  if (!supabase) return
  const profilePayload = {
    id: maker.id,
    name: maker.name,
    initials: maker.initials,
    avatar_url: maker.avatarUrl ?? null,
    role: maker.role,
    bio: maker.bio,
    tone: maker.tone,
    updated_at: new Date().toISOString(),
  }
  let { error } = await supabase.from('makers').upsert(profilePayload, { onConflict: 'id' })
  if (error?.message.includes('avatar_url')) {
    const { avatar_url: _avatarUrl, ...legacyPayload } = profilePayload
    ;({ error } = await supabase.from('makers').upsert(legacyPayload, { onConflict: 'id' }))
  }
  throwIfError(error)
}

export async function setRemoteBookmark(userId: string, appId: string, isBookmarked: boolean) {
  if (!supabase) return
  if (isBookmarked) {
    const { error } = await supabase.from('app_bookmarks').insert({ user_id: userId, app_id: appId })
    throwIfError(error)
    return
  }
  const { error } = await supabase.from('app_bookmarks').delete().eq('user_id', userId).eq('app_id', appId)
  throwIfError(error)
}

export async function toggleRemoteLike(appId: string) {
  if (!supabase) return false
  const { data, error } = await supabase.rpc('toggle_app_like', { p_app_id: appId })
  throwIfError(error)
  return data === true
}

export async function recordRemotePlay(appId: string) {
  if (!supabase) return
  const { error } = await supabase.rpc('increment_app_plays', { p_app_id: appId })
  throwIfError(error)
}

export async function verifyRemoteCrewAccessCode(code: string) {
  if (!supabase) return false
  const { data, error } = await supabase.rpc('verify_crew_access_code', { p_code: code })
  throwIfError(error)
  return data === true
}
