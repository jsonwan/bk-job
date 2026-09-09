# 知识库资源目录

本目录下每一个子目录对应 AIDEV 平台上的一个知识库，**目录名即知识库 code**，目录内的 Markdown 文件会按原有层级同步到平台。

目录结构示例：

```plain
knowledgebases/
└── bk-job-user-guide/          # 知识库 code
    ├── README.md
    └── guides/
        └── execute.md
```

## 维护约定

- 知识库内容由各模块单独维护，本目录只承载已确定要同步到平台的 Markdown 文件；
- 新增知识库后，需要在 `../bkai.yaml` 的 `resources` 中补上 `knowledgebases/<code>`，否则不会被同步；
- 若需要被 Agent 引用，还需在 `../agents/bk_job_ai.yaml` 的 `spec.knowledgebases.items` 中登记该 code；
- 不要在知识库文件中写入 Token、Secret 等敏感凭证。

> 当前尚未纳入任何知识库，本文件仅用于占位与说明。
