import type { ReactNode } from 'react'

type MarkdownBlock =
  | { type: 'paragraph'; lines: string[] }
  | { type: 'heading'; level: 1 | 2 | 3; text: string }
  | { type: 'unordered-list' | 'ordered-list'; items: string[] }
  | { type: 'blockquote'; lines: string[] }
  | { type: 'code'; value: string }
  | { type: 'rule' }

const INLINE_PATTERN = /(\n|\[[^\]\n]+\]\([^\)]+\)|`[^`\n]+`|\*\*[^*\n]+\*\*|__[^_\n]+__|\*[^*\n]+\*|_[^_\n]+_)/g

function flushParagraph(blocks: MarkdownBlock[], lines: string[]) {
  if (lines.length === 0) return
  blocks.push({ type: 'paragraph', lines: [...lines] })
  lines.length = 0
}

function parseMarkdown(source: string): MarkdownBlock[] {
  const lines = source.replace(/\r\n?/g, '\n').split('\n')
  const blocks: MarkdownBlock[] = []
  const paragraph: string[] = []
  let index = 0

  while (index < lines.length) {
    const line = lines[index]
    const trimmed = line.trim()

    if (!trimmed) {
      flushParagraph(blocks, paragraph)
      index += 1
      continue
    }

    const fence = trimmed.match(/^```(?:\s*[\w-]+)?\s*$/)
    if (fence) {
      flushParagraph(blocks, paragraph)
      const codeLines: string[] = []
      index += 1
      while (index < lines.length && !/^\s*```\s*$/.test(lines[index])) {
        codeLines.push(lines[index])
        index += 1
      }
      if (index < lines.length) index += 1
      blocks.push({ type: 'code', value: codeLines.join('\n') })
      continue
    }

    const heading = trimmed.match(/^(#{1,3})\s+(.+?)\s*#*$/)
    if (heading) {
      flushParagraph(blocks, paragraph)
      blocks.push({ type: 'heading', level: heading[1].length as 1 | 2 | 3, text: heading[2] })
      index += 1
      continue
    }

    if (/^(?:-{3,}|\*{3,}|_{3,})$/.test(trimmed)) {
      flushParagraph(blocks, paragraph)
      blocks.push({ type: 'rule' })
      index += 1
      continue
    }

    const unorderedItem = trimmed.match(/^[-*+]\s+(.+)$/)
    if (unorderedItem) {
      flushParagraph(blocks, paragraph)
      const items: string[] = []
      while (index < lines.length) {
        const item = lines[index].trim().match(/^[-*+]\s+(.+)$/)
        if (!item) break
        items.push(item[1])
        index += 1
      }
      blocks.push({ type: 'unordered-list', items })
      continue
    }

    const orderedItem = trimmed.match(/^\d+[.)]\s+(.+)$/)
    if (orderedItem) {
      flushParagraph(blocks, paragraph)
      const items: string[] = []
      while (index < lines.length) {
        const item = lines[index].trim().match(/^\d+[.)]\s+(.+)$/)
        if (!item) break
        items.push(item[1])
        index += 1
      }
      blocks.push({ type: 'ordered-list', items })
      continue
    }

    const quoteLine = trimmed.match(/^>\s?(.*)$/)
    if (quoteLine) {
      flushParagraph(blocks, paragraph)
      const quoteLines: string[] = []
      while (index < lines.length) {
        const quote = lines[index].trim().match(/^>\s?(.*)$/)
        if (!quote) break
        quoteLines.push(quote[1])
        index += 1
      }
      blocks.push({ type: 'blockquote', lines: quoteLines })
      continue
    }

    paragraph.push(line.trimEnd())
    index += 1
  }

  flushParagraph(blocks, paragraph)
  return blocks
}

function safeHref(value: string) {
  const href = value.trim()
  if (href.startsWith('/') || href.startsWith('#')) return href

  try {
    const url = new URL(href)
    return ['http:', 'https:', 'mailto:'].includes(url.protocol) ? href : null
  } catch {
    return null
  }
}

function renderInline(value: string, keyPrefix: string): ReactNode[] {
  return value.split(INLINE_PATTERN).map((part, index) => {
    if (!part) return null
    const key = `${keyPrefix}-${index}`

    if (part === '\n') return <br key={key} />
    if (part.startsWith('`') && part.endsWith('`')) return <code key={key}>{part.slice(1, -1)}</code>
    if (part.startsWith('**') && part.endsWith('**')) return <strong key={key}>{renderInline(part.slice(2, -2), key)}</strong>
    if (part.startsWith('__') && part.endsWith('__')) return <strong key={key}>{renderInline(part.slice(2, -2), key)}</strong>
    if (part.startsWith('*') && part.endsWith('*')) return <em key={key}>{renderInline(part.slice(1, -1), key)}</em>
    if (part.startsWith('_') && part.endsWith('_')) return <em key={key}>{renderInline(part.slice(1, -1), key)}</em>

    const link = part.match(/^\[([^\]]+)\]\(([^)]+)\)$/)
    if (link) {
      const href = safeHref(link[2])
      if (!href) return link[1]
      return <a key={key} href={href} target="_blank" rel="noreferrer">{renderInline(link[1], key)}</a>
    }

    return part
  }).filter(Boolean)
}

export function MarkdownContent({ content }: { content: string }) {
  const blocks = parseMarkdown(content)

  return (
    <div className="markdown-content">
      {blocks.map((block, index) => {
        const key = `markdown-${index}`

        if (block.type === 'heading') {
          const Heading = `h${block.level}` as 'h1' | 'h2' | 'h3'
          return <Heading key={key}>{renderInline(block.text, key)}</Heading>
        }
        if (block.type === 'paragraph') return <p key={key}>{renderInline(block.lines.join('\n'), key)}</p>
        if (block.type === 'unordered-list') return <ul key={key}>{block.items.map((item, itemIndex) => <li key={`${key}-${itemIndex}`}>{renderInline(item, `${key}-${itemIndex}`)}</li>)}</ul>
        if (block.type === 'ordered-list') return <ol key={key}>{block.items.map((item, itemIndex) => <li key={`${key}-${itemIndex}`}>{renderInline(item, `${key}-${itemIndex}`)}</li>)}</ol>
        if (block.type === 'blockquote') return <blockquote key={key}>{renderInline(block.lines.join('\n'), key)}</blockquote>
        if (block.type === 'code') return <pre key={key}><code>{block.value}</code></pre>
        return <hr key={key} />
      })}
    </div>
  )
}
