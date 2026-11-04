import { ABOUT_STYLESHEET } from '@/pages/about/stylesheet';

// v13 ZIP 랜딩의 "우리의 기술 이야기" 섹션을 그대로 옮겼다.
// AI·백엔드(추천 하나 뒤의 세 가지 고민)와 프론트엔드(페이지별 렌더링 선택) 이야기로 나뉜다.
// 카드의 "고른 이유와 한 일 보기"는 <details>로 카드마다 따로 펼친다.
export function TechTalk() {
  return (
    <div className="about-page">
      <link rel="stylesheet" href={ABOUT_STYLESHEET} precedence="about" />
      <section className="wrap ref-technical-heading">
        <p className="eyebrow blue">BUILDING SHOUT-OUTZ</p>
        <h2>
          이 경험을 만들기까지,
          <br />
          <em>우리의 기술 이야기.</em>
        </h2>
        <p>자료를 연결하고 빠르게 전달하기 위해 고민한 과정이에요.</p>
      </section>
      <section className="jd-tech wrap" id="behind">
        <p className="eyebrow blue">08 / BEHIND THE MATCH</p>
        <h2>
          추천 하나 뒤에 있는
          <br />
          <em>세 가지 고민.</em>
        </h2>
        <p className="jd-tech-intro">
          공고를 등록하는 순간부터 크루 자료를 보여주기까지,
          <br />
          필요한 정보를 어떻게 다루고 연결할지 고민했어요.
        </p>
        <div className="jd-pipeline">
          <span className="on">01 공고 붙여넣기</span>
          <i>→</i>
          <span>02 역량 추출</span>
          <i>→</i>
          <span>03 자료 검색</span>
          <i>→</i>
          <span className="on">04 판정 · 설명</span>
          <i>→</i>
          <span>05 정렬</span>
          <i>→</i>
          <span>06 노출</span>
        </div>
        <div className="jd-tech-grid">
          <article>
            <small>01 공고 붙여넣기</small>
            <div className="jd-number">6곳 중 3곳</div>
            <p className="jd-stat">JD 원문이 HTML에 온전히 있던 채용 페이지</p>
            <div className="jd-issue">
              <b>문제</b>
              <p>조사한 채용 페이지 중 절반은 HTML에 공고 본문이 온전히 없었어요.</p>
            </div>
            <div className="jd-issue">
              <b>해결</b>
              <p>
                운영자가 기업 채용 페이지의 JD를 붙여넣고, 원문은 DB와 로그에 저장하지 않도록
                설계했어요. 변경 비교를 위한 해시만 남겨요.
              </p>
            </div>
          </article>
          <article>
            <small>04 판정 · 설명</small>
            <div className="jd-number">70 → 7번</div>
            <p className="jd-stat">역량 7개 × 후보 10개 판정 시 LLM 호출 수</p>
            <div className="jd-issue">
              <b>문제</b>
              <p>
                검색 점수가 높다고 모두 관련 자료는 아니에요. 후보마다 AI를 부르면 호출이 많아져요.
              </p>
            </div>
            <div className="jd-issue">
              <b>해결</b>
              <p>
                후보를 묶어 역량당 한 번 판정하고, 같은 호출에서 ‘이런 걸 얻을 수 있어요’ 설명도
                만들도록 설계했어요.
              </p>
            </div>
          </article>
          <article>
            <small>분석 실행</small>
            <div className="jd-number">180초</div>
            <p className="jd-stat">작업 기한이 지나면 다른 Worker가 이어받아요</p>
            <div className="jd-issue">
              <b>문제</b>
              <p>분석 도중 서버가 꺼지거나 작업이 실패할 수 있어요.</p>
            </div>
            <div className="jd-issue">
              <b>해결</b>
              <p>
                분석 상태를 DB에 두고 역량별로 처리해요. 실패한 작업은 정해진 간격으로 다시
                시도하도록 설계했어요.
              </p>
            </div>
          </article>
        </div>
        <p className="jd-disclaimer">
          6곳은 2026년 10월 직접 조사한 결과이며, 70 → 7번은 설계 기준 계산값이에요. 기능은 준비
          중이에요.
        </p>
        <div className="jd-tech-links">
          <a href="https://github.com/woowacourse-teams/2026-shout-outz/pull/526">
            역량 추출 API 살펴보기 ↗
          </a>
          <a href="https://github.com/woowacourse-teams/2026-shout-outz/pull/537">
            분석 Worker 살펴보기 ↗
          </a>
        </div>
      </section>

      <section className="fe-section wrap" id="rendering" aria-labelledby="rendering-heading">
        <p className="eyebrow blue">09 / UNDER THE HOOD · FRONTEND</p>
        <h2 id="rendering-heading">
          페이지마다,
          <br />
          <em>가장 빨리 보이는 방법으로.</em>
        </h2>
        <p className="fe-intro">
          샤라웃은 React SPA 위에 SSG와 SSR을 직접 얹었어요.
          <br />
          페이지마다 내용이 얼마나 자주 바뀌는지, 얼마나 빨리 공유되는지를 보고 렌더링 방식을
          골랐어요.
        </p>
        <div className="fe-cards">
          <article className="fe-card">
            <span className="fe-badge ssg">SSG · 미리 만든 HTML</span>
            <h3>
              들어오자마자 로딩 없이
              <br />
              본문이 보여요
            </h3>
            <p className="fe-metric">FCP · 첫 화면이 보이기까지</p>
            <strong className="fe-value">54% 단축</strong>
            <div className="fe-bars">
              <div>
                <span>브라우저 렌더링</span>
                <b>4.26초</b>
                <i>
                  <em style={{ width: '100%' }}></em>
                </i>
              </div>
              <div>
                <span>SSG</span>
                <b>1.96초</b>
                <i>
                  <em className="ssg-bar" style={{ width: '46%' }}></em>
                </i>
              </div>
            </div>
            <p className="fe-note">프로젝트 상세 · 느린 4G · CPU 4배 감속 모바일 기준</p>
            <details>
              <summary>고른 이유와 한 일 보기</summary>
              <h4>대상 페이지</h4>
              <p>프로젝트 상세, 소식 상세, 매거진 상세, 소개 페이지</p>
              <h4>고른 이유</h4>
              <p>
                내용이 자주 바뀌지 않고 글을 쓰자마자 공유하는 경우도 드물어, 빌드할 때 HTML을 미리
                만들어 CDN에 올려두었어요.
              </p>
              <h4>측정 결과</h4>
              <ul>
                <li>
                  FCP 4.26초 → <strong>1.96초</strong> (54% 단축)
                </li>
                <li>
                  대표 이미지 다운로드 시작 5.23초 → <strong>0.64초</strong>
                </li>
              </ul>
              <h4>기술적으로 한 일</h4>
              <p>
                라우트에 <code>{'staticData: { prerender: true }'}</code>를 선언해 SSG 대상을
                정하고, 동적 경로는 <code>generateStaticParams</code>로 목록을 받아 생성해요. 빌드
                중 일부 페이지가 실패하면 성공·실패 내역을 정리해요.
              </p>
            </details>
          </article>
          <article className="fe-card">
            <span className="fe-badge ssr">SSR · 요청마다 서버에서</span>
            <h3>
              방금 올린 글도 공유하면
              <br />
              바로 열려요
            </h3>
            <p className="fe-metric">LCP · 본문 제목이 보이기까지</p>
            <strong className="fe-value">65% 단축</strong>
            <div className="fe-bars">
              <div>
                <span>브라우저 렌더링</span>
                <b>4.80초</b>
                <i>
                  <em style={{ width: '100%' }}></em>
                </i>
              </div>
              <div>
                <span>SSR</span>
                <b>1.66초</b>
                <i>
                  <em className="ssr-bar" style={{ width: '34.6%' }}></em>
                </i>
              </div>
            </div>
            <p className="fe-note">커뮤니티 글 상세 · 느린 4G · CPU 4배 감속 모바일 기준</p>
            <details>
              <summary>고른 이유와 한 일 보기</summary>
              <h4>대상 페이지</h4>
              <p>커뮤니티 글 상세</p>
              <h4>고른 이유</h4>
              <p>
                새 글이 계속 올라오고 쓰자마자 공유되는 특성을 고려해, 요청마다 서버에서 해당 글의
                HTML을 만들어요.
              </p>
              <h4>측정 결과</h4>
              <ul>
                <li>
                  LCP 4.80초 → <strong>1.66초</strong> (65% 단축)
                </li>
                <li>
                  FCP 4.00초 → <strong>1.62초</strong>
                </li>
                <li>
                  Lighthouse 성능 점수 65 → <strong>89</strong>
                </li>
              </ul>
              <h4>기술적으로 한 일</h4>
              <p>
                라우트에 <code>{'staticData: { ssr: true }'}</code>를 선언하고{' '}
                <code>renderToPipeableStream</code>으로 HTML을 스트리밍해요. 실패 시 미리 만든 HTML
                또는 브라우저 렌더링 셸로 대체해요. 사용자마다 다른 댓글과 추천 질문은 브라우저에서
                그려요.
              </p>
            </details>
          </article>
          <article className="fe-card">
            <span className="fe-badge chunk">청크 분리 · preload</span>
            <h3>
              페이지에 필요한 JS만 받아
              <br />더 빨리 도착해요
            </h3>
            <p className="fe-metric">첫 화면에 받는 JS · gzip</p>
            <strong className="fe-value">38~62%</strong>
            <div className="fe-bars">
              <div>
                <span>앱 전체</span>
                <b>519KB</b>
                <i>
                  <em style={{ width: '100%' }}></em>
                </i>
              </div>
              <div>
                <span>페이지별</span>
                <b>195~321KB</b>
                <i>
                  <em className="chunk-range" style={{ width: '62%' }}></em>
                  <em className="chunk-bar" style={{ width: '38%' }}></em>
                </i>
              </div>
            </div>
            <p className="fe-note">커뮤니티 목록 210KB · 글 상세 264KB · 프로젝트 상세 308KB</p>
            <details>
              <summary>고른 이유와 한 일 보기</summary>
              <h4>고른 이유</h4>
              <p>
                커뮤니티 글을 읽으러 온 사람이 관리자 페이지나 글쓰기 에디터 코드까지 받을 이유가
                없어요.
              </p>
              <h4>측정 결과 · gzip 기준</h4>
              <p>
                앱 전체 JS는 519KB, 페이지별 첫 화면에 받는 JS는{' '}
                <strong>195~321KB(전체의 38~62%)</strong>예요.
              </p>
              <h4>기술적으로 한 일</h4>
              <p>
                framework, 라이브러리, 전 페이지 공통, 일부 페이지 공유 청크로 분리하고, 첫 화면에
                필요한 청크는 preload해요. 분석 도구처럼 필요하지 않은 코드는 낮은 우선순위로
                받아요.
              </p>
            </details>
          </article>
        </div>
        <div className="filmstrip">
          <h3>링크를 연 사람이 보는 화면</h3>
          <p>
            같은 커뮤니티 글을 느린 4G 모바일 환경에서 열었을 때, 같은 시각의 화면을 나란히
            놓았어요.
          </p>
          <div className="film-scroll">
            <div className="film-grid">
              <span></span>
              <ol className="film-time">
                <li>1초</li>
                <li>2초</li>
                <li>3초</li>
                <li>4초</li>
                <li>5초</li>
                <li>6초</li>
              </ol>
              <div className="film-label">
                <strong>
                  <i className="ssr-dot"></i> SSR
                </strong>
                <span>LCP 1.66초</span>
              </div>
              <figure className="film-image">
                <img
                  src="/about-assets/assets/ssr-feed.webp"
                  width="1548"
                  height="330"
                  loading="lazy"
                  alt="SSR로 커뮤니티 글을 연 1초부터 6초까지의 화면"
                />
                <span className="film-mark ssr-mark" aria-hidden="true">
                  <b>본문 표시</b>
                </span>
              </figure>
              <div className="film-label">
                <strong>
                  <i className="csr-dot"></i> 브라우저 렌더링
                </strong>
                <span>LCP 4.80초</span>
              </div>
              <figure className="film-image">
                <img
                  src="/about-assets/assets/csr-feed.webp"
                  width="1548"
                  height="330"
                  loading="lazy"
                  alt="브라우저 렌더링으로 커뮤니티 글을 연 1초부터 6초까지의 화면"
                />
                <span className="film-mark csr-mark" aria-hidden="true">
                  <b>본문 표시</b>
                </span>
              </figure>
            </div>
          </div>
          <p className="film-caption">
            각 칸은 그 시각과 가장 가까운 순간에 찍힌 화면이고, 테두리 칸부터 본문 제목이 보여요.
            LCP는 5회 측정 중앙값이에요.
          </p>
        </div>
        <div className="fe-decisions">
          <h3>페이지별 렌더링 선택</h3>
          <p>두 가지 질문으로 페이지마다 방식을 골랐어요.</p>
          <div className="decision-flow">
            <div className="decision-row">
              <div className="decision-question">
                <small>QUESTION 01</small>
                <strong>
                  로그인한 사람만 보거나,
                  <br />
                  사람마다 다른 화면인가요?
                </strong>
                <span>
                  예 → <b>CSR</b>
                  {'\u3000'} 아니오 → <b>다음 질문</b>
                </span>
              </div>
              <div className="decision-arrow yes-arrow" aria-hidden="true">
                <span>예</span>→
              </div>
              <div className="decision-result csr-result">
                <b>CSR</b> <span>브라우저 렌더링</span>
                <p>글쓰기·수정, 관리자, 마이페이지, 회원가입, 목록</p>
                <small>서버에서 미리 그릴 필요가 없어요.</small>
              </div>
            </div>
            <div className="decision-down" aria-hidden="true">
              <span>아니오</span> ↓
            </div>
            <div className="decision-row">
              <div className="decision-question">
                <small>QUESTION 02</small>
                <strong>
                  새 글이 계속 올라오고,
                  <br />
                  쓰자마자 링크로 공유되나요?
                </strong>
                <span>
                  예 → <b>SSR</b>
                  {'\u3000'} 아니오 → <b>SSG</b>
                </span>
              </div>
              <div className="decision-branches" aria-hidden="true">
                <span>예 →</span>
                <span>아니오 →</span>
              </div>
              <div className="decision-outcomes">
                <div className="decision-result ssr-result">
                  <b>SSR</b> <span>요청마다 서버에서</span>
                  <p>커뮤니티 글 상세</p>
                  <small>빌드 때는 없던 글도 요청이 오면 서버에서 바로 그려요.</small>
                </div>
                <div className="decision-result ssg-result">
                  <b>SSG</b> <span>미리 만든 HTML</span>
                  <p>프로젝트 상세, 소식 상세, 매거진 상세, 소개</p>
                  <small>자주 바뀌지 않는 내용은 미리 만들어 보여줘요.</small>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div className="fe-fallback">
          <h3>실패해도 다음 방법으로</h3>
          <p>커뮤니티 글 상세는 한 단계가 실패하면 다음 단계로 넘어가 응답해요.</p>
          <ol>
            <li>
              <span>1</span>
              <div>
                <strong>
                  서버에서 그리기 <small>SSR</small>
                </strong>
                <p>요청마다 서버에서 그 글의 HTML을 만들어요.</p>
                <b>실패하면 다음 단계로 →</b>
              </div>
            </li>
            <li>
              <span>2</span>
              <div>
                <strong>
                  미리 만든 HTML <small>SSG</small>
                </strong>
                <p>SSR이 실패하면 빌드 때 만든 HTML로 응답해요.</p>
                <b>실패하면 다음 단계로 →</b>
              </div>
            </li>
            <li>
              <span>3</span>
              <div>
                <strong>
                  브라우저 렌더링 셸 <small>CSR</small>
                </strong>
                <p>그것도 없으면 브라우저에서 그리는 셸로 응답해요.</p>
              </div>
            </li>
          </ol>
        </div>
        <p className="fe-disclaimer">
          측정 조건: 로컬 빌드, Lighthouse 12.8.2 모바일 프로필, 느린 4G 네트워크와 CPU 4배 감속,
          gzip 압축, 방식별 5회 측정 중앙값. 화면 캡처는 그중 한 번의 측정에서 찍힌 장면이고, 위쪽
          노란 띠는 로컬 개발 환경 안내 배너예요.
        </p>
      </section>
    </div>
  );
}
