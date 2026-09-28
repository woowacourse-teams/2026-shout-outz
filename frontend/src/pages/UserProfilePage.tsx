import {
  useMutation,
  useQuery,
  useQueryClient,
  useSuspenseInfiniteQuery,
  useSuspenseQuery,
} from '@tanstack/react-query';
import { getRouteApi, Link } from '@tanstack/react-router';

import {
  userFeedsInfiniteQueryOptions,
  userProfileQueryOptions,
  userProjectsInfiniteQueryOptions,
} from '@/api/user';
import { FeedCard } from '@/components/feeds/FeedCard';
import { Footer } from '@/components/Footer';
import { AppGnb } from '@/components/AppGnb';
import { ProfileHeader } from '@/components/users/ProfileHeader';
import { ProfileTabs } from '@/components/users/ProfileTabs';
import { ProjectCard } from '@/components/projects/ProjectCard';
import { DEFAULT_PROFILE_TAB } from '@/constants/user';
import { type ProfileTab } from '@/types/user';
import { sessionQuery } from '@/apis/session';
import { myProfileSummaryQuery, updateMyProfileMutation } from '@/apis/user';
import { AvatarUploadButton } from '@/components/users/AvatarUploadButton';
import { type UserProfile } from '@/types/user';
import { Button, getButtonStyles } from '@/components/Button';
import { analytics } from '@/utils/analytics';

const route = getRouteApi('/users/$handle');

/**
 * 공개 프로필 페이지.
 *
 * 상단 프로필과 탭(프로젝트·피드)으로 구성한다.
 *
 * handle(path param)과 탭(search param)은 이 안에서 직접 읽고, 탭을 바꿀 때도 여기서 URL을 갱신한다.
 * 라우트는 Suspense와 errorComponent 경계만 잡는다.
 *
 * 각 탭 목록은 첫 페이지만 조회한다. 다음 커서 처리는 나중에 더한다.
 */
export function UserProfilePage() {
  const { handle } = route.useParams();
  const { tab } = route.useSearch();
  const navigate = route.useNavigate();

  const currentTab = tab ?? DEFAULT_PROFILE_TAB;
  const { data: profile } = useSuspenseQuery(userProfileQueryOptions(handle));

  const changeTab = (next: ProfileTab) => {
    analytics.track({ name: 'profile_tab_changed', tab: next });
    navigate({ search: (previous) => ({ ...previous, tab: next }) });
  };

  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>{`${profile.displayName} | shout-outz`}</title>
      <AppGnb />

      <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 px-4 pt-6 pb-12 md:gap-8 md:pt-10 md:pb-20">
        <ProfileHeader
          displayName={profile.displayName}
          cohort={profile.cohort}
          track={profile.track}
          bio={profile.bio}
          githubProfileUrl={profile.githubProfileUrl}
          blogUrl={profile.blogUrl}
          avatarUrl={profile.avatarUrl}
        />

        <MyProfileActions profile={profile} />

        <ProfileTabs
          value={currentTab}
          projectCount={profile.counts.projects}
          feedCount={profile.counts.feeds}
          onChange={changeTab}
        />

        {currentTab === 'projects' ? <ProjectTab handle={handle} /> : <FeedTab handle={handle} />}
      </main>

      <Footer />
    </div>
  );
}

/**
 * 내 프로필에서만 보이는 줄.
 *
 * 프로필 사진은 여기서 바로 바꾼다. 수정 화면을 따로 두지 않고, 고르는 즉시 올려 저장한다.
 * `PUT /api/v1/users/me`는 전체 교체라 지금 값을 그대로 같이 실어 보낸다.
 */
function MyProfileActions({ profile }: { profile: UserProfile }) {
  const client = useQueryClient();
  const session = useQuery({ ...sessionQuery, enabled: typeof window !== 'undefined' });
  const authenticated = session.data?.status === 'AUTHENTICATED' && session.data.userId !== null;
  const me = useQuery({
    ...myProfileSummaryQuery(session.data?.userId ?? 0),
    enabled: authenticated,
  });
  const update = useMutation({
    ...updateMyProfileMutation,
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ['users', profile.handle] }),
        client.invalidateQueries({ queryKey: ['my-profile-summary'] }),
        client.invalidateQueries({ queryKey: ['my-profile'] }),
      ]);
    },
  });

  if (me.data?.handle !== profile.handle) return null;

  const saveAvatar = (avatarImageId: number | null) =>
    update.mutate({
      displayName: profile.displayName,
      bio: profile.bio,
      blogUrl: profile.blogUrl,
      githubProfileUrl: profile.githubProfileUrl,
      avatarImageId,
    });

  return (
    <div className="flex flex-wrap items-center gap-2">
      <AvatarUploadButton
        label={profile.avatarUrl ? '사진 변경' : '프로필 사진 추가'}
        disabled={update.isPending}
        onUploaded={saveAvatar}
      />
      {profile.avatarUrl && (
        <Button
          variant="ghost"
          size="sm"
          disabled={update.isPending}
          onClick={() => saveAvatar(null)}
        >
          기본 이미지로
        </Button>
      )}
      {/* 같은 줄의 사진 버튼들과 높이를 맞춘다. getButtonStyles의 기본 size는 md다. */}
      <Link
        to="/mypage/verification"
        className={getButtonStyles({ variant: 'outline', size: 'sm' })}
      >
        구성원 인증
      </Link>
      {update.isPending && <span className="text-xs text-gray-500">저장 중…</span>}
      {update.isError && (
        <span role="alert" className="text-xs text-red-600">
          프로필 사진을 저장하지 못했습니다.
        </span>
      )}
    </div>
  );
}

function ProjectTab({ handle }: { handle: string }) {
  const query = useSuspenseInfiniteQuery(userProjectsInfiniteQueryOptions(handle));
  const projects = query.data.pages.flatMap((page) => page.data);

  return (
    <section aria-label="프로젝트">
      {projects.length === 0 ? (
        <p className="py-16 text-center text-sm text-gray-500">등록한 프로젝트가 없습니다.</p>
      ) : (
        <ul className="grid grid-cols-1 gap-x-5 gap-y-8 md:grid-cols-2">
          {projects.map((project) => (
            <li key={project.id} className="min-w-0">
              <ProjectCard
                title={project.title}
                tagline={project.tagline}
                cohort={project.cohort}
                likeCount={project.likeCount}
                commentCount={project.commentCount}
                techTags={project.techTags}
                thumbnailUrl={project.thumbnailUrl}
                members={project.members}
              />
            </li>
          ))}
        </ul>
      )}
      {query.hasNextPage && (
        <button
          type="button"
          disabled={query.isFetchingNextPage}
          onClick={() => void query.fetchNextPage()}
        >
          {query.isFetchingNextPage ? '불러오는 중…' : '프로젝트 더 보기'}
        </button>
      )}
    </section>
  );
}

function FeedTab({ handle }: { handle: string }) {
  const query = useSuspenseInfiniteQuery(userFeedsInfiniteQueryOptions(handle));
  const feeds = query.data.pages.flatMap((page) => page.data);

  return (
    <section aria-label="피드">
      {feeds.length === 0 ? (
        <p className="py-16 text-center text-sm text-gray-500">작성한 피드가 없습니다.</p>
      ) : (
        <ul>
          {feeds.map((feed) => (
            <li key={feed.feedId} className="min-w-0">
              <FeedCard feed={feed} surface="profile" />
            </li>
          ))}
        </ul>
      )}
      {query.hasNextPage && (
        <button
          type="button"
          disabled={query.isFetchingNextPage}
          onClick={() => void query.fetchNextPage()}
        >
          {query.isFetchingNextPage ? '불러오는 중…' : '피드 더 보기'}
        </button>
      )}
    </section>
  );
}
