import { readFile, rename, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const GITHUB_API_URL = 'https://api.github.com'
const GITHUB_API_VERSION = '2026-03-10'
const SCRIPT_DIRECTORY = dirname(fileURLToPath(import.meta.url))
const INPUT_PATH = resolve(SCRIPT_DIRECTORY, 'prev-repo-list.json')
const OUTPUT_PATH = resolve(SCRIPT_DIRECTORY, 'prev-crew.json')
const TEMP_OUTPUT_PATH = `${OUTPUT_PATH}.tmp`

function isEscaped(value, index) {
  let slashCount = 0
  for (let cursor = index - 1; cursor >= 0 && value[cursor] === '\\'; cursor -= 1) slashCount += 1
  return slashCount % 2 === 1
}

function findMarkdownImageSources(markdown) {
  const sources = []
  let cursor = 0

  while (cursor < markdown.length - 1) {
    const imageStart = markdown.indexOf('![', cursor)
    if (imageStart === -1) break

    let altEnd = imageStart + 2
    let bracketDepth = 0
    for (; altEnd < markdown.length; altEnd += 1) {
      const character = markdown[altEnd]
      if (character === '\n' || character === '\r') break
      if (isEscaped(markdown, altEnd)) continue
      if (character === '[') bracketDepth += 1
      if (character === ']' && bracketDepth > 0) bracketDepth -= 1
      else if (character === ']') break
    }

    if (markdown[altEnd] !== ']') {
      cursor = imageStart + 2
      continue
    }

    let destinationStart = altEnd + 1
    while (destinationStart < markdown.length && /[ \t]/.test(markdown[destinationStart])) destinationStart += 1
    if (markdown[destinationStart] !== '(') {
      cursor = altEnd + 1
      continue
    }

    destinationStart += 1
    while (destinationStart < markdown.length && /[ \t]/.test(markdown[destinationStart])) destinationStart += 1

    let destinationEnd = destinationStart
    if (markdown[destinationStart] === '<') {
      destinationStart += 1
      destinationEnd = destinationStart
      while (destinationEnd < markdown.length && (markdown[destinationEnd] !== '>' || isEscaped(markdown, destinationEnd))) destinationEnd += 1
      if (markdown[destinationEnd] !== '>') {
        cursor = altEnd + 1
        continue
      }
    } else {
      let parenthesisDepth = 0
      while (destinationEnd < markdown.length) {
        const character = markdown[destinationEnd]
        if (character === '\n' || character === '\r') break
        if (isEscaped(markdown, destinationEnd)) {
          destinationEnd += 1
          continue
        }
        if (character === '(') parenthesisDepth += 1
        else if (character === ')' && parenthesisDepth > 0) parenthesisDepth -= 1
        else if (character === ')' || (/\s/.test(character) && parenthesisDepth === 0)) break
        destinationEnd += 1
      }
    }

    if (destinationEnd > destinationStart) {
      sources.push({
        start: destinationStart,
        end: destinationEnd,
        value: markdown.slice(destinationStart, destinationEnd),
      })
    }
    cursor = Math.max(destinationEnd + 1, altEnd + 1)
  }

  return sources
}

function findHtmlImageSources(markdown) {
  const sources = []
  const tagPattern = /<img\b(?:"[^"]*"|'[^']*'|[^'">])*>/gi
  let tagMatch

  while ((tagMatch = tagPattern.exec(markdown)) !== null) {
    const tag = tagMatch[0]
    const sourceAttribute = /\bsrc\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'=<>`]+))/i.exec(tag)
    if (!sourceAttribute) continue

    const value = sourceAttribute[1] ?? sourceAttribute[2] ?? sourceAttribute[3]
    if (!value) continue

    const isQuoted = sourceAttribute[1] !== undefined || sourceAttribute[2] !== undefined
    const valueOffset = sourceAttribute.index + sourceAttribute[0].length - value.length - (isQuoted ? 1 : 0)
    const start = tagMatch.index + valueOffset
    sources.push({ start, end: start + value.length, value })
  }

  return sources
}

function normalizeRepositoryPath(value) {
  const suffixIndex = value.search(/[?#]/)
  const path = suffixIndex === -1 ? value : value.slice(0, suffixIndex)
  const suffix = suffixIndex === -1 ? '' : value.slice(suffixIndex)
  const normalizedParts = []

  for (const part of path.replace(/^\/+/, '').split('/')) {
    if (!part || part === '.') continue
    if (part === '..') normalizedParts.pop()
    else normalizedParts.push(part)
  }

  return `${normalizedParts.join('/')}${suffix}`
}

export function toAbsoluteImageUrl(value, { owner, repo, defaultBranch }) {
  const source = value.trim()
  if (!source) return source
  if (source.startsWith('//')) return `https:${source}`
  if (/^[a-z][a-z\d+.-]*:/i.test(source) || source.startsWith('#')) return source

  const repositoryPath = normalizeRepositoryPath(source)
  const encodedBranch = defaultBranch.split('/').map(encodeURIComponent).join('/')
  const rawBaseUrl = `https://raw.githubusercontent.com/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/${encodedBranch}/`
  return `${rawBaseUrl}${encodeURI(repositoryPath)}`
}

export function rewriteReadmeImages(markdown, repository) {
  const foundMatches = [
    ...findMarkdownImageSources(markdown),
    ...findHtmlImageSources(markdown),
  ].sort((left, right) => left.start - right.start)

  const matches = []
  for (const match of foundMatches) {
    const previousMatch = matches.at(-1)
    if (!previousMatch || match.start >= previousMatch.end) matches.push(match)
  }

  const images = matches.map(({ value }) => toAbsoluteImageUrl(value, repository))
  let readme = ''
  let cursor = 0

  for (let index = 0; index < matches.length; index += 1) {
    const match = matches[index]
    readme += markdown.slice(cursor, match.start)
    readme += images[index]
    cursor = match.end
  }
  readme += markdown.slice(cursor)

  return { images, readme }
}

export function parseRepositoryList(source) {
  const repositories = JSON.parse(source)
  if (!Array.isArray(repositories)) throw new Error('입력 파일의 최상위 값은 배열이어야 합니다.')

  return repositories.map((repository, index) => {
    const owner = repository?.owner
    const repo = repository?.repo
    const title = repository?.title
    const year = repository?.year
    const generation = repository?.generation
    if (typeof owner !== 'string' || !owner.trim() || typeof repo !== 'string' || !repo.trim()) {
      throw new Error(`${index + 1}번째 항목에 유효한 owner와 repo 문자열이 필요합니다.`)
    }
    if (!Number.isInteger(year) || year < 1 || typeof generation !== 'string' || !generation.trim()) {
      throw new Error(`${index + 1}번째 항목에 유효한 year 정수와 generation 문자열이 필요합니다.`)
    }
    if (title !== undefined && (typeof title !== 'string' || !title.trim())) {
      throw new Error(`${index + 1}번째 항목의 title은 비어 있지 않은 문자열이어야 합니다.`)
    }

    return {
      owner: owner.trim(),
      repo: repo.trim(),
      ...(typeof title === 'string' ? { title: title.trim() } : {}),
      year,
      generation: generation.trim(),
    }
  })
}

function createGitHubClient(token) {
  const headers = {
    Accept: 'application/vnd.github+json',
    Authorization: `Bearer ${token}`,
    'User-Agent': 'dropit-prev-crew-generator',
    'X-GitHub-Api-Version': GITHUB_API_VERSION,
  }

  async function request(path, accept = headers.Accept) {
    const url = path.startsWith('http') ? path : `${GITHUB_API_URL}${path}`
    const response = await fetch(url, { headers: { ...headers, Accept: accept } })
    if (!response.ok) {
      let detail = ''
      try {
        const body = await response.json()
        detail = typeof body?.message === 'string' ? `: ${body.message}` : ''
      } catch {
        // The status code still provides a useful error when GitHub returns a non-JSON body.
      }
      throw new Error(`GitHub API ${response.status} ${response.statusText}${detail}`)
    }
    return response
  }

  async function getJson(path) {
    const response = await request(path)
    return { data: await response.json(), response }
  }

  return { getJson, request }
}

function nextLink(linkHeader) {
  if (!linkHeader) return null
  for (const part of linkHeader.split(',')) {
    const match = part.match(/<([^>]+)>;\s*rel="([^"]+)"/)
    if (match?.[2] === 'next') return match[1]
  }
  return null
}

async function fetchAllContributors(client, owner, repo) {
  const contributors = []
  let nextUrl = `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/contributors?per_page=100`

  while (nextUrl) {
    const { data, response } = await client.getJson(nextUrl)
    if (!Array.isArray(data)) throw new Error('기여자 API 응답이 배열이 아닙니다.')
    contributors.push(...data)
    nextUrl = nextLink(response.headers.get('link'))
  }

  return contributors
}

// Bot 계정은 GitHub App 이라 /users 조회가 404 로 실패한다.
// (dependabot[bot] 처럼 조회되는 것도 있지만 Copilot 처럼 안 되는 것도 있다.)
// 어차피 크루 명단에 넣지 않을 대상이므로 수집 단계에서 제외한다.
function isBotContributor(contributor) {
  const login = contributor?.login ?? ''
  return contributor?.type === 'Bot' || login.endsWith('[bot]') || login === 'Copilot'
}

async function fetchContributorProfiles(client, contributors) {
  const profiles = []

  for (const contributor of contributors) {
    if (typeof contributor?.login !== 'string' || !contributor.login) {
      throw new Error('login이 없는 기여자를 발견했습니다.')
    }
    if (isBotContributor(contributor)) continue

    const { data: user } = await client.getJson(`/users/${encodeURIComponent(contributor.login)}`)
    profiles.push({
      login: user.login,
      name: user.name ?? null,
      bio: user.bio ?? null,
      avatarUrl: user.avatar_url,
      htmlUrl: user.html_url,
    })
  }

  return profiles
}

async function fetchProject(client, repository, id) {
  const { owner, repo, title, year, generation } = repository
  const repositoryPath = `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}`
  const { data: metadata } = await client.getJson(repositoryPath)

  if (typeof metadata?.name !== 'string' || typeof metadata?.default_branch !== 'string') {
    throw new Error('저장소 메타데이터에 name 또는 default_branch가 없습니다.')
  }

  const defaultBranch = metadata.default_branch
  const readmeResponse = await client.request(
    `${repositoryPath}/readme?ref=${encodeURIComponent(defaultBranch)}`,
    'application/vnd.github.raw+json',
  )
  const originalReadme = await readmeResponse.text()
  const { images, readme } = rewriteReadmeImages(originalReadme, { owner, repo, defaultBranch })
  const contributors = await fetchAllContributors(client, owner, repo)
  const contributorProfiles = await fetchContributorProfiles(client, contributors)

  return {
    id,
    year,
    generation,
    stars: Number.isInteger(metadata.stargazers_count) ? metadata.stargazers_count : 0,
    owner,
    repo,
    githubUrl: metadata.html_url ?? `https://github.com/${owner}/${repo}`,
    title: title ?? metadata.name,
    about: metadata.description ?? '',
    defaultBranch,
    images,
    readme,
    contributors: contributorProfiles,
  }
}

// 기존 결과 파일을 읽어 repo 를 키로 하는 Map 으로 만든다.
// 파일이 없거나 형식이 깨졌으면 빈 Map 을 돌려주고 전체 수집으로 진행한다.
async function readExistingProjects(outputPath) {
  try {
    const parsed = JSON.parse(await readFile(outputPath, 'utf8'))
    if (!Array.isArray(parsed)) return new Map()
    return new Map(parsed.filter((project) => project?.repo).map((project) => [project.repo, project]))
  } catch {
    return new Map()
  }
}

export async function generatePreviousCrew({
  token,
  inputPath = INPUT_PATH,
  outputPath = OUTPUT_PATH,
  // 기본은 증분 수집. 이미 수집된 저장소는 GitHub 을 다시 부르지 않고 그대로 재사용한다.
  // 전체를 최신화하려면 refresh: true (CLI 는 --refresh).
  refresh = false,
} = {}) {
  if (!token) throw new Error('GITHUB_TOKEN 환경변수가 필요합니다. VITE_ 접두사는 사용하지 마세요.')

  const repositories = parseRepositoryList(await readFile(inputPath, 'utf8'))
  const existing = refresh ? new Map() : await readExistingProjects(outputPath)
  const client = createGitHubClient(token)
  const projects = []
  let reused = 0
  let fetched = 0
  let failed = 0

  for (const [index, repository] of repositories.entries()) {
    const label = `${repository.owner}/${repository.repo}`
    const position = `[${index + 1}/${repositories.length}]`
    const cached = existing.get(repository.repo)

    // 목록에서 title, year, generation 을 고쳤을 수 있으므로 그 값은 항상 최신 목록을 따른다.
    if (cached) {
      projects.push({
        ...cached,
        id: projects.length + 1,
        title: repository.title ?? cached.title,
        year: repository.year,
        generation: repository.generation,
      })
      reused += 1
      console.log(`${position} ${label} 건너뜀 (이미 수집됨)`)
      continue
    }

    console.log(`${position} ${label} 수집 중`)
    try {
      projects.push(await fetchProject(client, repository, projects.length + 1))
      fetched += 1
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error)
      console.warn(`[경고] ${label} 건너뜀: ${message}`)
      failed += 1
    }
  }

  const temporaryPath = outputPath === OUTPUT_PATH ? TEMP_OUTPUT_PATH : `${outputPath}.tmp`
  await writeFile(temporaryPath, `${JSON.stringify(projects, null, 2)}\n`, 'utf8')
  await rename(temporaryPath, outputPath)
  console.log(
    `완료: ${projects.length}/${repositories.length}개 프로젝트를 ${outputPath}에 저장했습니다. ` +
      `(신규 수집 ${fetched}, 재사용 ${reused}, 실패 ${failed})`,
  )
  return projects
}

async function main() {
  try {
    await generatePreviousCrew({
      token: process.env.GITHUB_TOKEN,
      refresh: process.argv.includes('--refresh'),
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    console.error(`[오류] ${message}`)
    process.exitCode = 1
  }
}

if (process.argv[1] && pathToFileURL(resolve(process.argv[1])).href === import.meta.url) {
  await main()
}
