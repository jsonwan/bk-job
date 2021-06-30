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

package com.tencent.bk.job.execute.api.esb.v2;

import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.execute.model.esb.v2.EsbStepInstanceResultAndLog;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbGetJobInstanceLogRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.tencent.bk.job.common.i18n.locale.LocaleUtils.COMMON_LANG_HEADER;

/**
 * 根据作业实例ID查询作业执行日志-V2
 */
@RequestMapping("/esb/api/v2")
@RestController
public interface EsbGetJobInstanceLogResource {

    @PostMapping("/get_job_instance_log")
    EsbResp<List<EsbStepInstanceResultAndLog>> getJobInstanceLogUsingPost(
        @RequestHeader(value = COMMON_LANG_HEADER, required = false) String lang,
        @RequestBody EsbGetJobInstanceLogRequest request);

    @GetMapping("/get_job_instance_log")
    EsbResp<List<EsbStepInstanceResultAndLog>> getJobInstanceLog(
        @RequestHeader(value = COMMON_LANG_HEADER, required = false) String lang,
        @RequestParam(value = "bk_app_code") String appCode,
        @RequestParam(value = "bk_username") String username,
        @RequestParam(value = "bk_biz_id") Long appId,
        @RequestParam(value = "job_instance_id") Long taskInstanceId);


}
