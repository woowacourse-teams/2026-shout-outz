import { useEffect, useState } from 'react'
import type { ThumbnailVariant } from '../types'

interface AppThumbnailProps {
  variant: ThumbnailVariant
  label: string
  size?: 'small' | 'medium' | 'large'
  customImage?: string
}

export function AppThumbnail({ label, size = 'medium', customImage }: AppThumbnailProps) {
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
      className={`app-thumbnail app-thumbnail--fallback app-thumbnail--${size}`}
      role="img"
      aria-label={`${label} 미리보기`}
    />
  )
}
