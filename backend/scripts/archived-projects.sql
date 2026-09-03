-- 이전 기수 팀 프로젝트 아카이브 데이터
-- 입력: scripts/prev-crew.json
-- 프로젝트 52건, 멤버 344건 (봇 6건 제외)
-- 생성: backend/scripts/generate-archived-projects.py

BEGIN;

INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/2d1dbc2a-b557-4153-bed2-00a70fb79ebc', 7, NULL, '아맞다', 'ah-madda', '아맞다', '아맞다! 이벤트 또 놓쳤다고? – 미리 알려드릴게요. 😎', 39, '2026-08-07', 'CLOSED', 'APPROVED', '![아맞다!, 이벤트 리마인드 서비스_page-0001](https://github.com/user-attachments/assets/2d1dbc2a-b557-4153-bed2-00a70fb79ebc)

## 팀원
| Frontend | Frontend | Backend | Backend | Backend | Backend |
| :-: | :-: | :-: | :-: | :-: | :-: |
| <img src="https://avatars.githubusercontent.com/u/108217858?v=4" width="150"> | <img src="https://avatars.githubusercontent.com/u/91647696?v=4" width="150"> | <img src="https://avatars.githubusercontent.com/u/118044367?v=4" width="150"> | <img src="https://avatars.githubusercontent.com/u/62169861?v=4" width="150"> | <img src="https://avatars.githubusercontent.com/u/60121346?v=4" width="150"> | <img src="https://avatars.githubusercontent.com/u/126929413?v=4" width="150"> |
| [세라](https://github.com/keemsebin) | [에리얼](https://github.com/yeji0214) | [머피](https://github.com/joon6093) | [서프](https://github.com/abc5259) | [투다](https://github.com/praisebak) | [가이온](https://github.com/jumdo12) |
', 'https://github.com/woowacourse-teams/2025-ah-madda', NULL, '2025-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/2b661f18-5c04-4c46-8801-89196a118380', 7, NULL, '봄봄', 'bom-bom', '봄봄', '뉴스레터를 더 쉽게, 더 꾸준히, 더 가치 있게 뉴스레터 리딩 플랫폼  - 봄봄 🌸', 15, '2026-08-07', 'CLOSED', 'APPROVED', '# 봄봄 🌸

<p align="center">
  <img width="599" height="287" alt="스크린샷" src="https://github.com/user-attachments/assets/2b661f18-5c04-4c46-8801-89196a118380" />
</p>

단순한 뉴스레터 리더를 넘어 읽는 습관을 만들고, 읽은 콘텐츠를 나만의 자산으로 바꾸는 **읽기의 라이프 로그**

<div align="center">

[![Website](https://img.shields.io/badge/Web-%23212121?style=for-the-badge&logo=google-chrome&logoColor=white)](https://www.bombom.news)
[![App Store](https://img.shields.io/badge/App_Store-%230D96F6?style=for-the-badge&logo=apple&logoColor=white)](https://apps.apple.com/kr/app/%EB%B4%84%EB%B4%84/id6752014462)
[![Google Play](https://img.shields.io/badge/Google_Play-%233DDC84?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.antarctica.bombom&pcampaignid=web_share)

</div>



## 브랜드 스토리


: “봄봄, 당신의 하루에 찾아오는 작은 설렘”

- **탄생 배경**
    - 복잡한 메일함 속에서 놓쳐버리는 가치 있는 글들
    - 꼭 읽고 싶은 뉴스레터조차 흘려보내는 우리의 바쁜 일상
    - 이 모든 것을 한데 모아, 매일 “새로운 봄”처럼 신선한 콘텐츠를 전해 주고자 한다.
- **이름에 담긴 의미**
    1. **첫 번째 ‘봄’**: 사용자가 기대하며 구독한 뉴스레터들
    2. **두 번째 ‘봄’**: “봄봄” 플랫폼을 통해 다시 만나는 즐거움과 설렘

## 문제 정의

- **현황**
    - 메일함은 여러 개의 메일이 존재한다.
    - 여러 발신지에서 날아오는 뉴스레터가 메일함을 복잡하게 만든다.
    - 중요 뉴스레터가 스팸함으로 빠지거나 잊혀지기 쉽다.
    - 개인적인 메일, 공적인 메일 등등 구분 없이 섞여있다.
- **페인 포인트**
    - 뉴스레터 구독 관리가 번거롭고, 읽지 않은 뉴스레터 메일이 쌓여만 간다.
    - 읽지 않을 때마다 가치 있는 정보가 낭비된다.

## 타겟층

1. 뉴스레터를 구독중이지만 습관이 안되어 읽지 못하고 쌓아두는 사람
2. 뉴스레터를 편리하게 구독/관리하고 싶은 사람

## 해결책

1. 사용자가 하나의 전용 이메일 계정을 통해 다양한 뉴스레터를 구독·관리한다.
    - 메일함에 흩어져 있던 뉴스레터 콘텐츠를 한 곳에 모아 보기 쉽게 제공한다.
2. 개인·그룹 단위로 꾸준히 읽도록 동기부여를 제공한다.

## 핵심 기능

| **기능** | **설명** |
| --- | --- |
| **뉴스레터 통합 수신** | **여러 이메일 계정에서 들어오는 뉴스레터를 한 곳에 모아 정리** |
| 읽기 트래킹 | 각 뉴스레터의 **읽은 정도(Reading Progress)**, **읽은 시간**, **읽은 날짜** 등을 자동 기록 |
| 읽기 목표 설정 | 매일/매주 **읽기 목표**를 설정하고 진행률을 시각적으로 확인 |
| 하이라이트 및 메모 | 뉴스레터 내용 중 중요한 부분을 **하이라이트 및 메모**로 저장 |
| 팔로우 & 비교 | 다른 사용자를 팔로우하여 **읽기 현황을 비교**, **리더보드로 동기 부여** |
| 아카이빙 | 메모를 모아 **지식 노트처럼 관리**할 수 있는 공간 제공 |

## **봄봄이 주는 가치**

| **사용자 문제** | **봄봄의 해결 방식** | **전달 가치** |
| --- | --- | --- |
| 뉴스레터가 쌓여만 간다 | 읽기 기록 + 알림 + 목표 설정 | **읽는 습관 형성** |
| 어디까지 읽었는지 기억 안 남 | 읽기 진행률 자동 기록 | 편의성 |


<br/>

## 🤠 How to use?

<table>

  <!-- Row 1 -->
  <tr>
    <td style="width: 50%; vertical-align: top; text-align: center;">
      <img 
        src="https://github.com/user-attachments/assets/fce87c25-69cb-4f4c-af74-7f4c3950e475"
        height="300"
      />
    </td>
    <td style="width: 50%; vertical-align: top;">

### 간편하게 클릭 몇 번으로 구독하기

- 구독할 뉴스레터 정보 페이지에 접속하여 **구독하기**를 눌러보세요.
- 봄봄이 담아준 구독 이메일을 붙여넣기 하면 구독 완료!
- 구독에 필요한 이메일은 저희 **봄봄을 통해 만들 수 있습니다.**

    </td>
  </tr>

  <!-- Row 2 -->
  <tr>
    <td style="width: 50%; vertical-align: top; text-align: center;">
      <img 
        src="https://github.com/user-attachments/assets/48b6e101-3f7d-49f4-ac65-b6946ba01003"
        height="300"
      />
    </td>
    <td style="width: 50%; vertical-align: top;">

### 🌱 봄이랑 함께하는 읽기 모험!

- 매일 출석하고 아티클을 읽으면 귀여운 봄이가 쑥쑥 자라나요.
- 봄이와 함께 새로운 지식으로 성장해 보세요!

    </td>  
  </tr>

  <tr>

    <td style="width: 50%; vertical-align: top; text-align: center;">
      <img 
        src="https://github.com/user-attachments/assets/7bb1b5dd-ac68-42fd-9e25-580d340ebad9" 
        height="300"
      />
    </td>  
    
    <td style="width: 50%; vertical-align: top;">

### 중요한 문장만 쏙! 하이라이트 & 메모

- 중요한 구절을 **하이라이트**하고 메모를 남겨,  
  지식을 체계적으로 정리해 보세요.

- 원하는 문장을 드래그하여 선택하면  
  **하이라이트와 메모를 사용할 수 있는 퀵 메뉴**가 나타나요.

    </td>
  </tr>

    <!-- Row 4 -->
  <tr>
    <td style="width:50%; vertical-align:top; text-align:center;">
      <img src="https://github.com/user-attachments/assets/e0871376-147f-4da2-af73-6dbef30e46fc" height="300"/>
    </td>
    <td style="width:50%; v1445ertical-align:top;">

### 📚 내가 바로 이달의 독서왕
- 매 달 갱신되는 **읽기 랭킹**에 도전해보세요!
- 나의 **순위**를 확인하고 꾸준히 읽는 습관을 만들어보세요.

    </td>
  </tr>
</table>

<br/>
<br/>

## ⚒️ 아키텍처

### 🎨 Client

<img width="720" height="415" alt="image" src="https://github.com/user-attachments/assets/577c9ec8-e044-404c-9497-d07d550513d2" />

### 🌐 Server

<img width="900" height="703" alt="스크린샷 2025-11-24 오후 1 53 44" src="https://github.com/user-attachments/assets/00309847-6a5b-4397-9a0d-a5f6be6937af" />

<br/>

### 🎨 Frontend 

**WEB**

![Typescript](https://img.shields.io/badge/typescript-%233178C6.svg?style=for-the-badge&logo=typescript&logoColor=white)
![React](https://img.shields.io/badge/react-%23333333.svg?style=for-the-badge&logo=react&logoColor=#61DAFB)
![Webpack](https://img.shields.io/badge/webpack-%238DD6F9.svg?style=for-the-badge&logo=webpack&logoColor=000)
![Emotion CSS](https://img.shields.io/badge/Emotion-%23F786AD.svg?style=for-the-badge&logo=styledcomponents&logoColor=white)

**App**

![Expo](https://img.shields.io/badge/Expo-000000?style=for-the-badge&logo=Expo&logoColor=white)
![React Native](https://img.shields.io/badge/React%20Native-61DAFB?style=for-the-badge&logo=React&logoColor=black)

### 🌐 Backend

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/mysql-4479A1.svg?style=for-the-badge&logo=mysql&logoColor=white)
![Swagger](https://img.shields.io/badge/-Swagger-%23Clojure?style=for-the-badge&logo=swagger&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![AWS](https://img.shields.io/badge/amazonaws-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white)

## 팀원
## Backend 🔧

| 모루 | 새로이 | 조로 | 피글렛 |
|:-------:|:------:|:--------:|:--------:|
| <img src="https://avatars.githubusercontent.com/u/58469870?v=4" alt="모루" style="width:100px;height:100px;object-fit:cover;" /> | <img src="https://avatars.githubusercontent.com/u/76567238?v=4" alt="새로이" style="width:100px;height:100px;object-fit:cover;" /> | <img src="https://avatars.githubusercontent.com/u/115832836?v=4" alt="조로" style="width:100px;height:100px;object-fit:cover;" /> | <img src="https://avatars.githubusercontent.com/u/88280787?v=4" alt="피글렛" style="width:100px;height:100px;object-fit:cover;" /> |
| [@choidongjun0830](https://github.com/Choidongjun0830) | [@Ryan-Dia](https://github.com/Ryan-Dia) | [@kysub99](https://github.com/kysub99) | [@rladmstn](https://github.com/rladmstn) |

## Frontend 🎨

| 재오 | 제나 | 피터 |
|:--------:|:------:|:------:|
| <img src="https://avatars.githubusercontent.com/u/61729032?v=4" alt="재오" style="width:100px;height:100px;object-fit:cover;" /> | <img src="https://avatars.githubusercontent.com/u/106021313?v=4" alt="제나" style="width:100px;height:100px;object-fit:cover;" /> | <img src="https://avatars.githubusercontent.com/u/62178788?v=4" alt="피터" style="width:100px;height:100px;object-fit:cover;" /> |
| [@jaeyoung-kwon](https://github.com/jaeyoung-kwon) | [@JeLee-river](https://github.com/JeLee-river) | [@guesung](https://github.com/guesung) |
', 'https://github.com/woowacourse-teams/2025-bom-bom', NULL, '2025-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES (NULL, 7, NULL, '보따리', 'bottari', '보따리', '💼 당신의 여행, 행사, 일상 속 든든한 짐 친구 `보따리`', 26, '2026-08-07', 'CLOSED', 'APPROVED', '# 🧳 보따리

## 📌 프로젝트 설명

> 물품 누락으로 인한 시간·비용 낭비와 스트레스를 줄이고,  
> 개인 및 단체가 중요한 순간을 완벽하게 준비할 수 있도록 도움을 제공하는 서비스

<br>

## 🧑‍💻 팀원 소개
### Backend

| 훌라(김성준) | 호떡(민경윤) | 벨로(방예혁) | 코기(장재현) |
|--------|--------|--------|--------|
| <div align="center">[Sung-june27](https://github.com/Sung-june27)</div> | <div align="center">[unh6unh6](https://github.com/unh6unh6)</div> | <div align="center">[YehyeokBang](https://github.com/YehyeokBang)</div> | <div align="center">[jaehyeon2650](https://github.com/jaehyeon2650)</div> |
| <img src="https://avatars.githubusercontent.com/u/105531824?v=4" width="150"/> | <img src="https://avatars.githubusercontent.com/u/144558971?v=4" width="150"/> | <img src="https://avatars.githubusercontent.com/u/107793780?v=4" width="150"/> | <img src="https://avatars.githubusercontent.com/u/121144710?v=4" width="150"/> |

<br>

### Android

| 다이스(문장훈) | 시아(이예린) | 오이(이인협) |
|--------|--------|--------|
| <div align="center">[moondev03](https://github.com/moondev03)</div> | <div align="center">[Leeyerin0210](https://github.com/Leeyerin0210)</div> | <div align="center">[cucumber99](https://github.com/cucumber99)</div> |
| <img src="https://avatars.githubusercontent.com/u/105299421?v=4" width="150"/> | <img src="https://avatars.githubusercontent.com/u/102152510?v=4" width="150"/> | <img src="https://avatars.githubusercontent.com/u/58465973?v=4" width="150"/> |
', 'https://github.com/woowacourse-teams/2025-bottari', NULL, '2025-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES (NULL, 7, NULL, '런세권', 'course-pick', '런세권', '🏃‍♂️‍➡️ 런세권 : 내 주위 러닝 코스 🏃‍♀️‍➡️', 30, '2026-08-07', 'CLOSED', 'APPROVED', '# 런세권: 러닝 코스 탐색 서비스

## 프로젝트 설명

당신도 런세권에 살고 있다는 사실, 아셨나요? 앱으로 내 주변 러닝 코스를 찾아보세요.

역세권, 숲세권... 이제 러너에겐 런세권이 필요합니다!

''런세권''은 달리기를 사랑하는 당신을 위해, 우리 동네의 숨겨진 러닝 코스를 찾아주는 가장 똑똑한 방법입니다. 더 이상 어디서 달려야 할지 고민하지 마세요. 런세권이 제공하는 최고의 코스들이 당신을 기다리고 있습니다.

<h2>Download</h2>

<a href=''https://play.google.com/store/apps/details?id=io.coursepick.coursepick''><img alt=''다운로드하기 Google Play'' src=''https://play.google.com/intl/ko/badges/static/images/badges/ko_badge_web_generic.png'' width=''40%''/></a>


## 런세권의 주요 기능

📍 내 주변 코스 탐색: GPS 기반으로 현재 위치에서 가장 가까운 러닝 코스를 바로 찾아보세요. 익숙한 동네의 새로운 길을 발견하는 재미를 느껴보세요.

ℹ️ 상세한 코스 정보: 총 거리, 오르막/내리막 정보 등 달리기 전에 필요한 모든 정보를 확인하세요.

## 이런 분들께 추천해요

이제 막 달리기를 시작한 초보 러너: 어디서 달려야 할지, 몇 km가 적당할지 막막하다면 런세권이 제공하는 가깝고 쉬운 코스로 시작해 보세요.

새로운 자극이 필요한 베테랑 러너: 매일 똑같은 코스가 지겨워졌다면, 런세권이 제공한 도전적인 코스에 도전하며 러닝의 새로운 재미를 찾아보세요.

여행이나 출장 중인 러너: 낯선 동네에서도 헤매지 않고 안전하게 달릴 수 있는 최고의 코스를 런세권에게 추천받으세요.

당신의 러닝 라이프를 더욱 풍요롭게 만들어 줄 런세권과 함께 달려보세요.
오늘은 어디를 달려볼까요? 지금 바로 다운로드하세요!

## 팀원


|<img src="https://avatars.githubusercontent.com/u/161921046?v=4" width="125" />|<img src="https://avatars.githubusercontent.com/u/192606356?v=4" width="125" />|<img src="https://avatars.githubusercontent.com/u/108331578?v=4" width="125" />|<img src="https://avatars.githubusercontent.com/u/176254419?v=4" width="125" />|<img src="https://avatars.githubusercontent.com/u/46932235?v=4" width="125" />|<img src="https://avatars.githubusercontent.com/u/104622150?v=4" width="125" />|<img src="https://avatars.githubusercontent.com/u/111430281?v=4" width="125" />|
|:---------:|:---------:|:---------:|:----------:|:---------:|:---------:|:---------:|
|[디랙<br>(허찬)](https://github.com/doabletuple)|[모찌<br>(황채원)](https://github.com/wondroid-world)|[지오<br>(김준서)](https://github.com/giovannijunseokim)|[토바에<br>(박지원)](https://github.com/tobae-time)|[돔푸<br>(이창근)](https://github.com/dompoo)|[율무<br>(강기석)](https://github.com/kkiseug)|[짱구<br>(박준혁)](https://github.com/jhpark1227)|
|Android|Android|Android|Android|Backend|Backend|Backend|
', 'https://github.com/woowacourse-teams/2025-course-pick', NULL, '2025-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/b9f4b531-e259-4855-927a-939573544dcd', 7, NULL, '아인슈타임', 'estime', '아인슈타임', '똑똑하게 시간 약속 잡기 - 아인슈타임👨🏻‍🦳', 10, '2026-08-07', 'CLOSED', 'APPROVED', '# <img src="https://github.com/user-attachments/assets/b9f4b531-e259-4855-927a-939573544dcd" width="40" height="40" valign="middle" /> [아인슈타임](https://estime.today/)   

<img width="7680" height="4320" alt="서비스 썸네일 이미지" src="https://github.com/user-attachments/assets/238f2d64-bf78-4d36-9617-810a86d9d445" />


### Team Members

## Backend 🔧

| 강산 🏔️ | 리버 💧 | 제프리 🍎 | 플린트 🔥 |
|:-------:|:------:|:--------:|:--------:|
| <img src="https://github.com/user-attachments/assets/b8222adf-c28e-4686-b7b6-7014db3056f9" alt="강산" style="width:120px;height:120px;object-fit:cover;" /> | <img src="https://github.com/user-attachments/assets/67aadd73-1e95-4845-a8ee-293880e832ba" alt="리버" style="width:120px;height:120px;object-fit:cover;" /> | <img src="https://github.com/user-attachments/assets/122d491e-26e5-46d4-88d8-4921c846ba8c" alt="제프리" style="width:120px;height:120px;object-fit:cover;" /> | <img src="https://github.com/user-attachments/assets/509c085d-5935-4949-a470-b5fc18d933b7" alt="플린트" style="width:120px;height:120px;object-fit:cover;" /> |
| [@m-a-king](https://github.com/m-a-king) | [@yeonnhuu](https://github.com/yeonnhuu) | [@AppleMint98](https://github.com/AppleMint98) | [@jhan0121](https://github.com/jhan0121) |

## Frontend 🎨

| 메이토 🍅 | 마빈 🎮 | 해삐 😊 | 호이초이 🤡 |
|:--------:|:------:|:------:|:----------:|
| <img src="https://github.com/user-attachments/assets/04e762d7-9e2f-4204-9724-c0db6c25ac42" alt="메이토" style="width:120px;height:120px;object-fit:cover;" /> | <img src="https://github.com/user-attachments/assets/002d6291-3c30-4464-a4da-c464f60b3890" alt="마빈" style="width:120px;height:120px;object-fit:cover;" /> | <img src="https://github.com/user-attachments/assets/ac2c0666-784c-4c31-81b4-482f405da8b8" alt="해삐" style="width:120px;height:120px;object-fit:cover;" /> | <img src="https://github.com/user-attachments/assets/0af7f115-f599-4a48-bb40-b5af870c802e" alt="호이초이" style="width:120px;height:120px;object-fit:cover;" /> |
| [@Db0111](https://github.com/Db0111) | [@spoyodevelop](https://github.com/spoyodevelop) | [@thgml05](https://github.com/thgml05) | [@hoyyChoi](https://github.com/hoyyChoi) |

## 🛠️ Tech Stack

### 🎨 Frontend 


![Typescript](https://img.shields.io/badge/typescript-%233178C6.svg?style=for-the-badge&logo=typescript&logoColor=white)
![React](https://img.shields.io/badge/react-%23333333.svg?style=for-the-badge&logo=react&logoColor=#61DAFB)
![Storybook](https://img.shields.io/badge/storybook-%23FF4785.svg?style=for-the-badge&logo=storybook&logoColor=white)
![Jest](https://img.shields.io/badge/jest-%23C21325.svg?style=for-the-badge&logo=jest&logoColor=white)
![Emotion CSS](https://img.shields.io/badge/Emotion-%23F786AD.svg?style=for-the-badge&logo=styledcomponents&logoColor=white)
![Webpack](https://img.shields.io/badge/webpack-%238DD6F9.svg?style=for-the-badge&logo=webpack&logoColor=000)
![GitHub Actions](https://img.shields.io/badge/github%20actions-%232088FF.svg?style=for-the-badge&logo=githubactions&logoColor=white)



### 🌐 Backend

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/mysql-4479A1.svg?style=for-the-badge&logo=mysql&logoColor=white)
![Swagger](https://img.shields.io/badge/-Swagger-%23Clojure?style=for-the-badge&logo=swagger&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/github%20actions-%232671E5.svg?style=for-the-badge&logo=githubactions&logoColor=white)
![Nginx](https://img.shields.io/badge/nginx-%23009639.svg?style=for-the-badge&logo=nginx&logoColor=white)


## 🏭 Architecture
<img width="1992" height="3072" alt="스크린샷 2026-03-14 오후 8 05 12" src="https://github.com/user-attachments/assets/ada78661-8f10-4a4e-9cb3-94971d864965" />
', 'https://github.com/woowacourse-teams/2025-estime', NULL, '2025-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/400f1151-78a2-4f78-aa98-dad218fb495e', 7, NULL, '페스타북', 'festabook', '페스타북', '🎆 festabook ; 흩어진 정보를 하나로, 축제를 한 권에 담다 📖 (Deprecated)', 20, '2026-08-07', 'CLOSED', 'APPROVED', '> 📦 본 레포지토리는 더 이상 유지되지 않으며, 프로젝트는 [festabook 공식 레포지토리](https://github.com/festabook)로 이전되었습니다.

<br>

# 🎆 페스타북 festabook : 축제를 한 권에 담다 

[![Google Play](https://img.shields.io/badge/Google_Play-414141?style=for-the-badge&logo=googleplay&logoColor=white)](https://play.google.com/store/apps/details?id=com.daedan.festabook)
[![App Store](https://img.shields.io/badge/App_Store-0D96F6?style=for-the-badge&logo=appstore&logoColor=white)](https://apps.apple.com/kr/app/%ED%8E%98%EC%8A%A4%ED%83%80%EB%B6%81-festabook/id6752591661)
<img width="3240" height="1350" alt="festabook 소개 이미지" src="https://github.com/user-attachments/assets/400f1151-78a2-4f78-aa98-dad218fb495e" />

**🎆 대학 축제 정보, 더 이상 헤매지 마세요!**
많은 학생들이 대학 축제 정보를 찾는 데 불편함을 겪고 있다는 사실, 알고 계셨나요?  

공연 일정은 SNS에 흩어져 있고, 부스 위치는 직접 찾아가야 하고, 공지사항은 또 다른 채널에서 찾아야 하죠.  

**페스타북(festabook)** 은 이런 불편함을 해결하기 위해 만들어졌습니다.  
이제 흩어진 정보를 찾느라 헤매지 말고, 한 권의 책처럼 정리된 축제를 경험하세요.  


---
## 🧑‍💻 Team
<table style="width:100%">
  <thead>
    <tr>
      <th style="width:14.28%">Backend</th>
      <th style="width:14.28%">Backend</th>
      <th style="width:14.28%">Backend</th>
      <th style="width:14.28%">Backend</th>
      <th style="width:14.28%">Android</th>
      <th style="width:14.28%">Android</th>
      <th style="width:14.28%">Android</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td align="center" style="width:14.28%">
        <a href="https://github.com/soeun2537">
          <img src="https://github.com/soeun2537.png" style="width:100%"/><br/>
          미소
        </a>
      </td>
      <td align="center" style="width:14.28%">
        <a href="https://github.com/changuii">
          <img src="https://github.com/changuii.png" style="width:100%"/><br/>
          부기
        </a>
      </td>
      <td align="center" style="width:14.28%">
        <a href="https://github.com/taek2222">
          <img src="https://github.com/taek2222.png" style="width:100%"/><br/>
          비타
        </a>
      </td>
      <td align="center" style="width:14.28%">
        <a href="https://github.com/eoehd1ek">
          <img src="https://github.com/eoehd1ek.png" style="width:100%"/><br/>
          후유
        </a>
      </td>
      <td align="center" style="width:14.28%">
        <a href="https://github.com/oungsi2000">
          <img src="https://github.com/oungsi2000.png" style="width:100%"/><br/>
          밀러
        </a>
      </td>
      <td align="center" style="width:14.28%">
        <a href="https://github.com/parkjiminnnn">
          <img src="https://github.com/parkjiminnnn.png" style="width:100%"/><br/>
          제이
        </a>
      </td>
      <td align="center" style="width:14.28%">
        <a href="https://github.com/etama123">
          <img src="https://github.com/etama123.png" style="width:100%"/><br/>
          타마
        </a>
      </td>
    </tr>
  </tbody>
</table>

---
## 📱 앱 미리보기

| 탐색 | 홈 | 일정 | 지도 | 소식 |
|---|---|---|---|---|
| <img width="1200" height="2133" alt="542380817_18081825346927603_8857504627003636425_n" src="https://github.com/user-attachments/assets/a092026a-0621-4cb8-a5b0-4a7680ad4214" />| <img width="1200" height="2133" alt="Frame 2" src="https://github.com/user-attachments/assets/5fcaeb5a-3ae8-4f2f-84e6-d5fb506299ac" /> |<img width="1200" height="2133" alt="Frame 3" src="https://github.com/user-attachments/assets/5edcfd57-1c7b-4582-b91e-6930218d4dbd" /> | <img width="1200" height="2133" alt="Frame 4" src="https://github.com/user-attachments/assets/f7d5f5c6-4692-4a8c-87fd-e4b550ffb3e3" /> | <img width="1200" height="2133" alt="Frame 5" src="https://github.com/user-attachments/assets/79783764-0171-44c0-9db8-e7f6ff615209" /> |

- **탐색** : 다양한 학교 축제를 검색
- **홈** : 축제 포스터와 연예인 라인업을 한눈에 확인  
- **일정** : 공연과 행사를 놓치지 않도록 일정 확인  
- **지도** : 부스 · 학생 주점 · 무대 · 주요 시설 위치와 상세정보 제공  
- **소식** : 학생회 공지, FAQ, 분실물 안내까지 빠르고 정확하게 확인  

페스타북과 함께라면, 여러분의 축제는 더 즐겁고 편리해집니다 🎉  

---

## 📬 Contact
[![Gmail](https://img.shields.io/badge/Email-festabook2025@gmail.com-red?style=for-the-badge&logo=gmail&logoColor=white)](mailto:festabook2025@gmail.com)
[![Instagram](https://img.shields.io/badge/Instagram-@festabook.official-E4405F?style=for-the-badge&logo=instagram&logoColor=white)](https://www.instagram.com/festabook.official)

', 'https://github.com/woowacourse-teams/2025-festabook', NULL, '2025-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/dd21ef78-f14d-4ee1-9d18-171e1c3acb96', 7, NULL, '히어릿', 'hearit', '히어릿', '🎧 읽지 말고, 히어릿 — 귀로 듣는 개발 인사이트', 27, '2026-08-07', 'CLOSED', 'APPROVED', '![대표이미지](https://github.com/user-attachments/assets/dd21ef78-f14d-4ee1-9d18-171e1c3acb96)

# 히어릿 hEARit - 개발자 IT 팟캐스트 서비스

### 글 대신, 팟캐스트로 쉽게 공부하자!

빠르게 변하는 IT 트렌드와 기술 스택을 쉽게 들을 수 있는 팟캐스트 서비스입니다.
복잡한 문서나 긴 글 대신, 원하는 정보를 들으며  IT 세상을 따라가 보세요.
 
**개발자를 위한 오디오 학습 플랫폼**, 히어릿(hEARit)이 여러분의 귀를 채워드립니다.

<div style="display: flex;">
  <a href="https://play.google.com/store/apps/details?id=com.onair.hearit&hl=ko">
   <img 
      src="https://github.com/user-attachments/assets/e478564c-1f03-4c8c-9fbc-3fddf67e6b31"
      alt="Get it on Google Play"
      style="height: 75px; display: block;"/>
  </a><a href="https://apps.apple.com/kr/app/%ED%9E%88%EC%96%B4%EB%A6%BF-hearit-%EC%BD%94%EB%94%A9-%EA%B3%B5%EB%B6%80%EB%A5%BC-%EC%9C%84%ED%95%9C-it-%ED%8C%9F%EC%BA%90%EC%8A%A4%ED%8A%B8/id6756041112">
    <img
      src="https://github.com/user-attachments/assets/293cbdef-eef2-4072-b990-c5e25619ae30"
      alt="Download on the App Store"
      style="height: 75px; display: block;"
    />
  </a>
</div>

## 서비스 소개

### 오늘 추천하는 팟캐스트, 관심 있는 카테고리의 팟캐스트를 바로 볼 수 있어요 🎧
<p align="center">
  <img src="https://github.com/user-attachments/assets/97beac69-4fbc-48c7-bf7f-b1e9ee8eaada" width="25%">
</p>

***지금 딱, 당신이 좋아할 콘텐츠!***

관심 있는 카테고리의 팟캐스트를 한눈에 보고,
홈 화면에서 바로 들어보세요.

새로 올라온 팟캐스트부터 즐겨 듣던 콘텐츠까지
놓치지 않고 만나볼 수 있어요.

### 1분 숏캐스트로 자유롭게 팟캐스트를 발견해보세요 💿
<p align="center">
  <img src="https://github.com/user-attachments/assets/5dbc518e-4c68-4d1c-a4a6-61fb99fb8526" width="25%">
</p>

***짧게, 빠르게, 자유롭게!***

위아래로 스크롤하며 다양한 팟캐스트를 탐색해보세요.

당신의 취향에 맞는 IT 콘텐츠를
쉽게 발견할 수 있습니다.

### 팟캐스트를 들으며 스크립트와 출처까지 바로 확인할 수 있어요 🔊
<p align="center">
  <img src="https://github.com/user-attachments/assets/8aa2a5f4-d3c4-4666-b792-088d11d0230f" width="25%">
</p>

***듣는 재미에 보는 재미까지!***

스크립트와 출처를 함께 보며
더 풍성하게 즐겨보세요.

요약과 참고 자료까지 한눈에 확인할 수 있습니다.

### 검색 기능으로 원하는 팟캐스트를 찾아보세요 🔎
<p align="center">
  <img src="https://github.com/user-attachments/assets/115a6998-ef17-4086-866c-27e564da42f6" width="25%">
</p>

***듣고 싶은 팟캐스트, 바로 찾기!***

제목이나 키워드로 원하는 콘텐츠를 검색하고,
카테고리별로 정리된 팟캐스트도 한눈에 확인해보세요.

자주 듣는 콘텐츠는 최근 검색어로 손쉽게 
다시 찾아볼 수 있습니다.

## 멤버 소개

### Android

| 조이(김가현) | 미플(함범준) | 비비(장민정) | 
|--------|--------|--------|
| <div align="center">[gahyunkim](https://github.com/gahyunkim)</div> | <div align="center">[HamBeomJoon](https://github.com/HamBeomJoon)</div> | <div align="center">[rosemin928](https://github.com/rosemin928)</div> |
| <img src="https://avatars.githubusercontent.com/gahyunkim" width="150"/> | <img src="https://avatars.githubusercontent.com/HamBeomJoon" width="150"/> | <img src="https://avatars.githubusercontent.com/rosemin928" width="150"/> | 

### Backend

| 사나(조은산) | 멍구(이유영) | 벡터(백승주) | 가콩(최가빈) |
|--------|--------|--------|--------|
| <div align="center">[JO-eusan](https://github.com/JO-eusan)</div> | <div align="center">[YuyoungRhee](https://github.com/YuyoungRhee)</div> | <div align="center">[Byesol](https://github.com/Byesol)</div> | <div align="center">[gabean13](https://github.com/gabean13)</div> |
| <img src="https://avatars.githubusercontent.com/JO-eusan" width="150"/> | <img src="https://avatars.githubusercontent.com/YuyoungRhee" width="150"/> | <img src="https://avatars.githubusercontent.com/Byesol" width="150"/> | <img src="https://avatars.githubusercontent.com/gabean13" width="150"/> |

## 기술 스택

### Android

| **구분** | **사용 기술** |
| --- | --- |
| **Language** | Kotlin, XML |
| **Architecture** | MVVM |
| **Network** | Retrofit, OkHttp |
| **Serialization** | kotlinx-serialization |
| **Async** | Coroutines |
| **Jetpack** | Compose, ViewModel, DataBinding, Notification, Media3, LiveData, RecyclerView, Navigation |
| **ThirdParty** | Lottie, Flexbox, Coil, Kakao SDK, Firebase Analytics, Firebase Crashlytics, Timber |
| **LocalDB** | RoomDB, DataStore |
| **Test** | JUnit5, Junit4, Espresso, AssertJ |

### Backend

| **구분** | **사용 기술** |
| --- | --- |
| **Language / Framwork** | Java 21, Spring Boot 3.5.3 |
| **Library** | Spring Security, JPA, JWT, Lombok |
| **Database** | MySQL, H2 |
| **Infra** | AWS EC2, S3, Nginx, Docker |
| **Build Tool** | Gradle |
| **CI/CD** | GitHub Actions(Self-hosted Runner) |
| **Test** | JUnit 5, AssertJ, RestAssured, MockMvc, Mockito |
| **Document** | Swagger, Spring REST Docs |
| **Monitoring / Observability** | Grafana, Prometheus, Loki, Tempo, Node Exporter, Mysqld Exporter |
| **Logging** | Log4j2, Promtail |
| **Security / Auth** | JWT, HTTPS |

## 프로젝트 주요 성과

### Marketing

- 26일(9/25~10/20)만에 앱 사용자 101명을 추가 확보하여 총 141명의 사용자 달성 (초기 40명)
    - [**LinkedIn**](https://www.linkedin.com/company/%ED%9E%88%EC%96%B4%EB%A6%BF-hearit/), [**팀 Velog**](https://velog.io/@hearit/posts) 트러블 슈팅 기록 공유, 개발자 오픈채팅방 홍보

### Android

- **Media3 기반 오디오 재생 기능**으로 안정적이고 끊김 없는 청취 경험 제공
- **백그라운드 재생 및 이어듣기 기능** 지원으로 몰입감 높은 사용자 경험 구현
- **회원 / 비회원 모두 이용 가능한 구조**로 접근성과 진입 장벽 최소화
- 앱 전반의 **디자인과 인터랙션을 직접 설계**하여 hEARit 정체성과 일관된 사용자 경험 제공
- **사용자 중심의 직관적 UI와 자막, 애니메이션 효과 구현**으로 누구나 쉽게 콘텐츠를 찾고 몰입할 수 있도록 함
- 추천, 북마크, 최근 재생 등 **개인화된 홈 화면 구성**으로 재방문 유도
- Firebase Analytics & Crashlytics 연동으로 **사용자 행동 분석 및 지속적인 품질 개선**

### Backend

- **사용자 맞춤 추천 시스템 구축**
    - 콘텐츠 기반 필터링 알고리즘을 적용해 숏폼 팟캐스트를 개인화 추천 (북마크, 재생 기록, 최신 콘텐츠 데이터 활용)
    - 홈 화면에 사용자가 북마크한 카테고리를 기반으로 맞춤형 콘텐츠 제공
- **API 버전 관리로 하위 호환성 보장**
    - 앱 업데이트 이후에도 이전 버전 사용자가 안정적으로 이용할 수 있도록 버전별 API 관리 체계 구축
- **효율적인 협업을 위한 API 문서 자동화**
    - Spring Rest Docs와 Swagger UI를 연동해 테스트 기반 문서 자동 생성 개발
    - 코드와 문서 간 불일치를 제거하여 빠른 MVP 개발과 협업 효율성 향상
- **DB 부하 최소화를 위한 배치 저장 구조 도입**
    - 재생 기록의 빈번한 저장 요청을 스케줄러 기반 배치 처리로 전환
    - 요청당 처리 부하를 줄이고, 데이터 일괄 저장으로 성능 최적화

## For more..
히어릿 관련 정보(_기술 스택, 그라운드 룰, 코드 컨벤션, 기술 문서화 등_)는 모두 여기 정리되어 있습니다

👉 [히어릿 Wiki](https://github.com/woowacourse-teams/2025-hEARit/wiki)

👉 [히어릿 Discussions](https://github.com/woowacourse-teams/2025-hEARit/discussions)
', 'https://github.com/woowacourse-teams/2025-hEARit', NULL, '2025-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://brick-william-6f5.notion.site/image/attachment%3Ad07ea022-fe48-4104-9972-e452799b5086%3A%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA_2025-11-25_%E1%84%8B%E1%85%A9%E1%84%92%E1%85%AE_6.38.26.png?table=block&id=2b64e173-3f21-80d8-a783-dbf5bf8cdbb5&spaceId=e321b4cb-8569-4a87-9b86-2845eb22f8d7&width=2000&userId=&cache=v2', 7, NULL, '모아온', 'moaon', '모아온', '프로젝트를 모아모아, 모아온 📦', 17, '2026-08-07', 'CLOSED', 'APPROVED', '## [📦 **모아온(moaon)**](https://moaon.co.kr/)

<br/>

> [!NOTE]  
> 프로젝트의 맥락과 인사이트를 함께 탐색하는 새로운 경험, 모아온.

- 🔄 프로젝트와 아티클을 양방향으로 연결해 기술 선택의 이유와
  문제 해결 과정을 자연스럽게 이해할 수 있도록 돕습니다.

- 😮 관심 있는 주제와 기술 스택으로 탐색하고, 깊이 있는 아티클을 통해
  프로젝트의 숨은 이야기를 발견하세요.

- ✨ 프로젝트와 아티클이 만나는 곳, 당신의 성장을 연결하는 플랫폼

<br/>

![](https://brick-william-6f5.notion.site/image/attachment%3Ad07ea022-fe48-4104-9972-e452799b5086%3A%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA_2025-11-25_%E1%84%8B%E1%85%A9%E1%84%92%E1%85%AE_6.38.26.png?table=block&id=2b64e173-3f21-80d8-a783-dbf5bf8cdbb5&spaceId=e321b4cb-8569-4a87-9b86-2845eb22f8d7&width=2000&userId=&cache=v2)

<br/>

## 🙌  팀원 소개

|                                                                  BE                                                                  |                                                                        BE                                                                         |                                                                          BE                                                                          |                                                                         BE                                                                         |
| :----------------------------------------------------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------------------------------------------------------------: | :--------------------------------------------------------------------------------------------------------------------------------------------------: | :------------------------------------------------------------------------------------------------------------------------------------------------: |
| <a href="https://github.com/Minuring"><img src="https://avatars.githubusercontent.com/u/144205824?v=4" width="140" height="140"></a> | <a href="https://github.com/yesjuhee"><img src="https://avatars.githubusercontent.com/u/96484143?v=4" alt="profile" width="140" height="140"></a> | <a href="https://github.com/minjae8563"><img src="https://avatars.githubusercontent.com/u/171022147?v=4" alt="profile" width="140" height="140"></a> | <a href="https://github.com/eueo8259"><img src="https://avatars.githubusercontent.com/u/162389416?v=4" alt="profile" width="140" height="140"></a> |
|                                                [이민우](https://github.com/Minuring)                                                 |                                                       [노주희](https://github.com/yesjuhee)                                                       |                                                       [유민재](https://github.com/minjae8563)                                                        |                                                       [이정민](https://github.com/eueo8259)                                                        |

|                                                                 FE                                                                 |                                                                        FE                                                                        |                                                                         FE                                                                         |                                                                           FE                                                                           |
| :--------------------------------------------------------------------------------------------------------------------------------: | :----------------------------------------------------------------------------------------------------------------------------------------------: | :------------------------------------------------------------------------------------------------------------------------------------------------: | :----------------------------------------------------------------------------------------------------------------------------------------------------: |
| <a href="https://github.com/wo-o29"><img src="https://avatars.githubusercontent.com/u/154664697?v=4" width="140" height="140"></a> | <a href="https://github.com/mlnwns"><img src="https://avatars.githubusercontent.com/u/129190157?v=4" alt="profile" width="140" height="140"></a> | <a href="https://github.com/jin123457"><img src="https://avatars.githubusercontent.com/u/72060681?v=4" alt="profile" width="140" height="140"></a> | <a href="https://github.com/eunoia-jaxson"><img src="https://avatars.githubusercontent.com/u/62330979?v=4" alt="profile" width="140" height="140"></a> |
|                                                [이우혁](https://github.com/wo-o29)                                                 |                                                       [곽민준](https://github.com/mlnwns)                                                        |                                                       [진 솔](https://github.com/jin123457)                                                        |                                                       [김진서](https://github.com/eunoia-jaxson)                                                       |

<br/>

---

## BE 기술 스택 및 인프라

**Core & Framework:** `Java`, `Spring Boot`

**Database & Search**: `MySQL`, `Elasticsearch`, `Spring Data JPA`, `QueryDSL`, `Flyway`, `AWS RDS`

**Infrastructure & Deployment (CI/CD)**: `AWS EC2`, `Docker`, `Nginx`, `GitHub Actions`, `AWS S3`

**Monitoring & Logging**: `Grafana`, `Prometheus`, `Loki`, `Tempo`

**Test & Documentation**: `JUnit 5`, `RESTAssured`, `Testcontainers`

**Authentication & Utilities**: `JWT`

![](https://brick-william-6f5.notion.site/image/attachment%3A6c645b5e-17fb-4801-8088-7460aeec8530%3AGroup_2069.png?table=block&id=29d4e173-3f21-8023-8c14-e19010cd3f70&spaceId=e321b4cb-8569-4a87-9b86-2845eb22f8d7&width=2000&userId=&cache=v2)

## FE 기술 스택 및 인프라

**Language:** `TypeScript`

**Core Framework**: `React` , `React-Router`

**Bundling/Transpiling**: `webpack` , `babel`

**State Management:** `Tanstack Query`

**Styling**: `Emotion`

**Code Quality**: `Biome`

**Analytics & Monitoring**: `Google Anlytics4`, `Sentry`

![](https://brick-william-6f5.notion.site/image/attachment%3A1ef62e0f-8256-4b2c-824e-f3909cbd672e%3A%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA_2025-11-09_%E1%84%8B%E1%85%A9%E1%84%92%E1%85%AE_7.54.10.png?table=block&id=2a64e173-3f21-8002-be27-eb6f90c57434&spaceId=e321b4cb-8569-4a87-9b86-2845eb22f8d7&width=2000&userId=&cache=v2)
', 'https://github.com/woowacourse-teams/2025-moaon', NULL, '2025-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/fb610be3-f418-454e-a4c2-fbfb4e575f39', 7, NULL, '물깜', 'mul-kkam', '물깜', '💧 개인별 물 섭취량 관리 서비스: 물깜 (물, 깜빡하지 말아요)', 32, '2026-08-07', 'CLOSED', 'APPROVED', '# **💧 물깜, 물 깜빡하지 말아요.**

## 서비스 주제

> 물깜(“물 깜빡하지 말아요”)은 **개인 맞춤형 물 섭취 관리**를 돕는 서비스입니다.
>
> 목표 설정 → 기록/알림/위젯 → 통계 확인까지, **하루 습관 형성**에 초점을 맞췄습니다.

[Play Store 이동](https://play.google.com/store/apps/details?id=com.mulkkam)


# 🧱 백엔드
## 인프라 다이어그램
<img width="1368" height="892" alt="image" src="https://github.com/user-attachments/assets/fb610be3-f418-454e-a4c2-fbfb4e575f39" />

# 🤖 안드로이드

## **📌 주요 기능**

- **맞춤 목표 섭취량** 설정 및 진행률 표시
- **원탭 기록**: 홈 화면 **위젯**과 앱 내부 버튼에서 빠르게 기록
- **리마인드 알림**: 사용자 지정 주기로 푸시 알림 · 앱 진입 시 불필요 알림 자동 정리
- **통계 & 히스토리**: 일/주/월 단위 요약
- **선택적 연동**: Health Connect, Kakao 로그인
- **운영 품질**: Firebase Analytics / Crashlytics

---

## **⚙️ 개발 환경**

- **Android Studio**: 최신 Stable (권장: 내장 JDK 사용)
- **JDK**: 21
- **Android 9 (API 28)** 이상 기기/에뮬레이터
- **Gradle**: Version Catalog 기반 종속성 관리

빌드 & 실행:

```
./gradlew installDebug
adb shell am start -n "com.mulkkam/com.mulkkam.ui.splash.SplashActivity"
```

> 일부 기능(Firebase, 외부 API 연동, Kakao 로그인 등)을 위해서는
환경별 설정 값(예: API 키, Base URL)이 필요합니다.
>
> 해당 값은 **개인 로컬 환경 또는 별도 환경 변수 파일**을 통해 주입하세요.

---

## **🏗 아키텍처**

- **Clean Architecture (단일 모듈 내 레이어드 패키징)**
    - **UI**: Activity/Fragment, ViewModel, 위젯, 알림
    - **Data**: Remote + Local + Repository
    - **DI**: object 생성을 통한 수동 의존성 주입 기반 그래프
- **데이터 흐름**: 단방향 데이터 흐름(UDF)
    - 사용자 액션 → ViewModel → Repository → ViewModel 상태 갱신 → UI 반영
- **화면 진입 규약**: 각 Activity는 newIntent(context, …) 팩토리를 제공해, 받는 쪽에서 필요한 데이터를 명시

---

## **🔗 주요 의존성**

- **UI**: Jetpack Compose, Material3, ViewBinding
- **네트워크**: Retrofit, OkHttp, kotlinx.serialization
- **비동기**: Kotlin Coroutines
- **이미지**: Coil 3
- **알림/작업**: WorkManager, Notification API
- **로그인/품질**: Kakao SDK, Firebase Analytics/Crashlytics
- **연동**: Health Connect

(*버전 정보는 libs.versions.toml 참고*)

---

## **🧪 테스트**

- **단위 테스트 중심** (UI/통합 테스트는 진행하지 않음)
- 범위: ViewModel 상태 전이, Repository 변환, Util/규칙 검증
- JUnit5, Kotest, MockK, Coroutines Test 등

실행:

```
./gradlew test
```

---

## **🖼 디자인**

- 앱은 Material 3 가이드를 따릅니다.
- Figma 등 디자인 산출물은 프로젝트 디자인 문서에서 확인 가능합니다.

---

## 💼 컨벤션

- 자세한 컨벤션은 [GitHub Wiki](https://github.com/woowacourse-teams/2025-mul-kkam/wiki/%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-%EC%84%A4%EB%AA%85)를 참고해주세요.

---

## 🪄 이 레포지토리가 도움이 되셨나요?

- 잊지 말고 **스타(⭐️)** 를 눌러주세요!



# 백엔드
## 인프라 다이어그램
<img width="1368" height="892" alt="image" src="https://github.com/user-attachments/assets/fb610be3-f418-454e-a4c2-fbfb4e575f39" />


## 팀원 소개

|<img src="https://github.com/junseo511.png" width="125" />|<img src="https://github.com/hwannow.png" width="125" />|<img src="https://github.com/devfeijoa.png" width="125" />|<img src="https://github.com/CheChe903.png" width="125" />|<img src="https://github.com/2Jin1031.png" width="125" />|<img src="https://github.com/minSsan.png" width="125" />|<img src="https://github.com/Jin409.png" width="125" />|
|:---------:|:---------:|:---------:|:---------:|:---------:|:---------:|:---------:|
|[공백(최준서)](https://github.com/junseo511)|[환노(김은지)](https://github.com/hwannow)|[이든(장은영)](https://github.com/devfeijoa)|[체체(김진영)](https://github.com/CheChe903)|[칼리(이 진)](https://github.com/2Jin1031)|[밍곰(박민선)](https://github.com/minSsan)|[히로(진승희)](https://github.com/Jin409)|
|Android|Android|Android|Backend|Backend|Backend|Backend|
', 'https://github.com/woowacourse-teams/2025-mul-kkam', NULL, '2025-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/8ef4ddbd-d041-4f06-8d0d-134e8e8c5569', 7, NULL, 'PickEat', 'pick-eat', 'PickEat', '매번 반복되는 팀 식사 고민, 팀원들의 취향을 모두 담은 PickEat으로 결정하세요!✨', 17, '2026-08-07', 'CLOSED', 'APPROVED', '## 🧀 PickEat 서비스 소개

<br>

> **매번 반복되는 메뉴 고민, 이제는 팀원들의 취향을 기억하는 PickEat이 해결합니다.**

같이 식사할 때마다 “아무거나”라는 말 뒤에 숨겨진 기피 음식들, 다이어트 중이라 먹기 힘든 메뉴, 최근에 먹은 음식까지… 다양한 제약이 얽히다 보면 결국 소극적인 사람이 참고
말거나, 모두가 찜찜한 결정을 하게 됩니다.

저희는 이러한 고민을 줄이기 위해,
**팀원 각자의 기피/선호 음식 정보를 미리 저장해두고, 모든 제약을 반영해 최적의 메뉴를 추천하는 서비스**를 만들고자 했습니다. 회식이나 미팅처럼 반복되는 상황에서도 매번
정보를 새로 입력하지 않아도 되고, 소모적인 논의 없이 빠르게 결정할 수 있습니다.

<br>

## 🤝 팀원 소개

### 프런트엔드 팀원

|                                                          머핀                                                          |                                                          수이                                                          |                                                          카멜                                                          |
|:--------------------------------------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------------------------------------:|
| <img src="https://github.com/user-attachments/assets/8ef4ddbd-d041-4f06-8d0d-134e8e8c5569" width="120" height="120"> | <img src="https://github.com/user-attachments/assets/2cadab23-4bb0-4cb9-9e02-1808843e834e" width="120" height="120"> | <img src="https://github.com/user-attachments/assets/2e1e32f8-30b2-4534-81d5-6424808aa40a" width="120" height="120"> |
|                                        [GitHub](https://github.com/minji2219)                                        |                                         [GitHub](https://github.com/shuyeon)                                         |                                       [GitHub](https://github.com/dev-dino22)                                        |

### 백엔드 팀원

|                                                          몽이                                                          |                                                         슬링키                                                          |                                                          에드                                                          |                                                          랜디                                                          |
|:--------------------------------------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------------------------------------:|
| <img src="https://github.com/user-attachments/assets/ead4bda5-6354-450f-9c48-31dfe336d919" width="120" height="120"> | <img src="https://github.com/user-attachments/assets/b9ce3e53-4b2a-4f43-93ad-d729c7b0a96c" width="120" height="120"> | <img src="https://github.com/user-attachments/assets/cee0b677-dcf7-41ce-ab98-5a344ed07adf" width="120" height="120"> | <img src="https://github.com/user-attachments/assets/7d93584e-1cbc-4f38-8eca-992fe981cf85" width="120" height="120"> |
|                                        [GitHub](https://github.com/wodnd0131)                                        |                                       [GitHub](https://github.com/supernovaMK)                                       |                                        [GitHub](https://github.com/jinu0328)                                         |                                         [GitHub](https://github.com/KJungW)                                          |

## 💬 PickEat만의 팀 문화: 같이 일하자, 웃으면서.

---

### 💼 “10시 출근”보다 “10시 소통 시작!”

- **10시 전에 소통할 준비를 모두 마쳐주세요**

---

### 🕙 고정 연락 시간: 오후 10시, 슬랙 집결!

- **공지방에 모여 대기시간 줄이자. 질문은 빠르게, 소통은 자주!**
- **동료의 시간도 나의 시간이다!**
- **할 말 없어도 읽었으면 체크 표시**

---

### ⏰ 일할 땐 열심히, 쉴 땐 확실히: 50분 집중 + 10분 리프레시

- **50분간 몰입, 10분은 과열 방지타임!**
- **시간은 정각 기준, 타이머 돌려요 ⏱️**
- **눈치는 일할 때만 보세요~ 😏**

---

### 🧠 트러블슈팅은 모두의 자산

- **에러는 공유하고, 해결도 공유!**
- **나만 알고 있으면 팀이 삐끗.**
- **"이건 내가 해결했어요" 👉🏻 “우리가 안 겪게 해줘서 고마워요!”**

---

### 💬 회의는 존댓말

- **존중이 기본, 편안함은 옵션**
- **뾰족한 말보단 둥근 태도. 서로의 아이디어를 부드럽게 다뤄요**

---

### 🐲 과열 방지 매뉴얼: "용용체" 발동!

- **논의가 뜨거워졌다면? "용용체~ 🐉"**
- **분위기 식히는 마법의 언어. 웃고 다시 집중!**

---

### 📌 오늘의 할 일 공유 → 오후 6시 진척도 체크

- **아침엔 “오늘 뭐할지”, 저녁엔 “뭐했는지”**
- **안 물어봐도 다 보여요. 투명한 협업의 시작!**

---

### 🧀 수요일 = 치즈 데이 (보드게임 타임!)

- **분위기 환기, 주중 리프레쉬, 아이스브레이킹, 전투력 회복!**
- **규칙: 이 날 아침은 즐거울 것**

---

### 🧩 우리만의 도메인 언어

- **같은 말을 써야 같은 마음이 된다!**
- **팀 안의 소통은 우리만의 단어로 유쾌하게**
- _오늘 돈까스는 좀 AF한데? → (대충 돈까스 먹기 싫다는 말)_

---
', 'https://github.com/woowacourse-teams/2025-pick-eat', NULL, '2025-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://raw.githubusercontent.com/woowacourse-teams/2025-routie/develop/frontend/src/assets/images/thumbnail.jpeg', 7, NULL, 'Routie', 'routie', 'Routie', '✨친구들과 함께하는 동선 계획, Routie✨', 11, '2026-08-07', 'CLOSED', 'APPROVED', '<img src="https://raw.githubusercontent.com/woowacourse-teams/2025-routie/develop/frontend/src/assets/images/thumbnail.jpeg" alt="thumbnail.jpeg">

<img src="https://raw.githubusercontent.com/woowacourse-teams/2025-routie/develop/assets/gifs/routie_thumbnail.gif" alt="routie_thumbnail.gif" width="100%" />

<br />

| 🔗 링크 공유                                                             | 📌 장소 추가                                                               |
| ------------------------------------------------------------------------ | -------------------------------------------------------------------------- |
| 링크를 공유해 하나의 작업 공간에서 같이 계획을 만들어보아요!             | 가고 싶은 장소를 검색한 후, 나만의 해시태그를 달아 장소를 추가해보세요!    |
| <img src="https://raw.githubusercontent.com/woowacourse-teams/2025-routie/develop/assets/gifs/link.gif" alt="link.gif" controls width="100%" /> | <img src="https://raw.githubusercontent.com/woowacourse-teams/2025-routie/develop/assets/gifs/place.gif" alt="place.gif" controls width="100%" /> |

| 👍 좋아요                                                                | 🗺️ 지도                                                                           |
| ------------------------------------------------------------------------ | --------------------------------------------------------------------------------- |
| 가고 싶은 장소에 대한 선호도를 좋아요 버튼으로 표현해보세요!             | 친구들과 함께 동선을 만들어보세요! 동선을 지도에서 시각적으로 확인할 수 있습니다. |
| <img src="https://raw.githubusercontent.com/woowacourse-teams/2025-routie/develop/assets/gifs/like.gif" alt="like.gif" controls width="100%" /> | <img src="https://raw.githubusercontent.com/woowacourse-teams/2025-routie/develop/assets/gifs/map.gif" alt="map.gif" controls width="100%" />            |

<br />

# 🛠️ 기술 스택

## 🌐 프론트엔드

<img src="https://raw.githubusercontent.com/woowacourse-teams/2025-routie/develop/assets/images/frontend-tech-stack.jpeg" alt="frontend-tech-stack.jpeg">

## 🔙🔚 백엔드

<img src="https://raw.githubusercontent.com/woowacourse-teams/2025-routie/develop/assets/images/backend-tech-stack.jpeg" alt="backend-tech-stack.jpeg">

# 아키텍처

## ci/cd

<img src="https://raw.githubusercontent.com/woowacourse-teams/2025-routie/develop/assets/images/ci-flow.png" alt="ci-flow.png">
<img src="https://raw.githubusercontent.com/woowacourse-teams/2025-routie/develop/assets/images/cd-flow.png" alt="cd-flow.png">

## 로깅 및 모니터링

<img src="https://raw.githubusercontent.com/woowacourse-teams/2025-routie/develop/assets/images/logging-architecture.png" width="100%" alt="logging-architecture.png">

## 🤼 팀원

### 프론트엔드

| <img src="https://github.com/aydenote.png" width="100" height="100"/> | <img src="https://github.com/ohgus.png" width="100" height="100"/> | <img src="https://github.com/AHHYUNJU.png" width="100" height="100"/> | <img src="https://github.com/jeongyou.png" width="100" height="100"/> |
| :-------------------------------------------------------------------: | :----------------------------------------------------------------: | :-------------------------------------------------------------------: | :-------------------------------------------------------------------: |
|              [앵버(최승수)](https://github.com/aydenote)              |             [오거스(오명석)](https://github.com/ohgus)             |              [주렁(주아현)](https://github.com/AHHYUNJU)              |              [기린(정유정)](https://github.com/jeongyou)              |

### 백엔드

| <img src="https://github.com/cookie-meringue.png" width="100" height="100"/> | <img src="https://github.com/threepebbles.png" width="100" height="100"/> | <img src="https://github.com/DongchannN.png" width="100" height="100"/> | <img src="https://github.com/goohong.png" width="100" height="100"/> |
| :--------------------------------------------------------------------------: | :-----------------------------------------------------------------------: | :---------------------------------------------------------------------: | :------------------------------------------------------------------: |
|              [머랭(김대현)](https://github.com/cookie-meringue)              |             [헤일러(민서현)](https://github.com/threepebbles)             |              [차니(이동찬)](https://github.com/DongchannN)              |              [대니(정구홍)](https://github.com/goohong)              |

<br />
', 'https://github.com/woowacourse-teams/2025-routie', NULL, '2025-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/823163a4-91c0-4c16-a4bc-3c98ae0dc063', 7, NULL, '토독토독', 'todok-todok', '토독토독', '📖 읽고 ✏️ 기록하고 💬 토론하자, 독서 기반 토론 서비스 “토독토독”', 23, '2026-08-07', 'CLOSED', 'APPROVED', '# 토독토독

<img width="1000" height="800" alt="스크린샷1" src="https://github.com/user-attachments/assets/823163a4-91c0-4c16-a4bc-3c98ae0dc063" />

<br>

## 📚 서비스 소개
> 독서 기반 토론 서비스

`개발자를 위한 필독서가 많은데 추천해주는 책을 읽어도 저자의 의도가 무엇인지 모르겠어요.`  
`다른 개발자들과 함께 스터디를 통해 책을 읽어도 우리가 깨달은 바가 옳은 방향인지 모르겠어요.`  

이런 의문을 갖는 개발자들이 자유롭게 토론할 수 있는 서비스입니다.  
읽은 책을 기반으로 자신의 의견을 표출하거나 다른 사람의 의견을 묻고, 검색해볼 수 있습니다.

<br>

## 👥 팀원 소개

### 🤖 Android
| <img src="https://avatars.githubusercontent.com/u/82762769?v=4.png" width="100" height="100"> | <img src="https://avatars.githubusercontent.com/u/192606356?v=4.png" width="100" height="100"> | <img src="https://avatars.githubusercontent.com/u/84930748?v=4.png" width="100" height="100"> |
|:---:|:---:|:---:|
| [동전](https://github.com/donghyun81) | [모찌](https://github.com/wondroid-world) | [페토](https://github.com/chanho0908) |

### ⚙️ Backend
| <img src="https://avatars.githubusercontent.com/u/77716414?v=4.png" width="100" height="100"> | <img src="https://avatars.githubusercontent.com/u/109019081?v=4.png" width="100" height="100"> | <img src="https://avatars.githubusercontent.com/u/113325033?v=4.png" width="100" height="100"> | <img src="https://avatars.githubusercontent.com/u/156290096?v=4.png" width="100" height="100"> |
|:---:|:---:|:---:|:---:|
| [듀이](https://github.com/ljhee92) | [링크](https://github.com/sonjh919) | [모다](https://github.com/Chaeyoung714) | [제프](https://github.com/horizonpioneer) |

- 토독토독 팀이 궁금하다면? 👉 [📝 토독토독 위키](https://github.com/woowacourse-teams/2025-Todok-Todok/wiki)
', 'https://github.com/woowacourse-teams/2025-Todok-Todok', NULL, '2025-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/8613bdbe-f377-41b7-a60a-2b8025d82da1', 7, NULL, '튜립', 'turip', '튜립', '✈️🧑‍🤝‍🧑 우리 같이 . . . 튜립 떠날래요 ? ', 27, '2026-08-07', 'CLOSED', 'APPROVED', '## 💚 튜립 다운받기 -> [다운로드 링크](https://play.google.com/store/apps/details?id=com.on.turip)

<img width="4096" height="2304" alt="git장표" src="https://github.com/user-attachments/assets/8613bdbe-f377-41b7-a60a-2b8025d82da1" />

# ✈️🧑‍🤝‍🧑 튜립 (Turip)

> 유튜버의 여행 정보를 한눈에 빠르게 확인할 수 있는 서비스


## 📝 프로젝트 소개
> 이 서비스는 유튜버가 실제 다녀온 여행 동선, 장소를 수집하고 영상과 함께 제공하는 앱입니다.  
사용자는 지역을 기준으로 여행 루트를 검색할 수 있으며, 원하는 장소를 내 폴더에 찜! 할 수 있습니다.
친구, 가족, 연인과 함께 떠날 장소를 모아서 공유까지 해보세요! 💙

|<img src="https://github.com/user-attachments/assets/ba8703c2-71e1-4a8c-ab88-c1332db23d66" width="320" />|<img src="https://github.com/user-attachments/assets/ed392815-74e7-49ea-89f8-ac056adda6ae" width="320" />|<img src="https://github.com/user-attachments/assets/c934ac59-df64-45be-8452-19664c3e08ab" width="320" />|
|:---------:|:---------:|:---------:|
|**홈 화면**|**검색 화면**|**여행 일정**|

|<img src="https://github.com/user-attachments/assets/7373639d-3b53-4aa8-bacd-d55750ca9422" width="320" />|<img src="https://github.com/user-attachments/assets/453ddede-5957-4da8-877f-fc0e49a73e51" width="320" />|
|:---------:|:---------:|
|**장소 찜**|**콘텐츠 찜**|

## 인프라 아키텍처
<img width="808" height="912" alt="image" src="https://github.com/user-attachments/assets/9fb99fb9-1ca6-4321-9f53-5fed7e98d751" />


## CI/CD 
<img width="1894" height="970" alt="image" src="https://github.com/user-attachments/assets/dcf4b005-a4bd-4c60-a66e-375075cc9103" />


## 모니터링
<img width="914" height="786" alt="image" src="https://github.com/user-attachments/assets/54af9403-3c78-4951-adc6-673101133e61" />


## 👥 팀원

|<img src="https://avatars.githubusercontent.com/u/114990782?v=4" width="125" />|<img src="https://avatars.githubusercontent.com/u/183526990?v=4" width="125" />|<img src="https://avatars.githubusercontent.com/u/171224212?v=4" width="125" />|<img src="https://avatars.githubusercontent.com/u/183483852?v=4" width="125" />|<img src="https://avatars.githubusercontent.com/u/121426422?v=4" width="125" />|<img src="https://avatars.githubusercontent.com/u/86725408?v=4" width="125" />|
|:---------:|:---------:|:---------:|:---------:|:---------:|:---------:|
|[뭉치(손명지)](https://github.com/m6z1)|[제리(조현석)](https://github.com/jerry8282)|[채넛(장유범)](https://github.com/yrsel)|[라젤(문선영)](https://github.com/RaZel713)|[메이(김시원)](https://github.com/seaniiio)|[하루(구은선)](https://github.com/eunseongu)|
|Android|Android|Android|Backend|Backend|Backend|
', 'https://github.com/woowacourse-teams/2025-Turip', NULL, '2025-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/medAndro.png', 7, NULL, '야구보구', 'yagu-bogu', '야구보구', '⚾ 야구보구 🏟️ 인증하구 📊 통계보구 💬 얘기하구', 26, '2026-08-07', 'CLOSED', 'APPROVED', '# ⚾️ 야구보구 👀

## 📍 서비스 주제

야구 팬의 활동을 수치화해 성취감과 경쟁심을 자극하고, 직관의 순간을 기록해 팬심을 더욱 뜨겁게 만들어주는 서비스

## 👥 팀원 소개

|<img src="https://github.com/medAndro.png" width="125" />|<img src="https://github.com/ijh1298.png" width="125" />|<img src="https://github.com/jiyuneel.png" width="125" />|<img src="https://github.com/jjunh0.png" width="125" />|<img src="https://github.com/Starlight258.png" width="125" />|<img src="https://github.com/bowook.png" width="125" />|<img src="https://github.com/nourzoo.png" width="125" />|
|:---------:|:---------:|:---------:|:---------:|:---------:|:---------:|:---------:|
|[메다(장지형)](https://github.com/medAndro)|[크림(임준혁)](https://github.com/ijh1298)|[포르(이지윤)](https://github.com/jiyuneel)|[두리(김준호)](https://github.com/jjunh0)|[밍트(김명지)](https://github.com/Starlight258)|[우가(민보욱)](https://github.com/bowook)|[포라(이승연)](https://github.com/nourzoo)|
|Android|Android|Android|Backend|Backend|Backend|Backend|


## 💫 규칙들

### **🧑‍🤝‍🧑 TEAM CULTURE**

- **🐲 친절하게 말해주세용**

  → 논의가 과열되었을 때 용용체로 대화해용

- 🙉 **너의 목소리가 안 들려**

  → 무엇이든지 말해주세요

- **🍗 반마리보다 🐓한마리예요**

  → 회의는 존댓말로 진행해요

- **🤗 되면 대면해요**

  → 대면 협업을 우선해요


---

### **⏰ WORKFLOW**

- **📅 데일리 스크럼: 데일리 미팅 끝나고 바로**

  → 어제 한 일과 오늘 할 일을 공유해요

- 🍚 **수요일 == 외식 데이**

  → 다 같이 밥 먹어요


---

### **🗣️ COMMUNICATION**

- 🤔 **설득하거나 설득 당하거나**

  → 논의할 때 최소한의 입장을 가져요

- ✋ **세상에서 바보같은 질문은 없다**

  → 묻지 않고 넘기는 게 더 위험해요

- ⌛ **일과 시간은 팀과 시간을**

  → 일과 시간은 팀과 함께 움직여요

- ❤️ **좋아요가 좋아요**

  → 긍정적인 리액션이 좋아요


---

### **📚 KNOWLEDGE SHARING**

- ⬆️ **우리 함께 레벨업**

  → 알게 된 지식은 혼자 갖지 말고 공유해요

- 🌱 **기록하면 새록새록**

  → 모든 일을 기록해요
', 'https://github.com/woowacourse-teams/2025-yagu-bogu', NULL, '2025-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/2b795599-57c7-4fc6-8000-7d2789f84e49', 6, NULL, '총대마켓', 'chongdae-market', '총대마켓', '더 저렴하게, 원하는 만큼만, 내가 직접 만드는 공동구매 - 총대마켓 ⛳️', 23, '2026-08-07', 'CLOSED', 'APPROVED', '# 공동구매를 보다 쉽게, 총대마켓 ⛳️

더 저렴하게, 원하는 만큼만, 내가 직접 만드는 공동구매

<img width="1000" alt="image" src="https://github.com/user-attachments/assets/2b795599-57c7-4fc6-8000-7d2789f84e49">

### 팀블로그 🖌️

https://chongdae.oopy.io/

### PlayStore 🎁

https://play.google.com/store/apps/details?id=com.zzang.chongdae

## **Contributors**

<table align="center">
  <tr>
    <td align="center">Android</td>
    <td align="center">Android</td>
    <td align="center">Android</td>
  </tr>
  <tr>
    <td>
      <a href="https://github.com/chaehyuns">
        <img src="https://avatars.githubusercontent.com/u/80222352?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
    <td>
      <a href="https://github.com/Namyunsuk">
        <img src="https://avatars.githubusercontent.com/u/84739562?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
    <td>
      <a href="https://github.com/songpink">
        <img src="https://avatars.githubusercontent.com/u/138569524?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
  <tr>
  <tr>
    <td align="center">
      <a href="https://github.com/chaehyuns">채채</a>
    </td>
    <td align="center">
      <a href="https://github.com/Namyunsuk">서기</a>
    </td>
    <td align="center">
      <a href="https://github.com/songpink">알송</a>
    </td>
  </tr>
</table>
<table align="center">
  <tr>
    <td align="center">Backend</td>
    <td align="center">Backend</td>
    <td align="center">Backend</td>
    <td align="center">Backend</td>
  </tr>
  <tr>
    <td>
      <a href="https://github.com/masonkimseoul">
        <img src="https://avatars.githubusercontent.com/u/87306418?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
    <td>
      <a href="https://github.com/helenason">
        <img src="https://github.com/user-attachments/assets/14f51a8f-2e9a-42a2-9fdc-dbdd94ce4e65" width="150" style="max-width: 100%;">
      </a>
    </td>
    <td>
      <a href="https://github.com/fromitive">
        <img src="https://avatars.githubusercontent.com/u/46563149?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
    <td>
      <a href="https://github.com/ChooSeoyeon">
        <img src="https://avatars.githubusercontent.com/u/83302344?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
  <tr>
  <tr>
    <td align="center">
      <a href="https://github.com/masonkimseoul">메이슨</a>
    </td>
    <td align="center">
      <a href="https://github.com/helenason">에버</a>
    </td>
    <td align="center">
      <a href="https://github.com/fromitive">포케</a>
    </td>
    <td align="center">
      <a href="https://github.com/ChooSeoyeon">도라</a>
    </td>
  </tr>
</table>

# 서비스 소개

## 📑 목적 및 필요성

부담스러운 대량구매가 망설여지시나요? 대용량으로 사면 싼 물품을 소량 구매하기 위해선 왜 비싸게 사야 하는지 억울했던 적이 없으신가요. 때로는 1개만 사고 싶을 때 비싼 배송비가 부담된다는 생각이 들 때도 있을
겁니다. 이럴 때 같이 물건을 구매할 사람들이 있다면 얼마나 좋을까요?

예를 들어, 낱개로 사면 2,000원에 판매되는 양말이, 10개 단위로 사면 개당 890원에 판매된다고 상상해보세요. 여러분은 정말 10켤레를 모두 구매하실 건가요? 공동구매를 통해 잠깐의 직거래를 한다면 더는 우리
집 서랍은 꽉 차있지 않아도 돼요. 공동구매를 통해 여러분은 원하는 개수만큼 양말을 대량 구매할 때의 저렴한 가격으로 구매할 수 있습니다.

하지만 이런 공동 구매를 진행하기 위해 사람을 모으고 물건을 나누는 것이 처음에는 어려울 수 있습니다. 가까이 사는 지인 만으로 공동 구매를 진행할 인원을 충당한다는 게 어려운 일이죠. 총대마켓은 여러분의 공동
구매를 돕습니다. 총대마켓은 지역 기반으로 공동 구매에 관심 있는 사용자들을 한 곳에 모으고, 더 쉽고 빠르게 공동구매를 진행하거나 참여할 수 있습니다.

총대마켓 서비스를 사용해보세요. 구매하고 싶은 물건을 검색해보세요.

그리고 여러분과 같은 바람을 가진 사람들을 찾아보세요.

## 📑 **문제 정의와 솔루션**

우리는 종종 저렴한 구매를 위한 대량 구매를 진행할 때 불편함을 느낍니다. 이 불편함을 공동구매로 해결하기 위해 총대마켓 서비스가 탄생합니다.

- **Problems**
    - 소규모 구매자의 불편함 : 우리는 원하는 제품을 구매할 때, 도매나 대량 구매에서의 할인을 놓칩니다.
    - 공동구매 진행의 어려움 : 공동구매를 희망해도 실제로 거래를 진행하기 어렵습니다. 사람들을 모으는 데에 시간이 걸리기 때문이죠.
- **Solution**
    - 공동구매 진행 시스템 도입 : 총대마켓에서는 구매자 혹은 참여자로서 공동구매에 참여할 수 있습니다. 공동구매를 통해 구매자 간의 연결을 쉽게 하고, 소규모 구매자들이 대량 구매의 혜택을 누릴 수 있게
      돕습니다.

## **📑** 사용자는 누구인가?

- **Target**
    - 소매 소비자 : 저렴하게 적은 개수의 물품을 구매하고 싶지만 많은 비용을 지불하는 것은 어려운 소비자
    - 공동구매 진행자 : 원하는 상품에 대해 공동구매를 진행하고 싶은 소비자
- **Needs**
    - 소매 소비자: 공동구매 참여, 원하는 상품 검색, 공동구매 진행 상황 정보
    - 공동구매 진행자: 공동구매 모집 정보 게시, 불량 참여자 관리

## 📑 핵심 기능

| 기능                  | 설명                                                                       |
|---------------------|--------------------------------------------------------------------------|
| 공동구매 게시글 작성         | 공동구매 게시글을 작성한다.                                                          |
| 공동구매 게시글 목록 조회      | 작성한 게시글의 목록을 조회한다.                                                       |
| 공동구매 게시글 검색         | 검색어를 입력하여 검색어가 포함된 제목 또는 지역명을 가진 게시글 목록을 보여준다.                           |
| 공동구매 게시글 상세 조회      | 작성한 게시글을 조회한다.                                                           |
| 공동구매 게시글 수정         | 작성한 게시글을 수정한다.                                                           |
| 공동구매 게시글 삭제         | 작성한 게시글을 삭제한다.                                                           |
| 공동구매 참여             | 마감시간이 지나지 않고, 구매 확정되지 않은 공동구매 게시글에 참여할 수 있다. 게시글 참여와 동시에 공동구매 채팅방에 참여한다. |
| 공동구매 참여자간 채팅 메세지 작성 | 채팅방에 메세지를 작성한다. 채팅방에서 다른 참여자가 작성한 메세지를 확인할 수 있다.                         |
| 채팅 알림               | 채팅방에 메시지가 올라올 경우 알림이 온다.                                                 |

# Android Tech Stack
<img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=Kotlin&logoColor=white"><img src="https://img.shields.io/badge/Android Studio-3DDC84?style=for-the-badge&logo=AndroidStudio&logoColor=white"><img src="https://img.shields.io/badge/Jetpack-4285F4?style=for-the-badge&logo=Android&logoColor=white"><img src="https://img.shields.io/badge/Retrofit2-CC0000?style=for-the-badge&logo=Retrofit2&logoColor=white"><img src="https://img.shields.io/badge/Glide-4285F4?style=for-the-badge&logo=Glide&logoColor=white">
<img src="https://img.shields.io/badge/Room-007396?style=for-the-badge&logo=SQLite&logoColor=white"><img src="https://img.shields.io/badge/Lottie-F24E1E?style=for-the-badge&logo=Lottie&logoColor=white"><img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=Firebase&logoColor=white"><img src="https://img.shields.io/badge/Play%20Console-34A853?style=for-the-badge&logo=GooglePlay&logoColor=white"><img src="https://img.shields.io/badge/LeakCanary-4285F4?style=for-the-badge&logo=LeakCanary&logoColor=white">
<img src="https://img.shields.io/badge/Coroutines-4285F4?style=for-the-badge&logo=kotlin&logoColor=white"><img src="https://img.shields.io/badge/LiveData-34A853?style=for-the-badge&logo=android&logoColor=white"><img src="https://img.shields.io/badge/MockK-FF5722?style=for-the-badge&logo=mockk&logoColor=white"><img src="https://img.shields.io/badge/MVVM-4285F4?style=for-the-badge&logo=android&logoColor=white"><img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white"> 
<img src="https://img.shields.io/badge/JUnit-25A162?style=for-the-badge&logo=JUnit5&logoColor=white"><img src="https://img.shields.io/badge/Espresso-4CAF50?style=for-the-badge&logo=android&logoColor=white">

### 기술 선정 이유와 과정

[📱 안드로이드 기술 스택](https://www.notion.so/a98fafe408204ffbac5f6f0c6ab83d08?pvs=21)


# Backend Tech Stack


<img src="https://img.shields.io/badge/Java17-000000?style=for-the-badge&logo=java&color=F40D12"><img src="https://img.shields.io/badge/Spring_Boot_3-0?style=for-the-badge&logo=spring-boot&logoColor=white&color=%236DB33F"><img src="https://img.shields.io/badge/apache tomcat-F8DC75?style=for-the-badge&logo=apachetomcat&logoColor=white"><img src="https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonwebservices&logoColor=white"><img src="https://img.shields.io/badge/MySQL_8-0?style=for-the-badge&logo=mysql&logoColor=white&color=4479A1">
<img src="https://img.shields.io/badge/Hibernate-0?style=for-the-badge&logo=hibernate&logoColor=white&color=%2359666C"><img src="https://img.shields.io/badge/Amazon_EC2-0?style=for-the-badge&logo=amazon-ec2&logoColor=white&color=%23FF9900"><img src="https://img.shields.io/badge/Amazon_CloudWatch-0?style=for-the-badge&logo=amazon-cloudwatch&logoColor=white&color=%23FF4F8B"><img src="https://img.shields.io/badge/OAuth2-0?style=for-the-badge&logo=auth0&logoColor=white&color=%23000000"><img src="https://img.shields.io/badge/Gradle-0?style=for-the-badge&logo=gradle&logoColor=white&color=%2302303A">
<img src="https://img.shields.io/badge/Swagger-0?style=for-the-badge&logo=Swagger&logoColor=white&color=%2385EA2D"><img src="https://img.shields.io/badge/GitHub%20Actions-0?style=for-the-badge&logo=GitHub%20Actions&logoColor=white&color=%232088FF"><img src="https://img.shields.io/badge/Docker-0?style=for-the-badge&logo=docker&logoColor=white&color=2496ED"><img src="https://img.shields.io/badge/Amazon Elastic Load Balancing-0?style=for-the-badge&logo=awselasticloadbalancing&logoColor=white&color=8C4FFF"><img src="https://img.shields.io/badge/Amazon RDS-0?style=for-the-badge&logo=amazonrds&logoColor=white&color=527FFF"><img src="https://img.shields.io/badge/JUnit-25A162?style=for-the-badge&logo=JUnit5&logoColor=white">


# **소프트웨어 아키텍처**

# **📜 ERD 설계도**

![image](https://github.com/user-attachments/assets/6136f2c3-7029-4dfd-ac05-b40f7c98ca62)

# 👨‍👩‍👧‍👧팀 협업 규칙

## **📑 그라운드 룰**

### **1. 말(자원)은 모두가 공평하게**

**모두가 함께 성장**하기 위해 일부의 의견만 듣고 움직이기보단 **모두가 비슷한 양의 의견**을 내도록 합니다. 회의마다 슬랙 스레드를 관리해 말하고 싶거나 말이 너무 길어질 때 표현하는 수단을 마련합니다. 회의
내 deadlock과 starvation을 방지하고, **fair scheduling**을 추구합니다.

### **2. 기록은 언제나**

**모든 회의는 문서화**해 우리 팀의 역사를 기록합니다. 회의 외의 시간에 대화한 내용은 **슬랙으로 간단히 문서화**해 모든 팀원이 공유받을 수 있도록 합니다.

### **3. 회고는 충분히**

격주 금요일마다 **그라운드 룰, 개발 페어** 등에 대한 KPT 형식의 회고를 **매번 진행**합니다. 회고 시엔 날카로워지지 않게 쿠션어를 사용합니다. 피드백은 언제나 **칭찬 사이**에 배치합니다.

### **4. 식사를 함께**

매주 수요일마다 식사데이를 가집니다. 식사 후엔 **비개발 페어**를 맺어 **서로를 챙겨주며** 신뢰를 다집니다.

### **5. 칭찬은 확실하게**

아낌없는 칭찬으로 **서로가 최고**라는 생각을 가지게 되는 문화를 지향합니다. 칭찬은 총대를 춤추게 만들고, **불신을 신뢰로 전환**합니다.

## **📑 커밋 컨벤션**

[팀 협업 규칙 정하기](https://www.notion.so/18313380607140eca23253dfb5facf71?pvs=21)
', 'https://github.com/woowacourse-teams/2024-chongdae-market', NULL, '2024-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/f47b6023-fc55-4072-92b4-0551135a95d0', 6, NULL, '땅콩', 'ddangkong', '땅콩', '심심풀이 땅콩처럼 가벼운 주제로 친구들과 즐기는 단체 대화주제 제공 서비스 🥜', 53, '2026-08-07', 'CLOSED', 'APPROVED', '<div align="center">
  <h3><a href="https://ddangkong.kr/">땅콩 바로가기</a></h3>
 <img src="https://github.com/user-attachments/assets/f47b6023-fc55-4072-92b4-0551135a95d0" width="100" height="150" />
 <h4>심심풀이 땅콩처럼 가벼운 주제로 친구들과 즐기는 단체 대화주제 제공 서비스 🥜</h4>
</div>

# 서비스 소개

`심심풀이 땅콩` 이라는 말을 들어보신 적 있으신가요? 이 표현은 일부러라도 먹는 일을 만들어 무료함을 잊으려는 심리에서 유래되었습니다. 이처럼, 땅콩 서비스는 **가볍게 대화를 시작하고 이어나가고자 하는 사람들을 위해 만들어졌습니다.**

함께 모인 자리에서 대화 주제가 부족하진 않으신가요? 친해지고 싶은 사람들과 더 많은 대화를 나누고 싶지는 않으신가요?
대화를 하다보면 어색한 침묵이 흐르는 경우가 있습니다. 땅콩은 음식, MBTI, 연애, 만약에 등 `다양한 카테고리의 대화 주제를 제공` 하여 이를 해결하려고 합니다.

> [카테고리별 질문 예시]
>
> `음식` : 개구리 맛 초콜릿 vs 초콜릿 맛 개구리
>
> `MBTI` : 내가 ''이번주 토요일에 뭐해?''라고 물어볼 때는 : 약속을 잡으려고 물어본다 vs 단지 뭐하는지 궁금해서 물어본다
>
> `연애` : 외모 빼고 모든 것이 안 맞는 애인 vs 외모 빼고 모든 것이 잘 맞는 애인
>
> `만약에` : 전애인 친구랑 사귀기 vs 친구 전애인이랑 사귀기

이 주제들을 통해 서로의 생각을 공유하고, 자연스럽게 즐거운 대화를 이어나갈 수 있습니다. 땅콩과 함께라면, 더 이상 대화의 시작을 고민하지 않아도 됩니다. 누구나 쉽고 즐겁게 소통할 수 있는 기회를 제공하는 땅콩, **지금 당장 회원가입 없이 [땅콩](https://ddangkong.kr/)을 사용해보세요.**

# 주요 기능 소개

### 1. 다양한 대화 주제 제공

![대화주제_제공](https://github.com/user-attachments/assets/cfe966ea-698f-42b6-94f5-628d4e15e0e3)

### 2. `실시간 데이터 통신` 을 통해 시끄러운 오프라인 공간에서도 `멀티 플레이` 로 재밌게 즐길 수 있음

![멀티플레이](https://github.com/user-attachments/assets/046422a7-a389-4469-b8f0-cb72339946e9)

### 3. 게임 내에서 나와 같은 선택을 한 사람과의 `매칭도 순위` 를 통해 공감대 형성

![매칭도](https://github.com/user-attachments/assets/019f0779-df57-444b-842a-37fe6d970e07)

# 인프라

<img src="https://github.com/user-attachments/assets/d010f50c-4662-40ac-b06b-46b1b2b2fadf" width="400"  height="600"/>

# 팀원 소개

### 프론트엔드

| <img src="https://avatars.githubusercontent.com/u/63959171?v=4" width="100" height="100"/> | <img src="https://avatars.githubusercontent.com/u/74897720?v=4" width="100" height="100"/> | <img src="https://avatars.githubusercontent.com/u/111696934?v=4" width="100" height="100"/> |
| :----------------------------------------------------------------------------------------: | :----------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------: |
|                            [마루](https://github.com/rbgksqkr)                             |                             [썬데이](https://github.com/useon)                             |                            [포메](https://github.com/novice0840)                            |

### 백엔드

| <img src="https://avatars.githubusercontent.com/u/84304802?v=4" width="100" height="100"/> | <img src="https://avatars.githubusercontent.com/u/44027393?v=4" width="100" height="100"/> | <img src="https://avatars.githubusercontent.com/u/78288539?v=4" width="100" height="100"/> | <img src="https://avatars.githubusercontent.com/u/101033262?v=4" width="100" height="100"/> |
| :----------------------------------------------------------------------------------------: | :----------------------------------------------------------------------------------------: | :----------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------: |
|                             [이든](https://github.com/PgmJun)                              |                           [커찬](https://github.com/leegwichan)                            |                            [타칸](https://github.com/jhon3242)                             |                             [프린](https://github.com/GIVEN53)                              |

# 그라운드 룰

<img width="800" alt="image" src="https://github.com/user-attachments/assets/cb7a1016-e7be-4cec-b4eb-39327eef3ef9">
', 'https://github.com/woowacourse-teams/2024-ddangkong', NULL, '2024-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/329eb4ec-8d5c-4f73-93c9-bdca475dedb2', 6, NULL, '데벨업', 'devel-up', '데벨업', '나 혼자만 레벨업? 다 같이 데벨업!', 22, '2026-08-07', 'CLOSED', 'APPROVED', '# 🚀 데벨업

## 💡서비스 소개

개발 공부를 하면서 내가 잘 성장하고 있는지 불안할 때가 많지 않으셨나요?   
기업의 채용 형태가 다양해지고, 요구하는 역량의 기준이 높아져 취업 준비 과정에서 어려움을 겪는 분들이 많습니다.
이 문제를 해소하기 위해서는 역량을 판단할 수 있는 기준점을 마련하고, 다른 사람으로부터 피드백을 받으며 성장의 방향성을 잡아가는 것이 중요합니다.
하지만 경험이 부족한 취업 준비생으로서는 기준점을 판단하기 어렵고 피드백을 주고받을 동료나 선배가 없는 경우가 많습니다.   
그래서 저희는 **개발자 취업 준비생을 위한 상호 성장 플랫폼, ''데벨업''** 을 만들었습니다.

사용자는 데벨업에서 자신의 수준에 맞는 과제를 풀어 제출합니다.
제출된 과제는 자동으로 데벨업 홈페이지에 등록되고, 등록된 게시물을 바탕으로 다른 개발자들과 피드백을 주고받을 수 있게 됩니다.
또한 디스커션 게시판을 통해 과제를 풀며 생긴 궁금증과 고민 사항을 나누며 해소할 수 있습니다.
이를 통해 다양한 접근 방식을 학습하며 자신의 강점과 보완할 점을 알 수 있습니다. 그 과정에서 성장의 동료를 만날 기회도 얻을 수 있습니다.

어려워진 취업 시장에서 홀로 고민하고 헤매느라 고생 많으셨습니다. 이제는 혼자 걱정하지 마세요.   
함께 도전하고, 함께 성장하며, 더 큰 목표를 향해 나아가세요. 여러분의 성장 여정을 돕기 위해 데벨업이 있습니다.   
데벨업과 함께라면 가능합니다. 데벨업에서 레벨업하세요!

<br/>

### [Devel Up 바로가기](https://www.devel-up.co.kr/)

<br/>

![github1](https://github.com/user-attachments/assets/329eb4ec-8d5c-4f73-93c9-bdca475dedb2)
![github3](https://github.com/user-attachments/assets/82ec6c77-756d-4525-bd6f-6581f9f81243)
![github2](https://github.com/user-attachments/assets/237f8b74-51eb-4fc6-bc96-5a6627115467)
![github4](https://github.com/user-attachments/assets/ab68e53e-8c19-42a4-aff7-7ad6822f0dfb)
![github5](https://github.com/user-attachments/assets/d20dc3be-21db-442d-8a60-8fe0691c41f0)
![github6](https://github.com/user-attachments/assets/72b45dc6-673c-4630-9772-a65c658a7ccf)


## 기술 스택

### Frontend
![skills_frontend](https://github.com/user-attachments/assets/cd84c89b-0c55-442d-8547-1fb38e9ea304)

### Backend
![skills_backend](https://github.com/user-attachments/assets/72e7ea20-4679-4d8d-aadd-029404a10702)

## CI/CD 파이프라인

### Frontend CI/CD
![프론트엔드 CI/CD](https://github.com/user-attachments/assets/955be8c0-b6a1-4a9c-940a-0651dd7ffa7d)

### Backend CI/CD
![백엔드 CI/CD](https://github.com/user-attachments/assets/7fdf5fa3-768c-431e-a906-b74a252b1209)

## 서비스 아키텍처

![서비스 아키텍처](https://github.com/user-attachments/assets/db3dbc45-cf94-4673-be4b-993957643071)


## 👤멤버들

### 프론트엔드

| <img src="https://avatars.githubusercontent.com/u/109535991?v=4" width="130" height="130"> | <img src ="https://avatars.githubusercontent.com/u/80797824?v=4" width="130" height="130"> | <img src ="https://avatars.githubusercontent.com/u/121149171?v=4" width="130" height="130"> |
| :-: | :-: | :-: |
| [버건디][버건디 깃허브] | [라이언][라이언 깃허브] | [프룬][프룬 깃허브] |

### 백엔드

| <img src="https://avatars.githubusercontent.com/u/131349867?v=4" width="130" height="130"> | <img src="https://avatars.githubusercontent.com/u/140090179?v=4" width="130" height="130"> | <img src="https://avatars.githubusercontent.com/u/45223837?v=4" width="130" height="130"> | <img src="https://avatars.githubusercontent.com/u/39932141?v=4" width="130" height="130"> | <img src="https://avatars.githubusercontent.com/u/75781414?v=4" width="130" height="130"> |
| :-: | :-: | :-: | :-: | :-: |
| [리브][리브 깃허브] | [릴리][릴리 깃허브] | [로빈][로빈 깃허브] | [아톰][아톰 깃허브] | [구름][구름 깃허브] |

[버건디 깃허브]: https://github.com/brgndyy
[라이언 깃허브]: https://github.com/Parkhanyoung
[프룬 깃허브]: https://github.com/chosim-dvlpr
[리브 깃허브]: https://github.com/Minjoo522
[릴리 깃허브]: https://github.com/lilychoibb
[로빈 깃허브]: https://github.com/robinjoon
[아톰 깃허브]: https://github.com/le2sky
[구름 깃허브]: https://github.com/alstn113


### [Devel Up 바로가기](https://www.devel-up.co.kr/)
', 'https://github.com/woowacourse-teams/2024-devel-up', NULL, '2024-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/9613338d-7a5d-4306-9fed-656574fbe929', 6, NULL, '반갑개', 'friendogly', '반갑개', '강아지 사회화 장려 앱 반갑개 🐶', 22, '2026-08-07', 'CLOSED', 'APPROVED', '## 반갑개를 소개합니다!
<p align="middle">
  <img width="200px;" src="https://github.com/user-attachments/assets/9613338d-7a5d-4306-9fed-656574fbe929"/>
</p>

<p align="middle">
<b>"반려견도 사람처럼 사회적인 동물입니다."</b>
</p>

<p align="middle">
가족같은 강아지에게 친구를 만들어주고 싶으신가요?<br />
매일 하는 산책이 일처럼 느껴지지는 않으신가요?<br />
<b>그렇다면 강아지의 행복을 위한 친구 찾기 서비스, ‘반갑개’를 이용해보세요!</b>
</p>

<p align="middle">
반갑개를 통해 <b>오늘 산책할 장소에 어떤 강아지들이 올지 확인할 수 있어요.</b><br />
우리 강아지가 좋아하는 친구를 언제 만나볼 수 있는지 확인해보세요.
</p>

<p align="middle">
더욱 많은 강아지를 만나고 싶다면 먼저 신호를 보내보세요.<br />
내 강아지 정보와 산책 일정을 공유하고, 다른 강아지들이 우리 강아지에게 먼저 인사를 건네줄 수 있어요.
</p>

<p align="middle">
<b>반갑개를 통해 우리 동네 강아지들을 만나고, 반갑게 인사해보세요!</b>
</p>

## 팀원 
<table align="center">
  <tr>
    <td align="center">Android</td>
    <td align="center">Android</td>
    <td align="center">Android</td>
    <td align="center">Android</td>
  </tr>
  <tr>
    <td>
      <a href="https://github.com/jinuemong">
        <img src="https://avatars.githubusercontent.com/u/85734140?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
    <td>
      <a href="https://github.com/gaeun5744">
        <img src="https://avatars.githubusercontent.com/u/92314556?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
    <td>
      <a href="https://github.com/junjange">
        <img src="https://avatars.githubusercontent.com/u/69571848?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
    <td>
      <a href="https://github.com/dpcks0509">
        <img src="https://avatars.githubusercontent.com/u/102402485?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
  <tr>
  <tr>
    <td align="center">
      ☃️<a href="https://github.com/jinuemong">누누</a>
    </td>
    <td align="center">
      🌟<a href="https://github.com/gaeun5744">벼리</a>
    </td>
    <td align="center">
      🦊<a href="https://github.com/junjange">에디</a>
    </td>
    <td align="center">
      🐶<a href="https://github.com/dpcks0509">채드</a>
    </td>
  </tr>
</table>
<table align="center">
  <tr>
    <td align="center">Backend</td>
    <td align="center">Backend</td>
    <td align="center">Backend</td>
    <td align="center">Backend</td>
  </tr>
  <tr>
    <td>
      <a href="https://github.com/ehtjsv2">
        <img src="https://avatars.githubusercontent.com/u/79188587?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
    <td>
      <a href="https://github.com/J-I-H-O">
        <img src="https://avatars.githubusercontent.com/u/110461155?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
    <td>
      <a href="https://github.com/jimi567">
        <img src="https://avatars.githubusercontent.com/u/28584160?s=400&u=fec19721dfc3f34b532b8dd638f8b091dee8805f&v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
    <td>
      <a href="https://github.com/takoyakimchi">
        <img src="https://avatars.githubusercontent.com/u/37261785?v=4" width="150" style="max-width: 100%;">
      </a>
    </td>
  <tr>
  <tr>
    <td align="center">
      🦤<a href="https://github.com/ehtjsv2">도도</a>
    </td>
    <td align="center">
      ☠️<a href="https://github.com/J-I-H-O">땡이</a>
    </td>
    <td align="center">
      🚬<a href="https://github.com/jimi567">위브</a>
    </td>
    <td align="center">
      😼<a href="https://github.com/takoyakimchi">트레</a>
    </td>
  </tr>
</table>

<br>

## 핵심기능
|강아지 놀이터 기능|동네 강아지 모임 기능|
|:---:|:---:|
|![반갑개 놀이터영상](https://github.com/user-attachments/assets/dc9ffe57-726c-4c0a-8ebf-8da1fd2b2e8d)|![반갑개 모임영상](https://github.com/user-attachments/assets/6577798a-efd1-4c99-b620-da678fa02f62)|
|미리 주변 강아지 산책정보를 확인하고 산책을 나가보세요!|우리 동네의 여러 강아지 모임을 찾아보고 이야기를 나눠요!|

## 백엔드
### 기술스택
* JAVA, SpringBoot, SpringDataJpa, Junit5, MySQL, H2
* Prometheus, GrafanaLoki, Grafana, GitHubAction, SpringRestDocs, Swagger
* AWS EC2, S3, RDS, ALB
* FCM
### 인프라 아키텍처
![반갑개 백엔드 인프라아키텍처](https://github.com/user-attachments/assets/1095da42-2875-4212-b7bd-26d8e7208036)
', 'https://github.com/woowacourse-teams/2024-friendogly', NULL, '2024-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/9e51f7a3-0326-4c06-8b03-65aca574c10c', 6, NULL, '행동대장', 'haeng-dong', '행동대장', '👑 행동대장들의 행복한 행사 진행을 위해... 📢 행동개시!', 45, '2026-08-07', 'CLOSED', 'APPROVED', '# 행동대장들의 정산을 간편하게💰행동대장

![service introduce](https://github.com/user-attachments/assets/9e51f7a3-0326-4c06-8b03-65aca574c10c)

### 인프라
![infra](https://github.com/user-attachments/assets/c89bcedf-dee1-4c02-a3df-249e112186f6)


### Backend CI/CD 파이프라인
![backend drawio](https://github.com/user-attachments/assets/3e0d414e-b5cd-4f13-a334-3a26b5c942aa)

### Frontend CI/CD 파이프라인
![front drawio](https://github.com/user-attachments/assets/fc924c43-ea3a-47e3-b455-310afad1e61e)
', 'https://github.com/woowacourse-teams/2024-haeng-dong', NULL, '2024-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/1dd61a2f-0e07-4906-b01d-6201506cd41a', 6, NULL, 'Pokerogue Helper', 'pokerogue-helper', 'Pokerogue Helper', '포켓몬 배틀 도우미, Pokerogue Helper 🎮', 20, '2026-08-07', 'CLOSED', 'APPROVED', '#  Introduction

🍄 `PokéRogue` 게임 유저가 `PokéRogue Helper` 서비스를 통해 포켓몬 배틀을 온전히 즐길수 있도록 도와주는 `Application`
<br>
<br>
<img src="https://github.com/user-attachments/assets/1dd61a2f-0e07-4906-b01d-6201506cd41a" width="450" height="215">
<img src="https://github.com/user-attachments/assets/37cbf9b8-06a3-4d70-b608-8db113469be1" width="230" height="230">

<br>
<br>

😨 1302마리의 포켓몬을 마주하기가 막막하지 않나요?

😨 설마 아직도 낡은 상성표를 찾아 보고 계신가요?

😨 무슨 기술을 써야 이길 수 있을까? 타입 상성이 뭔데?

<br>


✨ 배틀 도우미: 내 포켓몬의 기술이 상대 포켓몬에 얼마나 타격을 주는지 수치로 알 수 있습니다.

✨ 타입 매칭: 내 포켓몬과 상대 포켓몬의 상성을 쉽고 빠르게 알 수 있습니다.

✨ 포켓몬 도감: 특성, 종족값, 진화 정보, 출현 바이옴 같이 배틀에 유용한 정보를 자세하게 알 수 있습니다.

✨ 바이옴 도감: 포켓몬의 출현 정보와 다음 바이옴을 알 수 있습니다.

✨ 특성 도감: 특성 정보와 특성에 해당하는 모든 포켓몬을 알려줍니다.

이제 포켓로그 배틀의 고수가 되어보세요! ⚡️ 
', 'https://github.com/woowacourse-teams/2024-pokerogue-helper', NULL, '2024-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/7794eb6a-5d04-4c21-b1f5-da5904de02ee', 6, NULL, '리뷰미', 'review-me', '리뷰미', '내 장점을 알고 싶다면? 리뷰미 🔎✨', 29, '2026-08-07', 'CLOSED', 'APPROVED', '<p align="center"><img src="https://github.com/user-attachments/assets/7794eb6a-5d04-4c21-b1f5-da5904de02ee" alt="리뷰미 로고" width="800px"/></p>

🔗[리뷰미 바로가기](https://review-me.page)

# 🔎 리뷰미 


## 프로젝트 소개
프로젝트를 함께한 동료들에게 받은 리뷰를 통해 자신이 어떤 개발자인지 파악하고 표현하는 데 도움을 주는 서비스입니다.  
기술뿐만 아니라 소프트 스킬, 나의 강점 등을 다방면으로 리뷰 받을 수 있어요.   
어쩌면 내가 몰랐던 내 모습을 발견할 수도 있겠죠?  

## 리뷰미가 세상에 나온 이유✨
> 🤔 나는 무엇을 잘하는 개발자일까?  
📚 어떤 점을 보완하면 내가 더 성장할 수 있을까?  
🫂 우리 팀원은 나를 어떻게 생각할까?

프로젝트를 하다보면 이런 고민이 들 때가 있지 않나요?  
우리는 이 고민의 답을 `동료들의 피드백`에서 찾았어요.  
동료들과 피드백을 주고받으며 `내가 팀에서 어떤 사람`이었고 `무엇을 잘하는지` 알 수 있었기 때문이에요.

그렇게 동료들과 피드백을 주고받을 수 있는 서비스, `리뷰미`가 탄생하였습니다.

## 주요 기능 소개

### 리뷰를 작성해보세요
뭐라고 리뷰를 써야할지 막막한가요? 리뷰미를 통해 그 때의 기억을 떠올리며 리뷰를 작성해보세요.

<p align="center"><img src="https://github.com/user-attachments/assets/fbcf3dbc-4524-4e07-86e1-372e987b51a8" height="400px"/></p>

### 리뷰를 확인해보세요
팀원들이 보는 내 모습은 어땠을까요? 작성한 리뷰를 확인해보세요!

<p align="center"><img src="https://github.com/user-attachments/assets/51508952-6b3e-493b-9404-0e5585b91e8e" width="800px"/></p>

### 리뷰로 나를 파악해보세요
받은 리뷰를 모아보고, 나를 파악하는데 도움이 된 부분을 형광펜으로 표시할 수 있어요.
<p align="center"><img src="https://github.com/user-attachments/assets/acb9899f-4483-4505-928a-2458062c355a" width="800px"/></p>

## 😮 리뷰미 서비스 사용 후기
<p align="center"><img src="https://github.com/user-attachments/assets/9753d451-42d9-4b9e-b483-1c34b5839464" width="800px" /></p>


## ⚙️ 기술 스택
### 프론트엔드
<p align="center"><img src="https://github.com/user-attachments/assets/7497d615-02f5-48e6-b5a2-847121c56df8" width="600px"/></p>

### 백엔드
<p align="center"><img src="https://github.com/user-attachments/assets/0a7aa302-b737-496d-9fec-54a8088da3b5" width="600px"/></p>

### Infrastructure
<p align=center><img src=https://github.com/user-attachments/assets/ea1c3226-f452-4910-9a90-11a9d3e1e1df width=800px/></p>

## 🧑‍💻 팀원 소개

### 프론트엔드
|  <img src="https://github.com/user-attachments/assets/467f08bd-043f-411a-b1da-090450d641b4" alt="bada" width="120px" max-height="120px">   |  <img src="https://github.com/user-attachments/assets/4851713e-e8c3-4c8a-8536-b63cac2e4dc6" alt="soosoo" width="120px" max-height="120px"> |   <img src="https://github.com/user-attachments/assets/a904d69c-c48b-4f75-a46d-47f6ddbe22d0" alt="fe" width="120px" max-height="120px"> | <img src="https://github.com/user-attachments/assets/12241d5a-ba87-4267-a82f-1704c497241b" alt="ollie" width="120px" max-height="120px"> |
| :---: | :---: | :---: | :---: |
| [🐋 바다](https://github.com/badahertz52)  | [😍 쑤쑤](https://github.com/soosoo22)    | [🔥 에프이](https://github.com/chysis)   | [👾 올리](https://github.com/ImxYJL)   |

### 백엔드
| <img src="https://review-me-blog.github.io/assets/images/sancho-a505ff332869b4eda5a1fa6cf296ddc8.jpeg" width="120px" max-height="120px">  |  <img src="https://review-me-blog.github.io/assets/images/aru-f1f92d2d3284aab8aa385afd817d2ae7.jpeg" width="120px" max-height="120px">   | <img src="https://review-me-blog.github.io/assets/images/kirby-c5c179939bc7a2fd587bcc2cbb6af129.png" width="120px" max-height="120px"> |   <img src="https://review-me-blog.github.io/assets/images/ted-a4f788b021e7619d4cc9ae7fc0ab336d.png" width="120px" max-height="120px">  |
| :---: | :---: | :---: | :---: |
| [🦧 산초](https://github.com/nayonsoso)  | [🤸🏻‍♂️ 아루](https://github.com/donghoony)   | [💃 커비](https://github.com/skylar1220)   | [🐻 테드](https://github.com/Kimprodp)   |

', 'https://github.com/woowacourse-teams/2024-review-me', NULL, '2024-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/ec7c126e-5824-4722-ac9b-f17c868cb142', 6, NULL, '스타카토', 'staccato', '스타카토', '🗺️ 지도 기반 일상 기록 서비스 스타카토', 48, '2026-08-07', 'CLOSED', 'APPROVED', '<img src="https://github.com/user-attachments/assets/ec7c126e-5824-4722-ac9b-f17c868cb142">

<br>

<br>

# 🧑‍🤝‍🧑 We are team Staccato!

[![Google Play Store](https://img.shields.io/badge/Google_Play-0F9D58?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.on.staccato)

<!-- [![App Store](https://img.shields.io/badge/App_Store-0D96F6?style=for-the-badge&logo=app-store&logoColor=white)](#) -->

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=flat-square&logo=openjdk&logoColor=white) ![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=flat-square&logo=spring&logoColor=white) ![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white) ![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=flat-square&logo=kotlin&logoColor=white) ![MySQL](https://img.shields.io/badge/mysql-%2300f.svg?style=flat-square&logo=mysql&logoColor=white) ![Slack](https://img.shields.io/badge/Slack-4A154B?style=flat-square&logo=slack&logoColor=fff) ![GitHub](https://img.shields.io/badge/GitHub-%23121011.svg?style=flat-square&logo=github&logoColor=white)

<br>

🔗 [Android Tech Stack](https://wonjunyoung.notion.site/9627eb4cbe8e4d2489afc20bd21cb428)  
🔗 [Backend Tech Stack](https://wonjunyoung.notion.site/4c63b9a066834561936213555b1feee8)

<br>

## 📱 Android
|빙티(이소민)|호두(원준영)|해나(공혜연)|
|:---:|:---:|:---:|
|<img src="https://ca.slack-edge.com/TFELTJB7V-U06GUARB6PM-5a713d81370a-512" width="150" height="150">|<img src="https://ca.slack-edge.com/TFELTJB7V-U06GUE1QFFU-32dc89270308-512" width="150" height="150">|<img src="https://ca.slack-edge.com/TFELTJB7V-U06GMQPSPDL-1bdc78fac7b6-512" width="150" height="150">|
|s6m1n|Junyoung-WON|hxeyexn|
|[GitHub](https://github.com/s6m1n)|[GitHub](https://github.com/Junyoung-WON)|[GitHub](https://github.com/hxeyexn)|
|[PR 모아보기](https://github.com/woowacourse-teams/2024-staccato/pulls?q=is%3Apr+assignee%3As6m1n)|[PR 모아보기](https://github.com/woowacourse-teams/2024-staccato/pulls?q=is%3Apr+assignee%3AJunyoung-WON+)|[PR 모아보기](https://github.com/woowacourse-teams/2024-staccato/pulls?q=is%3Apr+assignee%3Ahxeyexn)|

## 🌐 Backend
|리니(이예린)|카고(정민호)|폭포(이성주)|호티(윤주호)|
|:---:|:---:|:---:|:---:|
|<img src="https://ca.slack-edge.com/TFELTJB7V-U06GMQUAT70-410e342eb43f-512" width="150" height="150">|<img src="https://ca.slack-edge.com/TFELTJB7V-U06GMQV264E-f483078a2fc5-512" width="150" height="150">|<img src="https://ca.slack-edge.com/TFELTJB7V-U06GRGE7DNH-cd8e7f5322b1-512" width="150" height="150">|<img src="https://ca.slack-edge.com/TFELTJB7V-U06GUAX1PFD-0e000c1755a3-512" width="150" height="150">|
|linirini|devhoya97|BurningFalls|Ho-Tea|
|[GitHub](https://github.com/linirini)|[GitHub](https://github.com/devhoya97)|[GitHub](https://github.com/BurningFalls)|[GitHub](https://github.com/Ho-Tea)|
|[PR 모아보기](https://github.com/woowacourse-teams/2024-staccato/pulls?q=is%3Apr+assignee%3Alinirini)|[PR 모아보기](https://github.com/woowacourse-teams/2024-staccato/pulls?q=is%3Apr+assignee%3Adevhoya97)|[PR 모아보기](https://github.com/woowacourse-teams/2024-staccato/pulls?q=is%3Apr+assignee%3ABurningFalls)|[PR 모아보기](https://github.com/woowacourse-teams/2024-staccato/pulls?q=is%3Apr+assignee%3AHo-Tea)|


<br>

# 🤩 지도 기반 일상 기록 서비스, 스타카토를 소개합니다!

`스타카토`는 하나의 `기록`을 의미하며 ***지도 위에*** ***빠르고*** ***생생하게*** 남길 수 있어요!

### 지도에서
스타카토를 남기면 지도 위에서 마커로 확인할 수 있어요.  
나의 발자취를 한 눈에 확인해보세요!

### 빠르게
현재 위치와 시간을 자동으로 불러와 빠르게 스타카토를 남길 수 있어요.  
물론, 원하는 위치와 시간을 선택할 수도 있습니다.

### 생생하게
사진, 감정 캐릭터, 댓글을 더해서 그 순간을 생생하게 스타카토로 남겨보세요!

<img src="https://github.com/user-attachments/assets/3565643f-6120-43b4-916e-e8ba93abc348">
', 'https://github.com/woowacourse-teams/2024-staccato', NULL, '2024-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/1f8aa494-3132-48a8-8f74-e639f68db966', 6, NULL, '투룻', 'touroot', '투룻', '🛣️ to your route, 투룻', 53, '2026-08-07', 'CLOSED', 'APPROVED', '# **🛣️ 투룻 🛣️**

**To your route, touroot!**

<img width="1900px" src="https://github.com/user-attachments/assets/1f8aa494-3132-48a8-8f74-e639f68db966"/> 

다른 사람의 여행을 따라가고 싶은데, 정리하기는 귀찮은 적 있지 않으셨나요? 방대한 양의 여행을 어떻게 기록할지 막막한 적 있지 않으셨나요?

단 한 번의 클릭으로 다른 사람의 여행이 나의 여행 계획으로 바뀌고, 미리 준비된 템플릿을 따라 간편하게 여행을 기록할 수 있는 곳

귀찮은 기억은 내려놓고 즐거운 기억만 가져갈 수 있도록 투룻이 도와줄게요!

## 기획 목적

업무와 일상에 집중하다 보면 따로 여행을 계획하는 시간을 만드는 것이 힘들 수 있습니다.

또한, 여행지에 대한 정보는 넘쳐나지만, 이 정보를 자신의 필요에 맞게 정리하고 일정으로 구성하는 것은 많은 시간과 노력을 요구합니다.

이러한 어려움을 해결해 **여행 과정의 기록을 한 눈에 알아보기 쉽게 만들고 이를 통해 더 나은 여행 계획을 간단하게 만들 수 있게 하는 것이 투룻의 목적**입니다.

## 문제 정의 & 솔루션

### 🙋‍♂️ 이런 분들을 위해 만들었어요!

1. 여행 계획을 위해 여러 매체에서 수집한 여행 정보를 정리 하는 것에 지쳤어요.
2. 여행을 기록으로 남기고 싶은데 블로그는 시간과 노력이 많이 들어요.
3. 유튜브에서 본 여행을 따라가고 싶은데 직접 영상을 정리하며 계획을 세우는 것이 지쳐요.
4. 제가 세운 계획을 한 눈에 알아볼 수 있도록 정리해 공유하고 싶어요.
5. 제가 다녀온 여행이 다른 사람의 여행으로 활용되면 좋겠어요.

### 💡 투룻이 이렇게 도와드릴게요!

1. 마음에 드는 여행기를 한 번의 클릭으로 나의 여행 계획으로 바꿀 수 있어요.
2. 작성한 나의 여행 계획을 쉽고 간단하게 여행기로 바꿀 수 있어요.
3. 하나의 링크로 자신의 여행 계획을 친구들에게 공유할 수 있어요.
4. 여행기 혹은 여행 계획에서 일별 방문 장소와 장소에 대한 정보를 지도에서 한눈에 볼 수 있어요.
5. 원하는 여행기를 편하게 찾아볼 수 있어요.

## 핵심 기능

| 여행기 탐색   | 여행기 검색 |  여행기 - 여행 계획 전환  |
|--------|--------|--------|
|        |        |        |  

| 여행 계획 - 여행기 전환   | 여행 계획 공유 |
|--------|--------|
|        |        |


## 기술 스택

### 프론트엔드

<img width="600" src="https://github.com/user-attachments/assets/4efad8ba-1439-4591-932b-9d6d73112845"/>


### 백엔드

<img width="600" src="https://github.com/user-attachments/assets/1a6c368f-80d6-42d9-bcb9-27d07e720720"/>


## 인프라 구조도

<img width="600" src="https://github.com/user-attachments/assets/90a6280e-d9b4-425e-97ff-5067a42a98d6"/>


## 팀 구성 소개 및 위키 링크

[Team Touroot Wiki](https://github.com/woowacourse-teams/2024-touroot/wiki)

### 프론트엔드
| [리버](https://github.com/0jenn0)   | [지니](https://github.com/jinyoung234)   | [시모](https://github.com/simorimi)   |
|--------|--------|--------|
| <img src="https://avatars.githubusercontent.com/u/130737187?v=4"/> | <img src="https://avatars.githubusercontent.com/u/87177577?v=4"/> | <img src="https://avatars.githubusercontent.com/u/141118352?v=4"/> |

### 백엔드
| [리건](https://github.com/hangillee)   | [클로버](https://github.com/eunjungL) | [낙낙](https://github.com/nak-honest)   | [알파카](https://github.com/slimsha2dy) | [리비](https://github.com/Libienz)   |
|--------|--------|--------|--------|--------|
| <img src="https://avatars.githubusercontent.com/u/14046092?v=4"/> | <img src="https://avatars.githubusercontent.com/u/62099953?v=4"/> | <img src="https://avatars.githubusercontent.com/u/95845037?v=4"/> | <img src="https://avatars.githubusercontent.com/u/99064014?v=4"/> | <img src="https://avatars.githubusercontent.com/u/85234650?v=4"/> |
', 'https://github.com/woowacourse-teams/2024-touroot', NULL, '2024-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/1828955b-f5de-4f48-8de3-f2097d6392bb', 5, NULL, '땅땅땅', '3-ddang', '땅땅땅', '중고 경매 거래 플랫폼', 34, '2026-08-07', 'CLOSED', 'APPROVED', '# 🧑🏻‍⚖️ 땅땅땅을 소개합니다 🧑🏻‍⚖️

<img src="https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/1828955b-f5de-4f48-8de3-f2097d6392bb" width="150" />

## 팀원 소개

### Android

|                     Android                   |                     Android                   |                     Android                   |
| :-------------------------------------------: | :-------------------------------------------: | :-------------------------------------------: |
| <img src="https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/ae8cd313-a328-4640-a1ae-b69accdaf18e" width="100" /> | <img src="https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/a24e5cba-c865-4dec-bafc-53637c42a40e" width="100" /> | <img src="https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/914d167b-2336-47a3-bef6-50c77f99a1a6" width="100" /> |
| [글로<br>(이소정)](https://github.com/ippnsj) | [둘리<br>(송혜민)](https://github.com/hyemdooly) | [멘델<br>(고명진)](https://github.com/rhthrhrl0) |

### Backend

|                     Backend                   |                    Backend                    |                      Backend                      |                       Backend                       |
| :-------------------------------------------: | :-------------------------------------------: | :-----------------------------------------------: | :-------------------------------------------------: |
| <img src="https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/d1d5cc44-a4e2-42ec-9978-b0a3b17b83b8" width="100" /> | <img src="https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/0e659db9-418c-491e-81fb-6d03d3bfa0fa" width="100" /> | <img src="https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/238bfaf5-290a-4b1d-bc9b-9927b2fa9e7f" width="100" /> | <img src="https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/8b78f905-489d-4936-93dd-273210b492d6" width="100" /> |
| [메리<br>(최승원)](https://github.com/swonny) | [엔초<br>(권예진)](https://github.com/kwonyj1022) | [제이미<br>(임정수)](https://github.com/JJ503) | [지토<br>(김지민)](https://github.com/apptie) |
<br/>
<br/>

## 서비스 소개

땅땅땅은  ‘중고 거래 서비스’에 ‘경매’ 시스템을 도입한 서비스입니다.

경매를 통해 판매자와 구매자가 함께 적절한 가격을 결정할 수 있습니다.

또한, 입찰 내역을 통해 모든 거래 내역은 모든 구매자와 판매자들에게 투명하게 공개되어 얼마에 구매할지 혹은 판매할지에 대한 기준이 되어줍니다.

’땅땅땅’을 통해 중고 물품을 판매하려면, 기본적인 정보와 함께 경매 마감 시간과 경매 시작가와 입찰 단위를 입력합니다.

입찰자는 최소 입찰 가능 금액 이상으로 입찰하며 다른 입찰자들과 경쟁합니다.

낙찰자가 정해지면 이후 1대 1 쪽지를 통해 자율적으로 거래를 진행합니다.

이렇게 ''땅땅땅''은 판매자에게는 거래 수요가 있는 최고 가격에 물건을 판매할 기회를 제공합니다.

구매자에게는 선착순 거래가 아니라는 점에서 더 많은 구매 기회를 제공하며, 자신이 지불할 수 있는 금액 내에서 부담 없이 가격을 제안할 수 있습니다.

또한, 계속 변하는 입찰 내역을 보며 경매를 지켜보는 재미도 쏠쏠합니다.

지금 당장 ''땅땅땅''으로 원하는 물건을 가장 합리적인 가격에 거래해보세요!
<br/>
<br/>

## 땅땅땅 이용방법

<img src="https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/ddd935e9-478c-495c-a3e8-870df52ac517" width="200" height="300" />
<img src="https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/3264785a-37ac-404f-825f-bd49f6a8d921" width="200" height="300" />
<img src="https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/dc1aa784-75ae-41b5-b147-59d0d965c565" width="200" height="300" />
<br/>
<img src="https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/7a5bb375-7db7-427c-8658-82b66f500449" width="200" height="300" />
<img src="https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/46c62d39-60bf-4e65-9107-278cbe4f2fd5" width="200" height="300" />
<img src="https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/8ff9be2e-ad87-48e6-9f57-98d54e7fcb07" width="200" height="300" />
<br/>
<br/>

## 기술 스택

### Android

![image](https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/f63cfc8e-9fe1-42f5-951d-4f5926822f0b)

### Backend

![image](https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/d429f542-884e-43b8-8fb0-3252f1edb3c4)

### Infra

![image](https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/a3651d0a-f83f-44e4-89e9-92f1cfdbc9e8)

## 아키텍처 구조

### Android

![image](https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/7fc2f085-0c6f-481f-be39-6c19203f1ad3)

### Backend

![image](https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/cadf6ce1-6671-42d8-ac2d-533a752d5d20)

![image](https://github.com/woowacourse-teams/2023-3-ddang/assets/49394114/b446ae67-3df7-45e2-8879-a1d08119f5b4)

', 'https://github.com/woowacourse-teams/2023-3-ddang', NULL, '2023-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/woowacourse-teams/2023-baton/assets/39729721/20edcd1f-99b5-4b91-984a-e28065ccebae', 5, NULL, '바톤', 'baton', '바톤', '🏃🏃‍♀️ 바톤, 너도 좋고 나도 좋은 코드 리뷰 중개 사이트', 27, '2026-08-07', 'CLOSED', 'APPROVED', '# 💻 바톤, 너도 좋고 나도 좋은 코드 리뷰 중개 서비스 🏃‍♀️

<h2>👩‍👦‍👦👨‍👨‍👧‍👧 팀원</h2>

|                                       Frontend                                        |                                       Frontend                                        |                                       Frontend                                        |                                        Backend                                         |                                        Backend                                         |                                        Backend                                         |                                        Backend                                        |
|:-------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------:|
| <img src="https://avatars.githubusercontent.com/u/62369936?v=4" width=130px alt="에이든"> | <img src="https://avatars.githubusercontent.com/u/116625502?v=4" width=130px alt="가람"> | <img src="https://avatars.githubusercontent.com/u/103256030?v=4" width=130px alt="도리"> | <img src="https://avatars.githubusercontent.com/u/39729721?v=4" width=130px alt="디투"/> | <img src="https://avatars.githubusercontent.com/u/83010167?v=4" width=130px alt="에단"/> | <img src="https://avatars.githubusercontent.com/u/82203978?v=4" width=130px alt="헤나"/> | <img src="https://avatars.githubusercontent.com/u/67318165?v=4" width=130px alt="주디"> |
|                          [에이든](https://github.com/gyeongza)                           |                            [가람](https://github.com/guridaek)                            |                         [도리](https://github.com/tkdrb12)                          |                             [디투](https://github.com/shb03323)                             |                          [에단](https://github.com/cookienc)                          |                           [헤나](https://github.com/hyena0608)                           |                           [주디](https://github.com/eunbii0213)                           |

<br><br>

<h2>🌺 서비스 소개</h2>

<h3>1️⃣ 리뷰 요청</h3>
<p>코드 리뷰를 받고 싶으신가요?</p>
<ul>
  <li>코드 리뷰 요청글을 작성해주세요.</li>
  <li>코드 리뷰를 원하는 PR을 등록해서 올려주세요.</li>
</ul>

<img src="https://github.com/woowacourse-teams/2023-baton/assets/39729721/20edcd1f-99b5-4b91-984a-e28065ccebae">

<br><br>

<h3>2️⃣ 서포터 지원</h3>
<p>코드 리뷰를 원하시나요?</p>
<ul>
  <li>원하는 코드에 리뷰를 제안해보세요. 📨</li>
  <li>러너가 리뷰 제안을 수락하면 리뷰가 시작됩니다.</li>
  <li>제안이 수락되면 알림이 울릴거에요. ⏰</li>
</ul>

<img src="https://github.com/woowacourse-teams/2023-baton/assets/39729721/278a63a1-28ce-4270-b67a-5446ae8ce598">

<br><br>

<h3>3️⃣ 서포터 선택</h3>
<p>서포터가 지원했다구요?</p>
<ul>
  <li>여러분의 코드에 리뷰 지원한 서포터를 확인할 수 있습니다.</li>
  <li>코드 리뷰를 진행할 서포터를 선택해보세요! 😆</li>
</ul>

<img src="https://github.com/woowacourse-teams/2023-baton/assets/39729721/d3026c2e-3b45-492e-99d8-36f4b523fa24">

<br><br>

<h3>4️⃣ 리뷰 완료</h3>
<p>코드 리뷰가 완료되었나요?</p>
<ul>
  <li>리뷰가 완료되면 리뷰 완료 버튼을 눌러주세요!</li>
  <li>리뷰가 끝나면 러너에게 알림이 갈거에요! 🔔</li>
</ul>

<img src="https://github.com/woowacourse-teams/2023-baton/assets/39729721/40a3470c-8653-4ad1-9cac-081c7f49ce30">

<br><br>

<h3>5️⃣ 후기 작성</h3>
<p>후기를 남겨주세요!</p>
<ul>
  <li>서포터의 코드 리뷰는 어떠셨나요?</li>
  <li>피드백을 통해 더 나은 코드 리뷰 문화를 만들어주세요. 💌</li>
</ul>

<img src="https://github.com/woowacourse-teams/2023-baton/assets/39729721/89661756-d0c6-4cbf-a6da-f962fa98fc07">

<br><br>

<details>
<summary> <h2>📄 개발자들의 메시지</h2> </summary>

### To. 개발자 지망생 🐣

혼자서 개발 공부를 하고 계신가요? 주변에 도와줄 개발자가 없으신가요?

강의로 배운 지식들을 막상 내 코드에 적용하다보면, 내가 지금 알맞게 적용하고 있는지 궁금해집니다.

그래서 코드 리뷰를 받아야겠다는 생각을 하고 보니, 주변에 딱히 부탁할 사람도 없고 ''이 정도 코드를 리뷰 받아도 되는 건가?''라는 생각이 듭니다.

하지만 시중에 있는 멘토링 서비스들은 부담이 큽니다.

단건으로 코드를 봐줬으면 하는 것이지, 장기적으로 1대1 코칭을 받고 싶은 것은 아닙니다.

''바톤''은 이런 분들을 위해 만들었습니다.

''바톤''은 누구나 합리적인 가격으로 코드 리뷰를 받을 수 있는 서비스입니다.

코드 리뷰는 가격을 측정하기 어렵습니다.

누구는 코드의 복잡한 정도를 기준으로 할 수도 있고, 누구는 코드의 라인 수를 기준으로 삼습니다.

각자 가격을 매기는 기준이 천차만별이라 가격표도 만들기 어렵습니다.

그래서 ''바톤''은 코드를 먼저 보여줍니다.

PR을 올리고, 간단한 설명과 함께 리뷰 요청 글을 작성합니다.

그걸 본 여러 개발자들이 각자의 기준으로 책정한 리뷰 금액을 제시합니다.

그러면 리뷰이는 제시한 사람들의 목록과 제시 금액을 확인할 수 있습니다.

또한 리뷰어의 활동 기록과 프로필 소개글을 확인할 수 있습니다. 원한다면 대화도 나눌 수 있습니다.

이러한 조건들을 고려하여, 최종적으로 받고 싶은 리뷰어에게 코드 리뷰를 받을 수 있습니다.

리뷰가 끝나면 상대방에 대한 후기를 남길 수 있습니다.

리뷰가 맘에 들었을 수도 있고, 부족한 점을 느꼈을 수도 있습니다.

그러한 점들을 후기로 남기면서 서로에게 좋은 영향을 줄 수 있습니다.

좋았던 점은 더 열심히 공부하게 되는 원동력이 될 것이고, 아쉬웠던 점은 이후 더 발전할 수 있는 계기를 만들어 줄 것입니다.

### To. 현직 개발자 🧑🏻‍💻

내가 가진 능력을 활용하여 본업 이외의 추가 수익을 얻고싶지 않으신가요?

게다가 그 일이 누군가 도움이 되는 일이라면 좋지 않을까요?

''바톤''이 도와드릴 수 있습니다.

''바톤''에는 다양한 개발자 지망생들이 코드 리뷰를 기다리고 있습니다.

딱히 모든 코드를 리뷰할 필요는 없습니다.

꾸준히 리뷰를 진행해도 되고, 가끔씩 용돈벌이를 위해 진행해도 상관없습니다.

내가 자신 있는 언어로 작성된 것들만 리뷰를 해도 되고, 아니면 익숙하지 않은 언어로 된 코드를 리뷰하며 역량을 키워도 됩니다.

리뷰를 시작하는 것은 어렵지 않습니다.

우선 어떤 코드인지 확인하고, 리뷰를 하고 싶은 마음이 생겼으면 리뷰이에게 메세지와 제시 금액을 남기면 됩니다.

제시 금액에 대한 명확한 기준은 없습니다.

리뷰이에게 제시한 금액에 대한 합당한 이유, 자신의 역량을 보여주기만 하면 문제없습니다.

코드 리뷰가 종료되고 나면, 리뷰이, 리뷰어는 서로에게 후기를 남기게 됩니다.

모든 후기는 기록으로 남아 본인을 증명하는 수단 중 하나가 될 것입니다.

통계가 쌓이면, 태도가 불량한 사람인지, 리뷰를 성실하게 하는 사람인지 확인할 수도 있습니다.

모든 것은 당신이 활용하기 나름입니다.

<br>

**_''바톤''은 성장하고 싶은 사람들을 모아 매칭을 도와주는 win-win 서비스입니다._**

**_당신도 이에 해당이 된다면, 와서 함께 win-win 하시죠!_**

<p style="text-align: right; font-weight: 700">From. 바톤 🏃🏿‍♀️🏃🏾</p>

</details>

<br>

<h2>⚒️ 기술 스택</h2>
<h3>🎨 프론트엔드</h4>
<img src="https://github.com/woowacourse-teams/2023-baton/assets/39729721/082a295a-d24d-458e-aa97-d7f341286595">

<br><br>

<h3>🐳 백엔드</h4>
<img src="https://github.com/woowacourse-teams/2023-baton/assets/39729721/57879630-42bc-4311-83f9-dc3c11cdaf14">

<br><br>

<h3>💸 인프라 구조</h3>
<img src="https://github.com/woowacourse-teams/2023-baton/assets/39729721/b94fd856-8538-4274-a17a-8d391d5b42f8">
<br><br>

<h3>💸 CI/CD 구조</h3>
<img src="https://github.com/woowacourse-teams/2023-baton/assets/39729721/235bea9a-0d33-487d-9466-d3b97e08405a">

<br><br>

<h2>💾 데이터베이스 구조</h2>
<img src="https://github.com/woowacourse-teams/2023-baton/assets/39729721/bfbe0596-e78d-411b-beb4-57c50aab4d10" width="1000">

<br><br>

<h2>🐈‍⬛ Github Actions Secrets And Variables</h2>

```yaml
DOCKERHUB_DEPLOY_TOKEN : {백엔드 도커 허브 운영 서버용 계정 토큰}
DOCKERHUB_DEPLOY_USERNAME : {백엔드 도커 허브 운영 서버용 계정명}
DOCKERHUB_DEV_TOKEN : {백엔드 도커 허브 개발 서버용 계정 토큰}
DOCKERHUB_DEV_USERNAME : {백엔드 도커 허브 개발 서버용 계정명}

JWT_ISSUER : {백엔드 JWT 발행자}
JWT_SECRET_KEY : {백엔드 JWT 시크릿 키}

OAUTH_GITHUB_CLIENT_ID : {백엔드 깃허브 소셜 로그인, Github Oauth Client Id}
OAUTH_GITHUB_CLIENT_SECRET : {백엔드 깃허브 소셜 로그인, Github Oauth Client Secret Key}
OAUTH_GITHUB_REDIRECT_URI : {백엔드 깃허브 소셜 로그인, Github Oauth Client Redirect Uri}
OAUTH_GITHUB_SCOPE : {백엔드 깃허브 소셜 로그인, Github Oauth Scope}

PERSONAL_MISSION_ACCESS_TOKEN : {백엔드 미션 브랜치 생성, Github Social Token}

SUBMODULE_BE_TOKEN : {백엔드 서브모듈 토큰}

REACT_APP_CHANNELTALK_KEY : {프론트엔드 채널톡 키}
REACT_APP_DEV_BASE_URL : {프론트엔드 개발용 Base Url}
REACT_APP_GA_TRACKING_ID : {프론트엔드 GA Tracking Id}
REACT_APP_PROD_BASE_URL : {프론트엔드 운영용 Base Url}
```
', 'https://github.com/woowacourse-teams/2023-baton', NULL, '2023-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/title.png', 5, NULL, '카페인', 'car-ffeine', '카페인', '🚗 실시간 전기자동차 충전소 지도 및 사용 통계 조회 서비스⚡️', 64, '2026-08-07', 'CLOSED', 'APPROVED', '![title](https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/title.png)

# 2023 우아한테크코스 5기 카페인 팀

<a href="https://carffe.in">
  <p align="center">
    <img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/frontend/public/icons/192.png"/>
  </p>
  <p align="center" style="font-size: larger;">
    카페인 바로가기
  </p>
</a>
<p align="center" style="font-size: xx-small;">
  실시간 전기자동차 충전소 지도 및 사용 통계 조회 서비스
</p>

## 프로젝트 소개

카페인 서비스는 전국 24만개의 충전기 정보를 실시간으로 제공하고, 각 충전소의 혼잡도 정보를 제공함으로써 전기차 사용자들의 자발적인 분산 이용을 유도하는 서비스 입니다.

<br>

### 설치할 필요가 없습니다.

이 서비스는 웹 브라우저만 있다면 설치하지 않아도 구동할 수 있습니다. PC와 모바일 모두 대응하며, 앱 환경에서 사용을 원하시는 경우 브라우저에서 `홈 화면에 바로가기 추가`를 눌러 사용할 수 있습니다.

<br>

### 기존의 웹 서비스를 대체합니다.

기존에 출시된 전기자동차 충전소 조회 웹 서비스들은 굉장히 느리고 불편합니다. 카페인에서는 `사용자 경험을 네이티브 앱 환경만큼 크게 개선`하였습니다. 전국 충전소를 검색할 수 있는 것은 물론이고, 원하시는 지역을
검색하여 바로 이동할 수도 있습니다.

<br>

### 충전소 사용량을 통계로 제공합니다.

충전소의 상세한 정보를 제공하는 것은 물론이고, 지속적으로 수집한 충전기 사용량을 통계로 제공합니다. 충전소가 얼마나 인기있는 충전소인지 확인할 수 있습니다.

<br>

### 충전소 평가 및 고장 신고

정보가 잘못된 충전소는 운전자를 당황하게 합니다. 평소에 관리가 잘 되고 있는 충전소인지 확인할 수 있습니다.

<br>

## 주요 기능 소개

### 충전소 조회

<p align="center" >
  <img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/main.png" alt="main" />
</p>
<p align="center" >
  <img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/cf-zoom.gif" alt="cf-zoom" />
</p>
<p align="center" >
  <img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/cf-move.gif" alt="cf-move" />
</p>

<table>
  <tr>
    <td><img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/station_marker_big.png" alt="station_marker_big" /></td>
    <td><img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/station_marker.png" alt="station_marker" /></td>
  </tr>
</table>

<table>
  <tr>
    <td><img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/cluster_region.png" alt="cluster_region" /></td>
    <td><img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/cluster_city.png" alt="cluster_city" /></td>
  </tr>
</table>

### 전국 충전소 검색 및 필터링

<table>
  <tr>
    <td><img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/search.gif" alt="search" /></td>
    <td><img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/filter_server.gif" alt="filter_server" /></td>
  </tr>
</table>

### 충전소 정보 조회

<table>
  <tr>
    <td><img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/station_info.png" alt="station_info" /></td>
    <td><img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/station_details.png" alt="station_details" /></td>
  </tr>
</table>

<table>
  <tr>
    <td><img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/station_chargers.png" alt="station_chargers" /></td>
    <td><img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/statistics.png" alt="statistics" /></td>
  </tr>
</table>

<table>
  <tr>
    <td><img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/reports.png" alt="reports" /></td>
    <td><img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/reviews.png" alt="reviews" /></td>
  </tr>
</table>

### 지원 플랫폼

<table>
  <tr>
    <td align="center">
        <img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/pc.png" alt="PC" />
    </td>
    <td align="center">
        <img src="https://raw.githubusercontent.com/woowacourse-teams/2023-car-ffeine/develop/docs/home/mobile.png" alt="Mobile" />
    </td>
  </tr>
  <tr>
    <td align="center">
        PC
    </td>
    <td align="center">
        Mobile
    </td>
  </tr>
</table>

## 프로젝트 구조

- CI/CD
    - GitHub Actions
    - Docker
    - (작성 예정)
- 기술
    - Frontend
        - React + Typescript
        - Tanstack Query
        - Google Maps API
        - webpack
        - styled-components
        - Storybook
        - Jest
        - React Testing Library
        - Eslint / Prettier
        - GitHub Actions
        - AWS S3 (예정)
        - AWS CloudFront (예정)
    - Backend
        - (작성 예정)

## 팀원 소개

### Frontend

<table>
  <tr>
    <td align="center" width="200px">
      <a href="https://github.com/gabrielyoon7" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/69189073?v=4" alt="가브리엘(윤주현) 프로필" />
      </a>
    </td>
    <td align="center" width="200px">
      <a href="https://github.com/kyw0716" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/77326660?v=4" alt="센트(김영우) 프로필" />
      </a>
    </td>
    <td align="center" width="200px">
      <a href="https://github.com/feb-dain" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/108778921?v=4" alt="야미(이다인) 프로필" />
      </a>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="https://github.com/gabrielyoon7" target="_blank">
        가브리엘(윤주현)
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/kyw0716" target="_blank">
        센트(김영우)
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/feb-dain" target="_blank">
        야미(이다인)
      </a>
    </td>
  </tr>
</table>

### Backend

<table>
  <tr>
    <td align="center" width="200px">
      <a href="https://github.com/be-student" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/80899085?v=4" alt="누누(송은우) 프로필" />
      </a>
    </td>
    <td align="center" width="200px">
      <a href="https://github.com/drunkenhw" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/106640954?v=4" alt="박스터(한우석) 프로필" />
      </a>
    </td>
    <td align="center" width="200px">
      <a href="https://github.com/sosow0212" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/63213487?v=4" alt="제이(이재윤) 프로필" />
      </a>
    </td>
    <td align="center" width="200px">
      <a href="https://github.com/kiarakim" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/101039161?v=4" alt="키아라(김도희) 프로필" />
      </a>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="https://github.com/be-student" target="_blank">
        누누(송은우)
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/drunkenhw" target="_blank">
        박스터(한우석)
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/sosow0212" target="_blank">
        제이(이재윤)
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/kiarakim" target="_blank">
        키아라(김도희)
      </a>
    </td>
  </tr>
</table>

## 사용자 유치

[확인하기](./docs/ga4/ga4.md)

## 더보기

- [카페인팀 블로그](https://car-ffeine.github.io/archive)
', 'https://github.com/woowacourse-teams/2023-car-ffeine', NULL, '2023-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/woowacourse-teams/2023-celuveat/assets/102432453/9eaaf87a-5d9e-4f79-9d3e-b7c5183813df', 5, NULL, '셀럽잇', 'celuveat', '셀럽잇', '✨셀럽 ✨기반 맛집 탐색 서비스 "셀럽잇" 🍕', 70, '2026-08-07', 'CLOSED', 'APPROVED', '![셀럽잇 소개](https://github.com/woowacourse-teams/2023-celuveat/assets/102432453/9eaaf87a-5d9e-4f79-9d3e-b7c5183813df)
', 'https://github.com/woowacourse-teams/2023-celuveat', NULL, '2023-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/woowacourse-teams/2023-festa-go/assets/108349655/51fa15dc-b9e2-426c-b46b-abe4c562bc05', 5, NULL, '페스타고', 'festa-go', '페스타고', '🎪 페스타고, 대학 축제를 더욱 즐겁게!', 73, '2026-08-07', 'CLOSED', 'APPROVED', '# 페스타고, 대학 축제 티케팅 & 라인업 검색 서비스

**▷  개발, 출시, 유지보수 기간 총 1년 (2023.07 ~ 현재) </br>**
**▷  사용자 200 명 이상 유치** (Android, iOS 모두 100 명 이상)

![6 5형 (12 XS Max, 12 XR)](https://github.com/woowacourse-teams/2023-festa-go/assets/108349655/51fa15dc-b9e2-426c-b46b-abe4c562bc05)

**▷ 📲 Android 다운로드 |** [플레이스토어 바로가기](https://play.google.com/store/apps/details?id=com.festago.festago) </br>
**▷ 📲 iOS 다운로드 |** [앱스토어 바로가기](https://apps.apple.com/kr/app/%ED%8E%98%EC%8A%A4%ED%83%80%EA%B3%A0-%EB%8C%80%ED%95%99-%EC%B6%95%EC%A0%9C-%EB%9D%BC%EC%9D%B8%EC%97%85-%EA%B0%80%EC%88%98-%EA%B3%B5%EC%97%B0-%EC%95%84%EC%9D%B4%EB%8F%8C/id6502875848)
</br></br>
**▷ 📝 팀블로그 |** [Festago 팀블로그](https://festago.github.io/) </br>
**▷ 📧 연락처 |** team.festago@gmail.com

</br>

## 페스타고가 제공합니다

### 1. 축제 줄서기 (Deprecated)
> 현재 v1.X.X 에서만 제공합니다.

<img width="1192" alt="image" src="https://github.com/woowacourse-teams/2023-festa-go/assets/71129059/8e761c81-6066-4f93-89af-b9d2d22eb577">


### 2. 전국 대학 축제 라인업 모아보기

![플레이스토어 스크린샷](https://github.com/woowacourse-teams/2023-festa-go/assets/108349655/a52684ff-5e32-4d77-b758-4ed77de5efcf)


<br/>
<br/>

## Android

### 프로젝트 아키텍처
<img width="955" src="https://github.com/woowacourse-teams/2023-festa-go/assets/67777523/2fc5e26f-c628-41fe-b2d3-5b74da794fdc">

### 기술 스택
- DataBinding, MVVM 
- Android | Kotlin, Coroutines, Flow, Hilt, Room 
- OkHttp3, Retrofit2, Glide, JUnit, MockK, Turbine, Zxing 
- Firebase Analytics | Crashlytics, FCM, Kakao sdk 

<br/>
<br/>

## Backend

<div align="center">
  <h3> 백엔드 인프라 아키텍처 </h3>
  <img width="800" src="https://github.com/woowacourse-teams/2023-festa-go/assets/103228463/aad084a6-c1ca-41fa-89c0-f2d2371e59cc">
</div>


<div align="center">
  <h3> 기술 스택 </h3>
  <img src="https://img.shields.io/badge/Java17-000000?style=flat-square&logo=java&color=F40D12">
  <img src="https://img.shields.io/badge/Spring_Boot_3-0?style=flat-square&logo=spring-boot&logoColor=white&color=%236DB33F">
  <img src="https://img.shields.io/badge/MySQL_8-0?style=flat-square&logo=mysql&logoColor=white&color=4479A1">
  <img src="https://img.shields.io/badge/Nginx-0?style=flat-square&logo=nginx&logoColor=white&color=009639">
  <img src="https://img.shields.io/badge/Hibernate-0?style=flat-square&logo=hibernate&logoColor=white&color=%2359666C">
  <img src="https://img.shields.io/badge/Amazon_EC2-0?style=flat-square&logo=amazon-ec2&logoColor=white&color=%23FF9900">
  <img src="https://img.shields.io/badge/Flyway-0?style=flat-square&logo=flyway&color=%23CC0200">
  <br/>
  <img src="https://img.shields.io/badge/Amazon_CloudWatch-0?style=flat-square&logo=amazon-cloudwatch&logoColor=white&color=%23FF4F8B">
  <img src="https://img.shields.io/badge/OAuth2-0?style=flat-square&logo=oauth2&logoColor=white&color=%23000000">
  <img src="https://img.shields.io/badge/Gradle-0?style=flat-square&logo=gradle&logoColor=white&color=%2302303A">
  <img src="https://img.shields.io/badge/Swagger-0?style=flat-square&logo=Swagger&logoColor=white&color=%2385EA2D">
  <img src="https://img.shields.io/badge/GitHub%20Actions-0?style=flat-square&logo=GitHub%20Actions&logoColor=white&color=%232088FF">
  <img src="https://img.shields.io/badge/JUnit5-0?style=JUnit5-square&logo=junit5&logoColor=white&color=%2325A162">
  <img src="https://img.shields.io/badge/Jenkins-0?style=flat-square&logo=Jenkins&logoColor=white&color=%23D24939">
</div>
<br/>
<br/>

## 🎉 축제 스태프를 소개합니다

|BackEnd|BackEnd|BackEnd|BackEnd|iOS|
|:-:|:-:|:-:|:-:|:-:|
|![](https://avatars.githubusercontent.com/u/103228463?v=4&size=110)|![](https://avatars.githubusercontent.com/u/116627736?v=4&size=110)|![](https://avatars.githubusercontent.com/u/71129059?v=4&size=110)|![](https://avatars.githubusercontent.com/u/100915276?v=4&size=110)|![](https://avatars.githubusercontent.com/u/81206228?v=4&size=110)|
|<a href="https://github.com/woowacourse-teams/2023-festa-go/commits/dev?author=BGuga" title="Code">작업 내용 💻</a>|<a href="https://github.com/woowacourse-teams/2023-festa-go/commits/dev?author=seokjin8678" title="Code">작업 내용 💻</a>|<a href="https://github.com/woowacourse-teams/2023-festa-go/commits/dev?author=xxeol2" title="Code">작업 내용 💻</a>|<a href="https://github.com/woowacourse-teams/2023-festa-go/commits/dev?author=carsago" title="Code">작업 내용 💻</a>
|[푸우](https://github.com/BGuga)|[글렌](https://github.com/seokjin8678)|[애쉬](https://github.com/xxeol2)|[오리](https://github.com/carsago)|[닉](https://github.com/tea-hkim)|

|Android|Android|Android|Design|Design|
|:-:|:-:|:-:|:-:|:-:|
|![](https://avatars.githubusercontent.com/u/108349655?v=4&size=110)|![](https://avatars.githubusercontent.com/u/67777523?v=4&size=110)|![](https://avatars.githubusercontent.com/u/37167652?v=4&size=110)|![](https://avatars.githubusercontent.com/u/103024956?v=4&size=110)|<img src="https://avatars.githubusercontent.com/u/153623913?v=4" width="110" height="110" />|
|<a href="https://github.com/woowacourse-teams/2023-festa-go/commits/dev?author=SeongHoonC" title="Code">작업 내용 💻</a>|<a href="https://github.com/woowacourse-teams/2023-festa-go/commits/dev?author=EmilyCh0" title="Code">작업 내용 💻</a>|<a href="https://github.com/woowacourse-teams/2023-festa-go/commits/dev?author=re4rk" title="Code">작업 내용 💻</a>|
|[베르](https://github.com/SeongHoonC)|[해시](https://github.com/EmilyCh0)|[아크](https://github.com/re4rk)|[라우](https://github.com/lau0505)|[혜성](https://github.com/comet-stella)


<br>

## ⛔️ 공연 관람시 주의사항
> 페스타고 팀의 그라운드 룰을 소개합니다.

<img width="787" alt="image" src="https://github.com/woowacourse-teams/2023-festa-go/assets/71129059/85c4d4d0-24fa-4602-9eed-a9a2e4a6482e">
', 'https://github.com/woowacourse-teams/2023-festa-go', NULL, '2023-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/woowacourse-teams/2023-fun-eat/assets/80464961/85396306-1d3e-4d8e-8763-0e28e2a8be04', 5, NULL, '펀잇', 'fun-eat', '펀잇', '궁금해? 맛있을걸? 먹어봐 - 펀잇 🥄', 36, '2026-08-07', 'CLOSED', 'APPROVED', '<div align="center">

<br>

<img src="https://github.com/woowacourse-teams/2023-fun-eat/assets/80464961/85396306-1d3e-4d8e-8763-0e28e2a8be04" width="520px" />

<br>
<br>

<b>궁금해? 맛있을걸? 먹어봐! <br>
🍙 편의점 음식 리뷰 & 꿀조합 공유 서비스 🍙</b>

<br>

[![Application](http://img.shields.io/badge/funeat.site-D8EAFF?style=for-the-badge&logo=aHR0cHM6Ly9naXRodWIuY29tL3dvb3dhY291cnNlLXRlYW1zLzIwMjMtZnVuLWVhdC9hc3NldHMvODA0NjQ5NjEvOWI1OWY3NzktY2M5MS00MTJhLWE3NDUtZGQ3M2IzY2UxZGNk&logoColor=black&link=https://funeat.site/)](https://funeat.site/)
[![WIKI](http://img.shields.io/badge/-GitHub%20WiKi-FFEC99?style=for-the-badge&logoColor=black&link=https://github.com/woowacourse-teams/2023-fun-eat/wiki)](https://github.com/woowacourse-teams/2023-fun-eat/wiki)
[![Release](https://img.shields.io/github/v/release/woowacourse-teams/2023-fun-eat?style=for-the-badge&color=FFCFCF)](https://github.com/woowacourse-teams/2023-fun-eat/releases/tag/v1.3.0)

</div>

<br>

# 🥄 서비스 소개

![1_메인페이지](https://github.com/woowacourse-teams/2023-fun-eat/assets/55427367/9663f7b5-cd38-4f06-86fb-c6636fc364c6)

<br>

## 1. 편의점마다 특색있는 음식 궁금해?

![5_상품목록](https://github.com/woowacourse-teams/2023-fun-eat/assets/55427367/03fb9955-61fa-4228-a270-ce9dffc710c6)
![6_상품상세](https://github.com/woowacourse-teams/2023-fun-eat/assets/55427367/694bc8db-74bd-4fa1-b499-900cd27f5028)
![4_검색](https://github.com/woowacourse-teams/2023-fun-eat/assets/55427367/6a157e08-79d8-450b-9511-ffa461000a22)

<br>
<br>

## 2. 솔직한 리뷰를 보면 더 맛있을걸?

![2_리뷰](https://github.com/woowacourse-teams/2023-fun-eat/assets/55427367/4bf5ecd7-df08-45d0-b592-8629f3a4e3e6)

<br>
<br>

## 3. 생각지 못했던 꿀조합, 먹어봐!

![3_꿀조합](https://github.com/woowacourse-teams/2023-fun-eat/assets/55427367/8e560b40-d039-47ce-ad29-5e244cba4bf2)

<br>
<br>

# 🛠️ 기술 스택

### 백엔드

<div align="center">
  <img src=''https://github.com/woowacourse-teams/2023-fun-eat/assets/55427367/5b60393a-ffbf-4595-bb4c-166d091a7998'' width="400px" alt="BE_기술스택"/>
</div>

<br/>

### 프론트엔드

<div align="center">
  <img src=''https://github.com/woowacourse-teams/2023-fun-eat/assets/55427367/e3d76698-aaa4-4eea-a878-8c03f3faf395'' width="400px" alt="FE_기술스택"/>
</div>

<br/>

### 인프라

<div align="center">
  <img src=''https://github.com/woowacourse-teams/2023-fun-eat/assets/55427367/79399085-1245-4af4-be20-2d5402d53da7'' width="400px" alt="인프라_기술스택"/>
</div>

<br>
<br>

# 인프라 구조

### CI/CD

<div align="center">
    <img src="https://github.com/woowacourse-teams/2023-fun-eat/assets/55427367/3fbef028-d216-4abe-ab4f-c531b099dd33" alt="cicd">
</div>

### 구조

<div align="center">
    <img src="https://github.com/woowacourse-teams/2023-fun-eat/assets/55427367/3bbb9d40-f525-43ab-8ec2-ade6e6a07139" alt="인프라 구조" />
</div>

<br>
<br>

# 👨‍👨‍👧‍👧👩‍👦‍👦 팀원

|                                        Frontend                                         |                                        Frontend                                         |                                        Frontend                                         |                                         Backend                                          |                                         Backend                                          |                                         Backend                                          |                                         Backend                                          |
| :-------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------: | :--------------------------------------------------------------------------------------: | :--------------------------------------------------------------------------------------: | :--------------------------------------------------------------------------------------: | :--------------------------------------------------------------------------------------: |
| <img src="https://avatars.githubusercontent.com/u/55427367?v=4" width=200px alt="타미"> | <img src="https://avatars.githubusercontent.com/u/80464961?v=4" width=200px alt="해온"> | <img src="https://avatars.githubusercontent.com/u/78616893?v=4" width=200px alt="황펭"> | <img src="https://avatars.githubusercontent.com/u/79046106?v=4" width=200px alt="로건"/> | <img src="https://avatars.githubusercontent.com/u/33208246?v=4" width=200px alt="망고"/> | <img src="https://avatars.githubusercontent.com/u/91522259?v=4" width=200px alt="오잉"/> | <img src="https://avatars.githubusercontent.com/u/91244090?v=4" width=200px alt="우가"/> |
|                         [🐰 타미](https://github.com/xodms0309)                         |                          [🌞 해온](https://github.com/hae-on)                           |                        [🐧 황펭](https://github.com/Leejin-Yang)                        |                           [😺 로건](https://github.com/70825)                            |                        [🥭 망고](https://github.com/Go-Jaecheol)                         |                         [👻 오잉](https://github.com/hanueleee)                          |                          [🍖 우가](https://github.com/wugawuga)                          |

<br>

<div align="center">
  <img src="https://github.com/woowacourse-teams/2023-fun-eat/assets/55427367/27ba38de-34b4-4925-a554-9bed89089984" alt="팀소개"/>
</div>
', 'https://github.com/woowacourse-teams/2023-fun-eat', NULL, '2023-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/woowacourse-teams/2023-map-befine/assets/89172499/6dc8fc88-2966-4209-8fe4-28d631443d59', 5, NULL, '괜찮을지도', 'map-befine', '괜찮을지도', '💡 내 관심사로 🗺 만든 지도…?  괜찮을 지도!📍', 70, '2026-08-07', 'CLOSED', 'APPROVED', '# 우아한테크코스 5기 괜찮을지도 팀

<a href="https://mapbefine.com">
  <p align="center">
    <img width="240" alt="Frame 205" src="https://github.com/woowacourse-teams/2023-map-befine/assets/89172499/6dc8fc88-2966-4209-8fe4-28d631443d59">
  </p>
  <p align="center" style="font-size: larger;">
    괜찮을지도 바로가기
  </p>
</a>

## 프로젝트 소개 📝

💡 내 관심사로 🗺 만든 지도…? 괜찮을 지도!📍
<br><br>
괜찮을 지도는 “지도 기반 참여형 데이터 매핑 서비스”로서,<br>
당신의 관심사를 📍다양한 지도로 만들 수 있게 도와드리고 있어요!😉🌈

<br />

### 프로젝트를 왜 시작하게 됐나요? (배경)

커뮤니티 매핑이란, 공통적인 관심을 가진 사람들이 협력하여 특정 주제에 대한 정보를 직접 수집하고,
이를 지도로 만들어 공유하고 이용하는 과정을 말해요! 이 아이디어를 기반으로, 다양한 사람들이 다양한 아이디어를 조금 더 손 쉽게 지도로 만들 수 있게 도움을 주고자
해당 프로젝트를 시작하게 되었습니다.🚀

<br />

### 프로젝트의 핵심 가치는 뭔가요? (핵심 가치)

다양한 주제의 정보들을 지도 기반으로 한눈에 볼 수 있도록 기록📝하고 공유🛜하는 거예요!

<br />

### 성공하면 어떤 모습일까요? (기대 효과)

개개인이 기록한 지도들을 공유함으로써, 보다 많은 사람들🌍에게 정보를 제공할 수 있어요!
더 나아가 여러 사용자들이 서로 협력도 가능해요!
서로 다른 주제의 지도가 가진 정보들을 한 번에 나타냄으로써, 새로운 정보를 도출할 수 있어요!

<br />
<br />

## 주요 기능 소개 🖥️

<p align="center">
  <img src="https://github.com/woowacourse-teams/2023-map-befine/assets/89172499/9db64262-a6b8-4aa8-ae7e-4073221581ec" alt="image" />
</p>

<br />
<br />
<br />

### 관심 있는 지도들을 골라 여러 장소를 한 번에 모아보세요.

예를 들어, 산책 명소 지도와 카페 지도를 모아 데이트 코스를 만들 수 있어요.

![image](https://github.com/woowacourse-teams/2023-map-befine/assets/89172499/26a2d8cf-e7bb-4f7e-89b4-b75e4ad4d55a)

<br />

### 기록하거나 공유하고 싶은 장소를 추가해 보세요.

지도에 핀을 추가해보세요. 장소에 이름을 붙이고, 나만의 생각과 사진을 남겨보세요.

![image](https://github.com/woowacourse-teams/2023-map-befine/assets/89172499/2e33a06b-6f24-4ebb-bb7e-f8005cb38c75)

<br />

### 나만의 지도를 만들어 함께 채워나가 보세요.

다 함께 볼 수도, 혼자 볼 수도 있어요. 원하는 친구들만 지도에 참여하도록 정할 수 있어요.

![image](https://github.com/woowacourse-teams/2023-map-befine/assets/89172499/bbc93da0-eb71-47f4-96e8-c87f29f7bf96)

<br />

### 공개된 다른 지도에서 마음에 드는 핀을 가져올 수도 있어요.

마음에 드는 핀들만 뽑아서 새로운 지도를 쉽게 만들어보세요. 나만의 고유한 핀으로 복사할 수 있어요.

![image](https://github.com/woowacourse-teams/2023-map-befine/assets/89172499/43a367b2-92f5-48d0-992e-da5a0b12e343)

<br />
<br />

## 기술 스택 ⚙️

### 백엔드

![image](https://github.com/woowacourse-teams/2023-map-befine/assets/89172499/4887940f-c459-4374-be5c-430345061bec)

### 프론트엔드

![image](https://github.com/woowacourse-teams/2023-map-befine/assets/89172499/2b963043-0f4a-4231-95df-7a97a37d5b08)

<br />
<br />

## 인프라 🧬

### 인프라 구조

![인프라 png 001](https://github.com/woowacourse-teams/2023-map-befine/assets/89172499/7e8d69ca-424f-4aef-8791-131b02468c55)

### CI/CD 파이프라인

![KakaoTalk_Photo_2024-01-21-20-39-21](https://github.com/woowacourse-teams/2023-map-befine/assets/89172499/47010f11-c1cc-4314-a301-d4a9f38c475b)

<br />
<br />

## 팀원 👨‍👨‍👧‍👧👩‍👦‍👦

|                                        Frontend                                         |                                         Frontend                                          |                                         Frontend                                          |
| :-------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------: |
| <img src="https://avatars.githubusercontent.com/u/89172499?v=4" width=130px alt="세인"> | <img src="https://avatars.githubusercontent.com/u/33995840?v=4" width=130px alt="아이크"> | <img src="https://avatars.githubusercontent.com/u/72205402?v=4" width=130px alt="패트릭"> |
|                          [세인](https://github.com/semnil5202)                          |                           [아이크](https://github.com/afds4567)                           |                           [패트릭](https://github.com/GC-Park)                            |

|                                         Backend                                          |                                         Backend                                          |                                          Backend                                          |                                         Backend                                         |
| :--------------------------------------------------------------------------------------: | :--------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------: |
| <img src="https://avatars.githubusercontent.com/u/97426362?v=4" width=130px alt="도이"/> | <img src="https://avatars.githubusercontent.com/u/89840550?v=4" width=130px alt="매튜"/> | <img src="https://avatars.githubusercontent.com/u/112045553?v=4" width=130px alt="준팍"/> | <img src="https://avatars.githubusercontent.com/u/50602742?v=4" width=130px alt="쥬니"> |
|                            [도이](https://github.com/yoondgu)                            |                           [매튜](https://github.com/kpeel5839)                           |                           [준팍](https://github.com/junpakPark)                           |                           [쥬니](https://github.com/cpot5620)                           |

<br />
<br />

## 팀 문화 🏠

#### 기억보단 기록을

    - 결과물 없는 회의는 하지 않아요..
    - 클립보드에서 SSD로..!

#### 나 홀로 머지… 머지?

    - 나 혼자 밥을 먹고, 나 혼자 PR하고, 나 혼자 머지하고..
    - PR은 혼자, Merge는 다 함께!

#### 일주일에 한 번 식사와 회고를 한다.

    - 잡담이 경쟁력이다!
    - 내가 느낀 모든 것! 아쉬운 점도 감추지 않고 얘기해요.

#### 회의는 짧고 굵게!

    - 시간을 정해서 한 주제를 마무리 지어요.
    - 50분 회의에 10분 휴식

#### 10시 1분은 10시가 아니다.

    - 나의 1분은 모두의 7분입니다.
    - 돈으로 못사는 시간! 서로 존중합시다!

#### 협업은 일과시간 내에! (10 to 18)

    - 협업은 100M 달리기가 아닌, 마라톤
    - 혼자만의 시간 존중해주세요.

#### 침묵도 곧 부정이다!

    - 말하지 않으면 몰라요~
    - 누구인가? 누가 말하지 않았어?

#### 한 명이 말하면 여섯 명이 듣는다.

    - 더 좋은 아이디어를 알 기회를 놓치지는 않았나요?
    - 모두의 의견을 들을 때까지 잠시 더 기다려봐요.

#### 회의를 통해 결정된 것은 우리 모두의 책임이다.

    - 제발 현재를 살아…

#### 내가 뭐하는 지 팀이 알고, 팀이 뭐하는 지 내가 안다.

    - 당신의 Think, 우리와 Sync!
    - 데일리 미팅 (매일 10:00)
        - 컨디션 체크, 오늘 할 일, 공지사항 전파
    - 마무리 미팅 (매일 17:30)
        - 오늘까지의 진행 사항, 내일 할 일 전파

#### 질문을 두려워하지 마라

    - 이해하지 못했는데 알겠다고 대답하지 말아요.
    - 모르는 건 죄가 아니지만 모르는 데 아는 척 하는 건 죄입니다.
', 'https://github.com/woowacourse-teams/2023-map-befine', NULL, '2023-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/woowacourse-teams/2023-team-by-team/assets/79538610/4a919657-68e2-4f38-8576-95625649c55e', 5, NULL, '팀바팀', 'team-by-team', '팀바팀', '✨쉽고 간단한 팀플 플랫폼, 팀바팀✨', 102, '2026-08-07', 'CLOSED', 'APPROVED', '<div align="center">
<a href="https://teamby.team/">
<img width="150px" src="https://github.com/woowacourse-teams/2023-team-by-team/assets/79538610/4a919657-68e2-4f38-8576-95625649c55e" alt="팀바팀 이동하기"/>
</a>

[![](https://img.shields.io/badge/-teamby.team-important?style=flat&logo=airplayvideo&logoColor=white&labelColor=black&color=%233145FF)](https://teamby.team/) 
[![](https://img.shields.io/badge/-Tech%20Blog-important?style=flat&logo=angellist&logoColor=balck&labelColor=black&color=white)](https://team-by-team.github.io/) 
![GitHub Release](https://img.shields.io/github/v/release/woowacourse-teams/2023-team-by-team)


# 팀바팀

### ✨쉽고 간단한 팀플 플랫폼, 팀바팀✨

</div>

## 🗨️ About TeamByTeam

대학생의 학기를 빛내주는 동시에 그 과정을 힘들게 만드는 것, 바로 ''팀플''입니다. 서로 다른 팀과 팀원들, 그리고 다양한 프로젝트 일정으로 복잡해지는 팀플 생활... 이러한 불편함을 겪으신 적 있으신가요? 협업이 요구되며, 다양한 일정과 자료를 관리하고 의사소통을 원활히 이뤄내는 것은 쉽지 않죠.  
하지만 걱정하지 마세요.

팀플을 더욱 효율적으로, 즐겁게, 그리고 편안하게 관리할 수 있는 `팀바팀`이 여기 있으니까요!  
팀바팀은 팀플이 많은 대학생들을 위한 플랫폼입니다.

여러분의 귀중한 시간, 학점을 걱정없이 만족시킬 팀바팀의 매력적인 기능들을 살펴볼까요?

## 🖥️ Service

<table>
<tr >
<td align="center">
모아보기 페이지
</td>
</tr>
<tr>
<td align="center">
      <img src=''https://github.com/woowacourse-teams/2023-team-by-team/assets/79538610/bdd33938-1838-4b34-86f1-7cee4d8a37dc''>
    </td>
</tr>
</table>

|                                                        팀 캘린더                                                        |                                                         팀 채팅                                                         |
| :---------------------------------------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------------------------------------: |
| <img src=''https://github.com/woowacourse-teams/2023-team-by-team/assets/79538610/f223d8bc-9bb4-482d-acf1-34db66eb93e0''> | <img src=''https://github.com/woowacourse-teams/2023-team-by-team/assets/79538610/7440e842-3c81-4b5f-ba54-5a2787561e31''> |
|                                                     <b>팀 링크</b>                                                      |                                                <b>팀 생성 및 팀 참가</b>                                                |
| <img src=''https://github.com/woowacourse-teams/2023-team-by-team/assets/79538610/767f9240-04e3-43c4-8b10-d2ed14606be6''> | <img src=''https://github.com/woowacourse-teams/2023-team-by-team/assets/79538610/203982f5-1fc4-4d08-aa53-e6f1d5f76ab0''> |

<p align="center">
    <a href=''https://sites.google.com/woowahan.com/woowacourse-demo-5th/%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8/%ED%8C%80%EB%B0%94%ED%8C%80''>팀바팀을 더 자세히 알고 싶다면, 여기로!</a>
</p>
<br/>

## 👻 Member

<table>
<tr>
<td align="center"> 프론트엔드</td>
<td align="center"> 프론트엔드</td>
<td align="center"> 프론트엔드</td>
<td align="center"> 백엔드</td>
<td align="center"> 백엔드</td>
<td align="center"> 백엔드</td>
<td align="center"> 백엔드</td>
</tr>
  <tr>
    <td align="center" width="120px">
      <a href="https://github.com/hafnium1923" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/79538610?v=4" alt="루루 프로필" />
      </a>
    </td>
    <td align="center" width="120px">
      <a href="https://github.com/wzrabbit" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/87642422?v=4" alt="요술토끼 프로필" />
      </a>
    </td>
    <td align="center" width="120px">
      <a href="https://github.com/suyoungj" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/19235163?v=4" alt="유스 프로필" />
      </a>
    </td>
    <td align="center" width="120px">
      <a href="https://github.com/pilyang" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/30036534?v=4" alt="필립 프로필" />
      </a>
    </td>
    <td align="center" width="120px">
      <a href="https://github.com/the9kim" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/96895686?v=4" alt="로이 프로필" />
      </a>
    </td>
    <td align="center" width="120px">
      <a href="https://github.com/sh111-coder" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/95729738?v=4" alt="성하 프로필" />
      </a>
    </td>
    <td align="center" width="120px">
      <a href="https://github.com/SproutMJ" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/86831441?v=4" alt="엔델 프로필" />
      </a>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="https://github.com/hafnium1923" target="_blank">
        루루
      </a>
    </td>
     <td align="center">
      <a href="https://github.com/wzrabbit" target="_blank">
       요술토끼
      </a>
    </td> 
     <td align="center">
      <a href="https://github.com/suyoungj" target="_blank">
       유스
      </a>
       <td align="center">
      <a href="https://github.com/pilyang" target="_blank">
        필립
      </a>
    </td>
     <td align="center">
      <a href="https://github.com/the9kim" target="_blank">
       로이
      </a>
    </td> 
     <td align="center">
      <a href="https://github.com/sh111-coder" target="_blank">
       성하
      </a>
     <td align="center">
      <a href="https://github.com/SproutMJ" target="_blank">
       엔델
      </a>
  </tr>
</table>

## 🛠️ Skills

<img width="500px" src=''https://github.com/woowacourse-teams/2023-team-by-team/assets/79538610/cade7c34-977d-413c-bd37-e5796060c0bf''  alt="Skills"/>

## ⚙️ Infra

<img width="600px" src=''https://github.com/woowacourse-teams/2023-team-by-team/assets/79538610/05794e0c-651a-4fc2-a535-fe8312493d9f''  alt="Infra"/>

## 🪄 CI/CD

<img width="600px" src=''https://github.com/woowacourse-teams/2023-team-by-team/assets/79538610/79f30380-86d9-4cf1-b801-c19b3c866a88''  alt="CI/CD"/>

## 🏆 Ground Rules

<img width="600px" src=''https://github.com/woowacourse-teams/2023-team-by-team/assets/79538610/f917b168-a3f2-4aff-9eb8-61bb55b189ce''  alt="CI/CD"/>
', 'https://github.com/woowacourse-teams/2023-team-by-team', NULL, '2023-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/woowacourse-teams/2023-trip-draw/assets/58586537/215f0b59-d545-4043-84fa-871c192b8dbd', 5, NULL, '트립드로우', 'trip-draw', '트립드로우', '나만의 여행을 그리다. ✈️', 20, '2026-08-07', 'CLOSED', 'APPROVED', '<p align="center">
    <img src=https://github.com/woowacourse-teams/2023-trip-draw/assets/58586537/215f0b59-d545-4043-84fa-871c192b8dbd  width="320" height="320">
</p>

<div align="center">

  나만의 여행을 그리다. <br>
  트립드로우 ✈️

</div>
<br/>

<div align="center">

[![Team Blog](http://img.shields.io/badge/Team%20Blog-000000?style=flat&logo=docusaurus&logoColor=white&link=https://tripdraw.blog/)](https://tripdraw.blog/)
[![Release](https://img.shields.io/github/v/release/woowacourse-teams/2023-trip-draw?color=blue)](https://github.com/woowacourse-teams/2023-trip-draw/releases/tag/1.1.2)

</div>
<br/>

## 🚩 서비스 소개

<img width="250" alt="image" src="https://github.com/woowacourse-teams/2023-trip-draw/assets/69189793/f92b9d92-b4b5-4aad-b1d1-fea38caa1e32">
<img width="250" alt="image" src="https://github.com/woowacourse-teams/2023-trip-draw/assets/69189793/91613e0d-e528-4021-b166-7ed749890504">
<img width="250" alt="image" src="https://github.com/woowacourse-teams/2023-trip-draw/assets/69189793/e891084d-e165-4d25-8134-56e36473472d">   


여행을 시작하는 설렘부터, 여행을 마무리하는 아쉬움까지도 트립드로우가 언제나 당신의 여행과 함께 합니다!   

트립드로우는 당신이 내딛는 소중한 걸음을 모두 그려드려요.    

언젠가 꼭 다시 오기로 한 맛집의 이름을 외우지 마세요.    

머리 위로 쏟아질듯한 별들을 언제 만났는지 신경 쓰지 마세요.   

트립드로우가 전부 빼놓지 않고 적어드릴게요.    

당신은 이 여행을 온전히 즐기기만 하세요.


## 🚅 인프라 구조

<img width="900" alt="image" src="https://github.com/woowacourse-teams/2023-trip-draw/assets/58586537/4a9ce83a-d86a-4272-93c9-fd3ed830d1c4">

## ✈️ 우리팀

### Android

|[멧돼지](https://github.com/2chang5)|[수달](https://github.com/otter66)|[핑구](https://github.com/pingu244)|
|:-:|:-:|:--:|
|<img src="https://avatars.githubusercontent.com/u/54737136?v=4" alt="pig" width="100" height="100">|<img src="https://avatars.githubusercontent.com/u/69189793?v=4" alt="otter" width="100" height="100">|<img src="https://avatars.githubusercontent.com/u/69796976?v=4" alt="pingu" width="100" height="100">

### Backend

|[리오](https://github.com/Jaeyoung22)|[후추](https://github.com/Combi153)|[허브](https://github.com/greeng00se)|
|:-:|:-:|:--:|
|<img src="https://avatars.githubusercontent.com/u/89302528?v=4" alt="reo" width="100" height="100">|<img src="https://avatars.githubusercontent.com/u/106813090?v=4" alt="huchu" width="100" height="100">|<img src="https://avatars.githubusercontent.com/u/58586537?v=4" alt="herb" width="100" height="100">

<br>

## 🏃 팀문화

<img width="975" alt="image" src="https://github.com/woowacourse-teams/2023-trip-draw/assets/58586537/a7d8fd5e-fcab-4569-9c46-7df5c8bb0622">
', 'https://github.com/woowacourse-teams/2023-trip-draw', NULL, '2023-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/woowacourse-teams/2023-yozm-cafe/assets/86547109/45e62a4c-d2b0-4da3-8b8e-83964ccda70b', 5, NULL, '요즘카페', 'yozm-cafe', '요즘카페', '"트렌디한 성수 지역의 카페를 손쉽게 탐색하는 서비스, ☕️ 요즘카페"', 39, '2026-08-07', 'CLOSED', 'APPROVED', '# 2023-yozm-cafe
## 🖥️ 프로젝트 아키텍처 구조
### Product 환경
![image](https://github.com/woowacourse-teams/2023-yozm-cafe/assets/86547109/45e62a4c-d2b0-4da3-8b8e-83964ccda70b)
### CI/CD Pipeline
![image](https://github.com/woowacourse-teams/2023-yozm-cafe/assets/86547109/d78be142-9d72-437b-803d-fa349ad36cf9)

## ⭐️ 팀 요즘카페 소개
### 프론트엔드
|<img src="https://avatars.githubusercontent.com/u/122500517?v=4" width="150" height="150">|<img src ="https://avatars.githubusercontent.com/u/95906910?v=4)" width="150" height="150">|<img src ="https://avatars.githubusercontent.com/u/20203944?v=4)" width="150" height="150">
|:-:|:-:|:-:|
|[고니](https://github.com/jeongwusi)|[아인](https://github.com/geuntaek1013)|[솔로스타](https://github.com/solo5star)
### 백엔드
|<img src="https://avatars.githubusercontent.com/u/86547109?v=4" width="150" height="150">|<img src ="https://avatars.githubusercontent.com/u/93072571?v=4)" width="150" height="150">|<img src ="https://avatars.githubusercontent.com/u/96301958?v=4)" width="150" height="150">|<img src ="https://avatars.githubusercontent.com/u/96762301?v=4)" width="150" height="150">
|:-:|:-:|:-:|:-:|
|[오션](https://github.com/donghae-kim)|[연어](https://github.com/nuyh99)|[폴로](https://github.com/green-kong)|[도치](https://github.com/hum02)|
', 'https://github.com/woowacourse-teams/2023-yozm-cafe', NULL, '2023-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://user-images.githubusercontent.com/11745691/185735071-5eb23eaa-745b-4d69-a336-b64e5a6f011e.png', 4, NULL, '달록', 'dallog', '달록', '달력이 기록을 공유할 때, 달록 🌙', 158, '2026-08-07', 'CLOSED', 'APPROVED', '<div align="center">
<img src="https://user-images.githubusercontent.com/11745691/185735071-5eb23eaa-745b-4d69-a336-b64e5a6f011e.png" />

### 달력이 기록을 공유할 때, 달록 🌙

[<img src="https://img.shields.io/badge/-dallog.me-important?style=flat&logo=google-chrome&logoColor=white" />](https://dallog.me) [<img src="https://img.shields.io/badge/-tech blog-blue?style=flat&logo=google-chrome&logoColor=white" />](https://dallog.github.io) [<img src="https://img.shields.io/badge/release-v1.1.6-critical?style=flat&logo=google-chrome&logoColor=white" />](https://github.com/woowacourse-teams/2022-dallog/releases/tag/v1.1.6)

[](https://dallog.me)

</div>

## 🌙 소개

달록은 우아한테크코스 공유 캘린더입니다. 우아한테크코스 공식 일정, 데일리 팀, 스터디 등 파편화된 여러 일정을 모아 달록에서 관리할 수 있습니다. 사용자는 관심있는 일정 카테고리를 구독하여 개인화된 캘린더를 사용할 수 있습니다.

**[달록을 더 자세히 알아보고 싶다면, 여기로!](https://sites.google.com/woowahan.com/woowacourse-demo-4th/%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8/%EB%8B%AC%EB%A1%9D)**

## 🖥 서비스 화면

![](https://user-images.githubusercontent.com/11745691/194251748-1a5f5819-7ae8-4648-a45e-6c02399af812.png)

## 🛠 Tech Stacks

### Front-end

![](https://user-images.githubusercontent.com/11745691/197112888-c634aecc-fe5b-4087-94f9-cd4d0c4ab553.png)

### Back-end

![](https://user-images.githubusercontent.com/11745691/197112828-fd63411d-f7be-4501-b13e-5b450ccf0c40.png)

## ⚙️ Infrastructure

![](https://user-images.githubusercontent.com/11745691/197112936-d3b80ed4-f0fb-477a-8099-2600f36e9061.png)

## 🔀 CI/CD Pipeline

![](https://user-images.githubusercontent.com/11745691/197113000-dc562bfa-c1ad-4500-91d9-908b2d7c7014.png)

## 🌈 알록달록하게 일을 더 잘하는 9가지 방법

![](https://user-images.githubusercontent.com/11745691/185748153-bf170c7a-99cd-49ee-9420-397af9c7f35e.png)

## 👥 Members

|                   Backend                    |                      Backend                       |                     Backend                      |                   Backend                    |                    Frontend                    |                  Frontend                   |
| :------------------------------------------: | :------------------------------------------------: | :----------------------------------------------: | :------------------------------------------: | :--------------------------------------------: | :-----------------------------------------: |
| ![](https://github.com/hyeonic.png?size=120) | ![](https://github.com/gudonghee2000.png?size=120) | ![](https://github.com/summerlunaa.png?size=120) | ![](https://github.com/devHudi.png?size=120) | ![](https://github.com/daaaayeah.png?size=120) | ![](https://github.com/jhy979.png?size=120) |
|  [매트(최기현)](https://github.com/hyeonic)  |  [리버(구동희)](https://github.com/gudonghee2000)  |  [파랑(이하은)](https://github.com/summerlunaa)  |  [후디(조동현)](https://github.com/devHudi)  |  [티거(이다예)](https://github.com/daaaayeah)  |  [나인(장호영)](https://github.com/jhy979)  |
', 'https://github.com/woowacourse-teams/2022-dallog', NULL, '2022-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://user-images.githubusercontent.com/68512686/196422615-be297c9b-b082-41d0-a670-c8ba6580fa55.png', 4, NULL, '레벨로그', 'levellog', '레벨로그', '레벨 인터뷰의 모든 것, 레벨로그', 37, '2026-08-07', 'CLOSED', 'APPROVED', '<p align="center">
    <img width="400" alt="levellog-logo" src="https://user-images.githubusercontent.com/68512686/196422615-be297c9b-b082-41d0-a670-c8ba6580fa55.png">
</p>
<div align="center">
	우아한테크코스에서 진행되는 레벨로그(모의 인터뷰)의 준비부터 회고까지 모든 과정을 관리한다.
</div>

## 팀원 🤝

| [로마](https://github.com/kbsat) | [페퍼](https://github.com/SuyeonChoi) | [알린](https://github.com/OzRagwort) | [릭](https://github.com/nailseong) | [이브](https://github.com/2yujeong) | [결](https://github.com/yunjin-kim) | [해리](https://github.com/jihyeok-um) |
|:----------:|:----------:|:----------:|:----------:|:----------:|:----------:|:----------:|
|[<img src="https://user-images.githubusercontent.com/68512686/196420738-7db2611c-413e-49c3-a49e-8f54f1617cb4.png" alt="로마" width="260"/>](https://github.com/kbsat)|[<img src="https://user-images.githubusercontent.com/68512686/196420782-d9d5a877-03d6-4b21-96a7-9036862b530a.png" alt="페퍼" width="260"/>](https://github.com/SuyeonChoi)|[<img src="https://user-images.githubusercontent.com/68512686/196420811-d1fb2e47-8236-4497-a028-b6511835ec51.png" alt="알린" width="260"/>](https://github.com/OzRagwort)|[<img src="https://user-images.githubusercontent.com/68512686/196420831-87c86c93-356d-4827-aa9c-3c2a92c0f4ed.png" alt="릭" width="260"/>](https://github.com/nailseong)|[<img src="https://user-images.githubusercontent.com/68512686/196420847-9a5b7bfb-eb29-4b27-b0b7-5d381f67b17a.png" alt="이브" width="260"/>](https://github.com/2yujeong)|[<img src="https://user-images.githubusercontent.com/68512686/196421621-1d5ede01-c884-4229-aa29-c7d1bfbc9cbb.png" alt="결" width="260"/>](https://github.com/yunjin-kim)|[<img src="https://user-images.githubusercontent.com/68512686/196420876-22ffa03a-6d2b-404d-ae72-8a574b468a11.png" alt="해리" width="260"/>](https://github.com/jihyeok-um)|
|    BE    |    BE    |    BE    |    BE    |    BE    |    FE    |    FE    |

## 핵심가치 💎

<p align="center">
    <img width="1000" alt="core-value" src="https://user-images.githubusercontent.com/76840965/187828194-52ea0a32-40d5-412a-8814-3017955c13bc.png">
</p>

## 팀문화 📚

<p align="center">
    <img alt="culture" src="https://user-images.githubusercontent.com/68512686/196430596-72888c0b-b742-4c20-a228-d059a63c97fd.png" width="1000"/>
</p>

## 기술스택 🥞

### 💅 Frontend

<p align="center">
    <img alter="frontend-teck-stack" src="https://user-images.githubusercontent.com/68512686/196576181-7b0016ec-2d77-4181-863e-89f300be87be.png" width="600"/>
</p>
<br/>

### ⚙️ Backend

<p align="center">
    <img alter="backend-teck-stack" src="https://user-images.githubusercontent.com/68512686/196430649-f1374247-9fe4-44ed-8ce6-4a7a7eb67109.png" width="600"/>
</p>
<br/>

### 🧬 Infrastructure

<p align="center">    
    <img alter="infla-teck-stack" src="https://user-images.githubusercontent.com/68512686/196430664-0f814761-db5e-4459-9893-bff9189d0b60.png" width="600"/>
</p>

## 인프라 구조 ⛑

<p align="center">
    <img alter="ci-cd-process" src="https://user-images.githubusercontent.com/68512686/196430680-a64dd77f-731b-468f-b50d-debdee833c0b.png" width="800"/>
</p>

## CI/CD Process 🏗

<p align="center">
    <img alter="infra-structure" src="https://user-images.githubusercontent.com/68512686/196430698-916d28ad-16c2-4778-8622-515ce277d90c.png" width="800"/>
</p>
', 'https://github.com/woowacourse-teams/2022-levellog', NULL, '2022-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://user-images.githubusercontent.com/28296575/185556330-a213b12c-a647-4e65-a216-767bed85f183.png', 4, NULL, '모모', 'momo', '모모', '한 눈에 참여하는 모임 서비스, ''모두 모여라'' 모모입니다.', 48, '2026-08-07', 'CLOSED', 'APPROVED', '<div align="center">
<h1> 모두 모여라, MOMO </h1>
</div>

<p align="center">
    <img src="https://user-images.githubusercontent.com/28296575/185556330-a213b12c-a647-4e65-a216-767bed85f183.png" alt="pick-git-logo" width="220" height="220">
</p>

## 프로젝트 소개

한 눈에 참여하는 모임 서비스, **모모**입니다!

## 핵심 기능

🚀 **내가 원하는 모임을 쉽게 찾을 수 있어요!**

스터디, 모각코, 식사, 카페, 운동, 게임, 여행 등 다양한 카테고리에서 원하는 모임들을 찾아보세요!

새로운 사람들과 함께하는 즐거운 시간이 여러분을 기다립니다.

<img src="https://user-images.githubusercontent.com/57928612/196835204-718a978a-c835-4752-af8a-d08a648ce989.gif" height="400" />

<br>

🚀 **모임을 찾지 못하셨나요? 직접 만들어보세요!**

무엇을 적어야할지 몰라도 괜찮아요. 한 단계씩 따라가다보면 원하는 모임을 쉽게 만들 수 있어요!

<img src="https://user-images.githubusercontent.com/57928612/196837566-fcb22c45-3983-4daa-9411-8a8953934ab4.gif" height="400" />

<br>

🚀 **모임 내용이 변경되었나요? 걱정마세요!**

일정 변경도, 사소한 오타도 괜찮아요. 간단하게 수정할 수 있어요.

<img src="https://user-images.githubusercontent.com/57928612/196835458-f2b4db36-41fe-43df-8fc8-d2b37ead02cb.gif" height="400" />

<br>

🚀 **내가 원하는 모임을 찾아 참여해보세요!**

흥미있는 모임을 찾아 참여해보세요!

<img src="https://user-images.githubusercontent.com/57928612/196835497-7eaee8dc-163b-4e44-875f-1152a292a597.gif" height="400" />

<br>

🚀 **나의 모임을 확인할 수 있어요!**

내가 참여하거나, 주최하거나, 찜한 모임을 한 곳에 모아 관리해보세요!

<img src="https://user-images.githubusercontent.com/57928612/196835623-b11f0e7e-e515-41f0-b4d5-fb7ef5e696af.gif" height="400" />

<br>

## 프로젝트 기술 스택

### 프론트엔드

<img src="https://user-images.githubusercontent.com/57928612/196930938-2dc0b509-3085-479c-88b2-42786d0511ce.png" height="400" />

### 백엔드

- Java 11
- Springboot 2.6.9
- Mysql 8

<img src="https://user-images.githubusercontent.com/57928612/196930854-ed14853b-47ae-4da2-96be-5bad41c6a532.png" height="400" />

## 프로젝트 아키텍처

### 인프라 아키텍처

<img src="https://user-images.githubusercontent.com/57928612/196931135-cdb9464f-c379-4d2a-a3bc-8e4ec75bed39.png" height="450" />

### 프론트 CI/CD

<img src="https://user-images.githubusercontent.com/57928612/196931212-4a214017-1e0a-4e24-b41b-a9f7110bd06b.png" height="250" />

### 백엔드 CI/CD

<img src="https://user-images.githubusercontent.com/57928612/196931031-f62cd639-15ed-4e8a-a8cb-3a160e683da1.png" height="500" />

---

## 모모 서비스 이용하기

### 모모 방문하기 🏃🏻

👉 [https://www.moyeora.site/](https://www.moyeora.site/)

### 모모의 기술 블로그 🧇

👉 [바로가기](https://2022-momo.github.io/)

### Wiki 📑

👉 [Wiki Page](https://github.com/woowacourse-teams/2022-momo/wiki)

### Youtube 📺

| 1차 데모데이                                                                                  | 2차 데모데이                                                                                  | 3차 데모데이                                                                                  | 4차 데모데이                                                                                  |
| --------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| [![1차 데모데이](https://img.youtube.com/vi/-86HlsrqgJY/0.jpg)](https://youtu.be/-86HlsrqgJY) | [![2차 데모데이](https://img.youtube.com/vi/FvhTuj_Cxvk/0.jpg)](https://youtu.be/FvhTuj_Cxvk) | [![3차 데모데이](https://img.youtube.com/vi/W5Rloao4zuQ/0.jpg)](https://youtu.be/W5Rloao4zuQ) | [![4차 데모데이](https://img.youtube.com/vi/Qa944GNc2ec/0.jpg)](https://youtu.be/Qa944GNc2ec) |

## 팀원👨‍💻👩‍💻

|                                  Backend                                   |                                   Backend                                    |                                       Backend                                        |                                   Backend                                    |                                    Frontend                                    |                                         Frontend                                         |
| :------------------------------------------------------------------------: | :--------------------------------------------------------------------------: | :----------------------------------------------------------------------------------: | :--------------------------------------------------------------------------: | :----------------------------------------------------------------------------: | :--------------------------------------------------------------------------------------: |
|       ![image](https://avatars.githubusercontent.com/u/57744251?v=4)       |        ![image](https://avatars.githubusercontent.com/u/22176552?v=4)        |            ![image](https://avatars.githubusercontent.com/u/76891875?v=4)            |        ![image](https://avatars.githubusercontent.com/u/92148749?v=4)        |         ![image](https://avatars.githubusercontent.com/u/57928612?v=4)         |              ![image](https://avatars.githubusercontent.com/u/28296575?v=4)              |
| [렉스](https://github.com/Seongwon97)<br/>[📚 Blog](https://seongwon.dev/) | [이프](https://github.com/sinb57)<br/>[📚 Blog](https://sinb57.tistory.com/) | [라쿤](https://github.com/nbalance97)<br/>[📚 Blog](https://nbalance97.tistory.com/) | [유콩](https://github.com/kyukong)<br/>[📚 Blog](https://velog.io/@rudnf003) | [하리](https://github.com/LAH1203)<br/>[📚 Blog](https://lah1203.netlify.app/) | [유세지](https://github.com/usageness)<br/>[📚 Blog](https://blog-usageness.vercel.app/) |

## 🤼 모모의 팀 문화

<img src="https://user-images.githubusercontent.com/57744251/192219106-f2f9b3ed-8609-4430-80dc-efb5008c1252.png" alt="team culture" height="350">
', 'https://github.com/woowacourse-teams/2022-momo', NULL, '2022-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://raw.githubusercontent.com/woowacourse-teams/2022-moragora/dev/checkmate.png', 4, NULL, '체크메이트', 'moragora', '체크메이트', '주제만 정해주세요, 출첵은 제가할게요! ✅', 37, '2026-08-07', 'CLOSED', 'APPROVED', '<div align="center">
<img src="https://raw.githubusercontent.com/woowacourse-teams/2022-moragora/dev/checkmate.png" width="250px">
<h1>체크메이트</h1>

</div>

<div align="center">

<strong>체크메이트</strong>는 출석 체크와 지각 이력 관리를 간편하게 해주는 서비스입니다.

사람이 직접 출석 체크를 하고, 지각 횟수를 관리한다는 것은 누락되기도 쉽고 번거로운 일입니다.

그래서 체크메이트는 출석 관리를 더 편하게 할 수 있도록 아래의 핵심 기능을 제공합니다.

<strong>일정 관리&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;GPS 기반 출석 체크&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;모임 출결 관리&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;지각에 따른 페널티 부여</strong>

</div>

## 서비스 미리보기

<div align="center">
<img src="https://raw.githubusercontent.com/woowacourse-teams/2022-moragora/dev/checkmate-preview.png" width="600px">
</div>

## 아키텍쳐

### 백엔드

<img src="https://user-images.githubusercontent.com/66164361/200206297-072d5b03-f5c5-4b4c-8e36-2a473a21848d.png">

## 기술 스택

### 프론트엔드

<img src="https://user-images.githubusercontent.com/66164361/200259852-392e164a-53ff-4181-a1da-c1b11e906b7a.png">

### 백엔드

<img src="https://user-images.githubusercontent.com/66164361/200259607-273be273-e971-45cb-a840-663a54778fe5.png">

## 팀원

|                    Backend                    |                    Backend                    |                      Backend                      |                       Backend                       |                       Backend                       |                       Frontend                        |                   Frontend                    |
| :-------------------------------------------: | :-------------------------------------------: | :-----------------------------------------------: | :-------------------------------------------------: | :-------------------------------------------------: | :---------------------------------------------------: | :-------------------------------------------: |
| ![](https://github.com/syoun602.png?size=120) |  ![](https://github.com/YJGwon.png?size=120)  | ![](https://github.com/Hongdonggeon.png?size=120) |   ![](https://github.com/shindong96.png?size=120)   |  ![](https://github.com/progress0407.png?size=120)  |  ![](https://github.com/greenblues1190.png?size=120)  |  ![](https://github.com/kamwoo.png?size=120)  |
| [썬<br>(윤선용)](https://github.com/syoun602) | [포키<br>(권예진)](https://github.com/YJGwon) | [쿤<br>(홍동건)](https://github.com/Hongdonggeon) | [아스피<br>(신동석)](https://github.com/shindong96) | [필즈<br>(조성우)](https://github.com/progress0407) | [우디<br>(우정민)](https://github.com/greenblues1190) | [밧드<br>(감우영)](https://github.com/kamwoo) |
', 'https://github.com/woowacourse-teams/2022-moragora', NULL, '2022-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://user-images.githubusercontent.com/80666066/193769096-9162414f-16ff-4c74-878f-5661b0f671cc.png', 4, NULL, '줍줍', 'pickpick', '줍줍', '🐹 사라지는 Slack 메시지, 우리가 주워줄게!', 75, '2026-08-07', 'CLOSED', 'APPROVED', '<div align=center>
  <img width="492" alt="스크린샷 2022-09-30 오후 5 31 24" src="https://user-images.githubusercontent.com/80666066/193769096-9162414f-16ff-4c74-878f-5661b0f671cc.png">
  <h2> 사라지는 Slack 메세지, 우리가 주워줄게! </h2>
  https://jupjup.site/
  <br>
  <br>
  <strong>줍줍</strong>은 연결된 슬랙 워크스페이스 메시지를 실시간 백업하여
  <br>
  무료 워크스페이스라도 잠길 걱정 없이 언제든 볼 수 있게 해주는 서비스입니다
  <br>
  <br>

[![Application](http://img.shields.io/badge/Application-F46A54?style=flat&logo=github&logoColor=white&link=https://jupjup.site/)](https://jupjup.site/)
[![Storybook](http://img.shields.io/badge/Storybook-FF4785?style=flat&logo=Storybook&logoColor=white&link=https://62e64dc73aafd7bc9338ba73-imzhfpkupu.chromatic.com/?path=/story/layouts-header--default-template)](https://62e64dc73aafd7bc9338ba73-imzhfpkupu.chromatic.com/?path=/story/layouts-header--default-template )
[![API Docs](http://img.shields.io/badge/-API%20Docs-important?style=flat&logo=dev.to&logoColor=white&link=https://dev.jupjup.site/docs)](https://dev.jupjup.site/docs)
[![WIKI](http://img.shields.io/badge/-GitHub%20WiKi-395FC1?style=flat&logo=GitHub&logoColor=white&link=https://github.com/woowacourse-teams/2022-pickpick/wiki)](https://github.com/woowacourse-teams/2022-pickpick/wiki)
<br>
[![서비스_이용_가이드](http://img.shields.io/badge/-서비스_이용_가이드-81B441?style=flat&logo=Pinboard&logoColor=white&link=https://github.com/woowacourse-teams/2022-pickpick/wiki/%EC%84%9C%EB%B9%84%EC%8A%A4-%EC%9D%B4%EC%9A%A9-%EA%B0%80%EC%9D%B4%EB%93%9C)](https://github.com/woowacourse-teams/2022-pickpick/wiki/%EC%84%9C%EB%B9%84%EC%8A%A4-%EC%9D%B4%EC%9A%A9-%EA%B0%80%EC%9D%B4%EB%93%9C)
[![체험_가이드](http://img.shields.io/badge/-체험_가이드-6F53F3?style=flat&logo=Lemmy&logoColor=white&link=https://github.com/woowacourse-teams/2022-pickpick/wiki/%EC%84%9C%EB%B9%84%EC%8A%A4-%EC%B2%B4%ED%97%98-%EA%B0%80%EC%9D%B4%EB%93%9C)](https://github.com/woowacourse-teams/2022-pickpick/wiki/%EC%84%9C%EB%B9%84%EC%8A%A4-%EC%B2%B4%ED%97%98-%EA%B0%80%EC%9D%B4%EB%93%9C)


</div>

## 팀원 소개 👩🏻‍💻🧑🏻‍💻

|                                              [🐈‍⬛ 호프](https://github.com/moonheekim0118)                                               |                                              [👍 꼬재](https://github.com/kkojae91)                                               |                                               [🌱 봄](https://github.com/JangBomi)                                                |                                               [🏝 써머](https://github.com/hyewoncc)                                               |                                          [🪁 연로그](https://github.com/yeon-06)                                           |
| :-------------------------------------------------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------------------------------------------: | :------------------------------------------------------------------------------------------------------------------------: |
| <a href="https://github.com/moonheekim0118"> <img src="https://avatars.githubusercontent.com/u/61469664?v=4" width=200px alt="_"/> </a> | <a href="https://github.com/kkojae91"> <img src="https://avatars.githubusercontent.com/u/68001045?v=4" width=200px alt="_"/> </a> | <a href="https://github.com/JangBomi"> <img src="https://avatars.githubusercontent.com/u/55357130?v=4" width=200px alt="_"/> </a> | <a href="https://github.com/hyewoncc"> <img src="https://avatars.githubusercontent.com/u/80666066?v=4" width=200px alt="_"/> </a> | <a href="https://github.com/yeon-06"> <img src="https://avatars.githubusercontent.com/u/53105735?v=4" width=200px alt="_"> |
|                                                               프론트엔드                                                                |                                                            프론트엔드                                                             |                                                              백엔드                                                               |                                                              백엔드                                                               |                                                           백엔드                                                           |
|                                                 팀원들이 보는 호프는                                                                    |                                                       팀원들이 보는 꼬재는                                                        |                                                         팀원들이 보는 봄은                                                        |                                               팀원들이 보는 써머는                                                                |                                              팀원들이 보는 연로그는                                                        |
| 🕵️‍부지런한 해결사 <br/> 👩‍💻개발이 제일 좋아, 찐 개발자 <br/> 🍜밥 잘 먹고 코드 맛있게 짜는 사람 <br/> 🚗맡은 일은 끝까지 간다! 진격의 개발자|🤩분위기 메이커<br/>😁항상 웃긴 재밌는 사람<br/>🏃‍매일같이 문열고 문닫는 성실왕<br/>👨‍🏫사소한 디테일 놓치지 않는 꼼꼼왕|🥳 언제나 맑은 긍정왕 <br />🔫 듬직한 트러블 슈터 <br />🤩항상 밝은 분위기 메이커 <br />🎯 버그 꼼짝마! 백발백중 버그 퇴치|🙋‍♀️ 솔선수범 맏언니 <br />🧠 다재다능 아이디어뱅크 <br />💯 멋진 테스트코드에 관심 많은 사람 <br />👩‍💻 개발은 거들 뿐 뭐든지 맡겨만 줘|📚 깔끔하고 센스 있는 정리왕 <br />📰 모든걸 기록하는 꼼꼼한 사람 <br />🌳 팀의 버드나무 든든한 버팀목 <br />😎 코드리뷰는 소나큐브? 아니! 연나큐브!|

<br>

## ✨ 프론트엔드 기술 스택 

![프론트엔드기술스택](https://user-images.githubusercontent.com/80666066/198928140-3b6a081e-f3e6-401e-b58f-568792f67847.png)

## ⚡️ 백엔드 기술 스택 

![백엔드기술스택](https://user-images.githubusercontent.com/80666066/198928111-a3e64d8b-abfb-47ca-a97f-14d690fc40dc.png)

## ⚙️ 인프라 기술 스택 

![인프라](https://user-images.githubusercontent.com/80666066/198929767-4f45e341-aa26-4edd-9467-9e2c265df2e3.png)

## 🕊 프론트엔드 인프라 구조  

![프론트엔드 인프라](https://user-images.githubusercontent.com/80666066/198930210-c01588b1-ffca-4bb3-a2bc-9bbd5148c6e2.png)

## 🦉 백엔드 인프라 구조  

![백엔드 CI](https://user-images.githubusercontent.com/80666066/198933629-a48ebcf7-0d9b-444b-90da-43afe7dd11cc.png)

![백엔드 CD](https://user-images.githubusercontent.com/80666066/198933731-e90f2baf-cd2a-4299-955e-2b4cb9e0825b.png)

## 🤝 팀문화

#### 1. 매일 미팅을 가져요

- 오전 10시에 하루를 시작하는 미팅을 해요
- 오후 5시 40분에 마무리 미팅을 해요
- 주제는 업무 상황부터 사소한 잡담까지 자유로워요

#### 2. 매주 함께 회고해요

- 매주 금요일 오후에 주간 회고를 가져요
- 한 주 동안 세웠던 목표와 결과를 공유해요
- 감정 회고도 함께해 큰 갈등이 되기전에 해결하려 노력해요

#### 3. 의견은 자유롭게, 책임은 다 같이 져요

- 반대 의견도 자유롭게 내요
- 정해진 의견은 그 때부터 팀의 의견이니 문제가 생겨도 팀원을 탓하지 않아요

#### 4. 리액션과 칭찬은 햄스터도 춤추게 해요

- 사소한 것에도 적극적인 리액션과 칭찬으로 반응해요
- 줍줍에서는 후식 볶음밥만 잘 볶아도 칭찬받아요 👍

#### 5. 함께 자라요

- 공유의 생활화로 혼자가 아닌 함께 자라요

#### 6. 그 외에도 이런 문화가 있어요

- 서로를 신뢰하기 때문에 지각 패널티가 없어요
- PR은 지정된 리뷰어 모두가 승인해야 머지할 수 있어요

<br/>
  
  
', 'https://github.com/woowacourse-teams/2022-pickpick', NULL, '2022-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('http://img.youtube.com/vi/R8y-4GqqSg0/0.jpg', 4, NULL, '티타임', 'teatime', '티타임', '서로를 채워주는 시간, 티타임 ☕️', 35, '2026-08-07', 'CLOSED', 'APPROVED', '# 티타임☕️
우리는 면담을 통해서 위로를 받고 힘을 얻습니다. 하지만 면담을 신청하기가 어렵게 느껴질 때도 있습니다. 이러한 문제를 해결하고 면담을 활성화시키는 것이 저희 서비스의 목표입니다.
티타임을 이용해서 면담을 티타임처럼 즐겨주세요!
<br>

# Let''s Teatime! 👀
[![티타임 소개영상](http://img.youtube.com/vi/R8y-4GqqSg0/0.jpg)](https://youtu.be/R8y-4GqqSg0)<br>
⬆️ 티타임 소개 영상 보러가기<br><br>

|예약하기|질문지 작성|
|---|---|
|![S2rlanEzVoBhhNWdQ2D-e9mhnCnrwQl7GvQvckVZKm4UVWuE3sq-pfJFvcDZB0IDTmI1_pqMyXI9vVHZ_bTGsvOJn8jKUo93KgHteST1ig](https://user-images.githubusercontent.com/60432062/196633002-204e05b1-ef89-4259-a82d-480a42731f00.gif)|![sK5Gy2E9y0BjmpTG-RKwfLTrSPqZ-aaxGesEmfr7yy9F7n4EvFzrzx4rGnNvf1iHjtHLPrGfnJKf1SYP9mZWMS5PVCdeta7MBcKUpvQU5A](https://user-images.githubusercontent.com/60432062/196633305-d4b72c43-eabe-4333-be00-40a952e9571b.gif)|

|스케줄 등록|일정 관리|
|---|---|
|![aRHRNGbqVSnKssVF2PnVSVCDzMmoHRoHTYl3PBmNlXSJ8rh9TIAM3m06lbcFEtHlF1bAxyFe2gkgZDQs6mc4mVYbqYyRsUYkh9vmy8HJ5w](https://user-images.githubusercontent.com/60432062/196632784-5c153630-6676-445e-8e5a-4e506845ed5c.gif)|![imCmlf3extrTxRytSDEj0pKyYE-YEiFx_VN8oGn7XeDb4QsAVD-DEjWCdUKyx_keJiltim5Hr3VuhoInXqILmKmE9SKr39Exi-Dd9XGKgA](https://user-images.githubusercontent.com/60432062/196633487-6faabe26-343d-4211-8723-720fc422287a.gif)|
<br>

# 기술 스택 🛠
![image](https://user-images.githubusercontent.com/60432062/196638119-228df2b7-7f3a-463f-b38a-9e02fd50781a.png)
![image](https://user-images.githubusercontent.com/60432062/196638461-45916b20-7900-4733-9db7-9b900899fa9f.png)
<br>

# 인프라 구조 🎢
|Product|CI/CD|
|---|---|
|<img width="500" alt="image" src="https://user-images.githubusercontent.com/60432062/197088620-df54e8e3-805a-4550-844c-45c7e1c03e3f.png">|<img width="500" alt="image" src="https://user-images.githubusercontent.com/60432062/196639106-2735aec4-f53b-4e3c-bc6b-fa11086db7c0.png">|
<br>

# 팀원들 👩‍💻🧑‍💻
## Frontend
|[안](https://github.com/jin7969)|[코이](https://github.com/InKyoJeong)|
| :---------------------------------------------------: | :---------------------------------------------------: |
|<img width="100" alt="image" src="https://user-images.githubusercontent.com/60432062/196651650-e62ae3f3-ddcb-4abc-a458-83d355e7c492.png">|<img width="100" alt="image" src="https://user-images.githubusercontent.com/60432062/196651821-7b031513-4cdd-444b-b60b-8a5b8d3b7108.png">|
## Backend
|[마루](https://github.com/chawani)|[아키](https://github.com/yeongunheo)|[야호](https://github.com/pup-paw)|[잉](https://github.com/Yboyu0u)|
| :---------------------------------------------------: | :---------------------------------------------------: | :---------------------------------------------------: | :---------------------------------------------------: |
|<img width="100" alt="image" src="https://user-images.githubusercontent.com/60432062/196649143-ab615142-c547-4d70-8d69-d4c47d12d21f.png">|<img width="100" alt="image" src="https://user-images.githubusercontent.com/60432062/196650065-8ad8834c-73fb-4e28-abe9-a805be7979d2.png">|<img width="100" alt="image" src="https://user-images.githubusercontent.com/60432062/196652736-a0157ba6-ffcd-4da4-9435-fc44fa896ee6.png">|<img width="100" alt="image" src="https://user-images.githubusercontent.com/60432062/196652589-0dc92bf8-4608-4c01-9bff-8b88710823af.png">|
<br>

# 팀 문화 💌
## 🌟데일리 문화
1. 데일리는 두 번
2. 컨디션 관리도 실력이다! 개인 컨디션이 팀 컨디션을 좌우한다!
3. 매주 금요일에 팀 문화 회고하기
4. 지각 체크는 신뢰 기반‼️
    
## 👥 프로젝트 문화
1. 주마다 회의 팀장, 서기 한 명 씩 
2. 주제에서 벗어난 이야기가 나오면 / 집중 시키고 싶을때 종치기 🔔
3. 회의 생성시(안건별로) 시작 시간과 마감 시간을 정해두기
4. 이슈는 마주한 사람이 바로바로 공유된 곳에 정리해 놓기 (노션 활용 잘 하기)
5. 평일 10시부터 18시까지는 프로젝트에 집중하기
6. 50분하고 10분 쉬기 - 회의 시작할때 알람⏰ 설정해두기!

## 💬 소통 문화

1. 예쁜말 쓰기 🌈
2. 매주 친목 도모하는 컨텐츠 하기 💖
3. 의견, 질문은 최대한 많이 하고 최대한 잘 받아 주기 (리액션 필수!)
<br>
', 'https://github.com/woowacourse-teams/2022-teatime', NULL, '2022-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://user-images.githubusercontent.com/83059234/195292032-346867ea-256a-4db8-82c4-0efb5b569ef3.jpeg', 4, NULL, '터놓고', 'ternoko', '터놓고', '면담은 찐하게, 예약은 손쉽게! 올인원 면담 예약 서비스 터놓고 💖', 43, '2026-08-07', 'CLOSED', 'APPROVED', '#  면담은 터놓고 Ternoko 
<p align="center">
<img width="200" alt="Screen Shot 2022-10-11 at 2 49 34 PM" src="https://user-images.githubusercontent.com/83059234/195292032-346867ea-256a-4db8-82c4-0efb5b569ef3.jpeg">
   </p>
  <p align="center">
       면담은 찐하게~  예약은 손쉽게!  올인원 면담 예약 서비스 `터놓고`  👨‍👨‍👧‍👧
  </p>

<hr>


<h3  align="center">
                            🚀크루 - `면담 예약`을 손쉽게 할 수 있어요!
</h3>

<br>

<p align="center">
  <img width="450"  src="https://user-images.githubusercontent.com/83059234/195489810-696cfa68-3966-4188-827c-445440296cd8.gif" alt="ezgif com-gif-maker (2)" />
</p>

<p align="center">
코치의 면담 예약 가능 시간 `조회`, 면담 사전 질문 `작성`, `면담 예약`, `알림`까지!  
</p>
<p align="center">
한번에 터놓고에서 관리할 수 있어요.  
</p>
<p align="center">
코치와의 `찐한 면담 시간이 당신을 기다리고` 있습니다.  
</p>


<br>
<hr>

<h3  align="center">
                            🚀 코치 - `면담 관리`를 손쉽게 할 수 있어요!
</h3>

<br>

<p align="center">
  <img width="450" src="https://user-images.githubusercontent.com/83059234/195294288-631fcb7c-43c7-4eed-9e8b-c95cb8dee59f.gif" alt="ezgif com-gif-maker (1)" />
</p>


<p align="center">
면담 예약 시간 `열기`, 면담 신청 `조회`, 면담 내역 `관리`, `알림`까지  
</p>
<p align="center">
한번에 터놓고에서 관리할 수 있어요.  
</p>
<p align="center">
나의 크루와의 찐한 면담 시간이 당신을 기다리고 있습니다.  
</p>
  
  
## 터놓고 서비스 이용하기 🤍

👉 [서비스 실제 페이지](https://ternoko.site)

`우테코 크루들`만 `접근`할 수 있어요.

👉 [데모 페이지](https://demo.ternoko.site)

`외부인`을 위한 `데모 페이지`입니다.

### 터놓고 Tech **Wiki 📑**  

👉  [위키페이지로 이동](https://github.com/woowacourse-teams/2022-ternoko/wiki)  


### Youtube 📺  

| 1차 데모데이 | 2차 데모데이 | 3차 데모데이 | 4차 데모데이 | 5차 데모데이
| --- | --- | --- | --- | --- |
| [<img width="200px" src="https://i.ytimg.com/vi/mKV3osPRtdc/hq720.jpg" />](https://youtu.be/mKV3osPRtdc) | [<img width="200px" src="https://i.ytimg.com/vi/LQRxmFMnFfo/hq720.jpg" />](https://youtu.be/LQRxmFMnFfo) | [<img width="200px" src="https://i.ytimg.com/vi/y2cudTZ8seY/hq720.jpg" />](https://youtu.be/y2cudTZ8seY) | [<img width="200px" src="https://i.ytimg.com/vi/-Y4DfIsRrzA/hqdefault.jpg" />](https://youtu.be/-Y4DfIsRrzA) | [<img width="200px" src="https://i.ytimg.com/vi/mKV3osPRtdc/hq720.jpg" />](https://youtu.be/mKV3osPRtdc) |


### 팀원👨‍💻👩‍💻



|FRONTEND|FRONTEND|BACKEND|BACKEND|BACKEND|BACKEND|BACKEND
|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
|<img width="120px" src="https://avatars.githubusercontent.com/u/19251499?s=100&v=4" />|<img width="120px" src="https://avatars.githubusercontent.com/u/38878617?s=100&v=4" />|<img width="120px" src="https://avatars.githubusercontent.com/u/26570275?s=100&v=4" />|<img width="120px" src="https://avatars.githubusercontent.com/u/54317630?s=100&v=4" />|<img width="120px" src="https://avatars.githubusercontent.com/u/36189291?s=100&v=4" />|<img width="120px" src="https://avatars.githubusercontent.com/u/83059234?s=100&v=4" />|<img width="120px" src="https://avatars.githubusercontent.com/u/43205258?s=100&v=4" />
|[록바](https://github.com/lokba)|[아놀드](https://github.com/sanaandmomo)|[수달](https://github.com/her0807)|[애쉬](https://github.com/dongho108)|[바니](https://github.com/HyeonbinSa)|[앤지](https://github.com/soominsohn)|[열음](https://github.com/Juhyung990122)|


<h2 align="center">
FrontEnd 기술스택 & 인프라 💛
<h2>

<p align="center">
<img width="340"  src="https://user-images.githubusercontent.com/43205258/195761355-073d69f5-429d-400f-a30d-183426c6b5b0.png"> <img width="350" src="https://user-images.githubusercontent.com/43205258/195761140-331c881c-115c-4039-9881-837b5de4acfc.png">
<p>


<h2 align="center">
  BackEnd 기술스택 & 인프라💙
<h2>
<p align="center">
<img width="350" alt="image" src="https://user-images.githubusercontent.com/43205258/195761514-4092d0b9-0471-4391-989a-aff3ce077977.png"> <img width="350"  src="https://user-images.githubusercontent.com/43205258/195761556-e13424d6-478c-408c-8fc2-3bc370ea7945.png">
<p>

<h2 align="center">
 우리의 약속 🤍
<h2>

 <p align="center">
 Version 2
<p>
   
<p align="center">
<img width="350" alt="image" src="https://user-images.githubusercontent.com/26570275/197690468-9ec8eee9-abe0-44f2-ac6a-cd9f7361f83a.png"> 
<p>
   

   
<p align="center">
 Version 1
<p>

   
<p align="center">
<img width="350" alt="Screen Shot 2022-10-11 at 2 49 34 PM" src="https://user-images.githubusercontent.com/83059234/195291896-ca005fa9-dff4-44ca-96af-938971891ce9.png">
<p>






', 'https://github.com/woowacourse-teams/2022-ternoko', NULL, '2022-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://user-images.githubusercontent.com/53412998/135798025-1158fe48-0841-4545-a28f-8015468c3328.png', 3, NULL, 'CVI', 'cvi', 'CVI', ' 💉 코로나19 백신 접종 후기 공유 플랫폼', 37, '2026-08-07', 'CLOSED', 'APPROVED', '<img src="https://user-images.githubusercontent.com/53412998/135798025-1158fe48-0841-4545-a28f-8015468c3328.png" width="100%"/>

# 프로젝트 소개

<br/>

<p align="center">
  <a target="_blank" href="https://www.youtube.com/watch?v=W1LziOGs_6g">
    <img src="https://user-images.githubusercontent.com/53412998/137051501-805cd497-b7d7-421e-b6ac-4f03549e0d93.png" width="50%" height="50%">
    <p align="center">(클릭하면 영상을 실행할 수 있습니다)</p>
  </a>
</p>

<br/>

코로나19 백신 접종 후기를 남기다! 우리는 [`Team CVI`](https://vaccine-review.com) 입니다.

- [`CVI`](https://vaccine-review.com) 는 코로나19 백신 정보를 제공해줘요.
- [`CVI`](https://vaccine-review.com) 에서는 백신 후기를 남길 수 있어요.
- 부작용이 걱정되신다구요? [`다른 사람들의 후기를 보러 가볼까요? :)`](https://vaccine-review.com)

서비스 URL: https://vaccine-review.com

<br/>

# 서비스 기능
## 간단 요약
![간단 요약](https://user-images.githubusercontent.com/53412998/135582980-53157888-c54d-4313-bff5-e06ccd01274f.gif)

<details>
<summary>소셜 로그인</summary>
  <img src="https://user-images.githubusercontent.com/53412998/135583011-4fd322c9-623a-4329-ae95-7ec4fa23fe37.gif"/>
</details>


<details>
<summary>글 작성</summary>
  <img src="https://user-images.githubusercontent.com/53412998/135581550-d38306bc-7ff3-4771-85e1-23cb20c79550.gif"/>
</details>

<details>
<summary>좋아요 누르기, 댓글 작성</summary>
  <img src="https://user-images.githubusercontent.com/53412998/135583033-adf9ee08-0846-4b0e-bf21-c3d22b0615b7.gif"/>
</details>

<details>
<summary>게시글 필터링, 정렬</summary>
  <img src="https://user-images.githubusercontent.com/53412998/135583001-e819f4d0-9c35-4f37-a53e-1996522450d4.gif"/>
</details>

<details>
<summary>마이페이지</summary>
  <img src="https://user-images.githubusercontent.com/53412998/135583010-b5a8757d-2eb5-4a19-b6e2-c0b507a69e13.gif"/>
</details>

<details>
<summary>접종 현황 통계</summary>
  <img src="https://user-images.githubusercontent.com/53412998/135583019-990a80aa-e9e3-4046-85e4-ea930fc1febb.gif"/>
</details>

<br/>

# 우리팀의 강점

[자세한 문서화를 했어요.](https://www.notion.so/4b6587fb182447eb93183d6160b5ef0a)

[애자일하게 서비스를 개발했어요.](https://www.notion.so/da2fc7e8d99f4f4484bad58ed2e1b233)

[우리만의 팀 문화를 만들었어요.](https://www.notion.so/ccf25ce39e2d42389c43ccf9b768b53b)

<br/>

# 기술 스택
<img width="100%" src="https://user-images.githubusercontent.com/40762111/135794163-9c4978df-7ac3-4a17-a97e-8dd3afe64533.png" />

<br/>

# 서비스아키텍처

##  사용자 요청 시나리오 (요청부터 응답까지)
![FE+BE](https://user-images.githubusercontent.com/43339385/135794655-511b9a4b-ce99-41ca-a003-d549b1e3f20a.png)

## 프로트엔드 아키텍쳐
![FE_CI:CD](https://user-images.githubusercontent.com/43339385/135794087-571dea0c-c90f-42c3-b8cf-b08130ea0d39.png)


##  백엔드 인프라 아키텍처
![image](https://user-images.githubusercontent.com/48986787/135793839-08fc58d6-c381-4af3-be58-16342d8ff5bb.png)


## 백엔드 CI / CD    
![image](https://user-images.githubusercontent.com/48986787/135793875-193bc33a-31fd-414a-ac1c-4591e44086cf.png)

<br/>

# 팀원 소개
<table>
  <tr>
    <td colspan="2" align="center"><strong>Front-end</strong></td>
    <td colspan="4" align="center"><strong>Back-end</strong></td>
  </tr>
  <tr>
    <td align="center">
      <a href="https://github.com/HyuuunjuKim">
        <img src="https://user-images.githubusercontent.com/67272922/135793917-03a2d388-eab0-4b5d-87cf-c4066a441c1d.png" width="100px;" alt=""/><br />
        <sub>
          <b>엘라(김현주)</b>
        </sub>
      </a><br />
    </td>
    <td align="center">
      <a href="https://github.com/jum0">
        <img src="https://user-images.githubusercontent.com/67272922/135793929-2f635802-8cc6-4645-937f-d59c8dc85356.png" width="100px;" alt=""/><br />
        <sub>
          <b>주모(한준모)</b>
        </sub>
      </a><br />
    </td>
    <td align="center">
      <a href="https://github.com/livenow14">
        <img src="https://user-images.githubusercontent.com/67272922/135793762-8104fdda-d777-4f3a-b514-14b60d5dd6be.png" width="100px;" alt=""/>
        <br />
        <sub>
          <b>검프(김태정)</b>
        </sub>
      </a><br />
    </td>
    <td align="center">
      <a href="https://github.com/younghoonkwon">
        <img src="https://user-images.githubusercontent.com/67272922/135793840-d9e4f3d7-d68e-46ef-b54c-374fe46fb85d.png" width="100px;" alt=""/><br />
        <sub>
          <b>라이언(권영훈)</b>
        </sub>
      </a><br />
    </td>
    <td align="center">
      <a href="https://github.com/thisisyoungbin">
        <img src="https://user-images.githubusercontent.com/67272922/135793855-a428ba5f-83e4-459b-9ebf-e20da7f8b98b.png" width="100px;" alt=""/><br />
        <sub>
          <b>욘(김영빈)</b>
        </sub>
      </a><br />
    </td>
    <td align="center">
      <a href="https://github.com/taehee-kim-dev">
        <img src="https://user-images.githubusercontent.com/67272922/135793886-e137d43e-00ad-4d4d-af28-a45abe99f4ee.png" width="100px;" alt=""/><br />
        <sub>
          <b>인비(김태희)</b>
        </sub>
      </a><br />
    </td>
  </tr>
  <tr>
    <td rowspan="1" align="center">
      <a href="https://github.com/woowacourse-teams/2021-cvi/issues?q=assignee%3AHyuuunjuKim" title="Code">issues</a>
    </td>
    <td rowspan="1" align="center">
      <a href="https://github.com/woowacourse-teams/2021-cvi/issues?q=assignee%3Ajum0" title="Code">issues</a>
    </td>
    <td rowspan="1" align="center">
      <a href="https://github.com/woowacourse-teams/2021-cvi/issues?q=assignee%3Alivenow14" title="Code">issues</a>
    </td>
    <td rowspan="1" align="center">
      <a href="https://github.com/woowacourse-teams/2021-cvi/issues?q=assignee%3Ayounghoonkwon" title="Code">issues</a>
    </td>
    <td rowspan="1" align="center">
      <a href="https://github.com/woowacourse-teams/2021-cvi/issues?q=assignee%3Athisisyoungbin" title="Code">issues</a>
    </td>
    <td rowspan="1" align="center">
      <a href="https://github.com/woowacourse-teams/2021-cvi/issues?q=assignee%3Ataehee-kim-dev" title="Code">issues</a>
    </td>
  </tr>
  <tr>
    <td rowspan="1" align="center">
      <a href="https://github.com/woowacourse-teams/2021-cvi/commits?author=HyuuunjuKim" title="Code">commits</a>
    </td>
    <td rowspan="1" align="center">
      <a href="https://github.com/woowacourse-teams/2021-cvi/commits?author=jum0" title="Code">commits</a>
    </td>
    <td rowspan="1" align="center">
      <a href="https://github.com/woowacourse-teams/2021-cvi/commits?author=livenow14" title="Code">commits</a>
    </td>
    <td rowspan="1" align="center">
      <a href="https://github.com/woowacourse-teams/2021-cvi/commits?author=younghoonkwon" title="Code">commits</a>
    </td>
    <td rowspan="1" align="center">
      <a href="https://github.com/woowacourse-teams/2021-cvi/commits?author=thisisyoungbin" title="Code">commits</a>
    </td>
    <td rowspan="1" align="center">
      <a href="https://github.com/woowacourse-teams/2021-cvi/commits?author=taehee-kim-dev" title="Code">commits</a>
    </td>
  </tr>
</table>
', 'https://github.com/woowacourse-teams/2021-cvi', NULL, '2021-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://user-images.githubusercontent.com/44080404/139180406-eed179d2-f176-43ea-acc8-3b6165c60fc9.png', 3, NULL, '놀토', 'nolto', '놀토', '부담없이 자랑하는 작고 소중한 내 프로젝트 🧸✨', 41, '2026-08-07', 'CLOSED', 'APPROVED', '
<p align="center">  
<h1 align="middle">🧸 놀토: 놀러오세요 토이프로젝트 🎈</h1>
<p align="center">
<img src="https://user-images.githubusercontent.com/44080404/139180406-eed179d2-f176-43ea-acc8-3b6165c60fc9.png" />
</p>


</p>

<p align="middle">부담없이 자랑하는 작고 소중한 내 토이프로젝트</p>
<p align="center"> 서툰 프로젝트라도 누구나 뿌듯하게 자랑하고 공유하는 공간,</p>
<h3 align="center"> 여기는 <b>놀토</b>입니다! </h3>

<h2 align="middle">🎥 놀토 소개 영상 </h2>

<p align="center">
  <a href="https://youtu.be/WsGyO4k2Kv0">
    <img src="http://img.youtube.com/vi/WsGyO4k2Kv0/0.jpg" alt="nolto video ">
  </a>
</p>


<h2 align="middle"> 🙋‍♀️ 놀토를 만든 사람들 🙋‍♂️</h2>
<p align="center">
  
| [아마찌](https://github.com/NewWisdom)   |  [조엘](https://github.com/joelonsw)  |   [포모](https://github.com/bosl95)      |  [미키](https://github.com/0307kwon)  | [지그](https://github.com/zigsong)   | [찰리](https://github.com/Gomding)   |
| :----------------------------------------------------------: | :----------------------------------------------------------: | :----------------------------------------------------------: | :----------------------------------------------------------: | :----------------------------------------------------------: | :----------------------------------------------------------: | 
| <img src="https://user-images.githubusercontent.com/43840561/129164013-2a88c2e7-1a93-4cc7-bbd8-c5818f5152c7.png"/> | <img src="https://user-images.githubusercontent.com/44080404/133540314-639cc580-1aa5-4bf4-8d54-b435bfe5e5f8.png" /> | <img src="https://user-images.githubusercontent.com/44080404/133540309-ae1e774e-4404-4801-bb5c-0037eab41818.PNG" /> | <img src="https://user-images.githubusercontent.com/44080404/133540317-20da5664-aa3d-4afb-809b-a7d4780a5a17.png" /> |  <img src="https://user-images.githubusercontent.com/44080404/133540321-7f8f4215-3e01-4f21-88e3-90d608377aab.png" /> | <img src="https://user-images.githubusercontent.com/44080404/133540503-22c158d4-1042-4e7c-9ee5-79c694bf5841.png" /> |

</p>

<br>
<br>

<p align="center">
<h2 align="middle"> 놀토에 사용된 기술 스택들 👨‍💻 </h2>
<img src="https://raw.githubusercontent.com/woowacourse-teams/2021-nolto/develop/img/tech.png">
<br>

<h2 align="middle"> 놀토 인프라 구성 🎡 </h2>
<img src="https://raw.githubusercontent.com/woowacourse-teams/2021-nolto/develop/img/before_infra.png">

- 21.11.08 이전 인프라 구조

<img src="https://raw.githubusercontent.com/woowacourse-teams/2021-nolto/develop/img/infra.jpg">
  
- 21.11.08 이후 인프라 구조 
  
<br>

<h2 align="middle"> 놀토 CI/CD 프로세스 🎯 </h2>
<img src="https://raw.githubusercontent.com/woowacourse-teams/2021-nolto/develop/img/ci_cd.jpg">
</p>

<br>
<br>

<h2 align="middle"> ⚙️ 기술 스택 ⚙️ </h2>

<p align="center">
<img src="https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=TypeScript&logoColor=white"> <img src="https://img.shields.io/badge/react-61DAFB?style=for-the-badge&logo=react&logoColor=black"> <img src="https://img.shields.io/badge/styled components-DB7093?style=for-the-badge&logo=styled-components&logoColor=white"> 
</p>  
<p align="center">
<img src="https://img.shields.io/badge/JAVA-007396?style=for-the-badge&logo=java&logoColor=white"> <img src="https://img.shields.io/badge/Spring Boot-6DB33F?style=for-the-badge&logo=Spring Boot&logoColor=white"> <img src="https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=JUnit5&logoColor=white">  <img src="https://img.shields.io/badge/mariaDB-003545?style=for-the-badge&logo=mariaDB&logoColor=white"> <img src="https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=Hibernate&logoColor=white"> 
  </p>
<p align="center">
<img src="https://img.shields.io/badge/Amazon AWS-232F3E?style=for-the-badge&logo=Amazon AWS&logoColor=white"> <img src="https://img.shields.io/badge/Amazon S3-569A31?style=for-the-badge&logo=Amazon S3&logoColor=white"> <img src="https://img.shields.io/badge/NGINX-009639?style=for-the-badge&logo=NGINX&logoColor=white">  <img src="https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=Jenkins&logoColor=white"> <img src="https://img.shields.io/badge/SonarQube-4E9BCD?style=for-the-badge&logo=SonarQube&logoColor=white"> <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=Docker&logoColor=white"> 
</p>

<p align="center">
<img src="https://img.shields.io/badge/ZOOM ZUN BANG-2D8CFF?style=for-the-badge&logo=ZOOM&logoColor=white"> 
</p>
', 'https://github.com/woowacourse-teams/2021-nolto', NULL, '2021-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://user-images.githubusercontent.com/50176238/129131809-52307863-7d29-4ebf-8595-46ef77ba2be8.png', 3, NULL, '깃들다', 'pick-git', '깃들다', '💻 Github Repo 기반 개발 장려 SNS', 155, '2026-08-07', 'CLOSED', 'APPROVED', '<p align="center">
    <img src="https://user-images.githubusercontent.com/50176238/129131809-52307863-7d29-4ebf-8595-46ef77ba2be8.png" alt="pick-git-logo" width="220" height="220">
</p>

<div align="center">

  💻 Github Repo 기반 개발 장려 SNS, <br>
  🖋 [깃-들다 (Pick-Git)](https://pick-git.com/)

</div>
<br/>

<div align="center">

[![Application](http://img.shields.io/badge/Application-fc3465?style=flat&logo=github&logoColor=white&link=https://pick-git.com/)](https://pick-git.com/)
[![Tech Blog](http://img.shields.io/badge/-Tech%20Blog-important?style=flat&logo=dev.to&logoColor=white&link=https://2021-pick-git.github.io/)](https://2021-pick-git.github.io/)
[![WIKI](http://img.shields.io/badge/-GitHub%20WiKi-395FC1?style=flat&logo=dev.to&logoColor=white&link=https://github.com/woowacourse-teams/2021-pick-git/wiki)](https://github.com/woowacourse-teams/2021-pick-git/wiki)
[![Release](https://img.shields.io/github/v/release/woowacourse-teams/2021-pick-git?color=skyblue)](https://github.com/woowacourse-teams/2021-pick-git/releases/tag/v1.2.0)

</div>
<br/>

## 💻 깃-들다 (Pick-Git)

|소셜 로그인|게시물 작성|프로필 및 활동 통계 조회|
|:-:|:-:|:-:|
|<img src=https://user-images.githubusercontent.com/56240505/135817249-985f31ac-cde3-431e-b16d-56cfecb2897e.gif>|<img src=https://user-images.githubusercontent.com/56240505/135817222-fb893165-18cf-4ef5-b240-eb292a318ca7.gif>|<img src=https://user-images.githubusercontent.com/56240505/135817239-a267f424-fa9d-473a-8010-e9e6232db8b9.gif>|
|<b>유저 검색 및 팔로우</b>|<b>게시물 검색</b>|<b>포트폴리오</b>|
|<img src=https://user-images.githubusercontent.com/56240505/135817233-243ebc51-70c2-4613-b76e-7edc4aaf0667.gif>|<img src=https://user-images.githubusercontent.com/56240505/135817748-25771911-1f98-437f-a956-8b53f626d9e8.gif>|<img src=https://user-images.githubusercontent.com/56240505/135817760-ac932970-c5ab-4c95-81da-cc6533f19f17.gif>|

<p align="center">
    <b>깃-들다</b>는 Github 기반의 SNS로서 <b>개발자의 자기 PR과 소통을 위한 공간</b>입니다.<br><br>
    <a href=https://sites.google.com/woowahan.com/wooteco-demo-3rd/%EA%B9%83-%EB%93%A4%EB%8B%A4?authuser=0>우아한테크코스 데모데이 소개 사이트에서 동영상 등 더 많은 내용을 확인해보세요.</a>
</p>
<br/>

## 🛠 Tech Stacks

### Frontend

![frontend_tech_stacks](https://user-images.githubusercontent.com/50176238/135874567-f03612e6-9e2e-4553-9e91-c39b79935817.png)

> <b>깃-들다</b>의 <b>프론트엔드</b>에 대해 더 자세하게 알고싶다면, [기술 블로그](https://2021-pick-git.github.io/) 또는 [GitHub WiKi - FE 중점 사항](https://github.com/woowacourse-teams/2021-pick-git/wiki/FE-%EC%A4%91%EC%A0%90-%EC%82%AC%ED%95%AD)을 참고해주세요.
<br/>

### Backend

![backend_tech_stacks](https://user-images.githubusercontent.com/56240505/137877225-07cbf85b-053d-4610-9164-1261b08ae047.png)

> <b>깃-들다</b>의 <b>백엔드</b>에 대해 더 자세하게 알고싶다면, [기술 블로그](https://2021-pick-git.github.io/) 또는 [GitHub WiKi - BE 중점 사항](https://github.com/woowacourse-teams/2021-pick-git/wiki/BE-%EC%A4%91%EC%A0%90-%EC%82%AC%ED%95%AD)을 참고해주세요.
<br/>

## 🔌 Infrastructures

![prod_environment](https://user-images.githubusercontent.com/56240505/137874986-81cd5840-8b69-4b4d-8b89-b1b3d8d08341.png)
![pipeline](https://user-images.githubusercontent.com/56240505/137875149-090f90b7-bc17-47d4-b881-3a88888d4b32.png)

> <b>깃-들다</b>의 <b>인프라</b>에 대해 더 자세하게 알고싶다면, [Pick-Git의 Infrasturcture](https://2021-pick-git.github.io/infrastructure/) 글을 참고해주세요.

<br/>

## 🏠 Members

### Frontend

|브콜|크리스|
|:-:|:--:|
|<img src="https://avatars.githubusercontent.com/u/57767891?v=4" alt="beucol" width="100" height="100">|<img src="https://avatars.githubusercontent.com/u/32982670?v=4" alt="chris" width="100" height="100">|
|[Tanney-102](https://github.com/Tanney-102)|[swon3210](https://github.com/swon3210)|

### Backend

|다니|마크|손너잘|코다|케빈|
|:-:|:-:|:--:|:-:|:-:|
|<img src="https://avatars0.githubusercontent.com/u/50176238?s=400&u=212ca9ffd06b88465746a94eaa6f88b10485497d&v=4" alt="daeun" width="100" height="100">|<img src="https://avatars.githubusercontent.com/u/56860124?v=4" alt="mark" width="100" height="100">|<img src="https://avatars.githubusercontent.com/u/33603557?v=4" alt="neozal" width="100" height="100">|<img src="https://avatars.githubusercontent.com/u/63405904?v=4" alt="koda" width="100" height="100">|<img src="https://avatars.githubusercontent.com/u/56240505?v=4" alt="kevin" width="100" height="100">|
|[da-nyee](https://github.com/da-nyee)|[binghe819](https://github.com/binghe819)|[bperhaps](https://github.com/bperhaps)|[yjksw](https://github.com/yjksw)|[xlffm3](https://github.com/xlffm3)|

<br>

## 🌈 Culture

<p align="center">
  <img src="https://user-images.githubusercontent.com/50176238/129133023-8492948f-01e4-45b7-91c4-fd556d3dc326.png" alt="pick-git-cultures" width="520" height="360">
</p>
', 'https://github.com/woowacourse-teams/2021-pick-git', NULL, '2021-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://user-images.githubusercontent.com/60066472/129216925-b8ad8ef2-e99e-4ca0-8045-4bc8666e55fb.png', 3, NULL, '여기서만나', 'see-you-there', '여기서만나', '여기서 만나 👋　만나기 좋은 중간지점을 추천해주는 서비스', 49, '2026-08-07', 'CLOSED', 'APPROVED', '[<img src="https://user-images.githubusercontent.com/60066472/129216925-b8ad8ef2-e99e-4ca0-8045-4bc8666e55fb.png" width="1300" alt="여기서만나">](https://seeyouthere.co.kr)

<br/>

<p align="center">
  어디서 만나야할지 잘 모르시겠다구요?
  <br/>
  걱정마세요. ''여기서만나''가 딱 정해드릴게요!
  <br/><br/>
  https://seeyouthere.co.kr
</p>

<br/><br/>

## 💙 백엔드 소개

- **기술스택**
  - <img src="https://img.shields.io/badge/Java-11-blue" alt="IntelliJ"> on IntelliJ
  - <img src="https://img.shields.io/badge/Spring-5.3.8-blue" alt="Spring badge"> <img src="https://img.shields.io/badge/Spring%20Boot-2.5.2-blue" alt="Spring Boot badge">
  - <img src="https://img.shields.io/badge/Jenkins-2.305-blue" alt="Jenkins badge"> <img src="https://img.shields.io/badge/MariaDB-10.1.48-blue" alt="MariaDB badge"> <img src="https://img.shields.io/badge/Redis-5.0.7-blue" alt="Redis badge">
  - <img src="https://img.shields.io/badge/restdocs-2.0.5-blue" alt="restdocs badge">
  - <img src="https://img.shields.io/badge/JUnit-5-blue" alt="JUnit badge"> <img src="https://img.shields.io/badge/Sonarqube-9.0.1-blue" alt="Sonarqube badge">
  - <img src="https://img.shields.io/badge/AWS%20ec2-18.04.1-blue" alt="AWS badge"> <img src="https://img.shields.io/badge/nginx-1.14.0-blue" alt="nginx badge">
- **팀원**

  |                                                                                            [<img src="https://user-images.githubusercontent.com/60066472/129212701-057db21f-9997-40fe-825e-79b9bb802c84.png" width="180" alt="와이비">](https://github.com/hybeom0720)                                                                                             |                                                                                          [<img src="https://user-images.githubusercontent.com/60066472/129204204-977f963c-52f9-48fb-b985-8f1ac7d05f3d.png" width="180" alt="멍토">](https://github.com/daum7766)                                                                                           |                                                                                           [<img src="https://user-images.githubusercontent.com/60066472/129212722-92ead5b9-fef1-49eb-8014-a4cff3707303.png" width="180" alt="춘식">](https://github.com/RinSabbit)                                                                                            |                                                                                             [<img src="https://user-images.githubusercontent.com/60066472/129213525-02132a89-7fe9-47b6-aa8a-7e5ddf79d11a.png" width="180" alt="영이">](https://github.com/choijy1705)                                                                                             |
  | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------: | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------: |
  |                                                                                                                                                                          bhc생맥주 좋아함                                                                                                                                                                          |                                                                                                                                                                        일식 좋아함                                                                                                                                                                         |                                                                                                                                                                       치즈케이크 좋아함                                                                                                                                                                       |                                                                                                                                                                            육류 좋아함                                                                                                                                                                            |
  | [<img src="https://user-images.githubusercontent.com/60066472/129203087-dd56c080-502e-4a84-8430-fb3f0c8b99ad.png" width="60" alt="와이비 깃허브">](https://github.com/hybeom0720) [<img src="https://user-images.githubusercontent.com/60066472/129203152-b187e046-4992-4860-8a71-6813aded9d7c.png" width="60" alt="와이비 블로그">](https://ksjm0720.tistory.com) | [<img src="https://user-images.githubusercontent.com/60066472/129203087-dd56c080-502e-4a84-8430-fb3f0c8b99ad.png" width="60" alt="멍토 깃허브">](https://github.com/daum7766) [<img src="https://user-images.githubusercontent.com/60066472/129203152-b187e046-4992-4860-8a71-6813aded9d7c.png" width="60" alt="멍토 블로그">](https://mungto.tistory.com) | [<img src="https://user-images.githubusercontent.com/60066472/129203087-dd56c080-502e-4a84-8430-fb3f0c8b99ad.png" width="60" alt="춘식 깃허브">](https://github.com/RinSabbit) [<img src="https://user-images.githubusercontent.com/60066472/129203152-b187e046-4992-4860-8a71-6813aded9d7c.png" width="60" alt="춘식 블로그">](https://rinsabbit.github.io/) | [<img src="https://user-images.githubusercontent.com/60066472/129203087-dd56c080-502e-4a84-8430-fb3f0c8b99ad.png" width="60" alt="영이 깃허브">](https://github.com/choijy1705) [<img src="https://user-images.githubusercontent.com/60066472/129203152-b187e046-4992-4860-8a71-6813aded9d7c.png" width="60" alt="영이 블로그">](https://choijy1705.tistory.com/) |

<br/><br/>

## 💙 프론트엔드 소개

- **기술스택**
  - <img src="https://img.shields.io/badge/JavaScript-ES6+-blue" alt="JavaScript"> on VScode
  - <img src="https://img.shields.io/badge/React-17.0.2-blue" alt="React"> <img src="https://img.shields.io/badge/PropTypes-15.7.2-blue" alt="PropTypes badge"> <img src="https://img.shields.io/badge/webpack-5.45.1-blue" alt="webpack badge"> <img src="https://img.shields.io/badge/react%20query-3.19.0-blue" alt="react-query">
  - <img src="https://img.shields.io/badge/styled%20components-5.3.0-blue" alt="styled-components badge">
  - <img src="https://img.shields.io/badge/eslint-7.31.0-blue" alt="eslint badge"> <img src="https://img.shields.io/badge/prettier-2.3.2-blue" alt="prettier badge">
  - <img src="https://img.shields.io/badge/webpack%20dev%20server-3.11.2-blue" alt="webpack-dev-server badge"> <img src="https://img.shields.io/badge/react%20refresh-2.3.2-blue" alt="react-refresh badge">
  - <img src="https://img.shields.io/badge/cypress-8.1.0-blue" alt="cypress badge"> with <img src="https://img.shields.io/badge/github%20actions-blue" alt="github actions badge">
- **UI/UX**
  - <img src="https://img.shields.io/badge/figma-blue" alt="figma badge"> <img src="https://img.shields.io/badge/procreate-blue" alt="procreate badge"> <img src="https://img.shields.io/badge/online%20usability%20test-blue" alt="online usability-test badge">
- **팀원**

  |                                                                                              [<img src="https://user-images.githubusercontent.com/60066472/129207942-76dffa96-270b-4d1a-b5fa-f0c4f1717d7e.png" width="180" alt="심바">](https://github.com/0imbean0)                                                                                               |                                                                                           [<img src="https://user-images.githubusercontent.com/60066472/129201577-bcc0ef75-13ee-4010-891e-13be749eb440.png" width="180" alt="하루">](https://github.com/365kim)                                                                                           |
  | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------: |
  |                                                                                                                                                                         아이스크림 좋아함                                                                                                                                                                          |                                                                                                                                                                      민트초코 좋아함                                                                                                                                                                      |
  | [<img src="https://user-images.githubusercontent.com/60066472/129203087-dd56c080-502e-4a84-8430-fb3f0c8b99ad.png" width="60" alt="심바 깃허브">](https://github.com/0imbean0) [<img src="https://user-images.githubusercontent.com/60066472/129203152-b187e046-4992-4860-8a71-6813aded9d7c.png" width="60" alt="심바 블로그">](https://seeyouthere.co.kr/notfound) | [<img src="https://user-images.githubusercontent.com/60066472/129203087-dd56c080-502e-4a84-8430-fb3f0c8b99ad.png" width="60" alt="하루 깃허브">](https://github.com/365kim) [<img src="https://user-images.githubusercontent.com/60066472/129203152-b187e046-4992-4860-8a71-6813aded9d7c.png" width="60" alt="하루 블로그">](https://365kim.tistory.com/) |

<br/><br/>

## 💙 전체 프로젝트 구조
![프로젝트 구조](https://user-images.githubusercontent.com/60066472/139381787-a2af982b-21d3-444f-a618-27c5f3f09b4a.png)

<br/><br/>

## 💙 팀 운영

- **소스코드 관리** 　<img src="https://img.shields.io/badge/Github-blue" alt="Github">
- **커뮤니케이션** 　<img src="https://img.shields.io/badge/Slack-blue" alt="Slack"> <img src="https://img.shields.io/badge/Zoom-blue" alt="Zoom"> <img src="https://img.shields.io/badge/Kakaotalk-blue" alt="Kakaotalk">
- **회의록 & 회고록 관리** 　<img src="https://img.shields.io/badge/Notion-blue" alt="Notion">
  <br>

![image](https://user-images.githubusercontent.com/60066472/129198538-7f052e18-6adf-478d-8914-ae9bf702a23f.png)

<br/><br/><br/><br/>

<img width="1800" alt="drawingLogin" src="https://user-images.githubusercontent.com/60066472/129211345-d9854dd0-ef43-4370-aeb4-7486574bc16b.png">
', 'https://github.com/woowacourse-teams/2021-see-you-there', NULL, '2021-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://raw.githubusercontent.com/woowacourse-teams/2020-seller-lee-company/develop/images/graphic.jpg', 2, NULL, '직고래', 'seller-lee-company', '직고래', '🐳 조직 내에서 중고 거래를! 직고래', 49, '2026-08-07', 'CLOSED', 'APPROVED', '<p align="center">
  <a target="_blank" href="https://sites.google.com/woowahan.com/wooteco-demo/%EC%A7%81%EA%B3%A0%EB%9E%98"><img src="https://raw.githubusercontent.com/woowacourse-teams/2020-seller-lee-company/develop/images/graphic.jpg" width="70%" height="70%"></a>
</p>

*대한민국 중고 거래 앱 곧 1등!*

*당신이 속한 그룹 어디든, 당신이 원하는 무엇이든 사고 팔 수 있는 조**직** 중**고** 거**래** 서비스, 직고래입니다.*

[![demo page](http://img.shields.io/badge/-Demo%20Page-0F9D58?style=flat&logo=Google%20Sheets&logoColor=white&link=https://sites.google.com/woowahan.com/wooteco-demo/%EC%A7%81%EA%B3%A0%EB%9E%98)](https://sites.google.com/woowahan.com/wooteco-demo/%EC%A7%81%EA%B3%A0%EB%9E%98)
[![Blogger](http://img.shields.io/badge/-Devlog-395FC1?style=flat&logo=dev.to&logoColor=white&link=https://seller-lee.github.io)](https://seller-lee.github.io/)
[![Google Play](http://img.shields.io/badge/-Google%20Play-414141?style=flat&logo=Google%20play&link=https://play.google.com/store/apps/details?id=com.sellerleecompany.jikgorae&hl=en_US)](https://play.google.com/store/apps/details?id=com.sellerleecompany.jikgorae&hl=en_US)
![release](https://img.shields.io/github/v/release/woowacourse-teams/2020-seller-lee-company?color=skyblue)
![last commit](https://img.shields.io/github/last-commit/woowacourse-teams/2020-seller-lee-company)
![most language](https://img.shields.io/github/languages/top/woowacourse-teams/2020-seller-lee-company)

<br/>

## 🐳 직고래 이야기

<p align="center">
  <a target="_blank" href="https://www.youtube.com/watch?v=IZWhBI0Tk2c">
    <img src="https://raw.githubusercontent.com/woowacourse-teams/2020-seller-lee-company/develop/images/youtube.png" width="50%" height="50%">
    <p align="center">(클릭하면 영상을 실행할 수 있습니다)</p>
  </a>
</p>

<br/>

## 🥰 직고래는 따듯한 교류가 있는 조직 벼룩시장을 꿈꾸고 있어요.

아무리 사소한 물건이라도 직고래를 이용하면 문제없어요.

조직 내에서 가까운 사람과 거래하기 때문에 가벼운 마음으로, 가까운 거리에서, 안전하게 거래를 할 수 있답니다.

조직 간의 전체 채팅을 통해 다양한 이야기를 주고받을 수도 있어요.

**더 끈끈한 조직 문화를 만들어내는 지름길, 직고래를 이용해보세요.**

<br/>

## 🛠 기술 스택

![stack](https://raw.githubusercontent.com/woowacourse-teams/2020-seller-lee-company/develop/images/stack.png)

<br/>

## 🏗 프로젝트 아키텍처

![project_architectures](https://raw.githubusercontent.com/woowacourse-teams/2020-seller-lee-company/develop/images/project_architectures.png)

<br/>

## 📲 CI/CD

![CICD](https://raw.githubusercontent.com/woowacourse-teams/2020-seller-lee-company/develop/images/CICD.png)

<br/>

## 🏠 팀소개

|![subway](https://raw.githubusercontent.com/woowacourse-teams/2020-seller-lee-company/develop/images/subway.png)|![tto](https://raw.githubusercontent.com/woowacourse-teams/2020-seller-lee-company/develop/images/tto.png)|![stitch](https://raw.githubusercontent.com/woowacourse-teams/2020-seller-lee-company/develop/images/stitch.png)|![kouz](https://raw.githubusercontent.com/woowacourse-teams/2020-seller-lee-company/develop/images/kouz.png)|![turtle](https://raw.githubusercontent.com/woowacourse-teams/2020-seller-lee-company/develop/images/turtle.png)|
|:---:|:---:|:---:|:---:|:---:|
|[Github](https://github.com/joseph415)|[Github](https://github.com/jnsorn)|[Github](https://github.com/lxxjn0)|[Github](https://github.com/kouz95)|[Github](https://github.com/begaonnuri)|
', 'https://github.com/woowacourse-teams/2020-seller-lee-company', NULL, '2020-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES (NULL, 2, NULL, 'Zeze', 'zeze', 'Zeze', 'Beautiful, Minimal Slides with Markdown', 20, '2026-08-07', 'CLOSED', 'APPROVED', '# Zeze

## How to

### Code Convention

다음 컨벤션을 기준으로 작성되었습니다.

- **[캠퍼스 핵데이 Java 코딩 컨벤션](https://naver.github.io/hackday-conventions-java)**
- **[Naver JavaScript Style Guide](https://github.com/naver/eslint-config-naver/blob/master/STYLE_GUIDE.md)**  

#### IntelliJ Code Style 설정

- `Editor > Code Style > Java` 에서 다음을 설정
  - `Scheme > Import scheme > IntelliJ IDEA code scheme XML`
  - `(프로젝트 최상단) .zeze-java-convention.xml` 선택
  - 이름을 입력해야하는 경우 `zeze-java-convention` 입력 
  - `Scheme`에서 `zeze-java-convention` 선택

#### CheckStyle (Java)

**gradle**
- `./gradlew checkstyleMain`
- `./gradlew checkstyleTest`

**IntelliJ**
- `Plugins`에서 `CheckStyle-IDEA` 설치
- `Tools > Checkstyle` 에서 다음을 설정
  - Checkstyle Version: 8.27 이상 (8.34 권장)
  - Scan Scope: Only Java sources (including tests)
  - Configuration file 하단 **+** 버튼으로 XML 다음을 추가
    - Description: `zeze-checkstyle`
    - Use a local Checkstyle file 체크 후 `Browse > (프로젝트 최상단) .zeze-checkstyle.xml` 선택
  - Import 한 CheckStyle 체크 후 apply

#### ESLint (TypeScript)

**Yarn / NPM**
- `cd client`
- (확인만 하고 싶다면) `yarn lint` (또는 `npm run lint`)
- (수정 가능한 부분들에 대해 자동 수정을 실행한다면) `yarn lint-fix` (또는 `npm run lint-fix`)

**IntelliJ**
- `Plugins`에서 `ESLint` 설치
- `Language & Frameworks > JavaScript > Code Quality Tools > ESLint` 에서 다음을 설정
  - `Automatic ESLint Configuration` 체크
  - (저장할 때마다 자동 수정을 원한다면) `Run eslint --fix on save` 체크


### Client Build (*Server와 연동 없이* Client만 Build하는 경우)

- `cd client`
- `yarn` (또는 `npm install`)
- `yarn build`
- (`serve`가 설치되어있지 않은 경우) `yarn global add serve` (또는 `npm install -g serve`)
- `serve -s build`


### Server Build 

- (Client 빌드 후 Server의 `resources/static`으로 옮기는 경우) `./gradlew buildClient`
- `./gradlew build`
- `java -jar ./build/libs/zeze-0.0.1-SNAPSHOT.jar`
', 'https://github.com/woowacourse-teams/2020-zeze', NULL, '2020-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/woowacourse-teams/2023-hang-log/assets/77482065/cfddda8a-e416-4f03-bc53-edd6eb0c4595', 5, NULL, 'Hang LOG', 'hang-log', 'Hang LOG', '장소 기반 여행 기록 서비스, "행록"⛱️', 239, '2026-08-07', 'CLOSED', 'APPROVED', '<div align="center">

## [행록 바로가기](https://hanglog.com)

<img src="https://github.com/woowacourse-teams/2023-hang-log/assets/77482065/cfddda8a-e416-4f03-bc53-edd6eb0c4595" width="30%">

<br>
<br>
<br>
<br>

<img src="https://github.com/woowacourse-teams/2023-hang-log/assets/49433615/184d3569-b1c6-4f4d-bef0-fb532cc554f3" width="100%"  style="background-color: #f0f0f0; padding-top:10px;">

<br>

![image2](https://github.com/woowacourse-teams/2023-hang-log/assets/49433615/2981c210-cda3-4644-887f-45ee14268767)

![image](https://github.com/woowacourse-teams/2023-hang-log/assets/49433615/c2fc4007-d9ba-4f6b-9f64-04c29fc46078)

![image4](https://github.com/woowacourse-teams/2023-hang-log/assets/49433615/29c24162-8702-4b47-87f6-0b703b4060fb)

![image5](https://github.com/woowacourse-teams/2023-hang-log/assets/49433615/1205a54a-58c1-4b63-8062-c89274658f9d)

![image6](https://github.com/woowacourse-teams/2023-hang-log/assets/49433615/6c4ab178-b9ca-4141-a904-f493a68ca082)

![image7](https://github.com/woowacourse-teams/2023-hang-log/assets/49433615/b3829bde-7870-4836-adab-3d1add7b42a1)

![image8](https://github.com/woowacourse-teams/2023-hang-log/assets/102305630/bc5d2fc3-1cbc-4cf6-9602-c63872034f6c)

### [행록 바로가기 ](https://hanglog.com)

<br>

</div>

<br>

## 기술 스택

### 프론트엔드

<img width="80%" alt="스크린샷 2024-01-22 오후 11 43 48" src="https://github.com/woowacourse-teams/2023-hang-log/assets/45068522/9284b40e-c931-4f04-ac78-9c97a548338e">

### 백엔드

<img width="80%" alt="스크린샷 2024-01-22 오후 11 43 16" src="https://github.com/woowacourse-teams/2023-hang-log/assets/45068522/b64dde4f-9467-4427-a361-e2301657efa4">

### 인프라

<img width="80%" alt="스크린샷 2024-01-22 오후 11 44 27" src="https://github.com/woowacourse-teams/2023-hang-log/assets/45068522/d0e03eb4-e159-405b-b22e-3d3fa79b99b7">

<br>

## 서비스 요청 흐름도

![서비스요청흐름도](https://github.com/woowacourse-teams/2023-hang-log/assets/102305630/dc9c1562-068d-4c73-84ef-d93e9051b679)

## CI/CD

![CICD](https://github.com/woowacourse-teams/2023-hang-log/assets/64852591/a55b3a1c-ce12-49d2-b4da-5b394c4de6c1)

## 모니터링 구조도

![모니터링 구조도](https://github.com/woowacourse-teams/2023-hang-log/assets/64852591/26da0064-7caf-42a7-b341-4e1f9db99865)

## 이미지 요청 흐름도

![이미지 요청 흐름도](https://github.com/woowacourse-teams/2023-hang-log/assets/64852591/65cdfaea-e546-43ab-80b3-2c57c9336544)

## [행록 디자인 시스템](https://github.com/hang-log-design-system/design-system)

![행록디자인시스템](https://github.com/woowacourse-teams/2023-hang-log/assets/49433615/23457a14-fb21-498c-9b65-a6c92826a0c3)

<br>

## 멤버

### 프론트엔드

| <img src="https://avatars.githubusercontent.com/u/45068522?v=4" width="130" height="130"> | <img src ="https://avatars.githubusercontent.com/u/51967731?v=4" width="130" height="130"> | <img src ="https://avatars.githubusercontent.com/u/102305630?v=4" width="130" height="130"> |
| :---------------------------------------------------------------------------------------: | :----------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------: |
|                         [슬링키](https://github.com/dladncks1217)                         |                          [애슐리](https://github.com/ashleysyheo)                          |                             [헤다](https://github.com/Dahyeeee)                             |

### 백엔드

| <img src="https://avatars.githubusercontent.com/u/49433615?v=4" width="130" height="130"> | <img src="https://avatars.githubusercontent.com/u/77482065?v=4" width="130" height="130"> | <img src="https://avatars.githubusercontent.com/u/64852591?v=4" width="130" height="130"> | <img src="https://avatars.githubusercontent.com/u/65850682?v=4" width="130" height="130"> | <img src="https://avatars.githubusercontent.com/u/91263263?v=4" width="130" height="130"> |
| :---------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------: |
|                             [이오](https://github.com/LJW25)                              |                            [디노](https://github.com/jjongwa)                             |                            [라온](https://github.com/mcodnjs)                             |                             [홍고](https://github.com/hgo641)                             |                         [달리](https://github.com/waterricecake)                          |
', 'https://github.com/woowacourse-teams/2023-hang-log', NULL, '2023-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://raw.githubusercontent.com/woowacourse-teams/2023-naaga/main/etc/images/header.png', 5, NULL, 'Naaga', 'naaga', 'Naaga', '장소의 사진을 보고 걸어다니며 추리하는 게임 서비스: "나아가"🕵🏻‍♂️', 37, '2026-08-07', 'CLOSED', 'APPROVED', '![제목](https://raw.githubusercontent.com/woowacourse-teams/2023-naaga/main/etc/images/header.png)

## 💌 나아가로부터의 초대장이 도착했습니다

반복되는 인스타 피드를 보고, 게임을 하며 보내는 일상이 지루하지 않으시나요?
공부와 일에 치여 실내에서 보내는 시간이 많을 텐데요. 작은 화면 속을 벗어나 현실 세계의 경험을 해보고 싶지 않으신가요?

그런 당신을 ‘나아가’로 초대합니다.

## 🚶🏻 추리와 발걸음의 만남
나아가는 현실 세계를 누비며 진행되는 추리 게임입니다. 게임을 시작하면, 당신 주변 어딘가의 사진이 제공됩니다. 사진이 알쏭달쏭하여 그곳이 어딘지 알아맞히기 어렵겠지만, 우선 발걸음을 옮겨보세요.


<a href="https://play.google.com/store/apps/details?id=com.now.naaga&pcampaignid=web_share"><img src="https://raw.githubusercontent.com/woowacourse-teams/2023-naaga/main/etc/images/google%20play%20store.png"/></a>

![상세 페이지](https://raw.githubusercontent.com/woowacourse-teams/2023-naaga/main/etc/images/service%2520intro.png)
', 'https://github.com/woowacourse-teams/2023-naaga', NULL, '2023-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://user-images.githubusercontent.com/79205414/194690088-90f39269-d6bf-472e-a633-a9de19a94904.png', 4, NULL, 'MO RAK', 'mo-rak', 'MO RAK', '🥳 모락: 모임을 즐겁게, 편하게!', 49, '2026-08-07', 'CLOSED', 'APPROVED', '<p align="center">
      <a href="https://mo-rak.com/" target="_blank">    
            <img src="https://user-images.githubusercontent.com/79205414/194690088-90f39269-d6bf-472e-a633-a9de19a94904.png" width="300"/>
      </a>
</p>

<div align = "center">

<h3>모임을 즐겁게 편하게, 모락</h3>

모임이 편해진다! <br>

[모락](https://mo-rak.com/)을 통해 모임을 즐겨보세요 🎉 <br> <br> <br>

</div>


# 🔎 소개 

**모락은 모임에서 필요한 기능들을 한 곳에서 제공**함으로써,

사람들이 **모임에 더욱 몰입**할 수 있게 도와주는 것을 목표로 하고 있어요!

모락이 모임을 더 편하고, 즐겁게 할 수 있도록 도와줄게요 👍


### 🗳 우리 투표하자!

![모락-투표하기](https://user-images.githubusercontent.com/79205414/194995574-eab0b766-ea99-4129-b0ac-d7c8bf95671a.gif)

### ⏰ 우리 언제 모여?

![모락-약속투표하기2](https://user-images.githubusercontent.com/79205414/194995561-c7eba34e-0455-4e5e-afb8-f056c28cb793.gif)

### 👑 오늘 무슨 역할이야?

![모락-역할정하기](https://user-images.githubusercontent.com/79205414/196872928-6577e22e-f0eb-4bf9-9259-c1590d0c5151.gif)

<br>
<br>

# 👨‍👩‍👧‍👦 팀 소개

## 팀 멤버

> 모락팀 멤버들을 소개합니다!

|Front end|Front end|Back end|Back end|Back end|Back end|
| :-: | :-: | :-: | :-: | :-: | :-: |
| <img src="https://user-images.githubusercontent.com/64825713/194213208-aa64bae2-16b3-48ab-bd9a-6d6029b1cfaf.png" alt="albur" width="150"> | <img src="https://user-images.githubusercontent.com/64825713/194213572-306c6b8c-0283-4615-ad54-f1421e8ec6cb.png" alt="위니" width="150"> | <img src="https://user-images.githubusercontent.com/64825713/194213401-f8fe16f9-6749-424e-b3b5-c685aec95a50.png" alt="에덴" width="150"> | <img src="https://user-images.githubusercontent.com/64825713/194213961-0c8c38d0-5795-4861-a997-9d3da2eb9dd7.png" alt="차리" width="150"> | <img src="https://user-images.githubusercontent.com/64825713/194214234-375362a1-2973-4460-be70-ffc3df759578.png" alt="엘리" width="150"> | <img src="https://user-images.githubusercontent.com/64825713/194214404-194f385e-2329-43e5-af07-524f8ff752d6.png" alt="배카라" width="150"> |
|[앨버(송상민)](https://github.com/al-bur)|[위니(김예지)](https://github.com/rladpwl0512)|[에덴(김성산)](https://github.com/leo0842)|[차리(이찬주)](https://github.com/cjlee38)|[엘리(한해리)](https://github.com/RIANAEH)|[배카라(박성우)](https://github.com/seong-wooo)|

<br>

## 팀 문화

> 우리는 이렇게 개발하고 있어요!

### 💡 모든 지각에는 이유가 있다.

- 지각에는 개인적인 이유, 팀적인 이유 등 다양한 이유가 있을 수 있어요.
- 비난보다는 함께 근본적인 이유를 파악하고 해결책을 찾아보도록 해요. 

### 📋 회고는 자주! KPT 방식으로!

- 회고에서만큼은 개발이 아닌 팀 문화나 감정에 관해 이야기를 나누기로해요.
- KPT 방식으로 회고를 진행해 좋은 부분은 유지하고(KEEP) 문제가 있었던 부분(PROBLEM)에 대해서는 액션 플랜(TRY)을 세우고 실천해요.

### 😆 매일 아침 데일리 미팅으로, 하루를 활기차게 ♪~ ᕕ( ᐛ )ᕗ

- 아침에 서로의 컨디션에 대해 이야기를 하면서, 하하호호 웃을 수 있는 시간을 가져요.
- 물론, 어제의 DID와 오늘의 TODO 도 공유해요. 

### 🌏 프론트엔드, 백엔드 우리는 하나

- 매 스프린트마다 프론트엔드, 백엔드 팀원들이 함께 몹 프로그래밍을 진행해 서로를 이해하는 시간을 가져요.
- 기능 요구사항이나 API 설계 관련 회의는 무조건 같이 진행해요. 

### 🤔 말하지 않으면 몰라요~

- 침묵은 동의가 아니라 오해를 낳을 수 있어요. 
- 의견이 있으면 망설이지 말고 이야기해요.

### 📚 너도 알고 나도 알고

- 개발일지를 작성하고 세미나를 통해 지식을 공유해요. 
- 서로 "왜?"라는 질문을 해주기로해요.

<br>
<br>

# ⚙️ 인프라

> 사용한 기술 스택과 인프라 구조는 다음과 같아요.  

![image](https://user-images.githubusercontent.com/45311765/196595945-41b38977-343c-451d-a1b2-af0a6e895c89.png)

<br>
<br>

# 📸 볼거리

> 모락의 재미있는 브이로그와 데모 영상들을 감상해보세요!

|[모락 브이로그](https://www.youtube.com/watch?v=sLBxjoZ6gKA)|[1차 데모](https://www.youtube.com/watch?v=R7JO6cLeyhU)|[2차 데모](https://www.youtube.com/watch?v=G4uQTNYNanY)|
| :-: | :-: | :-: |
|<img width="400" alt="image" src="https://user-images.githubusercontent.com/42317507/195745372-6791cc40-645c-4446-9667-f3957f8ae8b5.png">|<img width="400" alt="image" src="https://user-images.githubusercontent.com/42317507/195744540-2e08d8ed-57b3-4d5f-b19d-83c91d3c0852.png">|<img width="400" alt="image" src="https://user-images.githubusercontent.com/42317507/195744783-01661f3e-5ba8-4f6e-95cf-4be6faa7bc0d.png">|
|[3차 데모](https://www.youtube.com/watch?v=RSkr2x3n9B8)|[4차 데모](https://www.youtube.com/watch?v=u_INarrFVZ0)||
|<img width="400" alt="image" src="https://user-images.githubusercontent.com/42317507/195744439-2c362445-525e-447c-9aac-ee006927e130.png">|<img width="400" alt="image" src="https://user-images.githubusercontent.com/42317507/195745143-297cdc95-9e9d-4f4a-ad8a-e9590a5fbc69.png">|
', 'https://github.com/woowacourse-teams/2022-mo-rak', NULL, '2022-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/woowacourse-teams/2023-pium/assets/68818952/987ff41e-08fb-43dd-a4cf-07e7cc4a1dab', 5, NULL, 'Pium', 'pium', 'Pium', '반려식물 관리 서비스, 피움 🌱', 39, '2026-08-07', 'CLOSED', 'APPROVED', '# 피움 Wiki 🌱

피움🌱 의 Wiki에 오신걸 환영합니다 🎉

## 피움의 집사들을 소개합니다 🤗

|                                                              Backend                                                              |                                                               Backend                                                               |                                                            Backend                                                            |                                                                 Backend                                                                  |                                                              Frontend                                                               |                                                             Frontend                                                             |                                                            Frontend                                                             |
| :-------------------------------------------------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------------------------------------------: | :--------------------------------------------------------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------------------------------------------------: | :------------------------------------------------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------------------------------------------: |
| <a href="https://github.com/yeonkkk"><img src="https://avatars.githubusercontent.com/u/88660886?v=4" width=400px alt="조이"/></a> | <a href="https://github.com/kim0914"><img src="https://avatars.githubusercontent.com/u/68818952?v=4" width=400px alt="그레이"/></a> | <a href="github.com/Choi-JJunho"><img src="https://avatars.githubusercontent.com/u/49794401?v=4" width=400px alt="주노"/></a> | <a href="https://github.com/rawfishthelgh"><img src="https://avatars.githubusercontent.com/u/79038908?v=4" width=400px alt="하마드"></a> | <a href="https://github.com/hozzijeong"><img src="https://avatars.githubusercontent.com/u/50974359?v=4" width=400px alt="클린"></a> | <a href="https://github.com/WaiNaat"><img src="https://avatars.githubusercontent.com/u/77872742?v=4" width=400px alt="참새"></a> | <a href="https://github.com/bassyu"><img src="https://avatars.githubusercontent.com/u/54442420?v=4" width=400px alt="쵸파"></a> |
|                                                [조이](https://github.com/yeonkkk)                                                 |                                                [그레이](https://github.com/kim0914)                                                 |                                            [주노](https://github.com/Choi-JJunho)                                             |                                                [하마드](https://github.com/rawfishthelgh)                                                |                                                [클린](https://github.com/hozzijeong)                                                |                                                [참새](https://github.com/WaiNaat)                                                |                                                [쵸파](https://github.com/bassyu)                                                |

# 피움 서비스 소개

<img src="https://github.com/woowacourse-teams/2023-pium/assets/68818952/987ff41e-08fb-43dd-a4cf-07e7cc4a1dab" width="400px">

식물 관리법은 환경에 따라 다르기 때문에 경험을 통해서 터득할 수밖에 없어요.

여러분의 관리 경험을 기록한다면 각자에게 알맞은 관리법을 더 빠르게 도출할 수 있겠죠?

''피움''은 이러한 가치를 바탕으로 탄생하게 되었습니다.

리마인더를 이용해서 반려 식물 관리 이력을 기록하고, 타임라인을 이용해서 여러분의 관리 이력을 일목요연하게 확인하는 것.

이 두 가지가 피움이 여러분께 제공하는 기능입니다.

[피움 소개글 자세히 보러가기](https://github.com/woowacourse-teams/2023-pium/wiki/%ED%94%BC%EC%9B%80-%EC%86%8C%EA%B0%9C%EA%B8%80)

# 프로젝트 실행 방법

## 프론트엔드

```shell
git clone https://github.com/woowacourse-teams/2023-pium.git

cd 2023-pium/frontend
```

### env파일 설정

env 파일을 생성한다.

```shell
mkdir env

vim env/local.env
```

- local.env 파일 내부에 다음과 같이 설정한다.
- 카카오 rest_key는 [카카오 로그인 문서](https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api)를 확인하고 적용해야 한다.

```env
KAKAO_REST_KEY={본인_카카오_REST_KEY}
KAKAO_REDIRECT_URL=http://localhost:8282/authorization
HOST=http://localhost:8080/
```

### 실행

```shell
npm install

npm run local
```

## 백엔드

```shell
git clone https://github.com/woowacourse-teams/2023-pium.git

cd 2023-pium/backend/pium
```

### properties 파일 설정

```shell
vim src/main/resources/application.properties
```

아래 내용을 참고하여 properties 파일을 작성한다.

```properties
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://{서버_HOST}/{DATABASE}?characterEncoding=UTF-8&serverTimezone=Asia/Seoul
spring.datasource.username={DB_ACCOUNT}
spring.datasource.password={DB_PASSWORD}

spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.hibernate.ddl-auto=create-drop

logging.level.org.hibernate.orm.jdbc.bind=trace

auth.kakao.token-request-uri=https://kauth.kakao.com/oauth/token
auth.kakao.member-info-request-uri=https://kapi.kakao.com/v2/user/me
auth.kakao.redirect-uri={REDIRECT_URI}
auth.kakao.unlink-uri=https://kapi.kakao.com/v1/user/unlink
auth.kakao.client-id={REST_API_KEY}
auth.kakao.admin-id={ADMIN_KEY}

server.servlet.session.cookie.same-site=none
server.servlet.session.cookie.secure=true
```

### 프로젝트 실행

```shell
./gradlew build

java -jar build/libs/pium.jar
```

---

폰트: [네이버 나눔 스퀘어 라운드](https://hangeul.naver.com/font)를 수정하여 사용했습니다.

라이선스 (License)
자세한 사항은 해당 문서를 참조하십시오.

See [LICENSE](https://help.naver.com/service/30016/contents/18088?osType=PC&lang=ko) for more information.
', 'https://github.com/woowacourse-teams/2023-pium', NULL, '2023-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES (NULL, 7, NULL, 'Moitz', 'moitz', 'Moitz', '우리 사이에 뭐 있지? 알려주는 모잇지!', 12, '2026-08-07', 'CLOSED', 'APPROVED', '<div align="center">

# MOITZ

우리 사이에 뭐 있지? 알려주는 모잇지!

### 수많은 약속, 어디서 만나야할지 모르겠다면?

### 위치, 조건 기반 만남 지역 추천 서비스

### [모잇지](https://moitz.kr/)가 도와드릴게요!

</div>

#### 🥺 모잇지가 필요해요

- 누가 어디서 오는지, 얼마나 걸리는지, 어디를 가고 싶은지… 고려할 것이 너무 많은 의견 조율 과정이 머리아파요.
- 약속 장소 정하기에 아무도 나서지 않아 답답해요. 결국 고통받는건 항상 저 뿐이에요.
- 단순한 중간 지점은 만족스럽지 않아요. 조건에 맞는 장소를 추천받고 싶어요!

#### 🎯 핵심 가치 제안

1. **쉽고 빠른 결정 지원**: 의견 조율 및 고민 시간을 줄여 사용자의 결정 피로 해소
2. **일상 밀착형 서비스**: 매일 마주하는 약속 장소 고민에 대한 실질적 해결책
3. **직관적인 사용성**: 복잡한 설정 없이 바로 사용 가능한 간편함

#### 🚀 주요 기능

- 출발지를 최소 2개 이상 입력하면, 모든 출발지로부터 이동 시간이 비슷한 5개의 지역을 추천받아요.
- 도착 시간을 입력하면, 해당 시간에 맞춰 이동 경로와 소요 시간을 확인할 수 있어요.
- 장소 추천 조건을 입력하면, 조건을 만족하는 지역을 추천받을 수 있어요.

## 🛠️ 기술 스택

### 💻 프론트엔드

<table>
  <tr>
    <td><img src="https://raw.githubusercontent.com/woowacourse-teams/2025-moitz/preview/docs/icons/JavaScript.png" width="20"/> JavaScript</td>
    <td><img src="https://raw.githubusercontent.com/woowacourse-teams/2025-moitz/preview/docs/icons/react.png" width="20"/> React.js</td>
  </tr>
</table>

### ⚙️ 백엔드

<table>
  <tr>
    <td><img src="https://raw.githubusercontent.com/woowacourse-teams/2025-moitz/preview/docs/icons/springboot.png" width="20"/> SpringBoot</td>
  </tr>
</table>

## ✨ 담당 파트

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/saera-yook"><img src="https://avatars.githubusercontent.com/u/148451132?v=4" alt="saera-yook" width="100" height="100" style="object-fit: cover; border-radius: 10px;"></a>
      <br />
      <strong>아이나</strong>
      <br />
      🔧 BE 
    </td>
    <td align="center">
      <a href="https://github.com/egaeng09"><img src="https://avatars.githubusercontent.com/u/151512150?v=4" alt="egaeng09" width="100" height="100" style="object-fit: cover; border-radius: 10px;"></a>
      <br />
      <strong>시소</strong>
      <br />
      🔧 BE
    </td>
    <td align="center">
      <a href="https://github.com/jbilee"><img src="https://avatars.githubusercontent.com/u/128875051?v=4" alt="jbilee" width="100" height="100" style="object-fit: cover; border-radius: 10px;"></a>
      <br />
      <strong>줄리</strong>
      <br />
      🔧 BE
    </td>
    <td align="center">
      <a href="https://github.com/dbsdndcks"><img src="https://avatars.githubusercontent.com/u/106324609?u=58bf663d76adfe190012e2dd2a452af0c88ed74c&v=4" alt="dbsdndcks" width="100" height="100" style="object-fit: cover; border-radius: 10px;"></a>
      <br />
      <strong>레몬</strong>
      <br />
      🔧 BE
    </td>
    <td align="center">
      <a href="https://github.com/eunsoa"><img src="https://avatars.githubusercontent.com/u/74090200?v=4" alt="eunsoa" width="100" height="100" style="object-fit: cover; border-radius: 10px;"></a>
      <br />
      <strong>클레어</strong>
      <br />
      💻 FE
    </td>
    <td align="center">
      <a href="https://github.com/kaori-killer"><img src="https://avatars.githubusercontent.com/u/75800958?v=4" alt="kaori-killer" width="100" height="100" style="object-fit: cover; border-radius: 10px;"></a>
      <br />
      <strong>헤일리</strong>
      <br />
      💻 FE
    </td>
  </tr>
</table>

### 공통 협업 파트

🔻Git 컨벤션 준수

모든 커밋은 팀 내에서 약속된 Git 컨벤션을 따릅니다. 커밋 이력을 명확하게 하고, 코드 변경 이력을 쉽게 추적할 수 있도록 돕습니다.

🔻 코드 리뷰 진행

모든 코드는 병합(Merge) 전 동료 개발자의 코드 리뷰를 거칩니다. 코드 리뷰는 코드 품질을 향상시키고, 잠재적인 오류를 미리 발견하며, 팀 전체의 기술 역량을 강화하는 데 기여합니다. 적극적인 피드백을 통해 더 나은 코드를 만들어 갑니다.

🔻 기술 문서 작성

프로젝트와 관련된 중요한 기술 정보나 의사 결정 사항은 기술 문서 저장소에 문서화됩니다.
', 'https://github.com/woowacourse-teams/2025-moitz', NULL, '2025-01-01');
INSERT INTO projects (thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, star_count, star_synced_at, service_status, approval_status, description_md, github_repository_url, deployment_url, created_at) VALUES ('https://github.com/user-attachments/assets/bd6bc509-16bf-4c2d-aab1-21fd346398f1', 7, NULL, '쫄', 'zzol', '쫄', '쫄릴 준비 됐어? 똥손도 즐기는 게임 기반 추첨 서비스 🎮', 14, '2026-08-07', 'CLOSED', 'APPROVED', '# 쫄 (ZZOL)

<div align="center">
  <img width="4800" height="1368" alt="banner" src="https://github.com/user-attachments/assets/bd6bc509-16bf-4c2d-aab1-21fd346398f1" />
</div>

## 쫄릴 준비 됐어? 똥손도 즐기는 게임 기반 추첨 서비스 🎮

친구들과 반복되는 “오늘은 누구로 정할까?”, 이제 지루하지 않으신가요?

**쫄(ZZOL)** 은 똥손도 재미있게 즐길 수 있는 **미니게임 기반 당첨자 추첨 서비스** 입니다.

단순한 뽑기 대신, 미니게임과 룰렛 시스템을 통해 당첨 확률에 직접 개입할 수 있어 더 유쾌하고 쫄깃한 경험을 제공합니다.

👉🏻[게임하러가기](https://zzol.site)

## 🎯 서비스 흐름

<img width="7680" height="12960" alt="쫄 리드미" src="https://github.com/user-attachments/assets/9cf5441e-32aa-4738-9e6a-2a67ee54827c" />

## 🛠 기술 스택

### 🌐 FrontEnd

<img width="4604" height="2544" alt="image" src="https://github.com/user-attachments/assets/6c91653d-dfa0-4473-a1d9-b2ea100cae87" />

### 🍃 BackEnd

<img width="4604" height="1872" alt="backend" src="https://github.com/user-attachments/assets/45763566-7311-47e1-8aa0-13d4b2b230aa" />

### ⚙️ Infra

<img width="4604" height="2544" alt="infra" src="https://github.com/user-attachments/assets/57877c54-8297-4580-8630-9bbdd884a9fd" />

## 📌 Infra Design 

### CI / CD

<img width="1000" height="833" alt="image" src="https://github.com/user-attachments/assets/7c52feab-d94e-432a-bc3f-453d6f902e14" />

### Application

<img width="1000" height="1055" alt="image" src="https://github.com/user-attachments/assets/52f80fd5-c77a-43b8-a988-159e0c8866c6" />

## 👥 멤버

### 프론트엔드

| <img src="https://github.com/user-attachments/assets/c0694fc2-3078-4417-ba7b-2f7a66af1cc8" width="130" height="130"> | <img src ="https://github.com/user-attachments/assets/f95731c4-2cd3-41f4-9d9b-b695bc48b372" width="130" height="130"> | <img src ="https://github.com/user-attachments/assets/b2325a15-4771-48c2-b1a8-52217f4ee92b" width="130" height="130"> |
| :---------------------------------------------------------------------------------------: | :----------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------: |
|                         [니야](https://github.com/sooyeoniya)                         |                          [메리](https://github.com/rosielsh)                          |                             [다이앤](https://github.com/Daeun-100)                             |

### 백엔드

| <img src="https://github.com/user-attachments/assets/431c8211-6ca8-4599-a5d0-46d292c1abe4" width="130" height="130"> | <img src="https://github.com/user-attachments/assets/1336fce2-2faf-4eee-ba7c-d2a4a99e06e0" width="130" height="130"> | <img src="https://github.com/user-attachments/assets/7819232f-1029-40b4-bca8-19a895df4123" width="130" height="130"> | <img src="https://github.com/user-attachments/assets/ec37aec0-c270-47af-817d-18f30edb504a" width="130" height="130"> |
| :---------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------: |
|                             [한스](https://github.com/20HyeonsuLee)                              |                            [엠제이](https://github.com/theminjunchoi)                             |                            [꾹이](https://github.com/kiwoook)                             |                             [루키](https://github.com/junhaa)                             |
', 'https://github.com/woowacourse-teams/2025-zzol', NULL, '2025-01-01');

INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ah-madda'), NULL, '108217858', 'keemsebin', '김세빈', 'https://avatars.githubusercontent.com/u/108217858?v=4', 'https://github.com/keemsebin', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ah-madda'), NULL, '118044367', 'jeyongsong', 'jeyong', 'https://avatars.githubusercontent.com/u/118044367?v=4', 'https://github.com/jeyongsong', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ah-madda'), NULL, '62169861', 'abc5259', 'LeeJaeHoon', 'https://avatars.githubusercontent.com/u/62169861?v=4', 'https://github.com/abc5259', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ah-madda'), NULL, '91647696', 'yeji0214', 'YEJI', 'https://avatars.githubusercontent.com/u/91647696?v=4', 'https://github.com/yeji0214', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ah-madda'), NULL, '126966681', 'ExceptAnyone', '장정안', 'https://avatars.githubusercontent.com/u/126966681?v=4', 'https://github.com/ExceptAnyone', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ah-madda'), NULL, '60121346', 'praisebak', 'praisebak', 'https://avatars.githubusercontent.com/u/60121346?v=4', 'https://github.com/praisebak', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ah-madda'), NULL, '126929413', 'jumdo12', 'SeungYeon', 'https://avatars.githubusercontent.com/u/126929413?v=4', 'https://github.com/jumdo12', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bom-bom'), NULL, '58469870', 'Choidongjun0830', 'ChoiDongjun', 'https://avatars.githubusercontent.com/u/58469870?v=4', 'https://github.com/Choidongjun0830', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bom-bom'), NULL, '76567238', 'Ryan-Dia', 'Cheol Won', 'https://avatars.githubusercontent.com/u/76567238?v=4', 'https://github.com/Ryan-Dia', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bom-bom'), NULL, '115832836', 'kysub99', 'geonwoo kim', 'https://avatars.githubusercontent.com/u/115832836?v=4', 'https://github.com/kysub99', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bom-bom'), NULL, '88280787', 'rladmstn', 'Kim Eunsu', 'https://avatars.githubusercontent.com/u/88280787?v=4', 'https://github.com/rladmstn', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bom-bom'), NULL, '63039855', 'minSsan', 'Park Minseon', 'https://avatars.githubusercontent.com/u/63039855?v=4', 'https://github.com/minSsan', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bom-bom'), NULL, '62178788', 'guesung', 'Kuesung Park', 'https://avatars.githubusercontent.com/u/62178788?v=4', 'https://github.com/guesung', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bom-bom'), NULL, '61729032', 'jaeyoung-kwon', NULL, 'https://avatars.githubusercontent.com/u/61729032?v=4', 'https://github.com/jaeyoung-kwon', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bom-bom'), NULL, '106021313', 'JeLee-river', 'Lee Jeongeun', 'https://avatars.githubusercontent.com/u/106021313?v=4', 'https://github.com/JeLee-river', 7);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bom-bom'), NULL, '248608029', 'kep-jerry-je2', NULL, 'https://avatars.githubusercontent.com/u/248608029?v=4', 'https://github.com/kep-jerry-je2', 8);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bom-bom'), NULL, '121426422', 'seaniiio', '시원', 'https://avatars.githubusercontent.com/u/121426422?v=4', 'https://github.com/seaniiio', 9);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bottari'), NULL, '105299421', 'moondev03', 'MunJangHun', 'https://avatars.githubusercontent.com/u/105299421?v=4', 'https://github.com/moondev03', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bottari'), NULL, '121144710', 'jaehyeon2650', '장재현', 'https://avatars.githubusercontent.com/u/121144710?v=4', 'https://github.com/jaehyeon2650', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bottari'), NULL, '105531824', 'Sung-june27', NULL, 'https://avatars.githubusercontent.com/u/105531824?v=4', 'https://github.com/Sung-june27', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bottari'), NULL, '107793780', 'YehyeokBang', 'Yehyeok Bang', 'https://avatars.githubusercontent.com/u/107793780?v=4', 'https://github.com/YehyeokBang', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bottari'), NULL, '102152510', 'Leeyerin0210', 'Sia', 'https://avatars.githubusercontent.com/u/102152510?v=4', 'https://github.com/Leeyerin0210', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bottari'), NULL, '144558971', 'unh6unh6', '민경윤', 'https://avatars.githubusercontent.com/u/144558971?v=4', 'https://github.com/unh6unh6', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'bottari'), NULL, '58465973', 'cucumber99', 'Inhyeop Lee', 'https://avatars.githubusercontent.com/u/58465973?v=4', 'https://github.com/cucumber99', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'course-pick'), NULL, '161921046', 'doabletuple', 'Chan Heo', 'https://avatars.githubusercontent.com/u/161921046?v=4', 'https://github.com/doabletuple', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'course-pick'), NULL, '46932235', 'dompoo', '이창근', 'https://avatars.githubusercontent.com/u/46932235?v=4', 'https://github.com/dompoo', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'course-pick'), NULL, '106965005', 'choizz156', '최민석', 'https://avatars.githubusercontent.com/u/106965005?v=4', 'https://github.com/choizz156', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'course-pick'), NULL, '192606356', 'wondroid-world', 'wondroid-world', 'https://avatars.githubusercontent.com/u/192606356?v=4', 'https://github.com/wondroid-world', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'course-pick'), NULL, '104622150', 'kkiseug', '슥', 'https://avatars.githubusercontent.com/u/104622150?v=4', 'https://github.com/kkiseug', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'course-pick'), NULL, '186542209', 'mmm307955', NULL, 'https://avatars.githubusercontent.com/u/186542209?v=4', 'https://github.com/mmm307955', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'course-pick'), NULL, '108331578', 'giovannijunseokim', 'Giovanni Junseo Kim', 'https://avatars.githubusercontent.com/u/108331578?v=4', 'https://github.com/giovannijunseokim', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'course-pick'), NULL, '176254419', 'tobae-time', '박지원', 'https://avatars.githubusercontent.com/u/176254419?v=4', 'https://github.com/tobae-time', 7);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'course-pick'), NULL, '111430281', 'jhpark1227', 'Junhyeok Park', 'https://avatars.githubusercontent.com/u/111430281?v=4', 'https://github.com/jhpark1227', 8);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'estime'), NULL, '73924592', 'spoyodevelop', NULL, 'https://avatars.githubusercontent.com/u/73924592?v=4', 'https://github.com/spoyodevelop', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'estime'), NULL, '110888511', 'hoyyChoi', '호이초이', 'https://avatars.githubusercontent.com/u/110888511?v=4', 'https://github.com/hoyyChoi', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'estime'), NULL, '126754298', 'm-a-king', '조재중', 'https://avatars.githubusercontent.com/u/126754298?v=4', 'https://github.com/m-a-king', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'estime'), NULL, '185307804', 'yeonnhuu', 'Yeonhu Lee', 'https://avatars.githubusercontent.com/u/185307804?v=4', 'https://github.com/yeonnhuu', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'estime'), NULL, '56645802', 'jhan0121', 'flinter', 'https://avatars.githubusercontent.com/u/56645802?v=4', 'https://github.com/jhan0121', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'festabook'), NULL, '83596813', 'soeun2537', '이소은', 'https://avatars.githubusercontent.com/u/83596813?v=4', 'https://github.com/soeun2537', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'festabook'), NULL, '122252160', 'changuii', 'changui', 'https://avatars.githubusercontent.com/u/122252160?v=4', 'https://github.com/changuii', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'festabook'), NULL, '118153233', 'taek2222', 'Taek_2', 'https://avatars.githubusercontent.com/u/118153233?v=4', 'https://github.com/taek2222', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'festabook'), NULL, '95472545', 'oungsi2000', 'YongJun Jung', 'https://avatars.githubusercontent.com/u/95472545?v=4', 'https://github.com/oungsi2000', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'festabook'), NULL, '49092390', 'eoehd1ek', 'Dae-dong', 'https://avatars.githubusercontent.com/u/49092390?v=4', 'https://github.com/eoehd1ek', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'festabook'), NULL, '83579348', 'etama123', 'Dongjoo Seo', 'https://avatars.githubusercontent.com/u/83579348?v=4', 'https://github.com/etama123', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'festabook'), NULL, '134397864', 'parkjiminnnn', '박지민', 'https://avatars.githubusercontent.com/u/134397864?v=4', 'https://github.com/parkjiminnnn', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'hearit'), NULL, '127360730', 'yuyoungrhee', '의엉', 'https://avatars.githubusercontent.com/u/127360730?v=4', 'https://github.com/yuyoungrhee', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'hearit'), NULL, '37996727', 'HamBeomJoon', 'Ham BeomJoon', 'https://avatars.githubusercontent.com/u/37996727?v=4', 'https://github.com/HamBeomJoon', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'hearit'), NULL, '13532613', 'Byesol', '백승주', 'https://avatars.githubusercontent.com/u/13532613?v=4', 'https://github.com/Byesol', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'hearit'), NULL, '108220648', 'gabean13', 'Gabin Choi', 'https://avatars.githubusercontent.com/u/108220648?v=4', 'https://github.com/gabean13', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'hearit'), NULL, '68581876', 'gahyunkim', 'gahyunkim', 'https://avatars.githubusercontent.com/u/68581876?v=4', 'https://github.com/gahyunkim', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'hearit'), NULL, '105639473', 'JO-eusan', 'San', 'https://avatars.githubusercontent.com/u/105639473?v=4', 'https://github.com/JO-eusan', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'hearit'), NULL, '121722789', 'rosemin928', 'Min', 'https://avatars.githubusercontent.com/u/121722789?v=4', 'https://github.com/rosemin928', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moaon'), NULL, '154664697', 'wo-o29', '우혁', 'https://avatars.githubusercontent.com/u/154664697?v=4', 'https://github.com/wo-o29', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moaon'), NULL, '129190157', 'mlnwns', '곽민준', 'https://avatars.githubusercontent.com/u/129190157?v=4', 'https://github.com/mlnwns', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moaon'), NULL, '72060681', 'jin123457', 'JIN', 'https://avatars.githubusercontent.com/u/72060681?v=4', 'https://github.com/jin123457', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moaon'), NULL, '96484143', 'yesjuhee', '노주희', 'https://avatars.githubusercontent.com/u/96484143?v=4', 'https://github.com/yesjuhee', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moaon'), NULL, '144205824', 'Minuring', 'Minuring', 'https://avatars.githubusercontent.com/u/144205824?v=4', 'https://github.com/Minuring', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moaon'), NULL, '171022147', 'minjae8563', 'minjae', 'https://avatars.githubusercontent.com/u/171022147?v=4', 'https://github.com/minjae8563', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moaon'), NULL, '162389416', 'eueo8259', NULL, 'https://avatars.githubusercontent.com/u/162389416?v=4', 'https://github.com/eueo8259', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'mul-kkam'), NULL, '106906887', 'hwannow', NULL, 'https://avatars.githubusercontent.com/u/106906887?v=4', 'https://github.com/hwannow', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'mul-kkam'), NULL, '111180367', '2Jin1031', 'kali', 'https://avatars.githubusercontent.com/u/111180367?v=4', 'https://github.com/2Jin1031', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'mul-kkam'), NULL, '137619133', 'CheChe903', 'CheChe903', 'https://avatars.githubusercontent.com/u/137619133?v=4', 'https://github.com/CheChe903', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'mul-kkam'), NULL, '77621712', 'Jin409', 'Seunghee Jin', 'https://avatars.githubusercontent.com/u/77621712?v=4', 'https://github.com/Jin409', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'mul-kkam'), NULL, '127238018', 'junseo511', 'GongBaek', 'https://avatars.githubusercontent.com/u/127238018?v=4', 'https://github.com/junseo511', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'mul-kkam'), NULL, '63039855', 'minSsan', 'Park Minseon', 'https://avatars.githubusercontent.com/u/63039855?v=4', 'https://github.com/minSsan', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'mul-kkam'), NULL, '183975833', 'devfeijoa', 'Eunyeong Jang', 'https://avatars.githubusercontent.com/u/183975833?v=4', 'https://github.com/devfeijoa', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pick-eat'), NULL, '126178440', 'shuyeon', '마', 'https://avatars.githubusercontent.com/u/126178440?v=4', 'https://github.com/shuyeon', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pick-eat'), NULL, '99790907', 'jinu0328', 'Jinwoo Kim', 'https://avatars.githubusercontent.com/u/99790907?v=4', 'https://github.com/jinu0328', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pick-eat'), NULL, '62841992', 'wodnd0131', 'wodnd0131', 'https://avatars.githubusercontent.com/u/62841992?v=4', 'https://github.com/wodnd0131', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pick-eat'), NULL, '128235227', 'KJungW', NULL, 'https://avatars.githubusercontent.com/u/128235227?v=4', 'https://github.com/KJungW', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pick-eat'), NULL, '141295691', 'dev-dino22', NULL, 'https://avatars.githubusercontent.com/u/141295691?v=4', 'https://github.com/dev-dino22', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pick-eat'), NULL, '119796600', 'minji2219', '서민지', 'https://avatars.githubusercontent.com/u/119796600?v=4', 'https://github.com/minji2219', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'routie'), NULL, '123801385', 'ohgus', NULL, 'https://avatars.githubusercontent.com/u/123801385?v=4', 'https://github.com/ohgus', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'routie'), NULL, '115888336', 'jeongyou', 'youjeong Jeong', 'https://avatars.githubusercontent.com/u/115888336?v=4', 'https://github.com/jeongyou', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'routie'), NULL, '183567170', 'AHHYUNJU', '주렁', 'https://avatars.githubusercontent.com/u/183567170?v=4', 'https://github.com/AHHYUNJU', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'routie'), NULL, '28076054', 'threepebbles', 'heiler', 'https://avatars.githubusercontent.com/u/28076054?v=4', 'https://github.com/threepebbles', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'routie'), NULL, '113977176', 'cookie-meringue', '머랭', 'https://avatars.githubusercontent.com/u/113977176?v=4', 'https://github.com/cookie-meringue', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'routie'), NULL, '96824025', 'DongchannN', NULL, 'https://avatars.githubusercontent.com/u/96824025?v=4', 'https://github.com/DongchannN', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'routie'), NULL, '85331323', 'goohong', 'Goohong Chung', 'https://avatars.githubusercontent.com/u/85331323?v=4', 'https://github.com/goohong', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'routie'), NULL, '77476077', 'aydenote', 'Seungsoo Choi', 'https://avatars.githubusercontent.com/u/77476077?v=4', 'https://github.com/aydenote', 7);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'todok-todok'), NULL, '84930748', 'chanho0908', '페토', 'https://avatars.githubusercontent.com/u/84930748?v=4', 'https://github.com/chanho0908', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'todok-todok'), NULL, '192606356', 'wondroid-world', 'wondroid-world', 'https://avatars.githubusercontent.com/u/192606356?v=4', 'https://github.com/wondroid-world', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'todok-todok'), NULL, '113325033', 'Chaeyoung714', 'chaeyounglee', 'https://avatars.githubusercontent.com/u/113325033?v=4', 'https://github.com/Chaeyoung714', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'todok-todok'), NULL, '82762769', 'donghyun81', '윤동현', 'https://avatars.githubusercontent.com/u/82762769?v=4', 'https://github.com/donghyun81', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'todok-todok'), NULL, '77716414', 'ljhee92', 'juhee', 'https://avatars.githubusercontent.com/u/77716414?v=4', 'https://github.com/ljhee92', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'todok-todok'), NULL, '156290096', 'horizonpioneer', 'Wooyoung', 'https://avatars.githubusercontent.com/u/156290096?v=4', 'https://github.com/horizonpioneer', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'todok-todok'), NULL, '109019081', 'sonjh919', 'Junhyung Son', 'https://avatars.githubusercontent.com/u/109019081?v=4', 'https://github.com/sonjh919', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'turip'), NULL, '183526990', 'jerry8282', NULL, 'https://avatars.githubusercontent.com/u/183526990?v=4', 'https://github.com/jerry8282', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'turip'), NULL, '171224212', 'yrsel', NULL, 'https://avatars.githubusercontent.com/u/171224212?v=4', 'https://github.com/yrsel', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'turip'), NULL, '121426422', 'seaniiio', '시원', 'https://avatars.githubusercontent.com/u/121426422?v=4', 'https://github.com/seaniiio', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'turip'), NULL, '86725408', 'eunseongu', '구은선', 'https://avatars.githubusercontent.com/u/86725408?v=4', 'https://github.com/eunseongu', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'turip'), NULL, '114990782', 'm6z1', 'Son Myeongji', 'https://avatars.githubusercontent.com/u/114990782?v=4', 'https://github.com/m6z1', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'turip'), NULL, '183483852', 'RaZel713', '라젤', 'https://avatars.githubusercontent.com/u/183483852?v=4', 'https://github.com/RaZel713', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'yagu-bogu'), NULL, '78211281', 'Starlight258', 'Mint', 'https://avatars.githubusercontent.com/u/78211281?v=4', 'https://github.com/Starlight258', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'yagu-bogu'), NULL, '94045552', 'jjunh0', NULL, 'https://avatars.githubusercontent.com/u/94045552?v=4', 'https://github.com/jjunh0', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'yagu-bogu'), NULL, '101489455', 'nourzoo', '포라', 'https://avatars.githubusercontent.com/u/101489455?v=4', 'https://github.com/nourzoo', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'yagu-bogu'), NULL, '129655108', 'bowook', 'wook', 'https://avatars.githubusercontent.com/u/129655108?v=4', 'https://github.com/bowook', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'chongdae-market'), NULL, '80222352', 'chaehyuns', '채현', 'https://avatars.githubusercontent.com/u/80222352?v=4', 'https://github.com/chaehyuns', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'chongdae-market'), NULL, '46563149', 'fromitive', 'MooSong Lee', 'https://avatars.githubusercontent.com/u/46563149?v=4', 'https://github.com/fromitive', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'chongdae-market'), NULL, '83302344', 'ChooSeoyeon', 'Seoyeon Choo', 'https://avatars.githubusercontent.com/u/83302344?v=4', 'https://github.com/ChooSeoyeon', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'chongdae-market'), NULL, '88581911', 'helenason', 'SCY', 'https://avatars.githubusercontent.com/u/88581911?v=4', 'https://github.com/helenason', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'chongdae-market'), NULL, '84739562', 'Yunseok-Nam', 'Yunseok Nam', 'https://avatars.githubusercontent.com/u/84739562?v=4', 'https://github.com/Yunseok-Nam', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'chongdae-market'), NULL, '138569524', 'songpink', 'alsong', 'https://avatars.githubusercontent.com/u/138569524?v=4', 'https://github.com/songpink', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ddangkong'), NULL, '63959171', 'rbgksqkr', '박규한', 'https://avatars.githubusercontent.com/u/63959171?v=4', 'https://github.com/rbgksqkr', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ddangkong'), NULL, '74897720', 'useon', 'Yuseon Kim(썬데이)', 'https://avatars.githubusercontent.com/u/74897720?v=4', 'https://github.com/useon', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ddangkong'), NULL, '111696934', 'novice0840', NULL, 'https://avatars.githubusercontent.com/u/111696934?v=4', 'https://github.com/novice0840', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ddangkong'), NULL, '84304802', 'PgmJun', 'Eden', 'https://avatars.githubusercontent.com/u/84304802?v=4', 'https://github.com/PgmJun', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ddangkong'), NULL, '44027393', 'leegwichan', 'Chung-an Lee', 'https://avatars.githubusercontent.com/u/44027393?v=4', 'https://github.com/leegwichan', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ddangkong'), NULL, '101033262', 'GIVEN53', 'Gibeom Nam', 'https://avatars.githubusercontent.com/u/101033262?v=4', 'https://github.com/GIVEN53', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ddangkong'), NULL, '78288539', 'jhon3242', 'Wonjun Choi', 'https://avatars.githubusercontent.com/u/78288539?v=4', 'https://github.com/jhon3242', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'devel-up'), NULL, '109535991', 'brgndyy', 'JEON TAEHEON', 'https://avatars.githubusercontent.com/u/109535991?v=4', 'https://github.com/brgndyy', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'devel-up'), NULL, '45223837', 'robinjoon', '임수빈', 'https://avatars.githubusercontent.com/u/45223837?v=4', 'https://github.com/robinjoon', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'devel-up'), NULL, '121149171', 'chosim-dvlpr', 'Minji', 'https://avatars.githubusercontent.com/u/121149171?v=4', 'https://github.com/chosim-dvlpr', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'devel-up'), NULL, '80797824', 'Parkhanyoung', '박한영(Ryan)', 'https://avatars.githubusercontent.com/u/80797824?v=4', 'https://github.com/Parkhanyoung', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'devel-up'), NULL, '131349867', 'Minjoo522', 'Kim Minjoo(김민주/리브)', 'https://avatars.githubusercontent.com/u/131349867?v=4', 'https://github.com/Minjoo522', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'devel-up'), NULL, '39932141', 'le2sky', 'Haneul Lee', 'https://avatars.githubusercontent.com/u/39932141?v=4', 'https://github.com/le2sky', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'devel-up'), NULL, '75781414', 'alstn113', 'Minsu Kim', 'https://avatars.githubusercontent.com/u/75781414?v=4', 'https://github.com/alstn113', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'devel-up'), NULL, '140090179', 'lilychoibb', 'yoonseo choi', 'https://avatars.githubusercontent.com/u/140090179?v=4', 'https://github.com/lilychoibb', 7);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'friendogly'), NULL, '69571848', 'junjange', 'JunJangE', 'https://avatars.githubusercontent.com/u/69571848?v=4', 'https://github.com/junjange', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'friendogly'), NULL, '37261785', 'takoyakimchi', 'Chungyul Lee', 'https://avatars.githubusercontent.com/u/37261785?v=4', 'https://github.com/takoyakimchi', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'friendogly'), NULL, '85734140', 'jinuemong', '김진우', 'https://avatars.githubusercontent.com/u/85734140?v=4', 'https://github.com/jinuemong', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'friendogly'), NULL, '79188587', 'ehtjsv2', '김도선', 'https://avatars.githubusercontent.com/u/79188587?v=4', 'https://github.com/ehtjsv2', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'friendogly'), NULL, '110461155', 'J-I-H-O', 'Jeong Jiho', 'https://avatars.githubusercontent.com/u/110461155?v=4', 'https://github.com/J-I-H-O', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'friendogly'), NULL, '92314556', 'gaeun5744', 'Gaeun Lee', 'https://avatars.githubusercontent.com/u/92314556?v=4', 'https://github.com/gaeun5744', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'friendogly'), NULL, '28584160', 'jimi567', '김기범(Kibum kim)', 'https://avatars.githubusercontent.com/u/28584160?v=4', 'https://github.com/jimi567', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'friendogly'), NULL, '102402485', 'dpcks0509', 'Yaechan Park', 'https://avatars.githubusercontent.com/u/102402485?v=4', 'https://github.com/dpcks0509', 7);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'haeng-dong'), NULL, '81083461', 'jinhokim98', 'JinHo Kim', 'https://avatars.githubusercontent.com/u/81083461?v=4', 'https://github.com/jinhokim98', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'haeng-dong'), NULL, '85233397', 'Todari', '토다리', 'https://avatars.githubusercontent.com/u/85233397?v=4', 'https://github.com/Todari', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'haeng-dong'), NULL, '64801796', 'pakxe', 'Pakxe', 'https://avatars.githubusercontent.com/u/64801796?v=4', 'https://github.com/pakxe', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'haeng-dong'), NULL, '77609591', 'soi-ha', 'Soyeon Choe', 'https://avatars.githubusercontent.com/u/77609591?v=4', 'https://github.com/soi-ha', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'haeng-dong'), NULL, '66822642', 'Arachneee', 'Arachne', 'https://avatars.githubusercontent.com/u/66822642?v=4', 'https://github.com/Arachneee', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'haeng-dong'), NULL, '85242378', 'kunsanglee', '이건상', 'https://avatars.githubusercontent.com/u/85242378?v=4', 'https://github.com/kunsanglee', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'haeng-dong'), NULL, '64410384', '3Juhwan', 'Juhwan Kim', 'https://avatars.githubusercontent.com/u/64410384?v=4', 'https://github.com/3Juhwan', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'haeng-dong'), NULL, '84626225', 'khabh', 'juha', 'https://avatars.githubusercontent.com/u/84626225?v=4', 'https://github.com/khabh', 7);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'haeng-dong'), NULL, '191424953', 'gosmdochee', 'ratel-이건상', 'https://avatars.githubusercontent.com/u/191424953?v=4', 'https://github.com/gosmdochee', 8);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pokerogue-helper'), NULL, '87055456', 'murjune', 'JUNWON LEE', 'https://avatars.githubusercontent.com/u/87055456?v=4', 'https://github.com/murjune', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pokerogue-helper'), NULL, '87695921', 'kkosang', 'SangHyun Ko', 'https://avatars.githubusercontent.com/u/87695921?v=4', 'https://github.com/kkosang', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pokerogue-helper'), NULL, '90040304', 'sh1mj1', 'sh1mj1', 'https://avatars.githubusercontent.com/u/90040304?v=4', 'https://github.com/sh1mj1', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pokerogue-helper'), NULL, '81362348', 'JoYehyun99', 'Yehyun Jo', 'https://avatars.githubusercontent.com/u/81362348?v=4', 'https://github.com/JoYehyun99', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pokerogue-helper'), NULL, '101439796', 'jongmee', '종미', 'https://avatars.githubusercontent.com/u/101439796?v=4', 'https://github.com/jongmee', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pokerogue-helper'), NULL, '105053478', 'jinchiim', 'Eugene Jang', 'https://avatars.githubusercontent.com/u/105053478?v=4', 'https://github.com/jinchiim', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pokerogue-helper'), NULL, '121424793', 'unifolio0', 'SANGHUN OH', 'https://avatars.githubusercontent.com/u/121424793?v=4', 'https://github.com/unifolio0', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'review-me'), NULL, '69838872', 'BadaHertz52', 'badahertz52', 'https://avatars.githubusercontent.com/u/69838872?v=4', 'https://github.com/BadaHertz52', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'review-me'), NULL, '31026350', 'donghoony', 'Donghoon Lee', 'https://avatars.githubusercontent.com/u/31026350?v=4', 'https://github.com/donghoony', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'review-me'), NULL, '64690761', 'chysis', 'Fe', 'https://avatars.githubusercontent.com/u/64690761?v=4', 'https://github.com/chysis', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'review-me'), NULL, '111052302', 'ImxYJL', 'Yejin Lee(이예진)', 'https://avatars.githubusercontent.com/u/111052302?v=4', 'https://github.com/ImxYJL', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'review-me'), NULL, '80167893', 'soosoo22', 'sooyeon', 'https://avatars.githubusercontent.com/u/80167893?v=4', 'https://github.com/soosoo22', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'review-me'), NULL, '76177848', 'nayonsoso', 'Yeongseo Na', 'https://avatars.githubusercontent.com/u/76177848?v=4', 'https://github.com/nayonsoso', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'review-me'), NULL, '145949635', 'Kimprodp', NULL, 'https://avatars.githubusercontent.com/u/145949635?v=4', 'https://github.com/Kimprodp', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'review-me'), NULL, '110809927', 'hyeonji1220', 'Hyeonji', 'https://avatars.githubusercontent.com/u/110809927?v=4', 'https://github.com/hyeonji1220', 7);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'staccato'), NULL, '101927543', 'linirini', NULL, 'https://avatars.githubusercontent.com/u/101927543?v=4', 'https://github.com/linirini', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'staccato'), NULL, '92203597', 'Junyoung-WON', 'Hodu', 'https://avatars.githubusercontent.com/u/92203597?v=4', 'https://github.com/Junyoung-WON', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'staccato'), NULL, '103019852', 'hxeyexn', 'Hyeyeon Gong', 'https://avatars.githubusercontent.com/u/103019852?v=4', 'https://github.com/hxeyexn', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'staccato'), NULL, '30232837', 'BurningFalls', 'Seongju Lee', 'https://avatars.githubusercontent.com/u/30232837?v=4', 'https://github.com/BurningFalls', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'staccato'), NULL, '46596035', 's6m1n', 'Somin Lee', 'https://avatars.githubusercontent.com/u/46596035?v=4', 'https://github.com/s6m1n', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'staccato'), NULL, '98626972', 'Ho-Tea', 'YoonJuHo', 'https://avatars.githubusercontent.com/u/98626972?v=4', 'https://github.com/Ho-Tea', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'staccato'), NULL, '146502065', 'kargowild', '와일드카고', 'https://avatars.githubusercontent.com/u/146502065?v=4', 'https://github.com/kargowild', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'touroot'), NULL, '85234650', 'Libienz', '리비', 'https://avatars.githubusercontent.com/u/85234650?v=4', 'https://github.com/Libienz', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'touroot'), NULL, '62099953', 'eunjungL', NULL, 'https://avatars.githubusercontent.com/u/62099953?v=4', 'https://github.com/eunjungL', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'touroot'), NULL, '14046092', 'hangillee', 'Hangil Lee', 'https://avatars.githubusercontent.com/u/14046092?v=4', 'https://github.com/hangillee', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'touroot'), NULL, '95845037', 'nak-honest', '이낙헌', 'https://avatars.githubusercontent.com/u/95845037?v=4', 'https://github.com/nak-honest', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'touroot'), NULL, '87177577', 'jinyoung234', '손진영', 'https://avatars.githubusercontent.com/u/87177577?v=4', 'https://github.com/jinyoung234', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'touroot'), NULL, '99064014', 'slimsha2dy', '최휘용', 'https://avatars.githubusercontent.com/u/99064014?v=4', 'https://github.com/slimsha2dy', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = '3-ddang'), NULL, '49394114', 'ippnsj', 'Sojung Lee', 'https://avatars.githubusercontent.com/u/49394114?v=4', 'https://github.com/ippnsj', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = '3-ddang'), NULL, '63184334', 'JJ503', '임정수', 'https://avatars.githubusercontent.com/u/63184334?v=4', 'https://github.com/JJ503', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = '3-ddang'), NULL, '57691173', 'apptie', 'apptie', 'https://avatars.githubusercontent.com/u/57691173?v=4', 'https://github.com/apptie', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = '3-ddang'), NULL, '67176829', 'rhthrhrl0', 'Mendel', 'https://avatars.githubusercontent.com/u/67176829?v=4', 'https://github.com/rhthrhrl0', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = '3-ddang'), NULL, '96688810', 'kwonyj1022', '권예진', 'https://avatars.githubusercontent.com/u/96688810?v=4', 'https://github.com/kwonyj1022', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = '3-ddang'), NULL, '15646373', 'hyemdooly', 'Song Hyemin', 'https://avatars.githubusercontent.com/u/15646373?v=4', 'https://github.com/hyemdooly', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = '3-ddang'), NULL, '81925468', 'swonny', '최승원', 'https://avatars.githubusercontent.com/u/81925468?v=4', 'https://github.com/swonny', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'baton'), NULL, '39729721', 'shb03323', 'Jeonghoon Park', 'https://avatars.githubusercontent.com/u/39729721?v=4', 'https://github.com/shb03323', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'baton'), NULL, '62369936', 'gyeongza', '박경현', 'https://avatars.githubusercontent.com/u/62369936?v=4', 'https://github.com/gyeongza', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'baton'), NULL, '83010167', 'cookienc', 'Ethan', 'https://avatars.githubusercontent.com/u/83010167?v=4', 'https://github.com/cookienc', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'baton'), NULL, '82203978', 'hyena0608', 'HyunSeo Park (Hyena)', 'https://avatars.githubusercontent.com/u/82203978?v=4', 'https://github.com/hyena0608', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'baton'), NULL, '116625502', 'guridaek', 'KangSan Lee', 'https://avatars.githubusercontent.com/u/116625502?v=4', 'https://github.com/guridaek', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'car-ffeine'), NULL, '69189073', 'gabrielyoon7', 'Ju Hyun, Yoon', 'https://avatars.githubusercontent.com/u/69189073?v=4', 'https://github.com/gabrielyoon7', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'car-ffeine'), NULL, '108778921', 'feb-dain', 'Dain Lee', 'https://avatars.githubusercontent.com/u/108778921?v=4', 'https://github.com/feb-dain', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'car-ffeine'), NULL, '77326660', 'kyw0716', 'Youngwoo Kim', 'https://avatars.githubusercontent.com/u/77326660?v=4', 'https://github.com/kyw0716', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'car-ffeine'), NULL, '106640954', 'drunkenhw', '한우석', 'https://avatars.githubusercontent.com/u/106640954?v=4', 'https://github.com/drunkenhw', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'car-ffeine'), NULL, '63213487', 'sosow0212', 'Jaeyoon Lee', 'https://avatars.githubusercontent.com/u/63213487?v=4', 'https://github.com/sosow0212', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'car-ffeine'), NULL, '101039161', 'kiarakim', 'Kiara Kim', 'https://avatars.githubusercontent.com/u/101039161?v=4', 'https://github.com/kiarakim', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'car-ffeine'), NULL, '80899085', 'be-student', '송은우', 'https://avatars.githubusercontent.com/u/80899085?v=4', 'https://github.com/be-student', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'celuveat'), NULL, '102432453', 'shackstack', 'Jeremy', 'https://avatars.githubusercontent.com/u/102432453?v=4', 'https://github.com/shackstack', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'celuveat'), NULL, '90550065', 'TaeyeonRoyce', 'Taeyeon', 'https://avatars.githubusercontent.com/u/90550065?v=4', 'https://github.com/TaeyeonRoyce', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'celuveat'), NULL, '52229930', 'shin-mallang', 'Donghun Shin', 'https://avatars.githubusercontent.com/u/52229930?v=4', 'https://github.com/shin-mallang', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'celuveat'), NULL, '51052049', 'D0Dam', 'Minjae Kim', 'https://avatars.githubusercontent.com/u/51052049?v=4', 'https://github.com/D0Dam', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'celuveat'), NULL, '84677292', 'odo27', NULL, 'https://avatars.githubusercontent.com/u/84677292?v=4', 'https://github.com/odo27', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'celuveat'), NULL, '66300965', 'kdkdhoho', '김동호', 'https://avatars.githubusercontent.com/u/66300965?v=4', 'https://github.com/kdkdhoho', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'celuveat'), NULL, '78203399', 'turtle601', '황준승', 'https://avatars.githubusercontent.com/u/78203399?v=4', 'https://github.com/turtle601', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'festa-go'), NULL, '116627736', 'seokjin8678', 'Seokjin Jeon', 'https://avatars.githubusercontent.com/u/116627736?v=4', 'https://github.com/seokjin8678', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'festa-go'), NULL, '108349655', 'SeongHoonC', 'Choi SeongHoon', 'https://avatars.githubusercontent.com/u/108349655?v=4', 'https://github.com/SeongHoonC', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'festa-go'), NULL, '103228463', 'BGuga', 'Guga', 'https://avatars.githubusercontent.com/u/103228463?v=4', 'https://github.com/BGuga', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'festa-go'), NULL, '37167652', 're4rk', '아크', 'https://avatars.githubusercontent.com/u/37167652?v=4', 'https://github.com/re4rk', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'festa-go'), NULL, '67777523', 'EmilyCh0', '해시', 'https://avatars.githubusercontent.com/u/67777523?v=4', 'https://github.com/EmilyCh0', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'festa-go'), NULL, '71129059', 'xxeol2', NULL, 'https://avatars.githubusercontent.com/u/71129059?v=4', 'https://github.com/xxeol2', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'festa-go'), NULL, '100915276', 'carsago', 'carsago', 'https://avatars.githubusercontent.com/u/100915276?v=4', 'https://github.com/carsago', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'fun-eat'), NULL, '78616893', 'Leejin-Yang', 'Leejin Yang', 'https://avatars.githubusercontent.com/u/78616893?v=4', 'https://github.com/Leejin-Yang', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'fun-eat'), NULL, '80464961', 'hae-on', 'sᴏʟʙɪ ☔️', 'https://avatars.githubusercontent.com/u/80464961?v=4', 'https://github.com/hae-on', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'fun-eat'), NULL, '55427367', 'xodms0309', 'Taeeun Kim', 'https://avatars.githubusercontent.com/u/55427367?v=4', 'https://github.com/xodms0309', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'fun-eat'), NULL, '79046106', '70825', 'Dabeen Jeong', 'https://avatars.githubusercontent.com/u/79046106?v=4', 'https://github.com/70825', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'fun-eat'), NULL, '91244090', 'wugawuga', '우가', 'https://avatars.githubusercontent.com/u/91244090?v=4', 'https://github.com/wugawuga', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'fun-eat'), NULL, '33208246', 'Go-Jaecheol', 'JFe', 'https://avatars.githubusercontent.com/u/33208246?v=4', 'https://github.com/Go-Jaecheol', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'fun-eat'), NULL, '91522259', 'hanueleee', 'Hanuel Lee', 'https://avatars.githubusercontent.com/u/91522259?v=4', 'https://github.com/hanueleee', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'map-befine'), NULL, '89172499', 'semnil5202', '이세민', 'https://avatars.githubusercontent.com/u/89172499?v=4', 'https://github.com/semnil5202', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'map-befine'), NULL, '97426362', 'yoondgu', 'Doyoung Yoo', 'https://avatars.githubusercontent.com/u/97426362?v=4', 'https://github.com/yoondgu', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'map-befine'), NULL, '89840550', 'kpeel5839', NULL, 'https://avatars.githubusercontent.com/u/89840550?v=4', 'https://github.com/kpeel5839', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'map-befine'), NULL, '72205402', 'GC-Park', 'ParkGeunCheol', 'https://avatars.githubusercontent.com/u/72205402?v=4', 'https://github.com/GC-Park', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'map-befine'), NULL, '33995840', 'jiwonh423', 'JIWON', 'https://avatars.githubusercontent.com/u/33995840?v=4', 'https://github.com/jiwonh423', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'map-befine'), NULL, '112045553', 'junpakPark', '박준현', 'https://avatars.githubusercontent.com/u/112045553?v=4', 'https://github.com/junpakPark', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'map-befine'), NULL, '50602742', 'cpot5620', 'zun', 'https://avatars.githubusercontent.com/u/50602742?v=4', 'https://github.com/cpot5620', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'team-by-team'), NULL, '79538610', 'hafnium1923', 'Rulu', 'https://avatars.githubusercontent.com/u/79538610?v=4', 'https://github.com/hafnium1923', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'team-by-team'), NULL, '30036534', 'pilyang', 'Jae_Philip_Yang', 'https://avatars.githubusercontent.com/u/30036534?v=4', 'https://github.com/pilyang', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'team-by-team'), NULL, '19235163', 'suyoungj', 'Suyoung', 'https://avatars.githubusercontent.com/u/19235163?v=4', 'https://github.com/suyoungj', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'team-by-team'), NULL, '87642422', 'wzrabbit', '요술토끼', 'https://avatars.githubusercontent.com/u/87642422?v=4', 'https://github.com/wzrabbit', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'team-by-team'), NULL, '95729738', 'sh111-coder', 'Seonghun Kim', 'https://avatars.githubusercontent.com/u/95729738?v=4', 'https://github.com/sh111-coder', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'team-by-team'), NULL, '86831441', 'SproutMJ', 'KIM MINJUN', 'https://avatars.githubusercontent.com/u/86831441?v=4', 'https://github.com/SproutMJ', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'team-by-team'), NULL, '96895686', 'the9kim', 'DEOKWOO KIM ', 'https://avatars.githubusercontent.com/u/96895686?v=4', 'https://github.com/the9kim', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'trip-draw'), NULL, '54737136', '2chang5', '이창환', 'https://avatars.githubusercontent.com/u/54737136?v=4', 'https://github.com/2chang5', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'trip-draw'), NULL, '58586537', 'greeng00se', 'Herb', 'https://avatars.githubusercontent.com/u/58586537?v=4', 'https://github.com/greeng00se', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'trip-draw'), NULL, '106813090', 'Combi153', 'Chanmin Ju(Hu chu)', 'https://avatars.githubusercontent.com/u/106813090?v=4', 'https://github.com/Combi153', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'trip-draw'), NULL, '89302528', 'Jaeyoung22', '오영택', 'https://avatars.githubusercontent.com/u/89302528?v=4', 'https://github.com/Jaeyoung22', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'trip-draw'), NULL, '97939198', 'beer-2000', 'Woojin', 'https://avatars.githubusercontent.com/u/97939198?v=4', 'https://github.com/beer-2000', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'trip-draw'), NULL, '69189793', 'otter66', '수달(김수연)', 'https://avatars.githubusercontent.com/u/69189793?v=4', 'https://github.com/otter66', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'trip-draw'), NULL, '69796976', 'pingu244', NULL, 'https://avatars.githubusercontent.com/u/69796976?v=4', 'https://github.com/pingu244', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'yozm-cafe'), NULL, '20203944', 'solo5star', NULL, 'https://avatars.githubusercontent.com/u/20203944?v=4', 'https://github.com/solo5star', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'yozm-cafe'), NULL, '122500517', 'jeongwusi', '정우시', 'https://avatars.githubusercontent.com/u/122500517?v=4', 'https://github.com/jeongwusi', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'yozm-cafe'), NULL, '96301958', 'green-kong', 'dev_kong', 'https://avatars.githubusercontent.com/u/96301958?v=4', 'https://github.com/green-kong', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'yozm-cafe'), NULL, '93072571', 'nuyh99', '황재현', 'https://avatars.githubusercontent.com/u/93072571?v=4', 'https://github.com/nuyh99', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'yozm-cafe'), NULL, '86547109', 'donghae-kim', NULL, 'https://avatars.githubusercontent.com/u/86547109?v=4', 'https://github.com/donghae-kim', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'yozm-cafe'), NULL, '96762301', 'hum02', '김동흠', 'https://avatars.githubusercontent.com/u/96762301?v=4', 'https://github.com/hum02', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'yozm-cafe'), NULL, '95906910', 'geuntaek1013', 'Geun Taek Ahn', 'https://avatars.githubusercontent.com/u/95906910?v=4', 'https://github.com/geuntaek1013', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'dallog'), NULL, '32920566', 'jhy979', '장호영 (JANG HO YEONG)', 'https://avatars.githubusercontent.com/u/32920566?v=4', 'https://github.com/jhy979', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'dallog'), NULL, '11745691', 'devHudi', 'Donghyun Cho', 'https://avatars.githubusercontent.com/u/11745691?v=4', 'https://github.com/devHudi', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'dallog'), NULL, '52729559', 'dayelop', '이다예', 'https://avatars.githubusercontent.com/u/52729559?v=4', 'https://github.com/dayelop', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'dallog'), NULL, '59357153', 'hyeonic', 'hyeoni.c', 'https://avatars.githubusercontent.com/u/59357153?v=4', 'https://github.com/hyeonic', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'dallog'), NULL, '71062817', 'gudonghee2000', '구동희', 'https://avatars.githubusercontent.com/u/71062817?v=4', 'https://github.com/gudonghee2000', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'dallog'), NULL, '77425729', 'summerlunaa', '이하은', 'https://avatars.githubusercontent.com/u/77425729?v=4', 'https://github.com/summerlunaa', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'levellog'), NULL, '79692272', 'yunjin-kim', '김윤진', 'https://avatars.githubusercontent.com/u/79692272?v=4', 'https://github.com/yunjin-kim', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'levellog'), NULL, '68512686', 'nailseong', 'Na Ilseong', 'https://avatars.githubusercontent.com/u/68512686?v=4', 'https://github.com/nailseong', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'levellog'), NULL, '75592315', 'frontend-jihyeok-um', '엄지혁 (Jihyeok Um)', 'https://avatars.githubusercontent.com/u/75592315?v=4', 'https://github.com/frontend-jihyeok-um', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'levellog'), NULL, '32123302', 'OzRagwort', '장원영', 'https://avatars.githubusercontent.com/u/32123302?v=4', 'https://github.com/OzRagwort', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'levellog'), NULL, '52696169', 'kbsat', 'Roma', 'https://avatars.githubusercontent.com/u/52696169?v=4', 'https://github.com/kbsat', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'levellog'), NULL, '28749734', 'SuyeonChoi', 'SuyeonChoi', 'https://avatars.githubusercontent.com/u/28749734?v=4', 'https://github.com/SuyeonChoi', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'levellog'), NULL, '76840965', '2yujeong', NULL, 'https://avatars.githubusercontent.com/u/76840965?v=4', 'https://github.com/2yujeong', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'levellog'), NULL, '254128215', 'wyRagwort', NULL, 'https://avatars.githubusercontent.com/u/254128215?v=4', 'https://github.com/wyRagwort', 7);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'momo'), NULL, '57928612', 'LAH1203', 'AhhyunLee', 'https://avatars.githubusercontent.com/u/57928612?v=4', 'https://github.com/LAH1203', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'momo'), NULL, '22176552', 'sinb57', 'Song In Bong', 'https://avatars.githubusercontent.com/u/22176552?v=4', 'https://github.com/sinb57', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'momo'), NULL, '28296575', 'usageness', 'Yongrae Kim', 'https://avatars.githubusercontent.com/u/28296575?v=4', 'https://github.com/usageness', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'momo'), NULL, '57744251', 'Seongwon97', 'SeongWon Oh', 'https://avatars.githubusercontent.com/u/57744251?v=4', 'https://github.com/Seongwon97', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'momo'), NULL, '76891875', 'nbalance97', 'bhlee', 'https://avatars.githubusercontent.com/u/76891875?v=4', 'https://github.com/nbalance97', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'momo'), NULL, '92148749', 'kyukong', 'kyukong', 'https://avatars.githubusercontent.com/u/92148749?v=4', 'https://github.com/kyukong', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moragora'), NULL, '44823900', 'greenblues1190', 'Jeongmin Woo', 'https://avatars.githubusercontent.com/u/44823900?v=4', 'https://github.com/greenblues1190', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moragora'), NULL, '61308364', 'kamwoo', 'kamwoo', 'https://avatars.githubusercontent.com/u/61308364?v=4', 'https://github.com/kamwoo', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moragora'), NULL, '89305335', 'YJGwon', 'Yejin Gwon(Forky)', 'https://avatars.githubusercontent.com/u/89305335?v=4', 'https://github.com/YJGwon', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moragora'), NULL, '67397679', 'syoun602', 'sun', 'https://avatars.githubusercontent.com/u/67397679?v=4', 'https://github.com/syoun602', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moragora'), NULL, '67885363', 'Hongdonggeon', 'Hongdonggeon', 'https://avatars.githubusercontent.com/u/67885363?v=4', 'https://github.com/Hongdonggeon', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moragora'), NULL, '70707629', 'shindong96', '신동석', 'https://avatars.githubusercontent.com/u/70707629?v=4', 'https://github.com/shindong96', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moragora'), NULL, '66164361', 'progress0407', 'Philo', 'https://avatars.githubusercontent.com/u/66164361?v=4', 'https://github.com/progress0407', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pickpick'), NULL, '61469664', 'moonheekim0118', NULL, 'https://avatars.githubusercontent.com/u/61469664?v=4', 'https://github.com/moonheekim0118', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pickpick'), NULL, '68001045', 'kkojae91', 'Jaejeung Ko', 'https://avatars.githubusercontent.com/u/68001045?v=4', 'https://github.com/kkojae91', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pickpick'), NULL, '55357130', 'JangBomi', NULL, 'https://avatars.githubusercontent.com/u/55357130?v=4', 'https://github.com/JangBomi', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pickpick'), NULL, '80666066', 'hyewoncc', '써머(최혜원)', 'https://avatars.githubusercontent.com/u/80666066?v=4', 'https://github.com/hyewoncc', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pickpick'), NULL, '53105735', 'yeon-06', 'yeonLog', 'https://avatars.githubusercontent.com/u/53105735?v=4', 'https://github.com/yeon-06', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pickpick'), NULL, '62681566', 'HJ-Rich', NULL, 'https://avatars.githubusercontent.com/u/62681566?v=4', 'https://github.com/HJ-Rich', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'teatime'), NULL, '48676844', 'InKyoJeong', 'Koy', 'https://avatars.githubusercontent.com/u/48676844?v=4', 'https://github.com/InKyoJeong', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'teatime'), NULL, '82227098', 'jin7969', 'Ahn', 'https://avatars.githubusercontent.com/u/82227098?v=4', 'https://github.com/jin7969', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'teatime'), NULL, '52141636', 'yeongunheo', 'Yeongun Heo', 'https://avatars.githubusercontent.com/u/52141636?v=4', 'https://github.com/yeongunheo', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'teatime'), NULL, '69156709', 'yaho99', 'YAHO', 'https://avatars.githubusercontent.com/u/69156709?v=4', 'https://github.com/yaho99', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'teatime'), NULL, '63579113', 'Yboyu0u', 'Youngwoo Yoo', 'https://avatars.githubusercontent.com/u/63579113?v=4', 'https://github.com/Yboyu0u', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'teatime'), NULL, '60432062', 'chawani', 'Maru', 'https://avatars.githubusercontent.com/u/60432062?v=4', 'https://github.com/chawani', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ternoko'), NULL, '19251499', 'lokba', 'Kimsanglok', 'https://avatars.githubusercontent.com/u/19251499?v=4', 'https://github.com/lokba', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ternoko'), NULL, '54317630', 'dongho108', 'ash', 'https://avatars.githubusercontent.com/u/54317630?v=4', 'https://github.com/dongho108', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ternoko'), NULL, '26570275', 'her0807', 'her0807', 'https://avatars.githubusercontent.com/u/26570275?v=4', 'https://github.com/her0807', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ternoko'), NULL, '43205258', 'Juhyung990122', 'Yeoleum', 'https://avatars.githubusercontent.com/u/43205258?v=4', 'https://github.com/Juhyung990122', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ternoko'), NULL, '36189291', 'HyeonbinSa', 'bin_sa', 'https://avatars.githubusercontent.com/u/36189291?v=4', 'https://github.com/HyeonbinSa', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ternoko'), NULL, '38878617', 'sanaandmomo', 'soobin', 'https://avatars.githubusercontent.com/u/38878617?v=4', 'https://github.com/sanaandmomo', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'ternoko'), NULL, '83059234', 'soominsohn', NULL, 'https://avatars.githubusercontent.com/u/83059234?v=4', 'https://github.com/soominsohn', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'cvi'), NULL, '43339385', 'bucketHaneul', 'Haneul Kim', 'https://avatars.githubusercontent.com/u/43339385?v=4', 'https://github.com/bucketHaneul', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'cvi'), NULL, '53412998', 'taehee-kim-dev', 'taehee-kim-dev', 'https://avatars.githubusercontent.com/u/53412998?v=4', 'https://github.com/taehee-kim-dev', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'cvi'), NULL, '48986787', 'Livenow14', 'livenow14', 'https://avatars.githubusercontent.com/u/48986787?v=4', 'https://github.com/Livenow14', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'cvi'), NULL, '40762111', 'jum0', 'JUNMO HAN', 'https://avatars.githubusercontent.com/u/40762111?v=4', 'https://github.com/jum0', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'cvi'), NULL, '67272922', 'thisisyoungbin', 'Youngbin Kim', 'https://avatars.githubusercontent.com/u/67272922?v=4', 'https://github.com/thisisyoungbin', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'nolto'), NULL, '48755175', '0307kwon', 'Sejin Kwon (Matthew)', 'https://avatars.githubusercontent.com/u/48755175?v=4', 'https://github.com/0307kwon', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'nolto'), NULL, '44080404', 'zigsong', 'jieun song', 'https://avatars.githubusercontent.com/u/44080404?v=4', 'https://github.com/zigsong', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'nolto'), NULL, '34594339', 'bosl95', 'pom', 'https://avatars.githubusercontent.com/u/34594339?v=4', 'https://github.com/bosl95', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'nolto'), NULL, '43840561', 'NewWisdom', 'NewWisdom', 'https://avatars.githubusercontent.com/u/43840561?v=4', 'https://github.com/NewWisdom', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'nolto'), NULL, '57378410', 'Gomding', '박민영', 'https://avatars.githubusercontent.com/u/57378410?v=4', 'https://github.com/Gomding', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'nolto'), NULL, '61370901', 'joelonsw', 'Yeongsang Jo', 'https://avatars.githubusercontent.com/u/61370901?v=4', 'https://github.com/joelonsw', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pick-git'), NULL, '57767891', 'Tanney-102', 'Tigger', 'https://avatars.githubusercontent.com/u/57767891?v=4', 'https://github.com/Tanney-102', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pick-git'), NULL, '32982670', 'swon3210', 'SONG WON LEE', 'https://avatars.githubusercontent.com/u/32982670?v=4', 'https://github.com/swon3210', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pick-git'), NULL, '56240505', 'xlffm3', 'Jinhong', 'https://avatars.githubusercontent.com/u/56240505?v=4', 'https://github.com/xlffm3', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pick-git'), NULL, '33603557', 'bperhaps', 'Minsung Son', 'https://avatars.githubusercontent.com/u/33603557?v=4', 'https://github.com/bperhaps', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pick-git'), NULL, '56860124', 'binghe819', 'BYEONGHWA KIM', 'https://avatars.githubusercontent.com/u/56860124?v=4', 'https://github.com/binghe819', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pick-git'), NULL, '63405904', 'yjksw', 'Yunjung Kim', 'https://avatars.githubusercontent.com/u/63405904?v=4', 'https://github.com/yjksw', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'see-you-there'), NULL, '47732237', 'hybeom0720', 'YeongBeom Heo', 'https://avatars.githubusercontent.com/u/47732237?v=4', 'https://github.com/hybeom0720', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'see-you-there'), NULL, '60066472', '365kim', 'Kim Haru', 'https://avatars.githubusercontent.com/u/60066472?v=4', 'https://github.com/365kim', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'see-you-there'), NULL, '45873044', 'daum7766', 'mungto', 'https://avatars.githubusercontent.com/u/45873044?v=4', 'https://github.com/daum7766', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'see-you-there'), NULL, '63634505', 'choijy1705', 'Junyoung Choi (jun)', 'https://avatars.githubusercontent.com/u/63634505?v=4', 'https://github.com/choijy1705', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'see-you-there'), NULL, '75007375', '0imbean0', '임선빈', 'https://avatars.githubusercontent.com/u/75007375?v=4', 'https://github.com/0imbean0', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'see-you-there'), NULL, '48675973', 'bimppap', 'Choonsik (안예장)', 'https://avatars.githubusercontent.com/u/48675973?v=4', 'https://github.com/bimppap', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'seller-lee-company'), NULL, '52931057', 'kouz95', 'Gyeongjun Kim', 'https://avatars.githubusercontent.com/u/52931057?v=4', 'https://github.com/kouz95', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'seller-lee-company'), NULL, '31095063', 'jnsorn', 'Sorin Jin', 'https://avatars.githubusercontent.com/u/31095063?v=4', 'https://github.com/jnsorn', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'seller-lee-company'), NULL, '196640556', 'leewnsdud', 'Junyoung Lee', 'https://avatars.githubusercontent.com/u/196640556?v=4', 'https://github.com/leewnsdud', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'seller-lee-company'), NULL, '39271364', 'begaonnuri', '남윤서', 'https://avatars.githubusercontent.com/u/39271364?v=4', 'https://github.com/begaonnuri', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'seller-lee-company'), NULL, '53935703', 'joseph415', 'EunSeok', 'https://avatars.githubusercontent.com/u/53935703?v=4', 'https://github.com/joseph415', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'zeze'), NULL, '26547475', 'woonjangahn', 'Woonjang Ahn', 'https://avatars.githubusercontent.com/u/26547475?v=4', 'https://github.com/woonjangahn', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'hang-log'), NULL, '51967731', 'ashleysyheo', 'Ashley Heo', 'https://avatars.githubusercontent.com/u/51967731?v=4', 'https://github.com/ashleysyheo', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'hang-log'), NULL, '77482065', 'jjongwa', '신종화', 'https://avatars.githubusercontent.com/u/77482065?v=4', 'https://github.com/jjongwa', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'hang-log'), NULL, '91263263', 'waterricecake', 'waterricecake', 'https://avatars.githubusercontent.com/u/91263263?v=4', 'https://github.com/waterricecake', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'hang-log'), NULL, '102305630', 'Dahyeeee', 'Dahye Yun', 'https://avatars.githubusercontent.com/u/102305630?v=4', 'https://github.com/Dahyeeee', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'hang-log'), NULL, '65850682', 'hgo641', 'hongo', 'https://avatars.githubusercontent.com/u/65850682?v=4', 'https://github.com/hgo641', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'hang-log'), NULL, '64852591', 'mcodnjs', 'Chaewon Moon', 'https://avatars.githubusercontent.com/u/64852591?v=4', 'https://github.com/mcodnjs', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'hang-log'), NULL, '45068522', 'dladncks1217', '임우찬', 'https://avatars.githubusercontent.com/u/45068522?v=4', 'https://github.com/dladncks1217', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'hang-log'), NULL, '49433615', 'LJW25', '이지우', 'https://avatars.githubusercontent.com/u/49433615?v=4', 'https://github.com/LJW25', 7);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'naaga'), NULL, '84285337', 'krrong', NULL, 'https://avatars.githubusercontent.com/u/84285337?v=4', 'https://github.com/krrong', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'naaga'), NULL, '79090478', 'dooboocookie', 'dooboocookie', 'https://avatars.githubusercontent.com/u/79090478?v=4', 'https://github.com/dooboocookie', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'naaga'), NULL, '78788847', 'briandr97', 'Kwon Yongmin', 'https://avatars.githubusercontent.com/u/78788847?v=4', 'https://github.com/briandr97', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'naaga'), NULL, '45879491', 'kokodak', NULL, 'https://avatars.githubusercontent.com/u/45879491?v=4', 'https://github.com/kokodak', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'mo-rak'), NULL, '64825713', 'al-bur', NULL, 'https://avatars.githubusercontent.com/u/64825713?v=4', 'https://github.com/al-bur', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'mo-rak'), NULL, '52344833', 'rladpwl0512', 'rladpwl0512', 'https://avatars.githubusercontent.com/u/52344833?v=4', 'https://github.com/rladpwl0512', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'mo-rak'), NULL, '52564093', 'cjlee38', 'chalee', 'https://avatars.githubusercontent.com/u/52564093?v=4', 'https://github.com/cjlee38', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'mo-rak'), NULL, '42317507', 'leo0842', 'Sungsan Kim', 'https://avatars.githubusercontent.com/u/42317507?v=4', 'https://github.com/leo0842', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'mo-rak'), NULL, '45311765', 'RIANAEH', 'ellie', 'https://avatars.githubusercontent.com/u/45311765?v=4', 'https://github.com/RIANAEH', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'mo-rak'), NULL, '79205414', 'seong-wooo', '박성우', 'https://avatars.githubusercontent.com/u/79205414?v=4', 'https://github.com/seong-wooo', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pium'), NULL, '49794401', 'Choi-JJunho', '최준호', 'https://avatars.githubusercontent.com/u/49794401?v=4', 'https://github.com/Choi-JJunho', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pium'), NULL, '50974359', 'hozzijeong', '정호진', 'https://avatars.githubusercontent.com/u/50974359?v=4', 'https://github.com/hozzijeong', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pium'), NULL, '77872742', 'WaiNaat', 'Q Kim', 'https://avatars.githubusercontent.com/u/77872742?v=4', 'https://github.com/WaiNaat', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pium'), NULL, '68818952', 'Kim0914', 'Kim0914', 'https://avatars.githubusercontent.com/u/68818952?v=4', 'https://github.com/Kim0914', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pium'), NULL, '54442420', 'bassyu', '유강현', 'https://avatars.githubusercontent.com/u/54442420?v=4', 'https://github.com/bassyu', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pium'), NULL, '88660886', 'yeonkkk', 'Seongyeon Kim', 'https://avatars.githubusercontent.com/u/88660886?v=4', 'https://github.com/yeonkkk', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'pium'), NULL, '79038908', 'rawfishthelgh', 'Geonhoe Lee', 'https://avatars.githubusercontent.com/u/79038908?v=4', 'https://github.com/rawfishthelgh', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moitz'), NULL, '74090200', 'eunsoA', 'Eunso Ahn', 'https://avatars.githubusercontent.com/u/74090200?v=4', 'https://github.com/eunsoA', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moitz'), NULL, '75800958', 'kaori-killer', 'Sojeong Yoo', 'https://avatars.githubusercontent.com/u/75800958?v=4', 'https://github.com/kaori-killer', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moitz'), NULL, '151512150', 'lifeishiphop', '갱민', 'https://avatars.githubusercontent.com/u/151512150?v=4', 'https://github.com/lifeishiphop', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moitz'), NULL, '148451132', 'saera-yook', 'Saera Yook', 'https://avatars.githubusercontent.com/u/148451132?v=4', 'https://github.com/saera-yook', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moitz'), NULL, '128875051', 'jbilee', 'Julie Hahn', 'https://avatars.githubusercontent.com/u/128875051?v=4', 'https://github.com/jbilee', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moitz'), NULL, '106324609', 'dbsdndcks', 'Lemon', 'https://avatars.githubusercontent.com/u/106324609?v=4', 'https://github.com/dbsdndcks', 5);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'moitz'), NULL, '112458620', 'youdame', '조유담', 'https://avatars.githubusercontent.com/u/112458620?v=4', 'https://github.com/youdame', 6);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'zzol'), NULL, '72564777', 'kiwoook', 'kiwook lee', 'https://avatars.githubusercontent.com/u/72564777?v=4', 'https://github.com/kiwoook', 0);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'zzol'), NULL, '26942349', 'theminjunchoi', 'MinJun Choi', 'https://avatars.githubusercontent.com/u/26942349?v=4', 'https://github.com/theminjunchoi', 1);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'zzol'), NULL, '94986147', 'junhaa', 'junhaa', 'https://avatars.githubusercontent.com/u/94986147?v=4', 'https://github.com/junhaa', 2);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'zzol'), NULL, '127578418', '20HyeonsuLee', 'Hyeonsu Lee', 'https://avatars.githubusercontent.com/u/127578418?v=4', 'https://github.com/20HyeonsuLee', 3);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'zzol'), NULL, '81847', 'claude', 'Claude', 'https://avatars.githubusercontent.com/u/81847?v=4', 'https://github.com/claude', 4);
INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login, display_name, avatar_url, github_profile_url, display_order) VALUES ((SELECT id FROM projects WHERE slug = 'zzol'), NULL, '87463004', 'sooyeoniya', '최수연 (SooYeon Choi)', 'https://avatars.githubusercontent.com/u/87463004?v=4', 'https://github.com/sooyeoniya', 5);

COMMIT;
