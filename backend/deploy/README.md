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

Actuator는 서비스 포트와 분리해 `127.0.0.1:8081`에만 연다(`management.server.*`). nginx는 8080만 넘기므로 `/actuator/health`, `/actuator/prometheus`는 외부에서 닿지 않고, 같은 서버의 배포 검증 스크립트와 CloudWatch Agent만 호출한다. 보안 그룹으로 막을 수 없으므로 `management.server.address`를 빠뜨리면 8081이 모든 인터페이스에 열린다. dev와 prod 서버 모두 `prod` 프로필이라 두 서버에 똑같이 적용된다.

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
sudo systemctl mask --now fwupd.service
sudo systemctl disable --now udisks2.service
sudo chmod -x /etc/update-motd.d/50-landscape-sysinfo
```

`50-landscape-sysinfo`는 SSH 로그인 시 시스템 요약을 출력하는 스크립트다. 메모리가 빠듯한 상태에서 이 스크립트의 할당 요청이 OOM killer를 발동시킨 사례가 있어 함께 비활성화한다. 반면 `unattended-upgrades`도 메모리를 쓰지만 보안 패치를 자동으로 적용하므로 끄지 않는다.

`fwupd`는 부팅 시 켜지는 서비스가 아니라 D-Bus 요청으로 기동되므로 `disable`로는 막히지 않는다. 실제로 `disable` 후에도 다시 떠 있는 것이 확인되어 `mask`로 기동 자체를 막는다.

애플리케이션의 힙 상한(`shout-outz-backend.service`의 `-Xmx`)은 위 절차를 적용한 상태를 전제로 계산한 값이다. 현재 `-Xmx320m`은 힙 320MB에 힙 외 영역 약 170MB, OS와 에이전트 약 360MB를 더해 903MB 안에 들어가도록 잡았다. Metaspace는 128MB에서 반복 종료가 발생해 상한을 256MB로 올렸으므로, 힙 외 영역은 170MB보다 커질 수 있다. 힙이 상한까지 차서 903MB를 넘으면 swap이 받치므로 메모리 사용량을 모니터링한다. 메모리 확보 절차를 건너뛰면 OS 몫이 460MB로 늘어 이 값으로도 물리 메모리를 초과하므로, 두 가지는 함께 적용해야 한다.

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

### CloudWatch Agent

애플리케이션 로그와 EC2 기본 지표에 없는 메모리·디스크·swap 사용률을 CloudWatch로 보낸다. 설정은 환경별로 `cloudwatch/cloudwatch-agent-dev.json`, `cloudwatch/cloudwatch-agent-prod.json`에 두며, 배포로 자동 반영되지 않으므로 서버에 직접 적용한다.

| 대상 | 보내는 곳 |
|------|----------|
| `/var/log/shout-outz/app.log` | 로그 그룹 `/shout-outz/dev/app`, `/shout-outz/prod/app` (30일 보관) |
| `mem_used_percent`, `swap_used_percent`, `disk_used_percent` | 지표 네임스페이스 `ShoutOutz` |
| (prod만) `127.0.0.1:8081/actuator/prometheus`의 API 요청·히카리 풀 지표 | 로그 그룹 `/shout-outz/prod/prometheus`를 거쳐 지표 네임스페이스 `ShoutOutz` |

공용 AWS 계정이라 기본 네임스페이스 `CWAgent`는 다른 팀 지표와 섞이므로 `ShoutOutz`로 분리한다. 시스템 지표(mem·swap·disk)에는 `InstanceId`가 붙어 dev와 prod 서버가 구분되고, 아래 앱 지표는 `env` 차원으로 구분된다. `multi_line_start_pattern`은 날짜로 시작하는 줄을 로그 한 건의 시작으로 보아, 스택트레이스를 한 건으로 묶는다.

EC2 인스턴스 역할에 `CloudWatchAgentServerPolicy` 권한이 있어야 한다. ARM64용 패키지를 설치하고, 서버 환경에 맞는 설정 파일(`ENV`에 `dev` 또는 `prod`)을 적용한다.

```bash
ENV=dev
cd /tmp
wget https://amazoncloudwatch-agent.s3.amazonaws.com/ubuntu/arm64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i -E ./amazon-cloudwatch-agent.deb
sudo install -m 0644 "cloudwatch-agent-${ENV}.json" /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
```

`install` 명령의 원본은 저장소의 `cloudwatch/cloudwatch-agent-${ENV}.json`을 서버로 옮긴 파일이다. `fetch-config`는 이 파일을 `/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.d/file_amazon-cloudwatch-agent.json`으로 옮기므로, 적용 후에는 원래 경로에 파일이 남지 않는다. 서버 설정이 저장소와 같은지는 옮겨진 파일과 저장소 파일의 체크섬을 비교해 확인한다.

```bash
sudo md5sum /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.d/file_amazon-cloudwatch-agent.json
```

적용 후 상태와 오류를 확인한다. `AccessDenied`가 보이면 인스턴스 역할의 권한을 점검한다.

```bash
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a status
sudo grep -iE "error|denied" /opt/aws/amazon-cloudwatch-agent/logs/amazon-cloudwatch-agent.log | tail -5
```

설정을 바꿀 때는 저장소 파일을 먼저 수정해 PR로 남긴 뒤 서버에 같은 방법으로 적용한다. 서버에서 직접 고치면 변경 이유가 남지 않고 저장소와 서버가 달라진다. 두 설정 파일은 `log_group_name`과 prod의 앱 지표 수집(`logs.metrics_collected.prometheus`)만 다르므로, 그 밖의 지표나 수집 대상을 바꿀 때는 두 파일을 함께 수정한다.

#### 앱 지표 (prod)

Agent가 1분마다 `127.0.0.1:8081/actuator/prometheus`를 읽어 EMF 로그로 보내고, CloudWatch가 이를 지표로 바꾼다. 수집 대상은 `cloudwatch/prometheus-prod.yaml`, 지표 변환은 `cloudwatch-agent-prod.json`의 `metric_declaration`이 정한다.

| 지표 | 의미 | 볼 통계 |
|------|------|--------|
| `http_server_requests_seconds_count` | API 요청 수 | Sum |
| `http_server_requests_seconds_sum` | API 응답 시간 합계(초) | Sum (평균 = sum ÷ count) |
| `http_server_requests_seconds_max` | API 최대 응답 시간(초) | Maximum |
| `hikaricp_connections_active` / `idle` | 사용 중·쉬는 커넥션 수 | Maximum / Minimum |
| `hikaricp_connections_pending` | 커넥션을 기다리는 스레드 수 | Maximum (0보다 크면 풀 부족) |

차원은 `env` 하나다. 커스텀 지표는 차원 조합마다 과금되므로 uri·method·status로 나누지 않고 전체 합산만 보낸다. API별 분석은 아래 [요청 로그 분석](#요청-로그-분석-logs-insights)으로 한다. 수집 단계(`metric_relabel_configs`)에서 위 지표 외에는 버리므로 JVM 등 나머지 지표는 로그로도 쌓이지 않는다.

애플리케이션이 8081을 연 상태로 배포된 뒤 적용한다. 위 설치 절차의 `fetch-config` 전에 수집 설정 파일을 같은 디렉터리에 둔다.

```bash
sudo install -m 0644 prometheus-prod.yaml /opt/aws/amazon-cloudwatch-agent/etc/prometheus.yaml
```

로그 그룹 `/shout-outz/prod/prometheus`는 Agent가 처음 보낼 때 자동으로 만들어지므로, 생성되면 콘솔에서 보관 기간(30일)과 필수 태그(`Service`, `Role`, `ProjectTeam`)를 직접 설정한다.

적용 후 다음을 확인한다.

```bash
curl -s http://127.0.0.1:8081/actuator/prometheus | grep -E "^(http_server_requests|hikaricp_connections)" | head
sudo ss -tlnp | grep 8081    # 127.0.0.1:8081 이어야 한다. 0.0.0.0 이나 * 이면 address 설정이 빠진 것이다
```

- 콘솔 지표 `ShoutOutz` 네임스페이스에 위 6개 지표가 `env=prod` 차원으로 보이는지 본다.
- 같은 시간대의 `http_server_requests_seconds_count` Sum이 요청 로그의 요청 수와 비슷한지 비교한다. Agent가 uri별로 나뉜 값을 `env` 하나로 합치는 방식은 문서에 명시되어 있지 않으므로 처음 적용할 때 한 번 확인한다.
- 외부에서 `curl --max-time 5 http://{EIP}:8081/actuator/health`가 타임아웃되는지 본다.

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

CodeDeploy의 `ValidateService` 단계는 최대 180초 동안 다음 엔드포인트가 `UP`인지 확인한다. Actuator가 관리 포트로 분리되어 있으므로 8080이 아니라 8081이다.

```text
http://127.0.0.1:8081/actuator/health
```

EC2에서 애플리케이션 로그를 확인한다.

```bash
sudo systemctl status shout-outz-backend --no-pager
sudo journalctl -u shout-outz-backend -n 200 --no-pager
```

같은 로그가 파일에도 남는다. `prod` 프로필의 `logging.file.name`이 경로를 정하고, 폴더는 `before_install.sh`가 서비스 사용자 소유로 만든다.

```bash
sudo tail -n 200 /var/log/shout-outz/app.log
```

`app.log`가 10MB를 넘거나 날짜가 바뀌면 `app.log.{날짜}.{번호}.gz`로 압축해 떼어 내고, 7일이 지나거나 합계가 200MB를 넘으면 오래된 것부터 지운다. 장기 보관은 CloudWatch가 맡는다.

- CloudWatch Agent 설정의 수집 경로는 `logging.file.name`과 같아야 한다. 한쪽을 바꾸면 다른 쪽도 함께 바꾼다.
- 폴더와 파일은 `shoutoutz` 소유이고 그룹 외에는 읽을 수 없다. CloudWatch Agent를 root가 아닌 사용자(`cwagent` 등)로 실행하면 그 사용자를 `shoutoutz` 그룹에 추가한다.
- `shout-outz-backend.service`에 `ProtectSystem=strict`를 추가하면 `/var/log`가 읽기 전용이 되어 로그 파일을 쓰지 못한다. 이때는 `ReadWritePaths=/var/log/shout-outz`를 함께 추가한다.

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

## 요청 로그 분석 (Logs Insights)

`HttpRequestLoggingFilter`가 요청마다 한 줄을 남기고, 이 로그는 Agent를 통해 `/shout-outz/{dev|prod}/app`에 쌓인다. API별 응답 시간은 지표가 아니라 이 로그로 본다.

```text
http_request method=GET path=/api/v1/projects/@shout-outz status=200 duration_ms=143 trace_id=... route=/api/v1/projects/@{slug}
```

- `route`는 Spring이 매칭한 URL 템플릿이다. `path`는 ID·slug마다 달라지므로 API 단위 집계는 `route`로 한다.
- 어떤 API에도 맞지 않은 요청은 `route=/**`, 컨트롤러에 닿기 전에 끝난 요청(보안 필터 401/403 등)은 `route=-`다.
- `duration_ms`는 요청이 필터에 들어온 뒤부터 잰다. Tomcat 스레드가 모두 바빠 필터에 들어오기 전에 기다린 시간은 포함되지 않으므로, 부하 테스트에서는 클라이언트가 잰 응답 시간과 함께 본다.

CloudWatch 콘솔 → 로그 → Logs Insights에서 로그 그룹과 시간 범위를 고르고 실행한다. 부하 테스트 요청에는 `X-Trace-Id: lt-...`처럼 접두사를 붙여 보내면(영문·숫자·`-`·`_` 64자 이내) `trace_id=lt-`로 실제 사용자 요청과 구분할 수 있다.

API별 응답 시간 순위:

```text
filter @message like "http_request"
| parse @message "method=* path=* status=* duration_ms=* trace_id=* route=*" as method, path, status, ms, trace, route
| stats count(*) as requests, avg(ms) as avg_ms, pct(ms, 95) as p95, pct(ms, 99) as p99, max(ms) as max_ms by method, route
| sort p95 desc
```

한 API의 시간대별 변화(부하를 올리며 어느 구간부터 느려지는지):

```text
filter @message like "http_request"
| parse @message "duration_ms=* trace_id=* route=*" as ms, trace, route
| filter route = "/api/v1/projects/@{slug}"
| stats count(*) as requests, pct(ms, 95) as p95 by bin(30s)
```

API별 서버 에러:

```text
filter @message like "http_request"
| parse @message "method=* path=* status=* duration_ms=* trace_id=* route=*" as method, path, status, ms, trace, route
| filter status >= 500
| stats count(*) as server_errors by method, route
| sort server_errors desc
```

느린 요청의 `trace_id`를 골라 `filter @message like "{trace_id}"`로 다시 검색하면 그 요청을 처리하며 남긴 로그를 모두 볼 수 있다. 원인은 같은 시점의 히카리 `pending`·`active`, RDS CPU, EC2 CPU 지표와 함께 좁힌다.
