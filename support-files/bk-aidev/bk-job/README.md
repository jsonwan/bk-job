# AIDEV 资源目录

本目录维护作业平台需要自动初始化到 AIDEV 平台的 AI 资源，由部署阶段的 `job-sync-bk-aidev` 任务统一同步，
通过 Helm 的 `bkai.enabled` 开关控制是否开启。镜像中资源被放在 `/bk-job` 目录下，与 AIDEV 方案文档的
目录约定（`/{系统标识}/bkai.yaml`）保持一致。

## 目录结构

```plain
support-files/bk-aidev/bk-job/
├── bkai.yaml                 # Agent Package 清单，声明本次要同步的资源
├── agents/
│   └── bk_job_ai.yaml        # Agent 定义（引用 MCP / Skill / 知识库）
├── skills/                   # Skill 资源，目录名即 Skill code
├── knowledgebases/           # 知识库资源，每个子目录一个知识库
└── bin/
    ├── sync-bkaidev.sh       # 同步脚本，执行占位符渲染与 bkai-init validate / diff / sync
    ├── render_placeholders.py   # 占位符渲染脚本
    └── resolve_bkai_username.py # 查询 bkai-init 需要的调用用户名
```

## 资源 code 命名规则

code 是平台定位资源的稳定标识，**一律小写**，与展示名称无关；不符合规则会被 `bkai-init` 直接拒绝，
且平台不做大小写转换或迁移。

| 资源 | 规则 | 本仓库取值 |
| --- | --- | --- |
| 智能体 / 子智能体 | `^ai-[a-z][a-z0-9-]{1,12}$`，总长 5–16 位，不支持下划线 | `ai-bkjob-web1` |
| 角色 / MCP / Skill | `^[a-z][a-z0-9_-]{0,63}$` | `bk-job-prod-mcp-task-context` |
| 知识库 | `^[a-z][a-z0-9_]{0,63}$`，**不支持中划线** | `bk_job_user_guide` |

知识库 code 直接用作 Milvus 集合名，Milvus 不接受中划线，故只能用下划线。

## 与 MCP 的关系

Agent 只引用 MCP，不创建 MCP。MCP Server 由 API 网关侧同步产生：

- 定义位置：`support-files/bk-api-gateway/v3/definition.yaml` 的 `stages[].mcp_servers`；
- 同步动作：`support-files/bk-api-gateway/v3/bin/sync-bkapigateway.sh` 中的 `sync_apigw_stage_mcp_servers`，
  由 `bkApiGatewayConfig.syncMcpServers` 开关控制，且必须排在网关资源发布之后；
- 引用位置：`agents/bk_job_ai.yaml` 的 `spec.mcps[].code`，取值与 `mcp_servers[].name` 一一对应。

两个同步任务之间没有强制先后关系，`bin/sync-bkaidev.sh` 通过有限次重试等待 MCP 就绪。

## 占位符渲染

资源文件中与部署环境相关的地址不写死，而是用 `${占位符}` 表示，同步前由 `bin/render_placeholders.py`
替换为真实值。占位符取值来自同名环境变量，环境变量在 Helm 的同步 Job 中注入。

| 占位符 | 含义 | 取值来源 |
| --- | --- | --- |
| `${JOB_URL_BASE}` | 作业平台访问地址，如 `http://job.example.com` | Chart helper `job.url.base` |

新增占位符时需要同时改两处：`render_placeholders.py` 中的 `PLACEHOLDER_NAMES` 名单，以及
`support-files/kubernetes/charts/bk-job/templates/job-migration/sync-bkaidev-job.yaml` 中对应的环境变量。

渲染刻意按名单逐个做字面量替换，而不是全量替换所有 `${xxx}`：知识库文档中大量出现作业平台魔法变量的
字面写法（如 `${JOB_NAMESPACE_ALL}`、`${svr_addr}`），全量替换会把这些说明文档中的示例清成空串。
渲染范围为 `bkai.yaml` 与 `agents`/`skills`/`knowledgebases` 三个目录下的文本文件，不含 `bin` 目录。
若某个占位符在资源文件中被使用但对应环境变量为空，脚本会直接报错退出，避免把空地址同步到平台。

渲染用 Python 而非 shell 的 `sed`：字面量替换不涉及 `sed` 中 `&`、`\` 与分隔符的转义规则，
文件编码固定 UTF-8 且不改动原有换行风格，行为不受基础镜像 GNU coreutils / BusyBox 差异影响。
因此基础镜像中需要有 Python 3，`sync-bkaidev.sh` 会依次探测 `python3`、`python`，
也支持通过 `PYTHON_BIN` 指定解释器，并在缺失或版本不符时明确报错。

## 同步方式

镜像中已内置 `bkai-init`，同步等价于：

```bash
bkai-init validate -f /bk-job/bkai.yaml --space "$SPACE_ID"
bkai-init diff     -f /bk-job/bkai.yaml --tenant-id system --space "$SPACE_ID"
bkai-init sync     -f /bk-job/bkai.yaml --tenant-id system --space "$SPACE_ID" \
  --confirm --publish --publish_config_only=1
```

几个容易踩的点：

- **`sync` 不带 `--confirm` 只是只读预览**，命令照样返回 0，但平台上什么都不会写；
- **`--space` 必填、无默认值，传的是目标空间 ID**，资源文件里不再声明 `space`；
- **不带 `--publish` 只同步草稿**，页面上的智能体不会更新，由 `bkai.publish` 控制；
- 平台地址与应用身份由 `BKAI_BASE_URL` / `BKAI_APP_CODE` / `BKAI_APP_SECRET` 三个环境变量提供，
  变量名由 `bkai-init` 约定，不可改名；调用用户名 `BKAI_USERNAME` 见下一节。

资源被用户在平台上手工改动、不希望再次覆盖时，可通过 `bkai.excludeResources` 传入 `kind/code`
（kind 小写，取值 `agent` / `collection` / `skill` / `knowledgebase`，如 `["agent/ai-bkjob-web1"]`）跳过该资源。
同步中途失败时已写入的资源不会自动回滚，重跑是按 code 覆盖式重入。

## 调用用户名（BKAI_USERNAME）

`bkai-init` 要求带上调用用户名，取值是蓝鲸用户管理中的 **`bk_username`**，不是登录名。部署阶段没有
真实操作人，取值规则与后端取管理员账号的规则（`virtualAccount.queryAdminUsername`）保持一致：

- **非多租户环境**（默认）：`bk_username` 就是 `admin`，不查接口，与后端 `OriginalAdminNameProvider` 一致；
- **多租户环境**（`queryAdminUsername=true`）：查虚拟账号 `bk_admin` 的 `bk_username`，与后端
  `VirtualAdminAccountCache` 一致，由 `bin/resolve_bkai_username.py` 在同步前查询并注入：

```text
GET ${bkUserApiGatewayUrl}/api/v3/open/tenant/virtual-users/-/lookup/?lookups=bk_admin&lookup_field=login_name
Header: X-Bk-Tenant-Id: system
Header: X-Bkapi-Authorization: {"bk_app_code": "...", "bk_app_secret": "..."}

{"data":[{"bk_username":"gtrydp5fs282bptk","login_name":"bk_admin", ...}]}
```

用 Python 标准库而不是 `curl` 发请求：AIDEV 基础镜像里没有 `curl`，但一定有 Python 3。

- 已知 `bk_username` 时，直接配 `bkai.username`，两种环境都跳过上面的推导；
- 查询失败会直接中断同步并给出提示，不会用空用户名继续。

## 维护约定

- `bkai.yaml` 的 `resources` 中列出的路径必须真实存在，否则 `bkai-init validate` 会失败；
- `resources` 中被引用的依赖（Skill、知识库、子智能体）要排在引用它们的 Agent 之前；
- 新增 Skill / 知识库后，需同时更新 `bkai.yaml` 与 `agents/bk_job_ai.yaml` 中的引用；
- Agent 不能配置 `metadata.version`（含 null）与 `metadata.space`，两者分别由平台分配和由 `--space` 传入；
- 自定义指令的 `agent_code` 省略即表示当前智能体；填其它智能体的 code 就变成「引用来源指令」，
  此时该 code 必须出现在 `subagents` 中，且来源指令必须已发布；
- 平台当前暂不支持自定义 Tools，`spec.tools` 保持空数组；
- 引用的 MCP **无论是否公开都要事先授权**，`bkai-init` 不会自动创建 MCP，也不会申请权限；
- 任何资源文件中都不要写入 Token、Secret 等敏感凭证，凭证统一通过部署时的环境变量注入。
