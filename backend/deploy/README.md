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

환경 변수 형식은 `shout-outz.env.example`을 참고한다. 최초 배포의 `BeforeInstall` 단계가 서비스 사용자를 만든 뒤 파일 소유권을 `root:shoutoutz`, 권한을 `0640`으로 변경한다.

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
