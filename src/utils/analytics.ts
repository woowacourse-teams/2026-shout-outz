type AnalyticsParams = Record<string, string | number | boolean | null | undefined>
export type AnalyticsConsent = 'granted' | 'denied'

export type AnalyticsServiceId = 'google-analytics' | 'microsoft-clarity'

export interface CrossBorderTransfer {
  recipient: string
  country: string
  transferMethod: string
  items: readonly string[]
  purposeAndRetention: string
}

export interface AnalyticsService {
  id: AnalyticsServiceId
  name: string
  provider: string
  purpose: string
  collectedData: readonly string[]
  requiresConsent: boolean
  privacyPolicyUrl: string
  crossBorderTransfer?: CrossBorderTransfer
}

const GOOGLE_ANALYTICS_SERVICE: AnalyticsService = {
  id: 'google-analytics',
  name: 'Google Analytics 4(GA4)',
  provider: 'Google LLC',
  purpose: '방문 규모와 기능 이용 흐름을 파악하고 서비스의 안정성과 사용성을 개선',
  collectedData: ['방문한 페이지와 서비스 이용 이벤트'],
  requiresConsent: false,
  privacyPolicyUrl: 'https://policies.google.com/privacy?hl=ko',
}

const MICROSOFT_CLARITY_SERVICE: AnalyticsService = {
  id: 'microsoft-clarity',
  name: 'Microsoft Clarity',
  provider: 'Microsoft Corporation',
  purpose: '화면 조작 흐름을 이해하고 UI 개선점을 찾기 위해 세션 리플레이와 히트맵을 분석',
  collectedData: ['마우스 이동·클릭·스크롤 등 화면 조작 기록(세션 리플레이) 및 클릭 히트맵'],
  requiresConsent: true,
  privacyPolicyUrl: 'https://privacy.microsoft.com/ko-kr/privacystatement',
  crossBorderTransfer: {
    recipient: 'Microsoft Corporation (Microsoft Clarity)',
    country: '미국 등 Microsoft가 서버를 운영·위탁하는 국가',
    transferMethod: '서비스 이용 시 네트워크를 통해 실시간 전송',
    items: ['세션 리플레이(화면 조작 기록)', '클릭 히트맵', '브라우저·기기 정보', '쿠키 식별자'],
    purposeAndRetention: '화면 조작 흐름 분석 및 UX 개선 목적으로 사용하며, Microsoft Clarity의 데이터 보관 정책에 따라 보관 후 파기',
  },
}

const ANALYTICS_CONSENT_KEY = 'dropit:analytics-consent'

declare global {
  interface Window {
    dataLayer?: unknown[]
    gtag?: (...args: unknown[]) => void
    clarity?: (...args: unknown[]) => void
  }
}

const measurementId = import.meta.env.VITE_GA_MEASUREMENT_ID?.trim()
const clarityProjectId = import.meta.env.VITE_CLARITY_PROJECT_ID?.trim()

export function resolveConfiguredAnalyticsServices(
  gaMeasurementId: string | undefined,
  clarityId: string | undefined,
): readonly AnalyticsService[] {
  const services: AnalyticsService[] = []
  const normalizedGaId = gaMeasurementId?.trim()
  const normalizedClarityId = clarityId?.trim()

  if (normalizedGaId && /^G-[A-Z0-9]+$/i.test(normalizedGaId)) services.push(GOOGLE_ANALYTICS_SERVICE)
  if (normalizedClarityId && /^[a-z0-9]+$/i.test(normalizedClarityId)) services.push(MICROSOFT_CLARITY_SERVICE)

  return services
}

const configuredServices = resolveConfiguredAnalyticsServices(measurementId, clarityProjectId)
const isGaConfigured = configuredServices.some(({ id }) => id === 'google-analytics')
const isClarityConfigured = configuredServices.some(({ id }) => id === 'microsoft-clarity')
let isGaInitialized = false
let isClarityInitialized = false
let lastPageView = ''
let runtimeConsent: AnalyticsConsent | null = null

export function getConfiguredAnalyticsServices(): readonly AnalyticsService[] {
  return configuredServices
}

export function isAnalyticsConsentRequired() {
  return configuredServices.some((service) => service.requiresConsent)
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

  // Google Analytics는 동의 없이 항상 수집되므로 동의 변경은 Clarity에만 반영합니다.
  if (isClarityInitialized) window.clarity?.('consent', consent === 'granted')
}

function initializeGoogleAnalytics() {
  if (!isGaConfigured || !measurementId || isGaInitialized) return

  window.dataLayer = window.dataLayer ?? []
  window.gtag = window.gtag ?? ((...args: unknown[]) => window.dataLayer?.push(args))
  window.gtag('js', new Date())
  window.gtag('config', measurementId, { send_page_view: false })

  if (!document.getElementById('google-analytics-script')) {
    const script = document.createElement('script')
    script.id = 'google-analytics-script'
    script.async = true
    script.src = `https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(measurementId)}`
    document.head.appendChild(script)
  }

  isGaInitialized = true
}

function initializeClarity() {
  if (!isClarityConfigured || !clarityProjectId || isClarityInitialized) return
  if (getAnalyticsConsent() !== 'granted') return

  type ClarityQueue = ((...args: unknown[]) => void) & { q?: unknown[][] }
  const clarity: ClarityQueue = window.clarity ?? ((...args: unknown[]) => {
    clarity.q = clarity.q ?? []
    clarity.q.push(args)
  })
  window.clarity = clarity

  if (!document.getElementById('microsoft-clarity-script')) {
    const script = document.createElement('script')
    script.id = 'microsoft-clarity-script'
    script.async = true
    script.src = `https://www.clarity.ms/tag/${encodeURIComponent(clarityProjectId)}`
    document.head.appendChild(script)
  }

  window.clarity('consent')
  isClarityInitialized = true
}

export function initializeAnalytics() {
  if (typeof window === 'undefined') return

  // GA4는 국내 기준 별도 동의 없이 바로 수집을 시작합니다.
  initializeGoogleAnalytics()
  // Clarity는 화면을 녹화하는 방식이라 동의한 경우에만 초기화합니다.
  initializeClarity()
}

export function trackPageView(path: string) {
  initializeAnalytics()
  if (!isGaConfigured || !window.gtag || lastPageView === path) return
  lastPageView = path
  window.gtag('event', 'page_view', {
    page_path: path,
    page_location: window.location.href,
    page_title: document.title,
  })
}

export function trackEvent(name: string, params: AnalyticsParams = {}) {
  initializeAnalytics()
  if (!isGaConfigured || !window.gtag) return
  window.gtag('event', name, params)
}
