# 백엔드 DEV 배포

`develop` 브랜치의 백엔드를 CodePipeline, CodeBuild, CodeDeploy를 통해 단일 EC2에 배포한다.

## 배포 흐름

```text
GitHub develop
→ CodePipeline Source
→ CodeBuild 테스트 및 bootJar
→ CodeDeploy
→ EC2 CodeDeploy Agent
→ systemd 재시작
→ Actuator 헬스 체크
```

CodeBuild가 생성하는 BuildArtifact는 다음 파일을 포함한다.

```text
app.jar
appspec.yml
deploy/
├── scripts/
│   ├── before_install.sh
│   ├── after_install.sh
│   ├── stop.sh
│   ├── start.sh
│   └── validate.sh
└── shout-outz-backend.service
```

## 요청 경로와 HTTPS

배포 흐름과 별개로, 외부 요청은 nginx를 거쳐 애플리케이션에 도달한다.

```text
브라우저
→ https://{API 도메인} (nginx 443, TLS 종료)
→ http://127.0.0.1:8080 (Spring Boot)
```

인증서는 Let's Encrypt에서 발급받아 nginx가 보유하고 certbot이 90일 주기로 갱신한다. 애플리케이션은 TLS를 다루지 않는다.

애플리케이션은 `application-prod.yml`의 `server.address`로 루프백에만 바인딩한다. 보안 그룹 `project-public`을 여러 팀이 공유해 8080 인바운드 규칙을 수정할 수 없으므로 외부 접근 차단을 이 바인딩으로 처리한 것이다. 따라서 nginx는 선택이 아니라 필수 구성 요소이며, nginx가 없으면 애플리케이션이 정상 기동해도 외부에서 접근할 수 없다.

인증서를 애플리케이션이 아니라 nginx가 들고 있으므로 인증서 수명은 배포 주기와 분리된다. 갱신에 애플리케이션 재시작이 필요하지 않고 배포가 TLS 연결을 끊지 않는다. 같은 이유로 TLS 종료 지점을 이후 ALB로 옮기더라도 애플리케이션 설정은 그대로 쓸 수 있다.

CodeDeploy가 애플리케이션을 재시작하는 동안 nginx는 502를 반환한다. 이전처럼 연결이 거부되지는 않으므로, 배포 직후의 502는 장애가 아니라 기동 대기로 읽는다.

nginx 설정은 `nginx/shout-outz-backend.conf`에 서버 파일의 사본으로 둔다. 자동으로 반영되지 않으므로 서버에서 설정을 변경하면 저장소 사본도 함께 갱신한다. 443 블록과 리다이렉트의 `# managed by Certbot` 주석 줄은 갱신 때 certbot이 다시 손대므로 직접 수정하지 않는다.

## EC2 사전 준비

배포 전에 EC2에 Java 21과 CodeDeploy Agent 2.0 이상을 설치한다.
현재 DEV 인스턴스는 Ubuntu 26.04 ARM64이므로 `latestv2` 설치기를 사용해야 한다.

```bash
sudo apt update
sudo apt install -y openjdk-21-jre-headless wget curl

cd /home/ubuntu
wget https://aws-codedeploy-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/latestv2/install
chmod +x install
sudo ./install auto
sudo systemctl enable --now codedeploy-agent
sudo systemctl status codedeploy-agent --no-pager
sudo /opt/codedeploy-agent/bin/codedeploy-agent --version
```

운영 비밀값은 Pipeline이나 BuildArtifact에 포함하지 않고 EC2에만 저장한다.

```bash
sudo install -d -o root -g root -m 0750 /opt/shout-outz
sudo editor /opt/shout-outz/shout-outz.env
sudo chown root:root /opt/shout-outz/shout-outz.env
sudo chmod 0600 /opt/shout-outz/shout-outz.env
```

필수 환경 변수 목록과 형식은 팀 내부 인프라 문서에서 관리한다. 실제 값은 저장소에 기록하지 않는다. 최초 배포의 `BeforeInstall` 단계는 필수 값이 모두 설정되었는지 확인하고, 서비스 사용자를 만든 뒤 파일 소유권을 `root:shoutoutz`, 권한을 `0640`으로 변경한다.

애플리케이션이 기동하려면 접근 가능한 PostgreSQL이 먼저 있어야 한다. DB가 EC2 내부에 있는지 RDS에 있는지와 관계없이 EC2에서 해당 주소와 5432 포트에 접근할 수 있어야 한다.

## AWS 구성

### CodeBuild

- Project name: `shout-outz-backend-dev-build`
- Source: CodePipeline
- Environment: AWS 관리형 Ubuntu 이미지
- Runtime: Java 21을 지원하는 이미지
- Privileged mode: 활성화
  - PostgreSQL 17 테스트 컨테이너 실행에 필요하다.
- Buildspec: 저장소 루트의 `buildspec.yml`
- Artifact: CodePipeline

### CodeDeploy

- Application name: `shout-outz-backend-dev`
- Deployment group name: `shout-outz-backend-dev-group`
- Compute platform: EC2/On-Premises
- Deployment type: In-place
- Service role: 기존에 제공된 CodeDeploy 서비스 역할
- 대상 선택: DEV EC2의 `Name = [BE-DEV] shout-outz` 태그
- Deployment configuration: `CodeDeployDefault.OneAtATime`
- 자동 롤백: 배포 실패 시 활성화
- Load balancer: 현재 트래픽 구조가 확정되기 전까지 비활성화

### CodePipeline

- Pipeline name: `shout-outz-backend-dev`
- Pipeline type: V2
- Source provider: GitHub via CodeConnections
- Repository: `woowacourse-teams/2026-shout-outz`
- Branch: `develop`
- Output artifact: `SourceArtifact`
- Build provider: AWS CodeBuild의 `shout-outz-backend-dev-build`
- Build output artifact: `BuildArtifact`
- Deploy provider: AWS CodeDeploy의 `shout-outz-backend-dev` / `shout-outz-backend-dev-group`

CodePipeline, CodeBuild, CodeDeploy, EC2는 서로 다른 역할을 사용한다. 새 역할을 만들거나 정책을 수정하지 말고, 같은 AWS 환경에서 이미 동작한 팀과 동일한 용도의 기존 역할을 선택한다.

초기에는 모든 `develop` 변경에 배포하고, 첫 배포가 안정화된 후 다음 경로로 Source Trigger 필터를 적용한다.

```text
backend/**
buildspec.yml
appspec.yml
```

## 배포 검증과 로그

CodeDeploy의 `ValidateService` 단계는 최대 180초 동안 다음 엔드포인트가 `UP`인지 확인한다.

```text
http://127.0.0.1:8080/actuator/health
```

EC2에서 애플리케이션 로그를 확인한다.

```bash
sudo systemctl status shout-outz-backend --no-pager
sudo journalctl -u shout-outz-backend -n 200 --no-pager
```

CodeDeploy Agent 로그를 확인한다.

```bash
sudo systemctl status codedeploy-agent --no-pager
sudo journalctl -u codedeploy-agent -n 200 --no-pager
```
