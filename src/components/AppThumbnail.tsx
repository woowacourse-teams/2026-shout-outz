import { useEffect, useState } from 'react'
import type { ThumbnailVariant } from '../types'

interface AppThumbnailProps {
  variant: ThumbnailVariant
  label: string
  size?: 'small' | 'medium' | 'large'
  customImage?: string
}

export function AppThumbnail({ variant, label, size = 'medium', customImage }: AppThumbnailProps) {
  const [imageFailed, setImageFailed] = useState(false)

  useEffect(() => setImageFailed(false), [customImage])

  if (customImage && !imageFailed) {
    return (
      <div className={`app-thumbnail app-thumbnail--${size} app-thumbnail--custom`}>
        <img src={customImage} alt={`${label} 미리보기`} onError={() => setImageFailed(true)} />
      </div>
    )
  }

  return (
    <div
      className={`app-thumbnail app-thumbnail--${variant} app-thumbnail--${size}`}
      role="img"
      aria-label={`${label} 미리보기`}
    >
      <div className="thumbnail-noise" aria-hidden="true" />
      <span className="thumbnail-mark thumbnail-mark--top" aria-hidden="true" />
      <span className="thumbnail-mark thumbnail-mark--bottom" aria-hidden="true" />
      <span className="thumbnail-word" aria-hidden="true">{thumbnailWord(variant)}</span>
      <span className="thumbnail-line thumbnail-line--one" aria-hidden="true" />
      <span className="thumbnail-line thumbnail-line--two" aria-hidden="true" />
    </div>
  )
}

function thumbnailWord(variant: ThumbnailVariant) {
  const words: Record<ThumbnailVariant, string> = {
    retro: '뭐지?',
    food: '메뉴',
    code: '{ }',
    roulette: '추천',
    css: '스타일',
    temperature: '22°',
    garden: '커밋',
    dungeon: '입력',
    naming: '이름()',
    http: '404',
    timer: '25:00',
    museum: '버그',
    new: '새 앱',
  }
  return words[variant]
}
