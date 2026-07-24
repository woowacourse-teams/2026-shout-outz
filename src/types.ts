export type Category = '전체' | '게임' | '생산성' | '학습' | '여행' | 'AI' | '하네스' | '자기개발' | '개발' | '디자인' | '생활' | '건강' | '생성기' | '소셜' | '실험'
export type AppCategory = Exclude<Category, '전체'>

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
  categories: AppCategory[]
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
  deletedAt?: string | null
  source?: 'seed' | 'submitted'
}

export interface AppFormValues {
  name: string
  tagline: string
  description: string
  appUrl: string
  githubUrl: string
  categories: AppCategory[]
  techTags: string
  thumbnailUrl: string
}

export interface AppDraft {
  values: AppFormValues
  thumbnailFileName: string
}

export const CATEGORIES: Category[] = ['전체', '게임', '생산성', '학습', '여행', 'AI', '하네스', '자기개발', '개발', '디자인', '생활', '건강', '생성기', '소셜', '실험']
