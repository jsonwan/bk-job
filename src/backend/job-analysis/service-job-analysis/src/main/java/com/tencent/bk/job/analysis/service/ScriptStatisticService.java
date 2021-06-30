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

package com.tencent.bk.job.analysis.service;

import com.tencent.bk.job.analysis.config.listener.StatisticConfig;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.model.web.ScriptCiteStatisticVO;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ScriptStatisticService extends CommonStatisticService {

    @Autowired
    public ScriptStatisticService(StatisticsDAO statisticsDAO, StatisticConfig statisticConfig,
                                  MetricResourceReslover metricResourceReslover, AppService appService) {
        super(statisticsDAO, statisticConfig, metricResourceReslover, appService);
    }

    public ScriptCiteStatisticVO scriptCiteInfo(List<Long> appIdList, String date) {
        List<StatisticsDTO> statisticsDTOList = statisticsDAO.getStatisticsList(appIdList, null,
            StatisticsConstants.RESOURCE_SCRIPT_CITE_INFO, StatisticsConstants.DIMENSION_SCRIPT_CITE_INFO_METRIC,
            StatisticsConstants.DIMENSION_VALUE_SCRIPT_CITE_INFO_METRIC_SCRIPT_COUNT, date);
        int scriptCount = 0;
        for (StatisticsDTO statisticsDTO : statisticsDTOList) {
            scriptCount += Integer.parseInt(statisticsDTO.getValue());
        }
        statisticsDTOList = statisticsDAO.getStatisticsList(appIdList, null,
            StatisticsConstants.RESOURCE_SCRIPT_CITE_INFO, StatisticsConstants.DIMENSION_SCRIPT_CITE_INFO_METRIC,
            StatisticsConstants.DIMENSION_VALUE_SCRIPT_CITE_INFO_METRIC_CITED_SCRIPT_COUNT, date);
        int citedScriptCount = 0;
        for (StatisticsDTO statisticsDTO : statisticsDTOList) {
            citedScriptCount += Integer.parseInt(statisticsDTO.getValue());
        }
        statisticsDTOList = statisticsDAO.getStatisticsList(appIdList, null,
            StatisticsConstants.RESOURCE_SCRIPT_CITE_INFO, StatisticsConstants.DIMENSION_SCRIPT_CITE_INFO_METRIC,
            StatisticsConstants.DIMENSION_VALUE_SCRIPT_CITE_INFO_METRIC_CITED_SCRIPT_STEP_COUNT, date);
        int citedScriptStepCount = 0;
        for (StatisticsDTO statisticsDTO : statisticsDTOList) {
            citedScriptStepCount += Integer.parseInt(statisticsDTO.getValue());
        }
        return new ScriptCiteStatisticVO(citedScriptCount, scriptCount, citedScriptStepCount);
    }
}
