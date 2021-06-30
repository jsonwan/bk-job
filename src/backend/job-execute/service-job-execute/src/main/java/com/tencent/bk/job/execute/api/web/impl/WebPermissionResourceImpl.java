/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.execute.api.web.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.i18n.MessageI18nService;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.execute.api.web.WebPermissionResource;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.web.request.OperationPermissionReq;
import com.tencent.bk.job.execute.service.ExecuteAuthService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class WebPermissionResourceImpl implements WebPermissionResource {
    private final WebAuthService webAuthService;
    private final ExecuteAuthService executeAuthService;
    private final TaskInstanceService taskInstanceService;
    private final MessageI18nService i18nService;

    public WebPermissionResourceImpl(WebAuthService webAuthService,
                                     ExecuteAuthService executeAuthService, TaskInstanceService taskInstanceService,
                                     MessageI18nService i18nService) {
        this.webAuthService = webAuthService;
        this.executeAuthService = executeAuthService;
        this.taskInstanceService = taskInstanceService;
        this.i18nService = i18nService;
    }

    @Override
    public ServiceResponse<String> getApplyUrl(String username, OperationPermissionReq req) {
//        authService.
        return null;
    }

    @Override
    public ServiceResponse<AuthResultVO> checkOperationPermission(String username, OperationPermissionReq req) {
        return checkOperationPermission(username, req.getAppId(), req.getOperation(), req.getResourceId(),
            req.isReturnPermissionDetail());
    }

    @Override
    public ServiceResponse<AuthResultVO> checkOperationPermission(String username, Long appId, String operation,
                                                                  String resourceId, Boolean returnPermissionDetail) {
        if (StringUtils.isEmpty(operation)) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, i18nService);
        }
        String[] resourceAndAction = operation.split("/");
        if (resourceAndAction.length != 2) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, i18nService);
        }
        String resourceType = resourceAndAction[0];
        String action = resourceAndAction[1];
        boolean isReturnApplyUrl = returnPermissionDetail == null ? false : returnPermissionDetail;

        switch (resourceType) {
            case "task_instance":
                long taskInstanceId = Long.parseLong(resourceId);
                TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
                if (taskInstance == null) {
                    return ServiceResponse.buildSuccessResp(
                        AuthResultVO.fail());
                }
                switch (action) {
                    case "view":
                    case "redo":
                        AuthResult authResult = executeAuthService.authViewTaskInstance(username, appId,
                            taskInstanceId);
                        if (!authResult.isPass() && isReturnApplyUrl) {
                            authResult.setApplyUrl(webAuthService.getApplyUrl(authResult.getRequiredActionResources()));
                        }
                        return ServiceResponse.buildSuccessResp(webAuthService.toAuthResultVO(authResult));
                }
                break;
        }
        return ServiceResponse.buildSuccessResp(AuthResultVO.fail());
    }
}
