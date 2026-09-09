#!/bin/bash
# 把 support-files/bk-aidev/bk-job 下的 AIDEV 资源（Agent / Skill / 知识库）同步到 AIDEV 平台。
# 由 job-sync-bk-aidev 镜像在部署阶段执行，镜像中已内置 bkai-cli。
set -e

log() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") [$1] $2"
}
log_info() { log "INFO" "$1"; }
log_warn() { log "WARN" "$1"; }
log_error() { log "ERROR" "$1"; }
title() { echo "====== $1 ======"; }

# 待同步的 Agent Package 清单
manifest_file="${BK_AIDEV_MANIFEST_FILE:-/data/bkai.yaml}"
# 目标空间，AIDEV 默认空间为 system-bkaidev
space="${BK_AIDEV_SPACE:-system-bkaidev}"
# 需要跳过的资源，格式为 Kind/code，多个以空格分隔；
# 用于资源已被用户在平台上手工改动、不希望被再次覆盖的场景。
exclude_resources="${BK_AIDEV_EXCLUDE_RESOURCES:-}"
# 同步重试参数：Agent 引用的 MCP Server 由网关同步任务异步注册，
# 两个任务之间没有强制先后关系，这里通过有限重试等待 MCP 就绪。
max_retry="${BK_AIDEV_SYNC_MAX_RETRY:-5}"
retry_interval="${BK_AIDEV_SYNC_RETRY_INTERVAL:-30}"

title "checking params"
if [ ! -f "${manifest_file}" ]; then
  log_error "Manifest file ${manifest_file} does not exist"
  exit 1
fi
# bkai-cli 调用 AIDEV 应用态接口需要应用身份，这里复用作业平台自身的 appCode/appSecret
if [ -z "${BK_APP_CODE}" ] || [ -z "${BK_APP_SECRET}" ]; then
  log_error "BK_APP_CODE / BK_APP_SECRET is required by bkai-cli"
  exit 1
fi
log_info "manifest=${manifest_file} space=${space} app_code=${BK_APP_CODE}"
log_info "exclude_resources=[${exclude_resources}] max_retry=${max_retry} retry_interval=${retry_interval}s"

# 组装 --exclude-resource 参数，未配置时不传该参数。
# 这里刻意用字符串拼接而非 bash 数组：资源标识形如 Kind/code，不含空格，
# 用字符串拼接可以让脚本在只有 sh 的精简基础镜像中同样可用。
exclude_args=""
for resource in ${exclude_resources}; do
  exclude_args="${exclude_args} --exclude-resource ${resource}"
done

title "validating aidev resources"
bkai-cli validate -f "${manifest_file}"

title "syncing aidev resources"
attempt=1
while true; do
  log_info "Syncing aidev resources, attempt ${attempt}/${max_retry}"
  # exclude_args 需要按空格拆分成多个参数，故刻意不加引号
  if bkai-cli sync -f "${manifest_file}" --space "${space}" ${exclude_args}; then
    log_info "Aidev resources synced successfully"
    break
  fi
  if [ "${attempt}" -ge "${max_retry}" ]; then
    log_error "Aidev resources sync failed after ${max_retry} attempts"
    exit 1
  fi
  log_warn "Sync failed, the referenced mcp server or skill may not be ready, retry in ${retry_interval}s"
  attempt=$((attempt + 1))
  sleep "${retry_interval}"
done

title "done"
