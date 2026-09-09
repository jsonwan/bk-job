# Skill 资源目录

本目录下每一个子目录对应 AIDEV 平台上的一个 Skill，**目录名即 Skill code**，由 `bkai-cli` 同步到平台。

目录结构示例：

```plain
skills/
└── job_skill/            # Skill code
    └── ...
```

## 维护约定

- 新增 Skill 后，需要在 `../bkai.yaml` 的 `resources` 中补上 `skills/<code>`，否则不会被同步；
- 若需要被 Agent 引用，还需在 `../agents/bk_job_ai.yaml` 的 `spec.skills` 中登记该 code，并按需通过 `env` 注入运行变量；
- Skill 当前不涉及自定义镜像，无需在 Helm Chart 中上报镜像信息；
- 不要在 Skill 文件中写入 Token、Secret 等敏感凭证。

> 当前尚未纳入任何 Skill，本文件仅用于占位与说明。
