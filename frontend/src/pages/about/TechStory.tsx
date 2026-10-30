import { useState } from 'react';

// v13 랜딩의 "기록에서 다음 기회까지" 네 장면 시각화. 마크업과 스타일(technology.*.css)은 v13을 그대로 옮겼다.
// 화면 속 프로젝트와 문장은 개발 중인 연결을 설명하는 가상 예시다.
const STYLESHEET = '/about-assets/technology.ec78a2301d.css';

type Requirement = 'concurrency' | 'performance';

const MATCHES: Record<Requirement, [string, string]> = {
  concurrency: ['중복 주문을 막은\n잠금 전략', '트랜잭션과 잠금을 선택한\n이유를 배울 수 있어요.'],
  performance: ['반복 조회를 줄인\n쿼리 개선', 'N+1 문제를 찾고\n조회 범위를 줄인 과정을 배워요.'],
};

export function TechStory() {
  const [requirement, setRequirement] = useState<Requirement>('concurrency');
  // 고를 때마다 결과 카드를 다시 붙여 연결 애니메이션을 처음부터 재생한다.
  const [matchCount, setMatchCount] = useState(0);
  const match = MATCHES[requirement];
  const select = (next: Requirement) => {
    setRequirement(next);
    setMatchCount((count) => count + 1);
  };

  return (
    <div className="about-page">
      <link rel="stylesheet" href={STYLESHEET} precedence="about" />
      <div className="technology-scope">
        <svg className="svg-defs" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
          <defs>
            <symbol id="i-arrow" viewBox="0 0 24 24">
              <path d="M5 12h14m-6-6 6 6-6 6" />
            </symbol>
            <symbol id="i-up" viewBox="0 0 24 24">
              <path d="M6 18 18 6M6 6h12v12" />
            </symbol>
            <symbol id="i-down" viewBox="0 0 24 24">
              <path d="M12 4v16m-6-6 6 6 6-6" />
            </symbol>
            <symbol id="i-file" viewBox="0 0 24 24">
              <path d="M14 3H5v18h14V8l-5-5Z" />
              <path d="M14 3v6h5M8 13h8m-8 4h6" />
            </symbol>
            <symbol id="i-branch" viewBox="0 0 24 24">
              <circle cx="6" cy="5" r="2" />
              <circle cx="18" cy="5" r="2" />
              <circle cx="6" cy="19" r="2" />
              <path d="M6 7v10m12-10c0 6-12 2-12 8" />
            </symbol>
            <symbol id="i-github" viewBox="0 0 24 24">
              <path d="M9 19c-4 1-4-2-6-2m12 5v-4c0-1 .1-2-1-3 4 0 7-2 7-6 0-2-.6-3-2-4 0-1 0-3-1-3-2 0-3 1-4 2a14 14 0 0 0-4 0C9 3 7 2 6 2c-1 0-1 2-1 3C3.6 6 3 7 3 9c0 4 3 6 7 6-1 1-1 2-1 3v4" />
            </symbol>
            <symbol id="i-spark" viewBox="0 0 24 24">
              <path d="m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5L12 3Zm8-1v4m-2-2h4" />
            </symbol>
            <symbol id="i-db" viewBox="0 0 24 24">
              <ellipse cx="12" cy="5" rx="8" ry="3" />
              <path d="M4 5v14c0 4 16 4 16 0V5M4 12c0 4 16 4 16 0" />
            </symbol>
            <symbol id="i-search" viewBox="0 0 24 24">
              <circle cx="10.5" cy="10.5" r="6.5" />
              <path d="m16 16 5 5" />
            </symbol>
            <symbol id="i-check" viewBox="0 0 24 24">
              <path d="m5 12 4 4L19 6" />
            </symbol>
            <symbol id="i-grid" viewBox="0 0 24 24">
              <rect x="3" y="3" width="6" height="6" rx="1" />
              <rect x="15" y="3" width="6" height="6" rx="1" />
              <rect x="3" y="15" width="6" height="6" rx="1" />
              <rect x="15" y="15" width="6" height="6" rx="1" />
              <path d="M12 4v16M4 12h16" />
            </symbol>
            <symbol id="i-chat" viewBox="0 0 24 24">
              <path d="M21 15a3 3 0 0 1-3 3H8l-5 4V6a3 3 0 0 1 3-3h12a3 3 0 0 1 3 3Z" />
              <path d="M7 8h10M7 12h6" />
            </symbol>
          </defs>
        </svg>
        <section className="technology" id="technology">
          <div className="container">
            <div className="tech-title">
              <div>
                <p className="eyebrow">경험을 연결하는 기술</p>
                <h2>
                  기록에서 <span>다음 기회</span>까지.
                </h2>
              </div>
              <div className="tech-character" aria-hidden="true">
                <span className="drawn-star">✳</span>
                <img src="/about-assets/character-small.webp" alt="" width="208" height="172" />
                <svg className="character-doodle" viewBox="0 0 80 40">
                  <path d="M3 30C25 37 28 3 55 10M46 3l13 7-12 8" />
                </svg>
              </div>
            </div>
            <div className="flow-player" id="flow-player" data-current="0">
              <div className="scene-area">
                <div className="scene-step">
                  <span>01</span> 기록 수집
                </div>
                <section id="scene-0" className="scene">
                  <div className="scene-heading">
                    <h3>경험은 이미, 기록 속에.</h3>
                    <p>흩어진 프로젝트 문서를 모읍니다.</p>
                  </div>
                  <div className="scene-visual collect-visual">
                    <div className="source-stack">
                      <div className="source-card">
                        <svg>
                          <use href="#i-github" />
                        </svg>
                        <b>저장소</b>
                        <span className="paper-lines" aria-hidden="true"></span>
                      </div>
                      <div className="source-card">
                        <svg>
                          <use href="#i-branch" />
                        </svg>
                        <b>PR</b>
                        <span className="paper-lines" aria-hidden="true"></span>
                      </div>
                      <div className="source-card">
                        <svg>
                          <use href="#i-file" />
                        </svg>
                        <b>기술 블로그</b>
                        <span className="paper-lines" aria-hidden="true"></span>
                      </div>
                    </div>
                    <div className="data-wire" aria-hidden="true">
                      <i></i>
                      <i></i>
                      <i></i>
                      <svg>
                        <use href="#i-arrow" />
                      </svg>
                    </div>
                    <div className="collector">
                      <span className="mini-page page-back" aria-hidden="true"></span>
                      <span className="mini-page page-mid" aria-hidden="true"></span>
                      <div className="collector-front">
                        <span className="orb">
                          <svg>
                            <use href="#i-file" />
                          </svg>
                        </span>
                        <b>모인 프로젝트 기록</b>
                        <small>문서와 원문 링크</small>
                        <span className="ready-stamp">
                          <svg>
                            <use href="#i-check" />
                          </svg>{' '}
                          수집과 정제
                        </span>
                      </div>
                    </div>
                  </div>
                </section>
              </div>
              <div className="scene-area">
                <div className="scene-step">
                  <span>02</span> 경험 추출
                </div>
                <section id="scene-1" className="scene">
                  <div className="scene-heading">
                    <h3>문제와 해결이, 하나의 경험으로.</h3>
                    <p>LLM으로 추출하고, 벡터로 바꿔 저장합니다.</p>
                  </div>
                  <div className="scene-visual extract-visual">
                    <div className="paper-card">
                      <span className="card-label">
                        <svg>
                          <use href="#i-branch" />
                        </svg>{' '}
                        프로젝트 PR
                      </span>
                      <p>
                        “주문이 몰리면
                        <br />
                        <em>재고가 맞지 않았어요.</em>
                      </p>
                      <p>
                        잠금을 적용해
                        <br />
                        <em>중복 주문을 막았어요.”</em>
                      </p>
                    </div>
                    <div className="transform">
                      <span className="orb">
                        <svg>
                          <use href="#i-spark" />
                        </svg>
                      </span>
                      <b>LLM</b>
                      <svg className="transform-arrow">
                        <use href="#i-arrow" />
                      </svg>
                    </div>
                    <div className="experience-card">
                      <span className="experience-spark" aria-hidden="true">
                        ✦
                      </span>
                      <div>
                        <span>문제</span>
                        <b>동시 주문에 재고 불일치</b>
                      </div>
                      <div>
                        <span>해결</span>
                        <b>트랜잭션과 잠금 적용</b>
                      </div>
                      <div>
                        <span>결과</span>
                        <b>중복 주문 방지 확인</b>
                      </div>
                      <p className="stored-caption">
                        <svg>
                          <use href="#i-db" />
                        </svg>{' '}
                        PostgreSQL + pgvector
                      </p>
                    </div>
                  </div>
                </section>
              </div>
              <div className="scene-area">
                <div className="scene-step">
                  <span>03</span> 경험 검색
                </div>
                <section id="scene-2" className="scene">
                  <div className="scene-heading">
                    <h3>찾고 싶은 경험을, 내 말로.</h3>
                    <p>질문과 경험을 벡터로 비교해 관련 문서를 찾습니다.</p>
                  </div>
                  <div className="scene-visual search-visual">
                    <div className="query-card">
                      <span className="card-label">이렇게 물어보면</span>
                      <div>
                        <svg>
                          <use href="#i-search" />
                        </svg>
                        <p>
                          동시 주문을
                          <br />
                          <b>안전하게 처리한 경험</b>
                        </p>
                      </div>
                    </div>
                    <div className="vector-engine transform">
                      <svg className="vector-field" viewBox="0 0 180 180" aria-hidden="true">
                        <circle className="space-ring" cx="90" cy="90" r="73" />
                        <circle className="space-ring" cx="90" cy="90" r="46" />
                        <path
                          className="vector-route"
                          d="M2 108C28 108 38 123 62 105L113 51Q141 31 177 58"
                        />
                        <circle className="quiet-point" cx="42" cy="43" r="5" />
                        <circle className="quiet-point" cx="66" cy="35" r="4" />
                        <circle className="quiet-point" cx="34" cy="67" r="4" />
                        <circle className="quiet-point" cx="135" cy="107" r="5" />
                        <circle className="quiet-point" cx="146" cy="130" r="4" />
                        <circle className="quiet-point" cx="111" cy="144" r="4" />
                        <circle className="quiet-point" cx="48" cy="143" r="4" />
                        <circle className="quiet-point" cx="27" cy="119" r="3" />
                        <circle className="query-wave" cx="62" cy="105" r="17" />
                        <circle className="query-point" cx="62" cy="105" r="10" />
                        <circle className="hit-wave" cx="113" cy="51" r="16" />
                        <circle className="hit-point" cx="113" cy="51" r="8" />
                        <path className="hit-tick" d="m109 51 3 3 5-6" />
                      </svg>
                      <b>pgvector</b>
                      <small>의미로 검색</small>
                    </div>
                    <div className="result-card">
                      <span className="result-seal" aria-hidden="true">
                        <svg>
                          <use href="#i-check" />
                        </svg>
                      </span>
                      <span className="card-label">이 경험으로 연결돼요</span>
                      <div className="sample-project">
                        <span>C</span>
                        <b>샘플 커머스</b>
                      </div>
                      <h4>
                        중복 주문을 막은
                        <br />
                        잠금 전략
                      </h4>
                      <span className="evidence-tag">
                        <svg>
                          <use href="#i-branch" />
                        </svg>{' '}
                        관련 경험 + 원문 링크
                      </span>
                    </div>
                  </div>
                  <div className="search-next">
                    <span>
                      지금 <b>벡터 검색</b>
                    </span>
                    <svg>
                      <use href="#i-arrow" />
                    </svg>
                    <span>
                      다음 <b>키워드 결합</b>
                    </span>
                    <svg>
                      <use href="#i-arrow" />
                    </svg>
                    <span>
                      실험 <b>후보 재정렬</b>
                    </span>
                  </div>
                </section>
              </div>
              <div className="scene-area">
                <div className="scene-step">
                  <span>04</span> JD 연결
                </div>
                <section id="scene-3" className="scene">
                  <div className="scene-heading">
                    <h3>공고의 한 줄이, 배울 경험으로.</h3>
                    <p>JD에서 뽑은 요구사항도 같은 검색 엔진에 연결합니다.</p>
                  </div>
                  <div className={`scene-visual jd-visual${matchCount ? 'is-matching' : ''}`}>
                    <div className="jd-card">
                      <span className="card-label">
                        <svg>
                          <use href="#i-file" />
                        </svg>{' '}
                        백엔드 채용공고
                      </span>
                      <b className="jd-card-title">이런 역량을 원해요</b>
                      <div className="requirements" aria-label="공고 요구사항">
                        <button
                          type="button"
                          aria-pressed={requirement === 'concurrency'}
                          onClick={() => select('concurrency')}
                        >
                          동시성 제어{' '}
                          <svg>
                            <use href="#i-arrow" />
                          </svg>
                        </button>
                        <button
                          type="button"
                          aria-pressed={requirement === 'performance'}
                          onClick={() => select('performance')}
                        >
                          조회 성능 개선{' '}
                          <svg>
                            <use href="#i-arrow" />
                          </svg>
                        </button>
                      </div>
                    </div>
                    <div className="vector-engine transform">
                      <svg className="vector-field" viewBox="0 0 180 180" aria-hidden="true">
                        <circle className="space-ring" cx="90" cy="90" r="73" />
                        <circle className="space-ring" cx="90" cy="90" r="46" />
                        <path
                          className="vector-route"
                          d="M2 108C28 108 38 123 62 105L113 51Q141 31 177 58"
                        />
                        <circle className="quiet-point" cx="42" cy="43" r="5" />
                        <circle className="quiet-point" cx="66" cy="35" r="4" />
                        <circle className="quiet-point" cx="34" cy="67" r="4" />
                        <circle className="quiet-point" cx="135" cy="107" r="5" />
                        <circle className="quiet-point" cx="146" cy="130" r="4" />
                        <circle className="quiet-point" cx="111" cy="144" r="4" />
                        <circle className="quiet-point" cx="48" cy="143" r="4" />
                        <circle className="quiet-point" cx="27" cy="119" r="3" />
                        <circle className="query-wave" cx="62" cy="105" r="17" />
                        <circle className="query-point" cx="62" cy="105" r="10" />
                        <circle className="hit-wave" cx="113" cy="51" r="16" />
                        <circle className="hit-point" cx="113" cy="51" r="8" />
                        <path className="hit-tick" d="m109 51 3 3 5-6" />
                      </svg>
                      <b>같은 검색 엔진</b>
                    </div>
                    <div key={matchCount} className="result-card jd-result">
                      <span className="result-seal" aria-hidden="true">
                        <svg>
                          <use href="#i-check" />
                        </svg>
                      </span>
                      <span className="card-label">살펴볼 프로젝트와 경험</span>
                      <div className="sample-project">
                        <span>C</span>
                        <b>샘플 커머스</b>
                      </div>
                      <h4 className="whitespace-pre-line">{match[0]}</h4>
                      <p className="whitespace-pre-line">{match[1]}</p>
                    </div>
                  </div>
                </section>
              </div>
            </div>
            <div className="tech-foot">
              <span>개발 중인 연결을 가상 데이터로 보여주는 예시입니다.</span>
              <details>
                <summary>현재 구현과 다음 단계</summary>
                <p>
                  현재 코드에서 벡터 검색과 JD 역량 추출을 확인했습니다. 수집 흐름은 연동 설계를, JD
                  관련성 평가와 추천 설명은 연결 목표를 보여줍니다. 키워드 결합과 후보 재정렬은 개선
                  방향입니다.
                </p>
              </details>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}
