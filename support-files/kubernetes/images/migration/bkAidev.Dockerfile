# AIDEV 资源同步镜像，基础镜像由 AIDEV 平台提供，其中已内置 bkai-cli
# 另需基础镜像中带有 python3，用于同步前渲染资源文件中的占位符（bin/render_placeholders.py）
FROM hub.bktencent.com/blueking/bk-aidev-init:0.1.0rc3

# 资源目录与 AIDEV 方案文档的目录约定保持一致：/{系统标识}/bkai.yaml
WORKDIR /bk-job

COPY bin /bk-job/bin
COPY bkai.yaml /bk-job/bkai.yaml
COPY agents /bk-job/agents
COPY skills /bk-job/skills
COPY knowledgebases /bk-job/knowledgebases

RUN chmod +x /bk-job/bin/sync-bkaidev.sh
