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

package com.tencent.bk.job.execute.engine.variable;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceVariableValuesDTO;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class VariableManager {

    private final TaskInstanceVariableService taskInstanceVariableService;
    private final StepInstanceVariableValueService stepInstanceVariableValueService;

    @Autowired
    public VariableManager(TaskInstanceVariableService taskInstanceVariableService,
                           StepInstanceVariableValueService stepInstanceVariableValueService) {
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.stepInstanceVariableValueService = stepInstanceVariableValueService;
    }

    /**
     * 根据步骤实例获取步骤输入变量
     *
     * @param stepInstance 步骤实例
     * @return 步骤输入变量
     */
    private StepInstanceVariableValuesDTO getStepInputVariables(StepInstanceDTO stepInstance) {
        List<TaskVariableDTO> taskVariables =
            taskInstanceVariableService.getByTaskInstanceId(stepInstance.getTaskInstanceId());
        TaskVariablesAnalyzeResult taskVariablesAnalyzeResult = new TaskVariablesAnalyzeResult(taskVariables);
        if (!taskVariablesAnalyzeResult.isExistAnyVar()) {
            return null;
        }
        return stepInstanceVariableValueService.computeInputStepInstanceVariableValues(
            stepInstance.getTaskInstanceId(), stepInstance.getId(), taskVariables);
    }

    public Map<String, String> buildReferenceGlobalVarValueMap(StepInstanceVariableValuesDTO stepInputVariables) {
        Map<String, String> globalVarValueMap = new HashMap<>();
        if (stepInputVariables == null || CollectionUtils.isEmpty(stepInputVariables.getGlobalParams())) {
            return globalVarValueMap;
        }
        stepInputVariables.getGlobalParams().forEach(globalParam -> {
            if (TaskVariableTypeEnum.valOf(globalParam.getType()) == TaskVariableTypeEnum.STRING) {
                globalVarValueMap.put(globalParam.getName(), globalParam.getValue());
            }
        });
        return globalVarValueMap;
    }

    public Map<String, String> buildReferenceGlobalVarValueMap(StepInstanceDTO stepInstance) {
        return buildReferenceGlobalVarValueMap(getStepInputVariables(stepInstance));
    }
}
