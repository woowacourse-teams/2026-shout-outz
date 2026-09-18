import { useSuspenseQuery } from '@tanstack/react-query';
import { getRouteApi } from '@tanstack/react-router';

import {
  userFeedsQueryOptions,
  userProfileQueryOptions,
  userProjectsQueryOptions,
} from '@/api/user';
import { FeedCard } from '@/components/feeds/FeedCard';
import { Footer } from '@/components/Footer';
import { Gnb } from '@/components/Gnb';
import { ProfileHeader } from '@/components/users/ProfileHeader';
import { ProfileTabs } from '@/components/users/ProfileTabs';
import { ProjectCard } from '@/components/projects/ProjectCard';
import { DEFAULT_PROFILE_TAB } from '@/constants/user';
import { type ProfileTab } from '@/types/user';

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
    navigate({ search: (previous) => ({ ...previous, tab: next }) });
  };

  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>{`${profile.displayName} | shout-outz`}</title>
      <Gnb />

      <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 px-4 pt-6 pb-12 md:gap-8 md:pt-10 md:pb-20">
        <ProfileHeader
          displayName={profile.displayName}
          cohort={profile.cohort}
          track={profile.track}
          bio={profile.bio}
          githubProfileUrl={profile.githubProfileUrl}
          blogUrl={profile.blogUrl}
        />

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

function ProjectTab({ handle }: { handle: string }) {
  const { data: projects } = useSuspenseQuery(userProjectsQueryOptions(handle));

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
                members={project.members}
              />
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function FeedTab({ handle }: { handle: string }) {
  const { data: feeds } = useSuspenseQuery(userFeedsQueryOptions(handle));

  return (
    <section aria-label="피드">
      {feeds.length === 0 ? (
        <p className="py-16 text-center text-sm text-gray-500">작성한 피드가 없습니다.</p>
      ) : (
        <ul>
          {feeds.map((feed) => (
            <li key={feed.feedId} className="min-w-0">
              <FeedCard feed={feed} />
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
