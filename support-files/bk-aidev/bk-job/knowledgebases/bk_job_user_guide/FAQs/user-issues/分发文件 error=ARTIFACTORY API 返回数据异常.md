# 分发文件 error=ARTIFACTORY API 返回数据异常

## 问题描述

标准运维调用 JOB 插件进行作业分发时出现以下报错：

```text
jobv3.fast_transfer_file返回失败, error=ARTIFACTORY API 返回数据异常,
```

## 问题原因

快速分发文件里面的本地文件默认只会保留 7 天。

## 解决方法

需要持久化分发，建议将文件保存到自己的服务器或者制品库。
