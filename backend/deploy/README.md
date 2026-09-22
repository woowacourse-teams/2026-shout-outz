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

CodeDeploy가 애플리케이션을 재시작하는 동안 nginx는 502를 반환한다. 애플리케이션을 내리는 시점부터 `ValidateService`가 헬스 체크를 통과할 때까지 약 50초가 걸리며, 이 구간의 502는 장애가 아니라 기동 대기로 읽는다. 파이프라인이 성공으로 바뀐 뒤에도 502가 계속되면 배포가 아닌 다른 원인이므로 아래 로그를 확인한다.

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

### 메모리 확보

DEV 인스턴스는 메모리가 1GB(리눅스가 실제로 사용하는 양은 903MB)이고, OS와 CodeDeploy Agent 등이 그중 절반 가까이를 쓴다. 애플리케이션 몫으로 남는 것은 400MB 남짓이므로 아래를 배포 전에 적용한다. 적용하지 않으면 메모리가 꽉 찼을 때 커널 OOM killer가 메모리를 가장 많이 쓰는 애플리케이션을 강제 종료한다.

먼저 swap을 구성해 메모리가 부족할 때 프로세스가 즉시 종료되지 않도록 한다. swap은 해결책이 아니라 안전망이며, 실제 상한은 `shout-outz-backend.service`의 `-Xmx`로 정한다.

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
echo 'vm.swappiness=10' | sudo tee /etc/sysctl.d/99-swappiness.conf
sudo sysctl -p /etc/sysctl.d/99-swappiness.conf
```

`vm.swappiness`를 낮추는 것은 여유가 있는 동안 swap을 쓰지 않게 하기 위함이다. 기본값 60은 JVM 페이지를 미리 디스크로 내보내 GC 지연을 만든다.

다음으로 우분투가 기본으로 켜지만 이 서버에서 할 일이 없는 서비스를 끈다. `fwupd`는 하드웨어 펌웨어 업데이트를, `udisks2`는 이동식 디스크 관리를 담당하며 가상 머신에는 대상이 없다. 둘이 합쳐 약 76MB를 사용한다.

```bash
sudo systemctl disable --now fwupd.service udisks2.service
sudo chmod -x /etc/update-motd.d/50-landscape-sysinfo
```

`50-landscape-sysinfo`는 SSH 로그인 시 시스템 요약을 출력하는 스크립트다. 메모리가 빠듯한 상태에서 이 스크립트의 할당 요청이 OOM killer를 발동시킨 사례가 있어 함께 비활성화한다. 반면 `unattended-upgrades`도 메모리를 쓰지만 보안 패치를 자동으로 적용하므로 끄지 않는다.

`fwupd`는 D-Bus로 다시 기동될 수 있다. 이후 `ps aux | grep fwupd`에 다시 보이면 `sudo systemctl mask fwupd.service`로 막는다.

애플리케이션의 힙 상한(`shout-outz-backend.service`의 `-Xmx`)은 위 절차를 적용한 상태를 전제로 계산한 값이다. 현재 `-Xmx320m`은 힙 320MB에 힙 외 영역 약 170MB, OS와 에이전트 약 360MB를 더해 903MB 안에 들어가도록 잡았다. 메모리 확보 절차를 건너뛰면 OS 몫이 460MB로 늘어 이 값으로도 물리 메모리를 초과하므로, 두 가지는 함께 적용해야 한다.

인스턴스 사양을 변경하면 `-Xmx`도 함께 조정한다. 비율 방식(`-XX:MaxRAMPercentage`)과 달리 절대값은 사양 변경을 자동으로 따라가지 않으므로, 메모리를 늘려도 애플리케이션은 기존 상한을 유지한다.

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

애플리케이션이 예고 없이 재시작했다면 메모리 부족으로 강제 종료되었는지 확인한다.

```bash
sudo journalctl -u shout-outz-backend --no-pager | grep -iE "oom|killed"
systemctl show shout-outz-backend -p NRestarts -p ExecMainStartTimestamp
free -m
```

`oom-kill`이나 `status=9/KILL`이 보이면 커널이 프로세스를 종료한 것이다. 위 메모리 확보 절차와 `shout-outz-backend.service`의 힙 상한을 함께 점검한다.

CodeDeploy Agent 로그를 확인한다.

```bash
sudo systemctl status codedeploy-agent --no-pager
sudo journalctl -u codedeploy-agent -n 200 --no-pager
```
