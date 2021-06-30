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

package com.tencent.bk.job.execute.model;

import lombok.Data;

/**
 * Agent任务执行信息
 */
@Data
public class AgentTaskExecutionDTO {
    /**
     * 云IP
     */
    private String cloudIp;
    /**
     * 执行次数
     */
    private Integer executeCount;
    /**
     * Agent ip显示名称，展示给用户使用该ip
     */
    private String displayIp;

    /**
     * 云区域ID
     */
    private Long cloudAreaId;

    /**
     * 云区域名称
     */
    private String cloudAreaName;

    /**
     * Agent任务执行状态
     */
    private Integer status;
    /**
     * 开始时间
     */
    private Long startTime;
    /**
     * 结束时间
     */
    private Long endTime;
    /**
     * 耗时,毫秒
     */
    private Long totalTime;
    /**
     * 脚本返回码
     */
    private Integer exitCode;
    /**
     * 脚本错误码
     */
    private Integer errorCode;
    /**
     * 脚本执行输出
     */
    private String tag;

    public void calculateTotalTime() {
        if (this.endTime != null && this.startTime != null && this.endTime > this.startTime) {
            this.totalTime = this.endTime - this.startTime;
        }
    }
}
