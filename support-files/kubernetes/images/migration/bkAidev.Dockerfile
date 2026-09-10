# AIDEV 资源同步镜像，基础镜像由 AIDEV 平台提供，其中已内置 bkai-cli
# 另需基础镜像中带有 python3，用于同步前渲染资源文件中的占位符（bin/render_placeholders.py）
# TODO 待填充：替换为 AIDEV 平台提供的正式基础镜像地址与 tag
FROM hub.bktencent.com/blueking/bkai-cli:latest

WORKDIR /data

COPY bin /data/bin
COPY bkai.yaml /data/bkai.yaml
COPY agents /data/agents
COPY skills /data/skills
COPY knowledgebases /data/knowledgebases

RUN chmod +x /data/bin/sync-bkaidev.sh
