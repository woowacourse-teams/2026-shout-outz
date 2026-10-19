const SITE_ORIGIN = 'https://shout-ou.tz';
const DEFAULT_IMAGE = `${SITE_ORIGIN}/og-image.jpg`;
const DEFAULT_DESCRIPTION = '궁금한 건 편하게 묻고, 서로의 이야기에 응원을 보내 보세요.';

interface OpenGraphProps {
  title?: string;
  description?: string;
  image?: string | null;
  pathname?: string;
  article?: boolean;
}

/** 기본 메타데이터와 상세 메타데이터를 한 곳에서 렌더링해 중복 태그를 방지합니다. */
export function OpenGraph({
  title = 'shout-outz',
  description,
  image,
  pathname,
  article = false,
}: OpenGraphProps) {
  const text = description?.trim() || DEFAULT_DESCRIPTION;
  const imageUrl = image ? new URL(image, SITE_ORIGIN).href : DEFAULT_IMAGE;
  const url = pathname ? new URL(pathname, SITE_ORIGIN).href : undefined;
  return (
    <>
      <meta property="og:type" content={article ? 'article' : 'website'} />
      <meta property="og:site_name" content="shout-outz" />
      <meta property="og:title" content={title} />
      <meta property="og:description" content={text} />
      <meta property="og:image" content={imageUrl} />
      {!image && <meta property="og:image:width" content="1200" />}
      {!image && <meta property="og:image:height" content="630" />}
      <meta
        property="og:image:alt"
        content={article ? title : '메가폰으로 응원을 외치는 shout-outz 캐릭터'}
      />
      {url && <meta property="og:url" content={url} />}
      <meta name="twitter:card" content="summary_large_image" />
      <meta name="twitter:title" content={title} />
      <meta name="twitter:description" content={text} />
      <meta name="twitter:image" content={imageUrl} />
      {article && <meta name="description" content={text} />}
      {url && <link rel="canonical" href={url} />}
    </>
  );
}
