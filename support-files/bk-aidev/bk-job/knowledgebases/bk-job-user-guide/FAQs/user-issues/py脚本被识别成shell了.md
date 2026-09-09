# py 脚本被识别成 shell 了

## 问题描述

上传的 Python 脚本被系统当作 shell 脚本执行，导致运行报错。

## 问题原因

脚本首行未指定 shebang，系统无法识别脚本类型，默认按 shell 处理。

## 解决方法

在脚本首行明确指定 shebang，例如：

```python
#!/usr/bin/env python3
```
