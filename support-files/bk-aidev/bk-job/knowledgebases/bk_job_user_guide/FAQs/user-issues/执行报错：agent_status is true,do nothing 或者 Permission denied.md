# 执行报错：agent_status is true,do nothing 或者 Permission denied

## 现象描述

执行作业时，日志中总是会弹出报错信息 `agent_status is true,do nothing`，或者弹出 `/tmp/bkjob/user00/xxxxx.env: Permission denied`。

## 问题定位及处理

需要修改目录权限，执行以下命令：

```bash
chmod 777 /tmp/bkjob/user00
```
