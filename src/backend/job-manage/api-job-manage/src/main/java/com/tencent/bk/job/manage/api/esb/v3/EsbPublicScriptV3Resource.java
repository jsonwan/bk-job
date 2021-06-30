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

package com.tencent.bk.job.manage.api.esb.v3;

import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.v3.EsbPageDataV3;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetScriptListV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetScriptVersionDetailV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetScriptVersionListV3Req;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbScriptV3DTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbScriptVersionDetailV3DTO;
import org.springframework.web.bind.annotation.*;

import static com.tencent.bk.job.common.i18n.locale.LocaleUtils.COMMON_LANG_HEADER;

/**
 * 公共脚本相关API-V3
 */
@RequestMapping("/esb/api/v3")
@RestController
public interface EsbPublicScriptV3Resource {

    @PostMapping("/get_public_script_list")
    EsbResp<EsbPageDataV3<EsbScriptV3DTO>> getPublicScriptList(
        @RequestHeader(value = COMMON_LANG_HEADER, required = false) String lang,
        @RequestBody EsbGetScriptListV3Req request);

    @PostMapping("/get_public_script_version_list")
    EsbResp<EsbPageDataV3<EsbScriptVersionDetailV3DTO>> getPublicScriptVersionList(
        @RequestHeader(value = COMMON_LANG_HEADER, required = false) String lang,
        @RequestBody EsbGetScriptVersionListV3Req request);

    @PostMapping("/get_public_script_version_detail")
    EsbResp<EsbScriptVersionDetailV3DTO> getPublicScriptVersionDetail(
        @RequestHeader(value = COMMON_LANG_HEADER, required = false) String lang,
        @RequestBody EsbGetScriptVersionDetailV3Req request);

}
