export type Category = '전체' | '게임' | '생산성' | '학습' | '생성기' | '소셜' | '실험'

export type ThumbnailVariant =
  | 'retro'
  | 'food'
  | 'code'
  | 'roulette'
  | 'css'
  | 'temperature'
  | 'garden'
  | 'dungeon'
  | 'naming'
  | 'http'
  | 'timer'
  | 'museum'
  | 'new'

export interface Maker {
  id: string
  name: string
  initials: string
  avatarUrl?: string | null
  role: string
  bio: string
  tone: string
}

export interface AppItem {
  id: string
  name: string
  tagline: string
  description: string
  category: Exclude<Category, '전체'>
  thumbnailVariant: ThumbnailVariant
  thumbnailUrl?: string
  appUrl: string
  githubUrl?: string
  maker: Maker
  techTags: string[]
  plays: number
  likes: number
  createdAt: string
  ownerId?: string
  source?: 'seed' | 'submitted'
}

export interface AppFormValues {
  name: string
  tagline: string
  description: string
  appUrl: string
  githubUrl: string
  category: Exclude<Category, '전체'> | ''
  techTags: string
  thumbnailUrl: string
}

export interface AppDraft {
  values: AppFormValues
  thumbnailFileName: string
}

export const CATEGORIES: Category[] = ['전체', '게임', '생산성', '학습', '생성기', '소셜', '실험']
