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

package com.tencent.bk.job.common.cc.model.result;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.StringUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Data
public class HostEventDetail {

    @JsonProperty("bk_host_id")
    private Long hostId;

    @JsonProperty("bk_host_innerip")
    private String hostInnerIp;

    @JsonProperty("bk_host_innerip_v6")
    private String innerIpv6;

    @JsonProperty("bk_agent_id")
    private String agentId;

    @JsonProperty("bk_host_name")
    private String hostName;

    @JsonProperty("bk_os_name")
    private String osName;

    @JsonProperty("bk_os_type")
    private String osType;

    @JsonProperty("bk_cloud_id")
    private String cloudId;

    @JsonProperty("bk_cloud_vendor")
    private String cloudVendorId;

    public static ApplicationHostDTO toHostInfoDTO(HostEventDetail eventDetail) {
        ApplicationHostDTO hostInfoDTO = new ApplicationHostDTO();
        hostInfoDTO.setHostId(eventDetail.hostId);
        List<String> ipList = StringUtil.strToList(eventDetail.hostInnerIp, String.class, ",");
        hostInfoDTO.setDisplayIp(eventDetail.hostInnerIp);
        String ipv6 = eventDetail.innerIpv6;
        int ipv6MaxLength = 2000;
        if (ipv6 != null && ipv6.length() > ipv6MaxLength) {
            log.warn("trunc ipv6 with ipv6MaxLength {}, ipv6={}, event={}", ipv6MaxLength, ipv6, eventDetail);
            ipv6 = ipv6.substring(0, ipv6MaxLength);
        }
        hostInfoDTO.setIpv6(ipv6);
        hostInfoDTO.setAgentId(eventDetail.agentId);
        hostInfoDTO.setIpList(ipList);
        if (ipList != null && !ipList.isEmpty()) {
            hostInfoDTO.setIp(ipList.get(0));
        }
        hostInfoDTO.setHostName(eventDetail.hostName);
        hostInfoDTO.setOsName(eventDetail.osName);
        hostInfoDTO.setOsType(eventDetail.osType);
        hostInfoDTO.setCloudAreaId(Long.parseLong(eventDetail.getCloudId()));
        hostInfoDTO.setCloudVendorId(eventDetail.cloudVendorId);
        return hostInfoDTO;
    }
}
