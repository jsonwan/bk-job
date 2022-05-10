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

package com.tencent.bk.job.execute.engine.common;

import com.tencent.bk.job.logsvr.model.service.ServiceIpLogDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class ServiceLogHelper {

    /**
     * 从日志Map中获取/生成初始化日志
     *
     * @param logs           日志Map<IP,日志对象>
     * @param stepInstanceId 步骤ID
     * @param executeCount   执行次数
     * @param ip             机器IP
     * @return 日志对象
     */
    public ServiceIpLogDTO initServiceLogDTOIfAbsent(Map<String, ServiceIpLogDTO> logs,
                                                     long stepInstanceId,
                                                     int executeCount,
                                                     String ip) {
        ServiceIpLogDTO ipTaskLog = logs.get(ip);
        if (ipTaskLog == null) {
            ipTaskLog = new ServiceIpLogDTO();
            ipTaskLog.setStepInstanceId(stepInstanceId);
            ipTaskLog.setIp(ip);
            ipTaskLog.setExecuteCount(executeCount);
            logs.put(ip, ipTaskLog);
        }
        return ipTaskLog;
    }

}
