#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""查询多租户环境下 bkai-init 需要的调用用户名（BKAI_USERNAME），打印到标准输出。

AIDEV 要求传入的是蓝鲸用户管理中的 bk_username，而不是登录名：部署阶段没有真实
操作人，这里用平台内置的虚拟账号（默认 bk_admin）换取其 bk_username，与后端
VirtualAdminAccountCache 的取值一致。非多租户环境不调用本脚本，直接用 admin。

用标准库发请求而不是 curl：AIDEV 基础镜像中没有 curl，但一定有 Python 3
（bkai-init 本身是 Python 包）。

取值来自环境变量，由 Helm 的同步 Job 注入：
  BK_AIDEV_USERNAME_LOOKUP_URL  用户管理 API 网关地址
  BK_AIDEV_USERNAME_LOGIN_NAME  待查询的登录名，默认 bk_admin
  BK_AIDEV_TENANT_ID            租户，默认 system
  BKAI_APP_CODE / BKAI_APP_SECRET  网关鉴权用的应用身份
"""
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

LOOKUP_PATH = "/api/v3/open/tenant/virtual-users/-/lookup/"
DEFAULT_LOGIN_NAME = "bk_admin"
DEFAULT_TENANT_ID = "system"
TIMEOUT_SECONDS = 10


def log(level, message):
    """日志一律走 stderr：stdout 只用于回传 bk_username，不能被日志污染。"""
    print("[{}] {}".format(level, message), file=sys.stderr, flush=True)


def build_request(base_url, tenant_id, app_code, app_secret, login_name):
    query = urllib.parse.urlencode({"lookups": login_name, "lookup_field": "login_name"})
    url = "{}{}?{}".format(base_url.rstrip("/"), LOOKUP_PATH, query)
    request = urllib.request.Request(url)
    request.add_header("X-Bk-Tenant-Id", tenant_id)
    request.add_header(
        "X-Bkapi-Authorization",
        json.dumps({"bk_app_code": app_code, "bk_app_secret": app_secret}),
    )
    return url, request


def pick_username(payload, login_name):
    """从 {"data": [{"bk_username": "...", "login_name": "..."}]} 中取 bk_username。"""
    users = payload.get("data")
    if not isinstance(users, list) or not users:
        log("ERROR", "No user matched login_name={}, response={}".format(login_name, payload))
        return None
    username = users[0].get("bk_username")
    if not username:
        log("ERROR", "Field bk_username is missing in response: {}".format(users[0]))
        return None
    return username


def resolve():
    base_url = os.environ.get("BK_AIDEV_USERNAME_LOOKUP_URL", "").strip()
    app_code = os.environ.get("BKAI_APP_CODE", "").strip()
    app_secret = os.environ.get("BKAI_APP_SECRET", "").strip()
    tenant_id = os.environ.get("BK_AIDEV_TENANT_ID", "").strip() or DEFAULT_TENANT_ID
    login_name = os.environ.get("BK_AIDEV_USERNAME_LOGIN_NAME", "").strip() or DEFAULT_LOGIN_NAME

    if not base_url:
        log("ERROR", "BK_AIDEV_USERNAME_LOOKUP_URL is empty, "
                     "set bkai.username in values to skip the lookup")
        return None
    if not app_code or not app_secret:
        log("ERROR", "BKAI_APP_CODE / BKAI_APP_SECRET is required to call the bk-user api")
        return None

    url, request = build_request(base_url, tenant_id, app_code, app_secret, login_name)
    log("INFO", "Looking up bk_username of {} from {}".format(login_name, url))
    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
            body = response.read().decode("utf-8")
    except urllib.error.HTTPError as error:
        log("ERROR", "Lookup failed with http {}: {}".format(error.code, error.read().decode("utf-8", "replace")))
        return None
    except urllib.error.URLError as error:
        log("ERROR", "Lookup failed, cannot reach {}: {}".format(url, error.reason))
        return None

    try:
        payload = json.loads(body)
    except ValueError as error:
        log("ERROR", "Lookup response is not valid json: {}, body={}".format(error, body))
        return None
    return pick_username(payload, login_name)


def main():
    username = resolve()
    if not username:
        return 1
    log("INFO", "Resolved bk_username={}".format(username))
    print(username)
    return 0


if __name__ == "__main__":
    sys.exit(main())
