import { CATEGORIES, type AppCategory, type AppDraft } from '../types'

const APP_DRAFT_KEY = 'dropit:app-draft'

function storageKey(userId: string) {
  return `${APP_DRAFT_KEY}:${userId}`
}

function isAppCategory(value: unknown): value is AppCategory {
  return typeof value === 'string' && value !== '전체' && CATEGORIES.includes(value as AppCategory)
}

export function readAppDraft(userId: string): AppDraft | null {
  try {
    const raw = window.localStorage.getItem(storageKey(userId))
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<AppDraft>
    if (!parsed.values || typeof parsed.values !== 'object' || Array.isArray(parsed.values)) return null
    const savedValues = parsed.values as Partial<AppDraft['values']> & { category?: unknown }
    const savedCategories = Array.isArray(savedValues.categories)
      ? savedValues.categories.filter(isAppCategory)
      : typeof savedValues.category === 'string' && isAppCategory(savedValues.category)
        ? [savedValues.category]
        : []
    return {
      values: {
        name: typeof savedValues.name === 'string' ? savedValues.name : '',
        tagline: typeof savedValues.tagline === 'string' ? savedValues.tagline : '',
        description: typeof savedValues.description === 'string' ? savedValues.description : '',
        appUrl: typeof savedValues.appUrl === 'string' ? savedValues.appUrl : '',
        githubUrl: typeof savedValues.githubUrl === 'string' ? savedValues.githubUrl : '',
        categories: savedCategories,
        techTags: typeof savedValues.techTags === 'string' ? savedValues.techTags : '',
        thumbnailUrl: typeof savedValues.thumbnailUrl === 'string' ? savedValues.thumbnailUrl : '',
      },
      thumbnailFileName: typeof parsed.thumbnailFileName === 'string' ? parsed.thumbnailFileName : '',
    }
  } catch {
    return null
  }
}

export function writeAppDraft(userId: string, draft: AppDraft) {
  try {
    window.localStorage.setItem(storageKey(userId), JSON.stringify(draft))
  } catch {
    // 브라우저 저장 공간이 부족하거나 차단된 경우에도 입력은 계속할 수 있습니다.
  }
}

export function clearAppDraft(userId: string) {
  try {
    window.localStorage.removeItem(storageKey(userId))
  } catch {
    // Storage can be blocked in private browsing.
  }
}
