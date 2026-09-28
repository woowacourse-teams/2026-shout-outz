import Markdown from 'react-markdown';

export function FeedMarkdown({
  content,
  hideCodeBlocks = false,
}: {
  content: string;
  hideCodeBlocks?: boolean;
}) {
  return (
    <Markdown
      skipHtml
      components={{
        a: ({ href, children }) => (
          <a
            href={href}
            target="_blank"
            rel="noopener noreferrer"
            className="text-primary-600 relative z-10 underline"
          >
            {children}
          </a>
        ),
        img: () => null,
        pre: ({ children }) =>
          hideCodeBlocks ? null : (
            <pre className="overflow-x-auto rounded-lg bg-gray-50 p-3">{children}</pre>
          ),
        ul: ({ children }) => <ul className="list-inside list-disc">{children}</ul>,
        ol: ({ children }) => <ol className="list-inside list-decimal">{children}</ol>,
      }}
    >
      {content}
    </Markdown>
  );
}
