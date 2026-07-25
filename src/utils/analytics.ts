type AnalyticsParams = Record<string, string | number | boolean | null | undefined>
export type AnalyticsConsent = 'granted' | 'denied'

const ANALYTICS_CONSENT_KEY = 'dropit:analytics-consent'

declare global {
  interface Window {
    dataLayer?: unknown[]
    gtag?: (...args: unknown[]) => void
  }
}

const measurementId = import.meta.env.VITE_GA_MEASUREMENT_ID?.trim()
const isConfigured = Boolean(measurementId && /^G-[A-Z0-9]+$/i.test(measurementId))
let isInitialized = false
let lastPageView = ''
let runtimeConsent: AnalyticsConsent | null = null

export function isAnalyticsConfigured() {
  return isConfigured
}

export function getAnalyticsConsent(): AnalyticsConsent | null {
  if (typeof window === 'undefined') return null

  try {
    const savedConsent = window.localStorage.getItem(ANALYTICS_CONSENT_KEY)
    return savedConsent === 'granted' || savedConsent === 'denied' ? savedConsent : null
  } catch {
    return runtimeConsent
  }
}

export function updateAnalyticsConsent(consent: AnalyticsConsent | null) {
  if (typeof window === 'undefined') return
  runtimeConsent = consent

  try {
    if (consent) window.localStorage.setItem(ANALYTICS_CONSENT_KEY, consent)
    else window.localStorage.removeItem(ANALYTICS_CONSENT_KEY)
  } catch {
    // 저장소가 차단되어도 현재 세션에서는 동의 상태를 반영합니다.
  }

  if (isInitialized && window.gtag) {
    window.gtag('consent', 'update', {
      analytics_storage: consent === 'granted' ? 'granted' : 'denied',
    })
  }

  if (consent !== 'granted') lastPageView = ''
}

export function initializeAnalytics() {
  if (!isConfigured || getAnalyticsConsent() !== 'granted' || isInitialized || typeof window === 'undefined') return

  window.dataLayer = window.dataLayer ?? []
  window.gtag = window.gtag ?? ((...args: unknown[]) => window.dataLayer?.push(args))
  window.gtag('js', new Date())
  window.gtag('config', measurementId, { send_page_view: false })

  if (!document.getElementById('google-analytics-script')) {
    const script = document.createElement('script')
    script.id = 'google-analytics-script'
    script.async = true
    script.src = `https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(measurementId ?? '')}`
    document.head.appendChild(script)
  }

  isInitialized = true
}

export function trackPageView(path: string) {
  initializeAnalytics()
  if (!isConfigured || getAnalyticsConsent() !== 'granted' || !window.gtag || lastPageView === path) return
  lastPageView = path
  window.gtag('event', 'page_view', {
    page_path: path,
    page_location: window.location.href,
    page_title: document.title,
  })
}

export function trackEvent(name: string, params: AnalyticsParams = {}) {
  initializeAnalytics()
  if (!isConfigured || getAnalyticsConsent() !== 'granted' || !window.gtag) return
  window.gtag('event', name, params)
}

export function setAnalyticsUserId(userId: string | null) {
  initializeAnalytics()
  if (!isConfigured || getAnalyticsConsent() !== 'granted' || !window.gtag) return
  window.gtag('set', { user_id: userId })
}
