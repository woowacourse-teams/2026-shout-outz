import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';
import rehypeSanitize from 'rehype-sanitize';

export function MarkdownContent({ children }: { children: string }) {
  if (!children.trim()) return <p className="text-gray-500">등록된 프로젝트 소개가 없습니다.</p>;

  return (
    <div className="[&_a]:text-primary-600 min-w-0 text-sm leading-7 break-words text-gray-600 [&_a]:underline [&_blockquote]:my-4 [&_blockquote]:border-l-4 [&_blockquote]:border-gray-200 [&_blockquote]:pl-4 [&_code]:rounded-sm [&_code]:bg-gray-100 [&_code]:px-1 [&_details]:my-4 [&_h1]:mt-8 [&_h1]:mb-4 [&_h1]:text-2xl [&_h1]:font-bold [&_h1]:text-gray-900 [&_h2]:mt-8 [&_h2]:mb-3 [&_h2]:text-xl [&_h2]:font-bold [&_h2]:text-gray-900 [&_h3]:mt-6 [&_h3]:mb-3 [&_h3]:text-lg [&_h3]:font-bold [&_h3]:text-gray-900 [&_h4]:font-bold [&_h5]:font-bold [&_h6]:font-bold [&_hr]:my-6 [&_hr]:border-gray-200 [&_img]:h-auto [&_img]:max-w-full [&_ol]:my-4 [&_ol]:list-decimal [&_ol]:pl-6 [&_p]:my-4 [&_pre]:my-4 [&_pre]:overflow-x-auto [&_pre]:rounded-lg [&_pre]:bg-gray-100 [&_pre]:p-4 [&_pre_code]:p-0 [&_summary]:cursor-pointer [&_summary]:font-medium [&_table]:w-full [&_table]:border-collapse [&_td]:border [&_td]:border-gray-200 [&_td]:px-3 [&_td]:py-2 [&_th]:border [&_th]:border-gray-200 [&_th]:bg-gray-50 [&_th]:px-3 [&_th]:py-2 [&_ul]:my-4 [&_ul]:list-disc [&_ul]:pl-6 [&>:first-child]:mt-0">
      <Markdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeRaw, rehypeSanitize]}
        components={{
          table: ({ node, ...props }) => {
            void node;
            return (
              <div className="my-4 overflow-x-auto">
                <table {...props} />
              </div>
            );
          },
          img: ({ node, ...props }) => {
            void node;
            return <img {...props} loading="lazy" />;
          },
        }}
      >
        {children}
      </Markdown>
    </div>
  );
}
