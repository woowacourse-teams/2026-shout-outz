import type { AppCategory, AppComment, AppItem, Category, Maker, ThumbnailVariant, VisitorStats } from '../types'
import { CATEGORIES } from '../types'
import { supabase } from './auth'

export interface RemoteState {
  apps: AppItem[]
  deletedApps: AppItem[]
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
  categories?: string[] | null
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
  deleted_at: string | null
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

interface RemoteCommentRow {
  id: string
  app_id: string
  user_id: string
  parent_id: string | null
  content: string
  created_at: string
}

const APP_COLUMNS = 'id,name,tagline,description,category,categories,thumbnail_variant,thumbnail_url,app_url,github_url,maker,tech_tags,plays,likes,created_at,owner_id,deleted_at,source'
const THUMBNAIL_VARIANTS: ThumbnailVariant[] = ['retro', 'food', 'code', 'roulette', 'css', 'temperature', 'garden', 'dungeon', 'naming', 'http', 'timer', 'museum', 'new']
const VISITOR_ID_KEY = 'dropit:visitor-id'

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value && typeof value === 'object' && !Array.isArray(value))
}

function getVisitorId() {
  try {
    const savedVisitorId = window.localStorage.getItem(VISITOR_ID_KEY)
    if (savedVisitorId) return savedVisitorId
    const visitorId = window.crypto?.randomUUID?.() ?? `visitor-${Date.now()}-${Math.random().toString(36).slice(2)}`
    window.localStorage.setItem(VISITOR_ID_KEY, visitorId)
    return visitorId
  } catch {
    return `visitor-${Date.now()}-${Math.random().toString(36).slice(2)}`
  }
}

function isCategory(value: unknown): value is AppCategory {
  return typeof value === 'string' && value !== '전체' && CATEGORIES.includes(value as Category)
}

function toCategories(value: unknown, fallback: unknown): AppCategory[] | null {
  const categories = Array.isArray(value) ? value.filter(isCategory) : []
  const uniqueCategories = [...new Set(categories)]
  if (uniqueCategories.length > 0) return uniqueCategories
  return isCategory(fallback) ? [fallback] : null
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
  const categories = toCategories(row.categories, row.category)
  if (!maker || !categories || !isThumbnailVariant(row.thumbnail_variant)) return null
  return {
    id: row.id,
    name: row.name,
    tagline: row.tagline,
    description: row.description,
    categories,
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
    deletedAt: row.deleted_at,
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

async function fetchRemoteMakers(userIds: string[]) {
  if (!supabase || userIds.length === 0) return { data: [], error: null }
  const currentRequest = await supabase.from('makers').select('id,name,initials,avatar_url,role,bio,tone').in('id', userIds)
  if (!currentRequest.error || !currentRequest.error.message.includes('avatar_url')) return currentRequest
  return supabase.from('makers').select('id,name,initials,role,bio,tone').in('id', userIds)
}

function appToRow(app: AppItem) {
  return {
    id: app.id,
    owner_id: app.ownerId ?? app.maker.id,
    name: app.name,
    tagline: app.tagline,
    description: app.description,
    category: app.categories[0],
    categories: app.categories,
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

function toCount(value: unknown) {
  const count = typeof value === 'number' ? value : typeof value === 'string' ? Number(value) : NaN
  return Number.isFinite(count) ? count : null
}

export async function fetchRemoteState(userId: string | null): Promise<RemoteState> {
  if (!supabase) return { apps: [], deletedApps: [], profile: null, bookmarkedIds: [], likedIds: [] }

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

  const parsedApps = (Array.isArray(appRows) ? appRows : []).map((row) => toRemoteApp(row as RemoteAppRow)).filter((app): app is AppItem => Boolean(app))

  return {
    apps: parsedApps.filter((app) => !app.deletedAt),
    deletedApps: userId ? parsedApps.filter((app) => app.deletedAt && app.ownerId === userId) : [],
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

export async function deleteRemoteApp(appId: string, userId: string) {
  if (!supabase) return
  const { error } = await supabase.from('apps').update({ deleted_at: new Date().toISOString() }).eq('id', appId).eq('owner_id', userId).is('deleted_at', null)
  throwIfError(error)
}

export async function restoreRemoteApp(appId: string, userId: string) {
  if (!supabase) return
  const { error } = await supabase.from('apps').update({ deleted_at: null }).eq('id', appId).eq('owner_id', userId).not('deleted_at', 'is', null)
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

function toRemoteComment(row: RemoteCommentRow, author: Maker): AppComment {
  return {
    id: row.id,
    appId: row.app_id,
    userId: row.user_id,
    parentId: row.parent_id,
    content: row.content,
    createdAt: row.created_at,
    author,
  }
}

export async function fetchRemoteComments(appId: string): Promise<AppComment[]> {
  if (!supabase) return []
  const { data: commentRows, error: commentsError } = await supabase
    .from('app_comments')
    .select('id,app_id,user_id,parent_id,content,created_at')
    .eq('app_id', appId)
    .order('created_at', { ascending: false })
  throwIfError(commentsError)

  const rows = (Array.isArray(commentRows) ? commentRows : []) as RemoteCommentRow[]
  const userIds = [...new Set(rows.map((row) => row.user_id).filter((id): id is string => typeof id === 'string'))]
  const { data: makerRows, error: makersError } = await fetchRemoteMakers(userIds)
  throwIfError(makersError)
  const makers = (Array.isArray(makerRows) ? makerRows : [])
    .map((row) => toRemoteMaker(row as RemoteMakerRow))
    .filter((maker): maker is Maker => Boolean(maker))
  const makersById = new Map(makers.map((maker) => [maker.id, maker]))

  return rows
    .map((row) => {
      const author = makersById.get(row.user_id)
      return author ? toRemoteComment(row, author) : null
    })
    .filter((comment): comment is AppComment => Boolean(comment))
}

export async function createRemoteComment(appId: string, userId: string, content: string, author: Maker, parentId: string | null = null): Promise<AppComment> {
  if (!supabase) throw new Error('Supabase is not configured')
  const { data, error } = await supabase
    .from('app_comments')
    .insert({ app_id: appId, user_id: userId, parent_id: parentId, content })
    .select('id,app_id,user_id,parent_id,content,created_at')
    .single()
  throwIfError(error)
  if (!data) throw new Error('댓글을 확인하지 못했습니다.')
  return toRemoteComment(data as RemoteCommentRow, author)
}

export async function deleteRemoteComment(commentId: string, userId: string) {
  if (!supabase) return
  const { error } = await supabase.from('app_comments').delete().eq('id', commentId).eq('user_id', userId)
  throwIfError(error)
}

export async function recordRemotePlay(appId: string) {
  if (!supabase) return
  const { error } = await supabase.rpc('increment_app_plays', { p_app_id: appId })
  throwIfError(error)
}

export async function recordRemoteSiteVisit(): Promise<VisitorStats | null> {
  if (!supabase) return null
  const { data, error } = await supabase.rpc('record_site_visit', { p_visitor_id: getVisitorId() })
  throwIfError(error)
  const row = Array.isArray(data) ? data[0] : data
  if (!isRecord(row)) return null
  const dailyVisitors = toCount(row.daily_visitors)
  const totalVisitors = toCount(row.total_visitors)
  if (dailyVisitors === null || totalVisitors === null) return null
  return { dailyVisitors, totalVisitors }
}

export async function verifyRemoteCrewAccessCode(code: string) {
  if (!supabase) return false
  const { data, error } = await supabase.rpc('verify_crew_access_code', { p_code: code })
  throwIfError(error)
  return data === true
}
