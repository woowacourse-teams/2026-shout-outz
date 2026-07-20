import type { CSSProperties } from 'react'
import { useEffect, useState } from 'react'
import type { Maker } from '../types'

export function Avatar({ maker, size = 'small' }: { maker: Maker; size?: 'small' | 'medium' | 'large' }) {
  const [imageError, setImageError] = useState(false)
  const showImage = Boolean(maker.avatarUrl) && !imageError

  useEffect(() => {
    setImageError(false)
  }, [maker.avatarUrl])

  return (
    <span
      className={`avatar avatar--${size}`}
      style={{ '--avatar-tone': maker.tone } as CSSProperties}
      aria-hidden="true"
    >
      {showImage ? <img src={maker.avatarUrl ?? undefined} alt="" onError={() => setImageError(true)} /> : maker.initials}
    </span>
  )
}
