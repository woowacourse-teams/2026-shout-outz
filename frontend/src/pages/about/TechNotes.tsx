import { IconChevronDown, IconTerminal2 } from '@tabler/icons-react';

import { Badge } from '@/components/Badge';
import techNotes from '@/pages/about/data/tech-notes.json';

interface BarRow {
  label: string;
  value: number;
  display?: string;
  note?: string;
  emph?: boolean;
}

interface Note {
  tags: string[];
  text: string;
  flow?: { kicker: string; title: string; emph?: boolean }[];
  charts: { caption: string; unit?: string; rows: BarRow[] }[];
}

const notes = techNotes.notes as Note[];

// 서버(Node)와 브라우저가 같은 결과를 내도록 로케일 대신 직접 천 단위 쉼표를 넣는다.
const formatNumber = (value: number) => String(value).replace(/\B(?=(\d{3})+(?!\d))/g, ',');

/** 자료를 모으고 다루는 방식과 그 수치. 기본으로 접어 둔다. */
export function TechNotes() {
  return (
    <details className="border-t border-gray-200 py-6">
      <summary className="focus-visible:outline-primary-600 flex cursor-pointer list-none items-center justify-between gap-4 rounded-lg py-2 text-sm font-semibold text-gray-700 hover:text-gray-900 focus-visible:outline-2 [&::-webkit-details-marker]:hidden">
        <span>자료와 구현 방식 보기</span>
      </summary>
      <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {notes.map((note) => (
          <aside
            key={note.text}
            aria-label="이 화면 뒤의 기술"
            className="min-w-0 rounded-2xl border border-dashed border-gray-300 bg-gray-50 p-5"
          >
            <p className="flex items-center gap-1.5 text-xs font-bold text-gray-900">
              <IconTerminal2 className="text-primary-500 size-4" aria-hidden="true" />이 화면
              뒤에서는
            </p>
            <ul className="mt-3 flex flex-wrap gap-1.5">
              {note.tags.map((tag) => (
                <li key={tag}>
                  <Badge tone="primary">{tag}</Badge>
                </li>
              ))}
            </ul>
            <p className="mt-3 text-xs leading-5 text-gray-600">{note.text}</p>
            <div className="mt-4">
              {note.flow && <Flow steps={note.flow} />}
              {note.charts.map((chart) => (
                <div key={chart.caption} className={note.flow ? 'mt-4' : undefined}>
                  <HorizontalBars {...chart} />
                </div>
              ))}
            </div>
          </aside>
        ))}
      </div>
      <p className="mt-4 text-xs text-gray-400">
        기술 메모의 수치는 {techNotes.asOf} 기준 스냅샷이고, 일부 비용은 예상치예요.
      </p>
    </details>
  );
}

/** 가로 막대 한 묶음. 한 계열이라 범례 없이 제목이 계열 이름이 되고, emph가 false인 막대는 회색으로 둔다. */
function HorizontalBars({ caption, unit = '', rows }: Note['charts'][number]) {
  const max = Math.max(...rows.map((row) => row.value));
  return (
    <figure className="min-w-0">
      <figcaption className="mb-3 text-xs font-semibold text-gray-500">{caption}</figcaption>
      <dl className="space-y-2.5">
        {rows.map((row) => {
          const shown = row.display ?? `${formatNumber(row.value)}${unit}`;
          return (
            <div
              key={row.label}
              className="group grid grid-cols-[5.5rem_1fr] items-center gap-3"
              title={`${row.label}: ${shown}${row.note ? ` (${row.note})` : ''}`}
            >
              <dt className="min-w-0 text-xs text-gray-600">
                <span className="block truncate">{row.label}</span>
                {row.note && <span className="block truncate text-gray-400">{row.note}</span>}
              </dt>
              <dd className="flex min-w-0 items-center gap-3">
                <span className="relative h-2 min-w-0 flex-1">
                  <span
                    className={`absolute inset-y-0 left-0 rounded-r-sm transition-opacity group-hover:opacity-80 ${row.emph === false ? 'bg-gray-300' : 'bg-primary-500'}`}
                    style={{ width: `${Math.max(1.5, (row.value / max) * 100).toFixed(1)}%` }}
                  />
                </span>
                <span className="w-20 shrink-0 text-right text-xs font-semibold text-gray-900 tabular-nums">
                  {shown}
                </span>
              </dd>
            </div>
          );
        })}
      </dl>
    </figure>
  );
}

/** 단계 상자를 화살표로 잇는 세로 흐름도 */
function Flow({ steps }: { steps: NonNullable<Note['flow']> }) {
  return (
    <ol className="space-y-1.5">
      {steps.map((step, index) => (
        <li key={step.title}>
          <div
            className={`rounded-lg border px-3 py-2 ${step.emph ? 'border-primary-200 bg-primary-50' : 'bg-background border-gray-200'}`}
          >
            <p className={`text-xs ${step.emph ? 'text-primary-600' : 'text-gray-500'}`}>
              {step.kicker}
            </p>
            <p className="text-sm font-bold text-gray-900">{step.title}</p>
          </div>
          {index < steps.length - 1 && (
            <span aria-hidden="true" className="flex justify-center text-gray-300">
              <IconChevronDown className="size-4" />
            </span>
          )}
        </li>
      ))}
    </ol>
  );
}
