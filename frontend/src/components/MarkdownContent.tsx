import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';
import rehypeSanitize, { defaultSchema } from 'rehype-sanitize';
import { parseImageSize } from '@/utils/markdown';

/** 기본 허용 목록에 글자 색 전용 태그 `<font>`만 더한다. color 속성은 기본으로 모든 태그에 허용된다. */
const SANITIZE_SCHEMA = {
  ...defaultSchema,
  tagNames: [...(defaultSchema.tagNames ?? []), 'font'],
};

/**
 * `color="red"` 값을 컬러 토큰으로 바꾼다. sanitize가 style·class는 지우지만 color 속성은 남기므로,
 * 정해둔 색만 토큰으로 칠하고 나머지 값은 무시한다. 제목·링크에 걸린 기본 글자색보다 우선하도록
 * important로 둔다.
 */
const TEXT_COLOR_CLASSES: Record<string, string> = {
  red: 'text-red-600!',
  blue: 'text-blue-600!',
  green: 'text-green-600!',
  yellow: 'text-yellow-600!',
  gray: 'text-gray-500!',
  primary: 'text-primary-600!',
};

interface HastNode {
  type: string;
  tagName?: string;
  properties?: Record<string, unknown>;
  children?: HastNode[];
}

/** sanitize를 거친 뒤 모든 태그의 color 속성을 토큰 클래스로 바꾸고, `<font>`는 `<span>`으로 그린다. */
function rehypeTextColor() {
  const paint = (node: HastNode) => {
    if (node.type === 'element' && node.properties) {
      if (node.tagName === 'font') node.tagName = 'span';
      const { color, className } = node.properties;
      if (typeof color === 'string') {
        delete node.properties.color;
        const colorClass = TEXT_COLOR_CLASSES[color.trim().toLowerCase()];
        if (colorClass) {
          node.properties.className = [...(Array.isArray(className) ? className : []), colorClass];
        }
      }
    }
    node.children?.forEach(paint);
  };
  return paint;
}

interface MdastNode {
  type: string;
  children?: MdastNode[];
  position?: { start: { line: number }; end: { line: number } };
}

/**
 * 블록 사이에 빈 줄을 두 줄 이상 넣으면, 첫 빈 줄은 기존처럼 문단 간격으로 두고 나머지는 한 줄짜리
 * 빈 문단(`<p><br></p>`)으로 그린다. 마크다운은 빈 줄이 몇 개든 문단 간격 하나로 합치기 때문이다.
 * 목록 안 빈 줄은 목록 간격 규칙이 따로 있어 건드리지 않는다.
 */
function remarkBlankLines() {
  const keep = (parent: MdastNode) => {
    if (!parent.children) return;
    parent.children = parent.children.flatMap((child, index, siblings) => {
      if (child.type === 'blockquote') keep(child);
      const prev = siblings[index - 1];
      if (!prev?.position || !child.position) return [child];
      const extraBlankLines = child.position.start.line - prev.position.end.line - 2;
      const spacers = Array.from({ length: Math.max(0, extraBlankLines) }, () => ({
        type: 'paragraph',
        children: [{ type: 'break' }],
      }));
      return [...spacers, child];
    });
  };
  return keep;
}

export function MarkdownContent({
  children,
  preserveBlankLines = false,
}: {
  children: string;
  /** 빈 줄을 여러 개 넣은 만큼 간격을 띄운다. 매거진처럼 편집자가 간격을 직접 잡는 본문에 쓴다. */
  preserveBlankLines?: boolean;
}) {
  if (!children.trim()) return <p className="text-gray-500">등록된 프로젝트 소개가 없습니다.</p>;

  // 글자 크기는 노션 기준: 본문 16px, 제목 30/24/20px, 코드 85%, 캡션 14px
  return (
    <div className="[&_a]:text-primary-600 min-w-0 text-base leading-normal break-words text-gray-600 [&_a]:underline [&_blockquote]:my-4 [&_blockquote]:border-l-4 [&_blockquote]:border-gray-200 [&_blockquote]:pl-4 [&_code]:rounded-sm [&_code]:bg-gray-100 [&_code]:px-1 [&_code]:text-[85%] [&_details]:my-4 [&_h1]:mt-8 [&_h1]:mb-4 [&_h1]:text-[1.875rem] [&_h1]:leading-[1.3] [&_h1]:font-bold [&_h1]:text-gray-900 [&_h2]:mt-8 [&_h2]:mb-3 [&_h2]:text-2xl [&_h2]:leading-[1.3] [&_h2]:font-bold [&_h2]:text-gray-900 [&_h3]:mt-6 [&_h3]:mb-3 [&_h3]:text-xl [&_h3]:leading-[1.3] [&_h3]:font-bold [&_h3]:text-gray-900 [&_h4]:font-bold [&_h5]:font-bold [&_h6]:font-bold [&_hr]:my-6 [&_hr]:border-gray-200 [&_img]:h-auto [&_img]:max-w-full [&_ol]:my-4 [&_ol]:list-decimal [&_ol]:pl-6 [&_p]:my-4 [&_pre]:my-4 [&_pre]:overflow-x-auto [&_pre]:rounded-lg [&_pre]:bg-gray-100 [&_pre]:p-4 [&_pre_code]:p-0 [&_summary]:cursor-pointer [&_summary]:font-medium [&_table]:w-full [&_table]:border-collapse [&_td]:border [&_td]:border-gray-200 [&_td]:px-3 [&_td]:py-2 [&_th]:border [&_th]:border-gray-200 [&_th]:bg-gray-50 [&_th]:px-3 [&_th]:py-2 [&_ul]:my-4 [&_ul]:list-disc [&_ul]:pl-6 [&>:first-child]:mt-0">
      <Markdown
        remarkPlugins={preserveBlankLines ? [remarkGfm, remarkBlankLines] : [remarkGfm]}
        rehypePlugins={[rehypeRaw, [rehypeSanitize, SANITIZE_SCHEMA], rehypeTextColor]}
        components={{
          table: ({ node, ...props }) => {
            void node;
            return (
              <div className="my-4 overflow-x-auto">
                <table {...props} />
              </div>
            );
          },
          // `![대체텍스트](주소 "캡션")`의 title을 이미지 아래 캡션으로 보여준다.
          // 이미지가 <p> 안에 그려지므로 figure 대신 block span으로 감싼다.
          img: ({ node, title, alt = '', style, ...props }) => {
            void node;
            const size = parseImageSize(alt);
            const image = (
              <img
                {...props}
                alt={size.alt}
                loading="lazy"
                style={
                  size.width
                    ? // 높이를 정하면 비율이 달라질 수 있어 잘리지 않게 맞춘다.
                      // 줄인 이미지가 문단 왼쪽에 붙지 않게 블록으로 그려 가운데에 둔다.
                      {
                        ...style,
                        width: size.width,
                        height: size.height,
                        objectFit: 'contain',
                        display: 'block',
                        marginInline: 'auto',
                      }
                    : style
                }
              />
            );
            if (!title?.trim()) return image;
            return (
              <span className="block">
                {image}
                <span className="mt-2 block text-center text-sm leading-normal text-gray-500">
                  {title.trim()}
                </span>
              </span>
            );
          },
        }}
      >
        {children}
      </Markdown>
    </div>
  );
}
