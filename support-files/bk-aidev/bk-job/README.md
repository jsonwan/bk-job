# AIDEV 资源目录

本目录维护作业平台需要自动初始化到 AIDEV 平台的 AI 资源，由部署阶段的 `job-sync-bk-aidev` 任务统一同步。

## 目录结构

```plain
support-files/bk-aidev/bk-job/
├── bkai.yaml                 # Agent Package 清单，声明本次要同步的资源
├── agents/
│   └── bk_job_ai.yaml        # Agent 定义（引用 MCP / Skill / 知识库）
├── skills/                   # Skill 资源，目录名即 Skill code
├── knowledgebases/           # 知识库资源，目录名即知识库 code
└── bin/
    └── sync-bkaidev.sh       # 同步脚本，执行 bkai-cli validate / sync
```

## 与 MCP 的关系

Agent 只引用 MCP，不创建 MCP。MCP Server 由 API 网关侧同步产生：

- 定义位置：`support-files/bk-api-gateway/v3/definition.yaml` 的 `stages[].mcp_servers`；
- 同步动作：`support-files/bk-api-gateway/v3/bin/sync-bkapigateway.sh` 中的 `sync_apigw_stage_mcp_servers`，
  由 `bkApiGatewayConfig.syncMcpServers` 开关控制，且必须排在网关资源发布之后；
- 引用位置：`agents/bk_job_ai.yaml` 的 `spec.mcps[].code`，取值与 `mcp_servers[].name` 一一对应。

两个同步任务之间没有强制先后关系，`bin/sync-bkaidev.sh` 通过有限次重试等待 MCP 就绪。

## 同步方式

镜像中已内置 `bkai-cli`，同步等价于：

```bash
bkai-cli validate -f /data/bkai.yaml
bkai-cli sync -f /data/bkai.yaml --space system-bkaidev
```

资源被用户在平台上手工改动、不希望再次覆盖时，可通过 `bkAidevConfig.excludeResources` 传入 `Kind/code`
（如 `Agent/bk-job-ai`）跳过该资源。

## 维护约定

- `bkai.yaml` 的 `resources` 中列出的路径必须真实存在，否则 `bkai-cli validate` 会失败；
- 新增 Skill / 知识库后，需同时更新 `bkai.yaml` 与 `agents/bk_job_ai.yaml` 中的引用；
- 平台当前暂不支持自定义 Tools，`spec.tools` 保持空数组；
- 任何资源文件中都不要写入 Token、Secret 等敏感凭证，凭证统一通过部署时的环境变量注入。
