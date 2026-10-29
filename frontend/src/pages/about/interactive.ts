import { matches, topicSamples } from '@/pages/about/samples';
import { jobResourceMarkup, resourceMarkup, type Topic } from '@/pages/about/renderSamples';
import snapshot from '@/pages/about/prototype-snapshot.json';
import { PROTOTYPE_URL } from '@/pages/about/links';

export function initAbout(root: HTMLElement) {
  const cleanups: (() => void)[] = [];
  const listen = (element: HTMLElement, type: string, callback: (event: Event) => void) => {
    element.addEventListener(type, callback);
    cleanups.push(() => element.removeEventListener(type, callback));
  };
  const query = <T extends HTMLElement = HTMLElement>(selector: string) =>
    root.querySelector<T>(selector)!;
  const queryAll = (selector: string) => root.querySelectorAll<HTMLElement>(selector);
  const byId = (id: string) => query(`#${id}`);
  const refGroups = byId('ref-groups');
  const refQuery = query<HTMLInputElement>('#ref-query');
  const refButtons = queryAll('[data-ref-topic]');
  function isTopic(key: string | undefined): key is Topic {
    return key !== undefined && Object.hasOwn(topicSamples, key);
  }
  function showRefTopic(key: Topic) {
    const sample = topicSamples[key];
    if (!sample) return;
    refQuery.value = sample.keyword;
    byId('ref-topic-label').textContent = `‘${sample.keyword}’`;
    byId('ref-category').textContent = sample.category;
    refGroups.innerHTML = resourceMarkup(key);
    refButtons.forEach((button) => {
      const selected = button.dataset.refTopic === key;
      button.classList.toggle('active', selected);
      button.setAttribute('aria-pressed', String(selected));
    });
  }
  refButtons.forEach((button) =>
    listen(
      button,
      'click',
      () => isTopic(button.dataset.refTopic) && showRefTopic(button.dataset.refTopic),
    ),
  );
  listen(byId('ref-search'), 'submit', (event) => {
    event.preventDefault();
    const q = refQuery.value.trim();
    if (q) window.location.href = PROTOTYPE_URL;
  });

  const jobButtons = queryAll('[data-job]');
  jobButtons.forEach((button) =>
    listen(button, 'click', () => {
      const index = Number(button.dataset.job);
      const item = snapshot.jobKeywords[index];
      if (!item) return;
      jobButtons.forEach((x) => {
        const active = x === button;
        x.classList.toggle('active', active);
        x.setAttribute('aria-pressed', String(active));
      });
      byId('ref-job-summary').textContent = item.reason;
      byId('ref-job-groups').innerHTML = jobResourceMarkup(index);
    }),
  );

  const heroSearch = byId('hero-search');
  const heroQuery = query<HTMLInputElement>('#hero-query');
  function searchPrototype(q: string) {
    if (q.trim()) window.location.href = PROTOTYPE_URL;
  }
  listen(heroSearch, 'submit', (event) => {
    event.preventDefault();
    searchPrototype(heroQuery.value);
  });
  queryAll('[data-hero-topic]').forEach((button) =>
    listen(button, 'click', () => {
      heroQuery.value = button.dataset.heroTopic ?? '';
      if (isTopic(button.dataset.topicKey)) showRefTopic(button.dataset.topicKey);
      queryAll('[data-hero-topic]').forEach((item) => {
        item.classList.toggle('active', item === button);
        item.setAttribute('aria-pressed', String(item === button));
      });
    }),
  );

  const requirements = [...queryAll('[data-requirement]')];
  requirements.forEach((button) =>
    listen(button, 'click', () => {
      requirements.forEach((item) => item.setAttribute('aria-pressed', String(item === button)));
      const visual = query('.jd-visual');
      visual.classList.remove('is-matching');
      void visual.offsetWidth;
      visual.classList.add('is-matching');
      const match =
        matches[button.dataset.requirement === 'performance' ? 'performance' : 'concurrency'];
      query('#jd-result-title').textContent = match[0];
      query('#jd-result-reason').textContent = match[1];
      query('#jd-result-title').style.whiteSpace = 'pre-line';
      query('#jd-result-reason').style.whiteSpace = 'pre-line';
    }),
  );

  return () => cleanups.forEach((cleanup) => cleanup());
}
