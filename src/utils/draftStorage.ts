import type { AppDraft } from '../types'

const APP_DRAFT_KEY = 'dropit:app-draft'

function storageKey(userId: string) {
  return `${APP_DRAFT_KEY}:${userId}`
}

export function readAppDraft(userId: string): AppDraft | null {
  try {
    const raw = window.localStorage.getItem(storageKey(userId))
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<AppDraft>
    if (!parsed.values || typeof parsed.values !== 'object' || Array.isArray(parsed.values)) return null
    return {
      values: {
        name: typeof parsed.values.name === 'string' ? parsed.values.name : '',
        tagline: typeof parsed.values.tagline === 'string' ? parsed.values.tagline : '',
        description: typeof parsed.values.description === 'string' ? parsed.values.description : '',
        appUrl: typeof parsed.values.appUrl === 'string' ? parsed.values.appUrl : '',
        githubUrl: typeof parsed.values.githubUrl === 'string' ? parsed.values.githubUrl : '',
        category: typeof parsed.values.category === 'string' ? parsed.values.category as AppDraft['values']['category'] : '',
        techTags: typeof parsed.values.techTags === 'string' ? parsed.values.techTags : '',
        thumbnailUrl: typeof parsed.values.thumbnailUrl === 'string' ? parsed.values.thumbnailUrl : '',
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
