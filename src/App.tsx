import { useCallback, useEffect, useMemo, useState } from 'react'
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { AnalyticsConsentBanner } from './components/AnalyticsConsentBanner'
import { CommentFeatureModal } from './components/CommentFeatureModal'
import { Layout } from './components/Layout'
import { AppDetailPage } from './pages/AppDetailPage'
import { EditAppPage } from './pages/EditAppPage'
import { BookmarksPage } from './pages/BookmarksPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { MakerPage } from './pages/MakerPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { PrivacyPolicyPage } from './pages/PrivacyPolicyPage'
import { SubmitPage } from './pages/SubmitPage'
import type { AppItem, Maker, VisitorStats } from './types'
import { isAuthConfigured, supabase, toAuthUser, type AuthUser } from './utils/auth'
import { getAnalyticsConsent, initializeAnalytics, isAnalyticsConfigured, type AnalyticsConsent, setAnalyticsUserId, trackEvent, trackPageView, updateAnalyticsConsent } from './utils/analytics'
import { createRemoteApp, deleteRemoteApp, fetchRemoteState, recordRemotePlay, recordRemoteSiteVisit, restoreRemoteApp, setRemoteBookmark, toggleRemoteLike, updateRemoteApp, upsertRemoteProfile, verifyRemoteCrewAccessCode } from './utils/supabaseData'

const SERVICE_UNAVAILABLE_MESSAGE = '현재 버전은 프로토타입이라 데이터를 불러오는 과정이 불안정할 수 있습니다.\n페이지를 새로고침해주세요.'
const COMMENT_FEATURE_HIDE_KEY = 'dropit:comment-feature-hide-date'

function localDateKey() {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
}

function isCommentFeatureHiddenToday() {
  try {
    return window.localStorage.getItem(COMMENT_FEATURE_HIDE_KEY) === localDateKey()
  } catch {
    return false
  }
}

function makerFromAuthUser(user: AuthUser): Maker {
  const initials = user.name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase() || user.name.slice(0, 2).toUpperCase()

  return {
    id: user.id,
    name: user.name,
    initials: initials || '나',
    avatarUrl: user.avatarUrl,
    role: 'GitHub 사용자',
    bio: '프로필을 설정하면 이곳에 소개가 표시됩니다.',
    tone: '#d9e6ff',
  }
}

function AuthLoadingPage() {
  return <div className="auth-loading page-pad">로그인 상태를 확인하고 있습니다.</div>
}

function DataStatusPage({ message, isError = false }: { message: string; isError?: boolean }) {
  return <div className="auth-loading data-status page-pad"><p>{message}</p>{isError ? <button type="button" className="button button--secondary" onClick={() => window.location.reload()}>새로고침</button> : null}</div>
}

function safeRedirectPath(value: string) {
  try {
    const requested = new URL(value, window.location.origin)
    if (requested.origin !== window.location.origin) return '/submit'
    if (requested.pathname === '/' || requested.pathname === '/bookmarks' || requested.pathname === '/submit' || requested.pathname === '/makers/me' || /^\/apps\/[^/]+$/.test(requested.pathname) || /^\/apps\/[^/]+\/edit$/.test(requested.pathname)) return requested.pathname
  } catch {
    // Fall back to the app registration page for malformed redirect values.
  }
  return '/submit'
}

function App() {
  const location = useLocation()
  const navigate = useNavigate()
  const [apps, setApps] = useState<AppItem[]>([])
  const [deletedApps, setDeletedApps] = useState<AppItem[]>([])
  const [profile, setProfile] = useState<Maker | null>(null)
  const [profileLoadedFor, setProfileLoadedFor] = useState<string | null>(null)
  const [authUser, setAuthUser] = useState<AuthUser | null>(null)
  const [authLoading, setAuthLoading] = useState(isAuthConfigured)
  const [authError, setAuthError] = useState('')
  const [dataLoading, setDataLoading] = useState(Boolean(supabase))
  const [dataError, setDataError] = useState(supabase ? '' : SERVICE_UNAVAILABLE_MESSAGE)
  const [bookmarkedIds, setBookmarkedIds] = useState<string[]>([])
  const [likedIds, setLikedIds] = useState<string[]>([])
  const [playCounts, setPlayCounts] = useState<Record<string, number>>({})
  const [visitorStats, setVisitorStats] = useState<VisitorStats | null>(null)
  const [commentFeatureOpen, setCommentFeatureOpen] = useState(false)
  const [commentFeatureDismissed, setCommentFeatureDismissed] = useState(false)
  const [analyticsConsent, setAnalyticsConsent] = useState<AnalyticsConsent | null>(() => getAnalyticsConsent())

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
    if (authLoading) return
    initializeAnalytics()
    setAnalyticsUserId(authUser?.id ?? null)
    trackPageView(`${location.pathname}${location.search}`)
  }, [analyticsConsent, authLoading, authUser?.id, location.pathname, location.search])

  useEffect(() => {
    if (!supabase) return
    let active = true
    void recordRemoteSiteVisit().then((stats) => {
      if (active) setVisitorStats(stats)
    }).catch(() => undefined)
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    if (!supabase) {
      setAuthLoading(false)
      return
    }

    let active = true
    const applySession = (user: Parameters<typeof toAuthUser>[0] | null) => {
      if (!active) return
      setAuthUser(user ? toAuthUser(user) : null)
      setAuthLoading(false)
    }

    void supabase.auth.getSession().then(({ data, error }) => {
      if (error) setAuthError('로그인 상태를 확인하지 못했습니다.')
      applySession(data.session?.user ?? null)
    })

    const { data: { subscription } } = supabase.auth.onAuthStateChange((event, session) => {
      if (event === 'SIGNED_IN') trackEvent('login', { method: 'github' })
      applySession(session?.user ?? null)
    })

    return () => {
      active = false
      subscription.unsubscribe()
    }
  }, [])

  useEffect(() => {
    if (authLoading) return

    setProfileLoadedFor(null)
    if (supabase) {
      let active = true
      setDataLoading(true)
      setDataError('')
      void fetchRemoteState(authUser?.id ?? null).then((state) => {
        if (!active) return
        const profileWithAvatar = state.profile && !state.profile.avatarUrl && authUser?.avatarUrl
          ? { ...state.profile, avatarUrl: authUser.avatarUrl }
          : state.profile
        const appsWithAvatar = authUser?.avatarUrl
          ? state.apps.map((app) => app.ownerId === authUser.id && !app.maker.avatarUrl ? { ...app, maker: { ...app.maker, avatarUrl: authUser.avatarUrl } } : app)
          : state.apps
        setApps(appsWithAvatar)
        setDeletedApps(state.deletedApps)
        setProfile(profileWithAvatar)
        setProfileLoadedFor(authUser?.id ?? null)
        setBookmarkedIds(state.bookmarkedIds)
        setLikedIds(state.likedIds)
        setPlayCounts({})
        setDataLoading(false)
      }).catch(() => {
        if (!active) return
        setDataError(SERVICE_UNAVAILABLE_MESSAGE)
        setDataLoading(false)
      })

      return () => {
        active = false
      }
    }
    setDataLoading(false)
    setDataError(SERVICE_UNAVAILABLE_MESSAGE)
  }, [authLoading, authUser?.id])

  useEffect(() => {
    if (authLoading || dataLoading || dataError || location.pathname !== '/' || commentFeatureOpen || commentFeatureDismissed || isCommentFeatureHiddenToday()) return
    setCommentFeatureOpen(true)
  }, [authLoading, commentFeatureDismissed, commentFeatureOpen, dataError, dataLoading, location.pathname])

  const currentMaker = useMemo(() => authUser ? profile ?? makerFromAuthUser(authUser) : null, [authUser, profile])
  const displayApps = useMemo(() => apps.map((app) => ({ ...app, likes: app.likes + (likedIds.includes(app.id) ? 1 : 0), plays: app.plays + (playCounts[app.id] ?? 0) })), [apps, likedIds, playCounts])

  const toggleBookmark = useCallback((id: string) => {
    if (!authUser) {
      const returnTo = `${window.location.pathname}${window.location.search}`
      navigate(`/login?returnTo=${encodeURIComponent(returnTo)}`)
      return
    }
    if (!supabase) {
      setDataError(SERVICE_UNAVAILABLE_MESSAGE)
      return
    }
    const wasBookmarked = bookmarkedIds.includes(id)
    const nextIds = wasBookmarked ? bookmarkedIds.filter((item) => item !== id) : [...bookmarkedIds, id]
    setBookmarkedIds(nextIds)
    void setRemoteBookmark(authUser.id, id, !wasBookmarked).then(() => {
      trackEvent(wasBookmarked ? 'bookmark_remove' : 'bookmark_add', { app_id: id })
    }).catch(() => {
      setBookmarkedIds((current) => wasBookmarked ? (current.includes(id) ? current : [...current, id]) : current.filter((item) => item !== id))
      setDataError('저장하지 못했습니다. 잠시 후 다시 시도해주세요.')
    })
  }, [authUser, bookmarkedIds, navigate])

  const toggleLike = useCallback((id: string) => {
    if (!authUser) {
      const returnTo = `${window.location.pathname}${window.location.search}`
      if (window.confirm('좋아요를 누르려면 로그인이 필요합니다.\n로그인하시겠습니까?')) {
        navigate(`/login?returnTo=${encodeURIComponent(returnTo)}`)
      }
      return
    }
    if (!supabase) return
    const wasLiked = likedIds.includes(id)
    setLikedIds(wasLiked ? likedIds.filter((item) => item !== id) : [...likedIds, id])
    void toggleRemoteLike(id).then((isLiked) => {
      setLikedIds((current) => isLiked ? (current.includes(id) ? current : [...current, id]) : current.filter((item) => item !== id))
      trackEvent(isLiked ? 'like_add' : 'like_remove', { app_id: id })
    }).catch(() => {
      setLikedIds((current) => wasLiked ? (current.includes(id) ? current : [...current, id]) : current.filter((item) => item !== id))
      setDataError('좋아요 상태를 반영하지 못했습니다. 잠시 후 다시 시도해주세요.')
    })
  }, [authUser, likedIds, navigate])

  const launchApp = useCallback((id: string) => {
    setPlayCounts((current) => ({ ...current, [id]: (current[id] ?? 0) + 1 }))
    trackEvent('app_launch', { app_id: id })
    if (supabase) {
      void recordRemotePlay(id).catch(() => setDataError('실행 수를 반영하지 못했습니다.'))
    }
  }, [])

  const addApp = useCallback((app: AppItem) => {
    if (!supabase || !authUser) return
    const ownedApp = currentMaker && authUser ? { ...app, ownerId: authUser.id, maker: currentMaker } : app
    setApps((current) => [ownedApp, ...current])
    void createRemoteApp(ownedApp).catch(() => setDataError('서비스를 등록하지 못했습니다. 잠시 후 다시 시도해주세요.'))
  }, [authUser, currentMaker])

  const verifyCrewCode = useCallback(async (code: string) => {
    if (!supabase || !authUser) return false
    return verifyRemoteCrewAccessCode(code)
  }, [authUser])

  const updateApp = useCallback((updatedApp: AppItem) => {
    if (!supabase || !authUser) return
    setApps((current) => current.map((app) => app.id === updatedApp.id ? updatedApp : app))
    void updateRemoteApp(updatedApp).catch(() => setDataError('서비스 수정 내용을 저장하지 못했습니다. 잠시 후 다시 시도해주세요.'))
  }, [authUser])

  const deleteApp = useCallback(async (appId: string) => {
    if (!supabase || !authUser) return false
    const appToDelete = apps.find((app) => app.id === appId)

    try {
      await deleteRemoteApp(appId, authUser.id)
      setApps((current) => current.filter((app) => app.id !== appId))
      if (appToDelete) {
        setDeletedApps((current) => [{ ...appToDelete, deletedAt: new Date().toISOString() }, ...current.filter((app) => app.id !== appId)])
      }
      setPlayCounts((current) => {
        const next = { ...current }
        delete next[appId]
        return next
      })
      navigate('/')
      return true
    } catch {
      return false
    }
  }, [apps, authUser, navigate])

  const restoreApp = useCallback(async (appId: string) => {
    if (!supabase || !authUser) return false
    const appToRestore = deletedApps.find((app) => app.id === appId)
    if (!appToRestore) return false

    try {
      await restoreRemoteApp(appId, authUser.id)
      const restoredApp = { ...appToRestore, deletedAt: null }
      setDeletedApps((current) => current.filter((app) => app.id !== appId))
      setApps((current) => [...current, restoredApp].sort((a, b) => b.createdAt.localeCompare(a.createdAt)))
      return true
    } catch {
      return false
    }
  }, [authUser, deletedApps])

  const saveProfile = useCallback((maker: Maker) => {
    if (!authUser) return
    const nextProfile = { ...maker, id: authUser.id }
    const nextApps = apps.map((app) => {
      const ownsApp = app.ownerId === authUser.id || (!app.ownerId && app.maker.id === authUser.id)
      return ownsApp ? { ...app, ownerId: authUser.id, maker: nextProfile } : app
    })
    setProfile(nextProfile)
    setApps(nextApps)
    const nextDeletedApps = deletedApps.map((app) => {
      const ownsApp = app.ownerId === authUser.id || (!app.ownerId && app.maker.id === authUser.id)
      return ownsApp ? { ...app, ownerId: authUser.id, maker: nextProfile } : app
    })
    setDeletedApps(nextDeletedApps)
    if (!supabase) return
    const ownedApps = nextApps.filter((app) => app.ownerId === authUser.id)
    const ownedDeletedApps = nextDeletedApps.filter((app) => app.ownerId === authUser.id)
    void Promise.all([upsertRemoteProfile(nextProfile), ...ownedApps.map((app) => updateRemoteApp(app)), ...ownedDeletedApps.map((app) => updateRemoteApp(app))]).catch(() => setDataError('프로필 정보를 저장하지 못했습니다. 잠시 후 다시 시도해주세요.'))
  }, [apps, authUser, deletedApps])

  const loginWithGithub = useCallback((redirectTo: string) => {
    if (!supabase) {
      setAuthError('로그인을 시작하지 못했습니다. 잠시 후 다시 시도해주세요.')
      return
    }
    setAuthError('')
    const redirectUrl = new URL(safeRedirectPath(redirectTo), window.location.origin).toString()
    void supabase.auth.signInWithOAuth({ provider: 'github', options: { redirectTo: redirectUrl } }).then(({ error }) => {
      if (error) setAuthError('GitHub 로그인에 실패했습니다. 다시 시도해주세요.')
    })
  }, [])

  const logout = useCallback(() => {
    if (!supabase) {
      setAuthUser(null)
      return
    }
    void supabase.auth.signOut().then(({ error }) => {
      if (error) {
        setAuthError('로그아웃하지 못했습니다.')
        return
      }
      setAuthUser(null)
      setProfile(null)
    })
  }, [])

  const closeCommentFeature = useCallback((hideForToday: boolean) => {
    if (hideForToday) {
      try {
        window.localStorage.setItem(COMMENT_FEATURE_HIDE_KEY, localDateKey())
      } catch {
        // 저장소가 차단되어도 팝업은 닫습니다.
      }
    }
    setCommentFeatureOpen(false)
    setCommentFeatureDismissed(true)
  }, [])

  const handleAnalyticsConsent = useCallback((consent: AnalyticsConsent) => {
    updateAnalyticsConsent(consent)
    setAnalyticsConsent(consent)
  }, [])

  const resetAnalyticsConsent = useCallback(() => {
    updateAnalyticsConsent(null)
    setAnalyticsConsent(null)
  }, [])

  const loginParams = new URLSearchParams(location.search)
  const loginReturnTo = loginParams.get('returnTo') ?? '/'
  const loginPage = <LoginPage isConfigured={isAuthConfigured} isLoading={authLoading} error={authError} returnTo={loginReturnTo} onLogin={loginWithGithub} />
  const submitLoginPage = <LoginPage isConfigured={isAuthConfigured} isLoading={authLoading} error={authError} returnTo="/submit" onLogin={loginWithGithub} />
  const bookmarksLoginPage = <LoginPage isConfigured={isAuthConfigured} isLoading={authLoading} error={authError} returnTo="/bookmarks" onLogin={loginWithGithub} />
  const dataStatusPage = dataError ? <DataStatusPage message={dataError} isError /> : dataLoading ? <DataStatusPage message="데이터를 불러오고 있습니다." /> : null
  const homePage = dataStatusPage ?? <HomePage apps={displayApps} bookmarkedIds={bookmarkedIds} onToggleBookmark={toggleBookmark} />
  const detailPage = dataStatusPage ?? <AppDetailPage apps={displayApps} profile={currentMaker} bookmarkedIds={bookmarkedIds} likedIds={likedIds} onToggleBookmark={toggleBookmark} onToggleLike={toggleLike} onLaunch={launchApp} onDeleteApp={deleteApp} />
  const makerPage = dataStatusPage ?? <MakerPage apps={displayApps} deletedApps={deletedApps} profile={profile} currentMaker={currentMaker} bookmarkedIds={bookmarkedIds} onSaveProfile={saveProfile} onToggleBookmark={toggleBookmark} onRestoreApp={restoreApp} />
  const bookmarksPage = authLoading ? <AuthLoadingPage /> : authUser ? dataStatusPage ?? <BookmarksPage apps={displayApps} bookmarkedIds={bookmarkedIds} onToggleBookmark={toggleBookmark} /> : bookmarksLoginPage
  const submitPage = authLoading ? <AuthLoadingPage /> : authUser && currentMaker ? dataStatusPage ?? <SubmitPage maker={currentMaker} onAddApp={addApp} onVerifyCrewCode={verifyCrewCode} /> : submitLoginPage
  const editPage = authLoading ? <AuthLoadingPage /> : authUser && currentMaker ? dataStatusPage ?? <EditAppPage apps={apps} currentUserId={authUser.id} maker={currentMaker} onUpdateApp={updateApp} /> : <LoginPage isConfigured={isAuthConfigured} isLoading={authLoading} error={authError} returnTo={window.location.pathname} onLogin={loginWithGithub} />
  const profilePage = authLoading ? <AuthLoadingPage /> : authUser && currentMaker ? dataStatusPage ?? <MakerPage apps={displayApps} deletedApps={deletedApps} profile={profile} currentMaker={currentMaker} isOwnProfile bookmarkedIds={bookmarkedIds} onSaveProfile={saveProfile} onToggleBookmark={toggleBookmark} onRestoreApp={restoreApp} /> : <LoginPage isConfigured={isAuthConfigured} isLoading={authLoading} error={authError} returnTo="/makers/me" onLogin={loginWithGithub} />
  const privacyPolicyPage = <PrivacyPolicyPage consent={analyticsConsent} onChangeAnalyticsSettings={resetAnalyticsConsent} onSetAnalyticsConsent={handleAnalyticsConsent} />
  const shouldRedirectToProfile = Boolean(authUser && profileLoadedFor === authUser.id && !authLoading && !dataLoading && !dataError && location.pathname !== '/makers/me' && location.pathname !== '/privacy' && !profile)

  if (shouldRedirectToProfile) return <Navigate to="/makers/me" replace />

  return (
    <>
      <Routes>
      <Route element={<Layout authUser={authUser} onLogout={logout} visitorStats={visitorStats} />}>
        <Route path="/" element={homePage} />
        <Route path="/login" element={authLoading ? <AuthLoadingPage /> : authUser ? homePage : loginPage} />
        <Route path="/apps/:appId" element={detailPage} />
        <Route path="/apps/:appId/edit" element={editPage} />
        <Route path="/submit" element={submitPage} />
        <Route path="/makers/me" element={profilePage} />
        <Route path="/makers/:makerId" element={makerPage} />
        <Route path="/bookmarks" element={bookmarksPage} />
        <Route path="/privacy" element={privacyPolicyPage} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
      </Routes>
      <CommentFeatureModal open={commentFeatureOpen} onClose={closeCommentFeature} />
      {isAnalyticsConfigured() && analyticsConsent === null && !commentFeatureOpen ? <AnalyticsConsentBanner onAccept={() => handleAnalyticsConsent('granted')} onReject={() => handleAnalyticsConsent('denied')} /> : null}
    </>
  )
}

export default App
