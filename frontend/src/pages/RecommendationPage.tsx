import { IconArrowLeft, IconPencil, IconSparkles } from '@tabler/icons-react';
import { AppGnb } from '@/components/AppGnb';
import { Footer } from '@/components/Footer';
import { Button } from '@/components/Button';
import { FeedMarkdown } from '@/components/feeds/FeedMarkdown';
import type { QuestionRecommendation } from '@/types/recommendation';

interface RecommendationPageProps {
  recommendation?: QuestionRecommendation;
  onBack: () => void;
  onCreate: () => void;
}

export function RecommendationPage({ recommendation, onBack, onCreate }: RecommendationPageProps) {
  return (
    <div className="bg-background flex min-h-dvh flex-col">
      <title>
        {recommendation
          ? `${recommendation.title} · 추천 질문 | shout-outz`
          : '추천 질문 | shout-outz'}
      </title>
      <meta name="robots" content="noindex" />
      <AppGnb />
      <main className="mx-auto w-full max-w-4xl flex-1 px-5 pt-7 pb-10 md:px-12 md:pt-8 md:pb-12">
        <Button
          variant="ghost"
          size="sm"
          className="mb-6 gap-2 px-0 text-gray-500"
          onClick={onBack}
        >
          <IconArrowLeft className="size-4" aria-hidden="true" />
          {recommendation ? '돌아가기' : '질문 목록으로'}
        </Button>
        {recommendation ? (
          <>
            <aside
              aria-label="미등록 추천 질문 안내"
              className="border-primary-100 bg-primary-50 mb-8 rounded-xl border p-5 md:p-6"
            >
              <div className="text-primary-700 flex items-center gap-2 text-sm font-bold">
                <IconSparkles className="size-5 shrink-0" aria-hidden="true" />
                아직 등록되지 않은 AI 추천 질문이에요
              </div>
              <p className="mt-2 text-sm leading-6 text-gray-600">
                이 내용이 궁금하다면 샤라웃에 질문을 등록하고 함께 이야기해 보세요. 제목과 내용을
                자유롭게 다듬을 수 있어요.
              </p>
              <Button className="mt-4 w-full gap-2 sm:w-auto" onClick={onCreate}>
                <IconPencil className="size-4" aria-hidden="true" />이 내용으로 질문 작성하기
              </Button>
            </aside>
            <article aria-label="추천 질문 미리보기" className="min-w-0">
              <p className="text-primary-600 text-xs font-medium">AI 추천 질문 · 미등록</p>
              <h1 className="mt-3 text-2xl leading-snug font-bold tracking-tight break-words text-gray-900 md:text-3xl">
                <span className="text-primary-600 mr-2">Q.</span>
                {recommendation.title}
              </h1>
              <div className="mt-7 space-y-6 border-t border-gray-100 pt-8 text-sm leading-6 break-words text-gray-600">
                <FeedMarkdown content={recommendation.content} />
              </div>
            </article>
            <div className="mt-8 border-t border-gray-100 pt-4">
              <Button
                variant="ghost"
                size="sm"
                className="gap-2 px-0 text-gray-500"
                onClick={onBack}
              >
                <IconArrowLeft className="size-4" aria-hidden="true" />
                돌아가기
              </Button>
            </div>
          </>
        ) : (
          <section className="rounded-xl border border-gray-200 px-6 py-16 text-center">
            <h1 className="text-xl font-bold text-gray-900">추천 목록에서 질문을 선택해 주세요</h1>
            <p className="mt-3 text-sm leading-6 text-gray-600">
              이 주소만으로는 추천 질문을 불러올 수 없어요.
              <br />
              질문 목록에서 궁금한 추천을 골라보세요.
            </p>
            <Button className="mt-6" onClick={onBack}>
              질문 목록으로 이동
            </Button>
          </section>
        )}
      </main>
      <Footer />
    </div>
  );
}
