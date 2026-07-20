import { useParams } from 'react-router-dom'
import type { AppItem, Maker } from '../types'
import { NotFoundPage } from './NotFoundPage'
import { SubmitPage } from './SubmitPage'

interface EditAppPageProps {
  apps: AppItem[]
  currentUserId: string | null
  maker: Maker | null
  onUpdateApp: (app: AppItem) => void
}

export function EditAppPage({ apps, currentUserId, maker, onUpdateApp }: EditAppPageProps) {
  const { appId } = useParams()
  const app = apps.find((item) => item.id === appId)
  const isOwner = Boolean(app && currentUserId && (app.ownerId ?? app.maker.id) === currentUserId)

  if (!app || !maker || !isOwner) return <NotFoundPage />

  return <SubmitPage key={app.id} editingApp={app} maker={maker} onUpdateApp={onUpdateApp} />
}
