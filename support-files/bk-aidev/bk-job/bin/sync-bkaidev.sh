#!/bin/bash
# 把 support-files/bk-aidev/bk-job 下的 AIDEV 资源（Agent / Skill / 知识库）同步到 AIDEV 平台。
# 由 job-sync-bk-aidev 镜像在部署阶段执行，镜像中已内置 bkai-init。
set -e

log() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") [$1] $2"
}
log_info() { log "INFO" "$1"; }
log_warn() { log "WARN" "$1"; }
log_error() { log "ERROR" "$1"; }
title() { echo "====== $1 ======"; }

# 本脚本所在目录，用于定位同目录下的 render_placeholders.py
script_dir=$(cd "$(dirname "$0")" && pwd)
# 待同步的 Agent Package 清单，路径与镜像中的资源目录一致
package_path="${BK_AIDEV_PACKAGE_PATH:-/bk-job/bkai.yaml}"
# 目标空间 ID，必填且没有默认值：资源文件不再声明 space，全部以该参数为准
space="${BK_AIDEV_SPACE:-}"
# 目标租户，bkai-init 默认 system，这里显式传入避免依赖默认值
tenant_id="${BK_AIDEV_TENANT_ID:-system}"
# 是否发布智能体：不发布时同步结果只是草稿，页面上的小鲸仍是旧版本
publish="${BK_AIDEV_PUBLISH:-true}"
# 需要跳过的资源，格式为 kind/code（kind 小写，取值 agent/collection/skill/knowledgebase），
# 多个以空格分隔；用于资源已被用户在平台上手工改动、不希望被再次覆盖的场景。
exclude_resources="${BK_AIDEV_EXCLUDE_RESOURCES:-}"
# 同步重试参数：Agent 引用的 MCP Server 由网关同步任务异步注册，
# 两个任务之间没有强制先后关系，这里通过有限重试等待 MCP 就绪。
max_retry="${BK_AIDEV_SYNC_MAX_RETRY:-5}"
retry_interval="${BK_AIDEV_SYNC_RETRY_INTERVAL:-30}"

title "checking params"
if [ ! -f "${package_path}" ]; then
  log_error "Agent package file ${package_path} does not exist"
  exit 1
fi
# 资源根目录，渲染与同步都以该目录为基准
resource_dir=$(dirname "${package_path}")
# 目标空间没有默认值，缺失时直接失败，避免把资源同步到非预期的空间
if [ -z "${space}" ]; then
  log_error "BK_AIDEV_SPACE (target space id) is required"
  exit 1
fi
# bkai-init 从环境变量读取平台地址与应用身份，这里复用作业平台自身的 appCode/appSecret
if [ -z "${BKAI_BASE_URL}" ]; then
  log_error "BKAI_BASE_URL is required by bkai-init"
  exit 1
fi
if [ -z "${BKAI_APP_CODE}" ] || [ -z "${BKAI_APP_SECRET}" ]; then
  log_error "BKAI_APP_CODE / BKAI_APP_SECRET is required by bkai-init"
  exit 1
fi
log_info "package=${package_path} tenant=${tenant_id} space=${space} app_code=${BKAI_APP_CODE}"
log_info "publish=${publish} exclude_resources=[${exclude_resources}] max_retry=${max_retry} retry_interval=${retry_interval}s"

# 组装 --exclude-resource 参数，未配置时不传该参数。
# 这里刻意用字符串拼接而非 bash 数组：资源标识形如 kind/code，不含空格，
# 用字符串拼接可以让脚本在只有 sh 的精简基础镜像中同样可用。
exclude_args=""
for resource in ${exclude_resources}; do
  exclude_args="${exclude_args} --exclude-resource ${resource}"
done

# 发布参数：publish_config_only=1 表示只发布配置，不走平台常规发布流程。
# 不开启发布时同步结果停留在草稿，页面上的智能体不会更新。
publish_args=""
if [ "${publish}" = "true" ]; then
  publish_args="--publish --publish_config_only=1"
fi

# 占位符渲染交给 Python 脚本处理：字面量替换不涉及 sed 的转义规则（& \ 与分隔符），
# 文件编码与渲染范围也更可控；占位符名单维护在 render_placeholders.py 中。
title "rendering placeholders"
# 解释器可通过 PYTHON_BIN 指定；未指定时依次探测 python3、python，
# 部分基础镜像只提供 python 而没有 python3。
python_bin="${PYTHON_BIN:-}"
if [ -z "${python_bin}" ]; then
  for candidate in python3 python; do
    if command -v "${candidate}" >/dev/null 2>&1; then
      python_bin="${candidate}"
      break
    fi
  done
fi
if [ -z "${python_bin}" ]; then
  log_error "python3 is required to render placeholders in resource files, but no python interpreter found"
  exit 1
fi
# 渲染脚本按 Python 3 编写，用 python2 执行会直接语法报错，这里提前给出明确提示
if ! "${python_bin}" -c 'import sys; sys.exit(0 if sys.version_info[0] >= 3 else 1)' >/dev/null 2>&1; then
  log_error "Python 3 is required to render placeholders, but ${python_bin} is not python3"
  exit 1
fi
log_info "Using python interpreter: ${python_bin}"
"${python_bin}" "${script_dir}/render_placeholders.py" --base-dir "${resource_dir}"

title "validating aidev resources"
bkai-init validate -f "${package_path}" --space "${space}"

# 对比线上配置，把本次将要覆盖的内容留在部署日志里，便于同步异常后追溯。
# diff 是只读操作，且「有差异」本身会返回非零退出码，故失败不阻断后续同步。
title "diffing aidev resources"
if ! bkai-init diff -f "${package_path}" --tenant-id "${tenant_id}" --space "${space}"; then
  log_warn "Diff returned non-zero (differences found or diff unavailable), continue to sync"
fi

title "syncing aidev resources"
attempt=1
while true; do
  log_info "Syncing aidev resources, attempt ${attempt}/${max_retry}"
  # 必须带 --confirm，否则 sync 只做只读预览，不会写入任何资源
  # exclude_args / publish_args 需要按空格拆分成多个参数，故刻意不加引号
  if bkai-init sync -f "${package_path}" --tenant-id "${tenant_id}" --space "${space}" \
      --confirm ${publish_args} ${exclude_args}; then
    log_info "Aidev resources synced successfully"
    break
  fi
  if [ "${attempt}" -ge "${max_retry}" ]; then
    log_error "Aidev resources sync failed after ${max_retry} attempts"
    exit 1
  fi
  # 同步中途失败时，此前已写入的资源不会自动回滚，重试是按 code 覆盖式重入
  log_warn "Sync failed, the referenced mcp server or skill may not be ready, retry in ${retry_interval}s"
  attempt=$((attempt + 1))
  sleep "${retry_interval}"
done

title "done"
