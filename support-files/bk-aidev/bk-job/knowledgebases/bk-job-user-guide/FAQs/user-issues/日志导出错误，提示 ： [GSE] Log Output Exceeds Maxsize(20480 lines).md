# 日志导出错误，提示：[GSE] Log Output Exceeds Maxsize(20480 lines)

## 现象描述

使用执行脚本进行测试时，导出日志报错 `[GSE] Log Output Exceeds Maxsize(20480 lines)`。

## 问题定位及处理

1. 日志有最大行数限制，上限为 20480 行。
2. 这个最大行数限制无法调整，因为再大平台无法承受，所以在 `agent` 侧做了限制。
