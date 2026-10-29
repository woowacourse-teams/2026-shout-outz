import { topicSamples } from '@/pages/about/samples';
import snapshot from '@/pages/about/prototype-snapshot.json';

export type Topic = keyof typeof topicSamples;
// 정적 스냅샷으로 SSG와 클라이언트 전환을 같은 형태로 렌더링합니다.
const escape = (value: string) =>
  value.replace(
    /[&<>"']/g,
    (char) =>
      ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;',
      })[char]!,
  );

const youtubeId = (url: string) => {
  try {
    const parsed = new URL(url);
    const id =
      parsed.hostname === 'youtu.be'
        ? parsed.pathname.slice(1)
        : /(^|\.)youtube\.com$/.test(parsed.hostname)
          ? parsed.searchParams.get('v')
          : null;
    return id && /^[a-zA-Z0-9_-]{11}$/.test(id) ? id : null;
  } catch {
    return null;
  }
};

// 프로토타입에서 사용하는 출처 표시. URL은 로고를 고르는 데만 사용합니다.
const sourceIcon = (type: string, url: string) => {
  const icon = (path: string) =>
    `<svg class="ref-document-source-icon" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${path}</svg>`;
  if (type === 'PR')
    return icon(
      '<circle cx="6" cy="6" r="3"/><circle cx="18" cy="18" r="3"/><path d="M6 9v12M18 15V9a3 3 0 0 0-3-3h-3m3-3-3 3 3 3"/>',
    );
  if (type === '팀 문서')
    return icon(
      '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M8 13h8M8 17h6"/>',
    );
  if (type !== '블로그') return '';
  try {
    const host = new URL(url).hostname;
    const asset =
      host === 'tistory.com' || host.endsWith('.tistory.com')
        ? 'tistory.svg'
        : host === 'velog.io' || host.endsWith('.velog.io')
          ? 'velog.ico'
          : host === 'rilog.kr' || host.endsWith('.rilog.kr')
            ? 'rilog.svg'
            : null;
    if (host === 'github.com' || host.endsWith('.github.io'))
      return `<svg class="ref-document-source-icon is-github" aria-hidden="true" viewBox="0 0 16 16" fill="currentColor"><path d="M8 0a8 8 0 0 0-2.53 15.59c.4.07.55-.17.55-.38v-1.36c-2.23.48-2.69-.94-2.69-.94-.36-.92-.89-1.17-.89-1.17-.73-.5.06-.49.06-.49.81.06 1.23.83 1.23.83.72 1.23 1.88.88 2.34.67.07-.52.28-.88.5-1.08-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.65 7.65 0 0 1 4 0c1.53-1.03 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.28.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.74.54 1.49v2.22c0 .21.15.46.55.38A8 8 0 0 0 8 0Z"/></svg>`;
    if (asset)
      return `<img class="ref-document-source-icon" src="/about-assets/assets/${asset}" alt="" loading="lazy">`;
  } catch {
    /* 스냅샷의 URL이 유효하지 않으면 출처 배지만 보여줍니다. */
  }
  return '';
};

const formatDate = (date: string) => {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date);
  return match ? `${match[1]}년 ${Number(match[2])}월 ${Number(match[3])}일` : '';
};

type ResourceGroup = (typeof topicSamples)[Topic]['groups'];
type Counts = (typeof topicSamples)[Topic]['counts'];

function renderResourceGroups(groups: ResourceGroup, counts: Counts, limit: number) {
  const steps = [
    { label: '블로그', types: ['블로그'], count: counts.blog },
    { label: 'PR', types: ['PR', '팀 문서'], count: counts.work },
    { label: '테코톡', types: ['테코톡'], count: counts.talk },
  ];
  return steps
    .map((step) => {
      const items = groups.filter((item) => step.types.includes(item.type)).slice(0, limit);
      const cards = items
        .map((item) => {
          const video = item.type === '테코톡' ? youtubeId(item.url) : null;
          return `
        <article class="ref-document">${video ? `<img class="ref-document-thumbnail" src="https://i.ytimg.com/vi/${video}/mqdefault.jpg" alt="" loading="lazy">` : ''}
          <div class="ref-document-meta">${sourceIcon(item.type, item.url)}<span class="ref-document-badge ${item.type === '테코톡' || item.type === '팀 문서' ? 'is-green' : ''}">${escape(item.type)}</span><span class="ref-document-project">${escape(item.project)}</span><time>${escape(formatDate(item.date))}</time></div>
          <h3>${escape(item.title)}</h3>
          <p class="ref-document-description">${escape(item.description)}</p>
        </article>`;
        })
        .join('');
      return `<section class="ref-step"><div class="ref-step-heading"><h3>${step.label}</h3><small>${step.count}</small></div><div class="ref-step-cards">${cards || '<p class="ref-step-empty">이 단계 자료는 아직 없어요.</p>'}</div></section>`;
    })
    .join('');
}

export function resourceMarkup(key: Topic) {
  const sample = topicSamples[key];
  return renderResourceGroups(sample.groups, sample.counts, 2);
}

export function jobResourceMarkup(index: number) {
  const sample = snapshot.jobKeywords[index];
  return sample ? renderResourceGroups(sample.groups, sample.counts, 1) : '';
}

export function jobButtonsMarkup() {
  return snapshot.jobKeywords
    .map(
      (item, index) =>
        `<button type="button" data-job="${index}" class="${index === 0 ? 'active' : ''}" aria-pressed="${index === 0}">${escape(item.keyword)}</button>`,
    )
    .join('');
}
