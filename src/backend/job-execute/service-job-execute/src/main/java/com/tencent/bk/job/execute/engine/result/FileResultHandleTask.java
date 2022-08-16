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

package com.tencent.bk.job.execute.engine.result;

import com.google.common.collect.Sets;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.gse.GseClient;
import com.tencent.bk.job.common.gse.constants.FileDistModeEnum;
import com.tencent.bk.job.common.gse.constants.GSECode;
import com.tencent.bk.job.common.gse.v2.model.AtomicFileTaskResult;
import com.tencent.bk.job.common.gse.v2.model.AtomicFileTaskResultContent;
import com.tencent.bk.job.common.gse.v2.model.FileTaskResult;
import com.tencent.bk.job.common.gse.v2.model.GetTransferFileResultRequest;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.execute.engine.consts.AgentTaskStatusEnum;
import com.tencent.bk.job.execute.engine.consts.FileDirTypeConf;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.model.FileGseTaskResult;
import com.tencent.bk.job.execute.engine.model.GseLogBatchPullResult;
import com.tencent.bk.job.execute.engine.model.GseTaskExecuteResult;
import com.tencent.bk.job.execute.engine.model.GseTaskResult;
import com.tencent.bk.job.execute.engine.model.JobFile;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.engine.util.GseUtils;
import com.tencent.bk.job.execute.engine.util.NFSUtils;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.FileAgentTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceHostLogDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件任务执行结果处理
 */
@Slf4j
public class FileResultHandleTask extends AbstractResultHandleTask<FileTaskResult> {
    /**
     * GSE 源 Agent 任务, Map<AgentId,AgentTask>
     */
    private final Map<String, AgentTaskDTO> sourceAgentTaskMap;
    /**
     * 待分发文件，文件传输的源文件
     */
    private final Set<JobFile> sendFiles;
    /**
     * 任务包含的源服务器
     */
    private final Set<String> sourceAgentIds = new HashSet<>();
    /**
     * 已经分析结果完成的源服务器
     */
    protected Set<String> analyseFinishedSourceAgentIds = new HashSet<>();
    /**
     * 成功的文件下载任务，key: agentId, value: 成功的文件任务名称
     */
    private final Map<String, Set<String>> successDownloadFileMap = new HashMap<>();
    /**
     * 已经结束的文件下载任务，key: agentId, value: 已完成文件任务名称
     */
    private final Map<String, Set<String>> finishedDownloadFileMap = new HashMap<>();
    /**
     * 下载文件任务数，key: agentId, value: 主机对应的文件任务数
     */
    private final Map<String, Integer> fileDownloadTaskNumMap = new HashMap<>();
    /**
     * 成功的文件上传任务，key: agentId, value: 成功的文件任务名称
     */
    private final Map<String, Set<String>> successUploadFileMap = new HashMap<>();
    /**
     * 已经结束的文件上传任务，key: agentId, value: 已完成文件任务名称
     */
    private final Map<String, Set<String>> finishedUploadFileMap = new HashMap<>();
    /**
     * 上传文件任务数，key: agentId, value: 主机对应的文件任务数
     */
    private final Map<String, Integer> fileUploadTaskNumMap = new HashMap<>();
    /**
     * 文件任务进度表
     */
    private final Map<String, Integer> processMap = new HashMap<>();
    /**
     * 本地文件上传目录
     */
    private final String localUploadDir;
    /**
     * 分发的源文件路径与分发之后的目标文件路径的映射关系
     */
    private final Map<String, String> sourceDestPathMap;
    /**
     * 源文件真实路径与显示路径的映射关系，用于本地文件分发场景下的真实路径隐藏
     */
    private final Map<String, String> sourceFileDisplayMap;
    /**
     * 下载全部结束的时间
     */
    private long downloadFinishedTime = 0;
    /**
     * 未开始任务的文件源服务器
     */
    private final Set<String> notStartedFileSourceAgentIds = new HashSet<>();
    /**
     * 正在执行任务的文件源服务器
     */
    private final Set<String> runningFileSourceAgentIds = new HashSet<>();
    /**
     * 文件任务执行结果处理调度策略
     */
    private volatile ScheduleStrategy scheduleStrategy;
    /**
     * 任务基本信息，用于日志输出
     */
    private String taskInfo;

    private Map<String, Long> ipAndCloudIMapping = null;

    public FileResultHandleTask(TaskInstanceService taskInstanceService,
                                GseTaskService gseTaskService,
                                LogService logService,
                                TaskInstanceVariableService taskInstanceVariableService,
                                StepInstanceVariableValueService stepInstanceVariableValueService,
                                TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager,
                                TaskEvictPolicyExecutor taskEvictPolicyExecutor,
                                FileAgentTaskService fileAgentTaskService,
                                StepInstanceService stepInstanceService,
                                GseClient gseClient,
                                TaskInstanceDTO taskInstance,
                                StepInstanceDTO stepInstance,
                                TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                Map<String, AgentTaskDTO> targetAgentTaskMap,
                                Map<String, AgentTaskDTO> sourceAgentTaskMap,
                                GseTaskDTO gseTask,
                                Set<JobFile> sendFiles,
                                String storageRootPath,
                                Map<String, String> sourceDestPathMap,
                                Map<String, String> sourceFileDisplayMap,
                                String requestId) {
        super(taskInstanceService,
            gseTaskService,
            logService,
            taskInstanceVariableService,
            stepInstanceVariableValueService,
            taskExecuteMQEventDispatcher,
            resultHandleTaskKeepaliveManager,
            taskEvictPolicyExecutor,
            fileAgentTaskService,
            stepInstanceService,
            gseClient,
            taskInstance,
            stepInstance,
            taskVariablesAnalyzeResult,
            targetAgentTaskMap,
            gseTask,
            requestId);
        this.sourceAgentTaskMap = sourceAgentTaskMap;
        this.sendFiles = sendFiles;
        this.localUploadDir = NFSUtils.getFileDir(storageRootPath, FileDirTypeConf.UPLOAD_FILE_DIR);
        if (sourceDestPathMap != null) {
            this.sourceDestPathMap = sourceDestPathMap;
        } else {
            this.sourceDestPathMap = new HashMap<>();
        }
        this.sourceFileDisplayMap = sourceFileDisplayMap;
        initFileTaskNumMap();
        initSourceAgentIds();

        log.info("InitFileResultHandleTask|stepInstanceId: {}|sendFiles: {}|fileSourceAgentIds: {}|targetAgentIds: {}|"
                + "fileUploadTaskNumMap: {}|fileDownloadTaskNumMap: {}",
            stepInstance.getId(), sendFiles, sourceAgentIds, targetAgentIds, fileUploadTaskNumMap,
            fileDownloadTaskNumMap);
    }


    /**
     * 初始化文件任务计数器
     */
    private void initFileTaskNumMap() {
        for (String agentId : this.targetAgentIds) {
            this.fileDownloadTaskNumMap.put(agentId, this.sendFiles.size());
        }

        for (JobFile sendFile : this.sendFiles) {
            String agentId = sendFile.getHost().getAgentId();
            this.fileUploadTaskNumMap.put(agentId, this.fileUploadTaskNumMap.get(agentId) == null ? 1 :
                (this.fileUploadTaskNumMap.get(agentId) + 1));
        }
    }

    private void initSourceAgentIds() {
        sourceAgentTaskMap.values().forEach(agentTask -> {
            this.notStartedFileSourceAgentIds.add(agentTask.getAgentId());
            this.sourceAgentIds.add(agentTask.getAgentId());
        });
    }

    @Override
    GseLogBatchPullResult<FileTaskResult> pullGseTaskResultInBatches() {
        GetTransferFileResultRequest request = new GetTransferFileResultRequest();
        request.setTaskId(gseTask.getGseTaskId());

        if (CollectionUtils.isNotEmpty(this.analyseFinishedSourceAgentIds)
            || CollectionUtils.isNotEmpty(this.analyseFinishedTargetAgentIds)) {
            List<String> notFinishedAgentIds = new ArrayList<>();
            notFinishedAgentIds.addAll(notStartedFileSourceAgentIds);
            notFinishedAgentIds.addAll(runningFileSourceAgentIds);
            notFinishedAgentIds.addAll(notStartedTargetAgentIds);
            notFinishedAgentIds.addAll(runningTargetAgentIds);
            request.setAgentIds(notFinishedAgentIds.stream().distinct().collect(Collectors.toList()));
        }
        FileTaskResult result = gseClient.getTransferFileResult(request);
        GseLogBatchPullResult<FileTaskResult> pullResult;
        if (result != null) {
            pullResult = new GseLogBatchPullResult<>(
                true, true, new FileGseTaskResult(result), null);
        } else {
            pullResult = new GseLogBatchPullResult<>(true, true, null, null);
        }
        return pullResult;
    }

    @Override
    GseTaskExecuteResult analyseGseTaskResult(GseTaskResult<FileTaskResult> taskDetail) {
        correctSourceAgentId(taskDetail.getResult());
        // 执行日志, Map<hostKey, 日志>
        Map<String, ServiceHostLogDTO> executionLogs = new HashMap<>();

        StopWatch watch = new StopWatch("analyse-gse-file-task");
        watch.start("analyse");
        for (AtomicFileTaskResult result : taskDetail.getResult().getAtomicFileTaskResults()) {

            if (!shouldAnalyse(result)) {
                continue;
            }

            AtomicFileTaskResultContent content = result.getContent();

            boolean isDownloadResult = content.isDownloadMode();
            String agentId = isDownloadResult ? content.getDestAgentId() : content.getSourceAgentId();

            if (isDownloadResult && this.targetAgentIds.contains(agentId)) {
                this.runningTargetAgentIds.add(agentId);
            } else if (!isDownloadResult && this.sourceAgentIds.contains(agentId)) {
                this.runningFileSourceAgentIds.add(agentId);
            }

            analyseFileResult(agentId, result, executionLogs, isDownloadResult);
        }
        watch.stop();

        // 保存文件分发日志
        watch.start("saveFileLog");
        writeFileTaskLogContent(executionLogs);
        watch.stop();

        // 保存任务执行结果
        watch.start("saveAgentTasks");
        batchSaveChangedGseAgentTasks();
        watch.stop();

        log.info("Analyse gse task log [{}] -> runningTargetAgentIds={}, " +
                "notStartedTargetAgentIds={}, runningFileSourceAgentIds={}, notStartedFileSourceAgentIds={}, " +
                "analyseFinishedTargetAgentIds={}, analyseFinishedSourceAgentIds={}, finishedDownloadFileMap={}, " +
                "successDownloadFileMap={}, finishedUploadFileMap={}, successUploadFileMap={}",
            this.stepInstanceId,
            this.runningTargetAgentIds,
            this.notStartedTargetAgentIds,
            this.runningFileSourceAgentIds,
            this.notStartedFileSourceAgentIds,
            this.analyseFinishedTargetAgentIds,
            this.analyseFinishedSourceAgentIds,
            this.finishedDownloadFileMap,
            this.successDownloadFileMap,
            this.finishedUploadFileMap,
            this.successUploadFileMap
        );

        if (watch.getTotalTimeMillis() > 1000L) {
            log.info("Analyse file gse task is slow, statistics: {}", watch.prettyPrint());
        }
        return analyseExecuteResult();
    }

    /**
     * GSE 1.0 BUG(GSE Agent 版本低于1.7.6), download结果可能没有源云区域ID，需要处理
     */
    @CompatibleImplementation(name = "ipv6", explain = "GSE 1.0 BUG(GSE Agent 版本低于1.7.6)", version = "> 3.7")
    private void correctSourceAgentId(FileTaskResult fileTaskResult) {
        for (AtomicFileTaskResult result : fileTaskResult.getAtomicFileTaskResults()) {
            if (result.getContent() != null && isDownloadResult(result.getContent().getMode())) {
                String sourceAgentId = result.getContent().getSourceAgentId();
                // 没有云区域ID, 尝试结合文件源主机推测(ip匹配)
                if (sourceAgentId.startsWith(":")) {
                    log.warn("[{}] Invalid sourceAgentId: {}", stepInstanceId, sourceAgentId);
                    String ip = sourceAgentId.substring(1);
                    if (ipAndCloudIMapping == null) {
                        initSourceIpAndCloudIdMapping();
                    }
                    Long bkCloudId = ipAndCloudIMapping.get(ip);
                    result.getContent().setSourceAgentId(bkCloudId + ":" + ip);
                    result.getContent().setTaskId(result.getContent().buildTaskId());
                }
            }
        }
    }

    private void initSourceIpAndCloudIdMapping() {
        if (ipAndCloudIMapping == null) {
            ipAndCloudIMapping = new HashMap<>();
            for (String agentId : sourceAgentIds) {
                String[] props = agentId.split(":");
                if (props.length == 2) {
                    ipAndCloudIMapping.put(props[1], Long.valueOf(props[0]));
                }
            }
        }
    }

    private void analyseFileResult(String agentId,
                                   AtomicFileTaskResult result,
                                   Map<String, ServiceHostLogDTO> executionLogs,
                                   boolean isDownloadResult) {
        AtomicFileTaskResultContent content = result.getContent();
        AgentTaskDTO agentTask = getAgentTask(isDownloadResult, agentId);
        if (agentTask.getStartTime() == null) {
            agentTask.setStartTime(System.currentTimeMillis());
        }
        agentTask.setErrorCode(result.getErrorCode());
        GSECode.AtomicErrorCode errorCode = GSECode.AtomicErrorCode.getErrorCode(result.getErrorCode());
        switch (errorCode) {
            case RUNNING:
                parseExecutionLog(result, executionLogs);
                agentTask.setStatus(AgentTaskStatusEnum.RUNNING);
                if (isDownloadResult) {
                    this.notStartedTargetAgentIds.remove(agentId);
                }
                break;
            case FINISHED:
                parseExecutionLog(result, executionLogs);
                if (content.getProgress() == 100) {
                    if (isDownloadResult) {
                        addFinishedFile(true, true,
                            content.getDestAgentId(), content.getTaskId());
                    } else {
                        addFinishedFile(true, false,
                            content.getSourceAgentId(), content.getTaskId());
                    }
                    // 分析日志，更新successAgentIds、notStartedAgentIds等状态集合
                    analyseAgentTaskResult(errorCode.getValue(), agentId, content.getStartTime(),
                        content.getEndTime(), isDownloadResult);
                } else {
                    agentTask.setStatus(AgentTaskStatusEnum.RUNNING);
                    this.notStartedTargetAgentIds.remove(agentId);
                }
                break;
            case TERMINATE:
                parseExecutionLog(result, executionLogs);
                if (isDownloadResult) {
                    addFinishedFile(false, true,
                        content.getDestAgentId(), content.getTaskId());
                } else {
                    addFinishedFile(false, false,
                        content.getSourceAgentId(), content.getTaskId());
                }
                analyseAgentTaskResult(errorCode.getValue(), agentId, content.getStartTime(),
                    content.getEndTime(), isDownloadResult);
                this.isTerminatedSuccess = true;
                break;
            default:
                dealIpTaskFail(result, executionLogs, isDownloadResult);
                break;
        }
    }

    private AgentTaskDTO getAgentTask(boolean isDownloadResult, String agentId) {
        if (isDownloadResult) {
            return targetAgentTasks.get(agentId);
        } else {
            return sourceAgentTaskMap.get(agentId);
        }
    }

    /*
     * 分析执行结果
     *
     * @return 任务执行结果
     */
    private GseTaskExecuteResult analyseExecuteResult() {
        GseTaskExecuteResult rst;
        // 目标下载全部完成
        if (this.notStartedTargetAgentIds.isEmpty() && this.runningTargetAgentIds.isEmpty()) {
            // 源上传全部完成
            if (this.notStartedFileSourceAgentIds.isEmpty() && this.runningFileSourceAgentIds.isEmpty()) {
                int successTargetIpNum = this.successTargetAgentIds.size();
                int targetIPNum = this.targetAgentIds.size();
                boolean isSuccess = !stepInstance.hasInvalidHost() && successTargetIpNum == targetIPNum;
                if (isSuccess) {
                    rst = GseTaskExecuteResult.SUCCESS;
                } else {
                    if (this.isTerminatedSuccess) {
                        rst = GseTaskExecuteResult.STOP_SUCCESS;
                    } else {
                        rst = GseTaskExecuteResult.FAILED;
                    }
                }
                log.info("[{}] AnalyseExecuteResult-> Result: finished. All source and target ip have completed tasks",
                    this.stepInstanceId);
            } else {
                // 场景：下载任务已全部结束，但是GSE未更新上传任务的状态。如果超过15s没有结束上传任务，那么任务结束
                if (this.downloadFinishedTime == 0) {
                    this.downloadFinishedTime = System.currentTimeMillis();
                }
                if (System.currentTimeMillis() - this.downloadFinishedTime > 15_000L) {
                    int targetIPNum = this.targetAgentIds.size();
                    int successTargetIpNum = this.successTargetAgentIds.size();
                    boolean isSuccess = !stepInstance.hasInvalidHost() && successTargetIpNum == targetIPNum;
                    rst = isSuccess ? GseTaskExecuteResult.SUCCESS : GseTaskExecuteResult.FAILED;
                    log.info("[{}] AnalyseExecuteResult-> Result: finished. Download tasks are finished, " +
                            "but upload tasks are not finished after 15 seconds. Ignore upload tasks",
                        this.stepInstanceId);
                } else {
                    rst = GseTaskExecuteResult.RUNNING;
                    log.info("[{}] AnalyseExecuteResult-> Result: running. Download tasks are finished, " +
                        "wait for upload task to complete.", this.stepInstanceId);
                }
            }

        } else {
            rst = GseTaskExecuteResult.RUNNING;
            log.info("[{}] AnalyseExecuteResult-> Result: running. Download tasks has not been finished",
                this.stepInstanceId);
        }
        return rst;
    }

    private boolean shouldAnalyse(AtomicFileTaskResult result) {
        if (result == null) {
            return false;
        }

        AtomicFileTaskResultContent content = result.getContent();
        boolean isDownloadMode = content.isDownloadMode();
        String agentId = isDownloadMode ?
            content.getDestAgentId() : content.getSourceAgentId();
        boolean shouldAnalyse = true;
        if (isDownloadMode) {
            if (this.analyseFinishedTargetAgentIds.contains(agentId) // 该ip已经日志分析结束，不要再分析
                // 该文件下载任务已结束
                || (this.finishedDownloadFileMap.get(agentId) != null
                && this.finishedDownloadFileMap.get(agentId).contains(content.getTaskId()))
                // 非目标IP
                || !this.fileDownloadTaskNumMap.containsKey(agentId)) {
                shouldAnalyse = false;
            }
        } else {
            if ((this.finishedUploadFileMap.get(agentId) != null
                && this.finishedUploadFileMap.get(agentId).contains(content.getTaskId()))
                // 非源IP
                || !this.fileUploadTaskNumMap.containsKey(agentId)) {
                shouldAnalyse = false;
            }
        }
        return shouldAnalyse;
    }

    private void recordDownloadFileFail(AtomicFileTaskResult result,
                                        Map<String, ServiceHostLogDTO> executionLogs) {
        AtomicFileTaskResultContent content = result.getContent();
        String destAgentId = content.getDestAgentId();
        String sourceAgentId = content.getSourceAgentId();
        AgentTaskDTO destAgentTask = targetAgentTasks.get(destAgentId);
        AgentTaskDTO sourceAgentTask = sourceAgentTaskMap.get(sourceAgentId);
        boolean isAddSuccess = addFinishedFile(false, true, destAgentId,
            AtomicFileTaskResultContent.buildTaskId(content.getMode(), sourceAgentId,
                content.getStandardSourceFilePath(), destAgentId, content.getStandardDestFilePath()));
        if (isAddSuccess) {
            addFileTaskLog(executionLogs,
                new ServiceFileTaskLogDTO(
                    FileDistModeEnum.DOWNLOAD.getValue(),
                    destAgentTask.getHostId(),
                    agentIdHostMap.get(destAgentTask.getAgentId()).getIp(),
                    content.getStandardDestFilePath(),
                    sourceAgentTask.getHostId(),
                    agentIdHostMap.get(sourceAgentTask.getAgentId()).getIp(),
                    agentIdHostMap.get(sourceAgentTask.getAgentId()).getIp(),
                    content.getStandardSourceFilePath(),
                    content.getStandardSourceFilePath() == null ? null :
                        sourceFileDisplayMap.get(content.getStandardSourceFilePath()), null,
                    FileDistStatusEnum.FAILED.getValue(),
                    FileDistStatusEnum.FAILED.getName(),
                    null,
                    null,
                    result.getErrorMsg())
            );
        }
    }

    private long getStartTimeOrDefault(AtomicFileTaskResultContent content) {
        return (content != null && content.getStartTime() != null && content.getStartTime() > 0) ?
            content.getStartTime() : System.currentTimeMillis();
    }

    private long getEndTimeOrDefault(AtomicFileTaskResultContent content) {
        return (content != null && content.getEndTime() != null && content.getEndTime() > 0) ?
            content.getEndTime() : System.currentTimeMillis();
    }

    private void dealIpTaskFail(AtomicFileTaskResult result,
                                Map<String, ServiceHostLogDTO> executionLogs,
                                boolean isDownloadResult) {
        long startTime = getStartTimeOrDefault(result.getContent());
        long endTime = getEndTimeOrDefault(result.getContent());
        AtomicFileTaskResultContent content = result.getContent();
        if (isDownloadResult) {
            String destAgentId = content.getDestAgentId();
            recordDownloadFileFail(result, executionLogs);
            analyseAgentTaskResult(result.getErrorCode(), destAgentId, startTime, endTime, true);
        } else {
            // 上传源IP本身处理
            String sourceAgentId = content.getSourceAgentId();
            analyseAgentTaskResult(result.getErrorCode(), sourceAgentId, startTime, endTime, false);

            // 如果上传源失败，除了影响上传，还会影响到下载的目标IP
            Set<String> affectedTargetIps = new HashSet<>();
            dealUploadFail(result, executionLogs, affectedTargetIps);
            for (String affectedTargetIp : affectedTargetIps) {
                analyseAgentTaskResult(0, affectedTargetIp, startTime, endTime, true);
            }
        }
    }

    /**
     * 根据copyFileRsp内容填充executionLogs与affectIps
     *
     * @param result                 GSE响应内容
     * @param executionLogs          执行日志总Map
     * @param affectedTargetAgentIds 受影响的目标Agent集合
     */
    private void dealUploadFail(AtomicFileTaskResult result, Map<String, ServiceHostLogDTO> executionLogs,
                                Set<String> affectedTargetAgentIds) {
        AtomicFileTaskResultContent content = result.getContent();
        String sourceAgentId = content.getSourceAgentId();
        HostDTO sourceHost = agentIdHostMap.get(sourceAgentId);
        // 记录源IP单个文件上传任务的结束状态
        addFinishedFile(false, false, content.getSourceAgentId(), content.getTaskId());

        String sourceFilePath = content.getStandardSourceFilePath();
        String displayFilePath = sourceFileDisplayMap.get(sourceFilePath);
        boolean isLocalUploadFile = sourceFilePath.startsWith(this.localUploadDir);
        log.debug("StandardSourceFilePath: {}, localUploadDir: {}, isLocalUploadFile: {}, displayFilePath: {}",
            sourceFilePath, this.localUploadDir, isLocalUploadFile, displayFilePath);

        // 增加一条上传源失败的上传日志
        addFileTaskLog(executionLogs,
            new ServiceFileTaskLogDTO(
                FileDistModeEnum.UPLOAD.getValue(),
                null,
                null,
                null,
                sourceHost.getHostId(),
                sourceHost.getIp(),
                sourceHost.getDisplayIp(),
                sourceFilePath,
                displayFilePath,
                null,
                FileDistStatusEnum.FAILED.getValue(),
                FileDistStatusEnum.FAILED.getName(),
                null,
                null,
                result.getErrorMsg())
        );

        // 源失败了，会影响所有目标IP对应的agent上的download任务
        for (String targetAgentId : this.targetAgentIds) {
            String destFilePath;
            HostDTO targetHost = agentIdHostMap.get(targetAgentId);
            if (isLocalUploadFile) {
                destFilePath = this.sourceDestPathMap.get(displayFilePath);
            } else {
                destFilePath =
                    this.sourceDestPathMap.get(
                        content.getSourceAgentId() + ":" + content.getStandardSourceFilePath());
            }
            // 记录目标IP单个文件下载任务的结束状态
            addFinishedFile(false, true, targetAgentId,
                AtomicFileTaskResultContent.buildTaskId(content.getMode(), content.getSourceAgentId(),
                    content.getStandardSourceFilePath(), targetAgentId, destFilePath));
            // 每个目标IP增加一条下载失败的日志到日志总Map中
            addFileTaskLog(executionLogs,
                new ServiceFileTaskLogDTO(
                    FileDistModeEnum.DOWNLOAD.getValue(),
                    targetHost.getHostId(),
                    targetHost.getIp(),
                    content.getStandardDestFilePath(),
                    sourceHost.getHostId(),
                    sourceHost.getIp(),
                    sourceHost.getDisplayIp(),
                    content.getStandardSourceFilePath(),
                    content.getStandardSourceFilePath() == null ? null :
                        sourceFileDisplayMap.get(content.getStandardSourceFilePath()),
                    null,
                    FileDistStatusEnum.FAILED.getValue(),
                    FileDistStatusEnum.FAILED.getName(),
                    null,
                    null,
                    result.getErrorMsg())
            );
            affectedTargetAgentIds.add(targetAgentId);
        }
    }

    /**
     * 根据errorCode、fileNum、successNum更新successAgentIds状态集合与agentTask状态
     *
     * @param errorCode  GSE错误码
     * @param cloudIp    IP
     * @param fileNum    文件总数
     * @param successNum 成功分发的文件总数
     * @param isDownload 是否为下载过程
     * @param agentTask  ip对应日志
     */
    private void updateFinishedIpStatusAndLog(int errorCode,
                                              String cloudIp,
                                              int fileNum,
                                              int successNum,
                                              boolean isDownload,
                                              AgentTaskDTO agentTask) {
        boolean isTargetIp = targetAgentIds.contains(cloudIp);
        if (successNum >= fileNum) {
            // 每个文件都处理完了，才算IP完成执行
            if (isDownload && isTargetIp) {
                agentTask.setStatus(AgentTaskStatusEnum.SUCCESS);
                this.successTargetAgentIds.add(cloudIp);
            }
        } else {
            AgentTaskStatusEnum agentTaskStatus = AgentTaskStatusEnum.FAILED;
            if (errorCode != 0) {
                agentTaskStatus = GseUtils.getStatusByGseErrorCode(errorCode);
            }
            agentTask.setStatus(agentTaskStatus);
        }
    }

    /**
     * 分析日志，更新successAgentIds、notStartedAgentIds等状态集合，用于判定最终整体任务状态
     *
     * @param errorCode        GSE错误码
     * @param agentId          agentId
     * @param startTime        任务起始时间
     * @param endTime          任务终止时间
     * @param isDownloadResult 是否为下载结果
     */
    private void analyseAgentTaskResult(int errorCode, String agentId, long startTime, long endTime,
                                        boolean isDownloadResult) {
        int finishedNum;
        int fileNum;
        int successNum;
        if (isDownloadResult) {
            finishedNum = this.finishedDownloadFileMap.get(agentId) == null ? 0 :
                this.finishedDownloadFileMap.get(agentId).size();
            fileNum = this.fileDownloadTaskNumMap.get(agentId) == null ? 0 : this.fileDownloadTaskNumMap.get(agentId);
            successNum = this.successDownloadFileMap.get(agentId) == null ? 0 :
                this.successDownloadFileMap.get(agentId).size();
        } else {
            finishedNum = this.finishedUploadFileMap.get(agentId) == null ? 0 :
                this.finishedUploadFileMap.get(agentId).size();
            successNum = this.successUploadFileMap.get(agentId) == null ? 0 :
                this.successUploadFileMap.get(agentId).size();
            fileNum = this.fileUploadTaskNumMap.get(agentId) == null ? 0 : this.fileUploadTaskNumMap.get(agentId);
        }

        AgentTaskDTO agentTask = getAgentTask(isDownloadResult, agentId);
        if (finishedNum >= fileNum) {
            log.info("[{}] Ip analyse finished! ip: {}, finishedTaskNum: {}, expectedTaskNum: {}",
                stepInstanceId, agentId, finishedNum, fileNum);
            // 更新AgentTask结果
            if (isDownloadResult) {
                dealAgentFinish(agentId, startTime, endTime, agentTask);
            } else {
                dealUploadIpFinished(agentId);
            }
            updateFinishedIpStatusAndLog(errorCode, agentId, fileNum, successNum, isDownloadResult, agentTask);
        } else {
            agentTask.setStatus(AgentTaskStatusEnum.RUNNING);
            this.notStartedTargetAgentIds.remove(agentId);
        }
    }

    private void dealUploadIpFinished(String sourceCloudIp) {
        this.runningFileSourceAgentIds.remove(sourceCloudIp);
        this.notStartedFileSourceAgentIds.remove(sourceCloudIp);
        this.analyseFinishedSourceAgentIds.add(sourceCloudIp);
    }

    /*
     * 从执行结果生成执行日志
     */
    private void parseExecutionLog(AtomicFileTaskResult result, Map<String, ServiceHostLogDTO> executionLogs) {
        AtomicFileTaskResultContent content = result.getContent();
        Integer mode = content.getMode();
        boolean isDownloadResult = isDownloadResult(mode);
        GSECode.AtomicErrorCode errorCode = GSECode.AtomicErrorCode.getErrorCode(result.getErrorCode());
        String key = content.getTaskId();
        Integer process = processMap.computeIfAbsent(key, k -> -1);
        if (errorCode == GSECode.AtomicErrorCode.RUNNING && process.equals(content.getProgress())) {
            return;
        }
        processMap.put(key, content.getProgress());

        StringBuilder logContent = new StringBuilder();

        String filePath = isDownloadResult ? content.getStandardDestFilePath() :
            content.getStandardSourceFilePath();
        String displayFilePath = buildDisplayFilePath(isDownloadResult, filePath);
        if (filePath.endsWith("/") || filePath.endsWith("\\")) {
            // 传输的是目录，目录名以‘/’或‘\’结束
            logContent.append("Directory: ");
        } else {
            logContent.append("FileName: ");
        }
        logContent.append(displayFilePath);

        String fileSize = "--";
        String speed = "";
        String processText = "";

        if (content.getSize() != null && content.getSize() > 0) {
            // 兼容GSE不返回size的情况
            fileSize = GseUtils.tranByteReadable(content.getSize());
            logContent.append(" FileSize: ").append(fileSize);
        }
        if (content.getStatus() != null) {
            logContent.append(" Status: ").append(content.getStatus());
        }
        if (StringUtils.isNotEmpty(content.getStatusInfo())) {
            logContent.append(" StatusDesc: ").append(content.getStatusInfo());
        }
        if (content.getSpeed() != null) {
            speed = formatSpeed(content.getSpeed()) + " KB/s";
            logContent.append(" Speed: ").append(speed);
        }
        if (content.getProgress() != null) {
            processText = content.getProgress() + "%";
            logContent.append(" Progress: ").append(processText);
        }
        if (StringUtils.isNotBlank(result.getErrorMsg())) {
            logContent.append(" Detail: ").append(result.getErrorMsg());
        }
        String logContentStr = logContent.toString();

        FileDistStatusEnum status = parseFileTaskStatus(result, isDownloadResult);

        if (isDownloadResult) {
            HostDTO sourceHost = agentIdHostMap.get(content.getSourceAgentId());
            HostDTO targetHost = agentIdHostMap.get(content.getDestAgentId());
            addFileTaskLog(executionLogs,
                new ServiceFileTaskLogDTO(
                    FileDistModeEnum.DOWNLOAD.getValue(),
                    targetHost.getHostId(),
                    targetHost.toCloudIp(),
                    content.getStandardDestFilePath(),
                    sourceHost.getHostId(),
                    sourceHost.toCloudIp(),
                    sourceHost.toCloudIp(),
                    content.getStandardSourceFilePath(),
                    content.getStandardSourceFilePath() == null ? null :
                        sourceFileDisplayMap.get(content.getStandardSourceFilePath()),
                    fileSize,
                    status.getValue(),
                    status.getName(),
                    speed,
                    processText,
                    logContentStr)
            );
        } else {
            HostDTO sourceHost = agentIdHostMap.get(content.getSourceAgentId());
            addFileTaskLog(executionLogs,
                new ServiceFileTaskLogDTO(
                    FileDistModeEnum.UPLOAD.getValue(),
                    null,
                    null,
                    null,
                    sourceHost.getHostId(),
                    sourceHost.toCloudIp(),
                    sourceHost.toCloudIp(),
                    filePath,
                    displayFilePath,
                    fileSize,
                    status.getValue(),
                    status.getName(),
                    speed,
                    processText,
                    logContentStr)
            );
        }
    }

    private String buildDisplayFilePath(boolean isDownloadResult, String originFilePath) {
        String displayFilePath = originFilePath;
        if (!isDownloadResult) {
            displayFilePath = sourceFileDisplayMap.get(originFilePath);
        }
        return displayFilePath;
    }

    private FileDistStatusEnum parseFileTaskStatus(AtomicFileTaskResult result, boolean isDownloadResult) {
        FileDistStatusEnum status;
        if (result.getErrorCode().equals(GSECode.AtomicErrorCode.RUNNING.getValue())) {
            if (isDownloadResult) {
                status = FileDistStatusEnum.DOWNLOADING;
            } else {
                status = FileDistStatusEnum.UPLOADING;
            }
        } else if (result.getErrorCode().equals(GSECode.AtomicErrorCode.FINISHED.getValue())) {
            AtomicFileTaskResultContent content = result.getContent();
            if (content.getProgress() != null && content.getProgress() < 100) {
                if (isDownloadResult) {
                    status = FileDistStatusEnum.DOWNLOADING;
                } else {
                    status = FileDistStatusEnum.UPLOADING;
                }
            } else {
                status = FileDistStatusEnum.FINISHED;
            }

        } else {
            status = FileDistStatusEnum.FAILED;
        }
        return status;
    }

    private boolean isDownloadResult(Integer mode) {
        return FileDistModeEnum.DOWNLOAD.getValue().equals(mode);
    }

    private String formatSpeed(int speed) {
        DecimalFormat formatter = new DecimalFormat("###,###");
        return formatter.format(speed);
    }


    private void addFileTaskLog(Map<String, ServiceHostLogDTO> hostLogs, ServiceFileTaskLogDTO fileTaskLog) {
        boolean isDownloadResult = isDownloadResult(fileTaskLog.getMode());
        Long hostId = isDownloadResult ? fileTaskLog.getDestHostId() : fileTaskLog.getSrcHostId();
        // tmp: 需要兼容发布过程中只有ip，没有hostId的场景
        String cloudIp = isDownloadResult ? fileTaskLog.getDestIp() : fileTaskLog.getSrcIp();
        String hostKey = buildHostKey(hostId, cloudIp);
        ServiceHostLogDTO hostLog = hostLogs.get(hostKey);
        if (hostLog == null) {
            hostLog = new ServiceHostLogDTO();
            hostLog.setStepInstanceId(stepInstanceId);
            hostLog.setHostId(hostId);
            hostLog.setIp(cloudIp);
            hostLog.setBatch(stepInstance.getBatch());
            hostLog.setExecuteCount(stepInstance.getExecuteCount());
            hostLogs.put(hostKey, hostLog);
        }
        hostLog.addFileTaskLog(fileTaskLog);
    }

    private String buildHostKey(Long hostId, String cloudIp) {
        if (hostId != null) {
            // 优先使用hostId
            return "HOST_ID_" + hostId;
        } else {
            return "HOST_IP_" + cloudIp;
        }
    }

    private void writeFileTaskLogContent(Map<String, ServiceHostLogDTO> executionLogs) {
        if (!executionLogs.isEmpty()) {
            logService.writeFileLogs(taskInstance.getCreateTime(), new ArrayList<>(executionLogs.values()));
        }
    }

    /**
     * 向某个IP上传/下载文件的已结束状态Map、已成功状态Map中添加记录
     *
     * @param isSuccess      单个文件上传/下载任务是否成功
     * @param isDownloadMode 是否为下载
     * @param agentId        bk_agent_id
     * @param taskId         单个文件任务唯一Key
     * @return 是否添加成功
     */
    private boolean addFinishedFile(boolean isSuccess, boolean isDownloadMode, String agentId, String taskId) {
        if (isDownloadMode) {
            return addFinishedFile(isSuccess, agentId, taskId, finishedDownloadFileMap, successDownloadFileMap);
        } else {
            return addFinishedFile(isSuccess, agentId, taskId, finishedUploadFileMap, successUploadFileMap);
        }
    }

    private boolean addFinishedFile(boolean isSuccess, String agentId, String taskId,
                                    Map<String, Set<String>> finishedFileMap,
                                    Map<String, Set<String>> successFileMap) {
        Set<String> finishedFileSet = finishedFileMap.computeIfAbsent(agentId, k -> Sets.newHashSet());
        boolean isAdd = finishedFileSet.add(taskId);

        if (isSuccess) {
            Set<String> successFileSet = successFileMap.computeIfAbsent(agentId, k -> Sets.newHashSet());
            successFileSet.add(taskId);
        }
        return isAdd;
    }

    @Override
    public boolean isFinished() {
        return !getExecuteResult().getResultCode().equals(GseTaskExecuteResult.RESULT_CODE_RUNNING);
    }

    @Override
    public ScheduleStrategy getScheduleStrategy() {
        if (scheduleStrategy == null) {
            this.scheduleStrategy = new FileTaskResultHandleScheduleStrategy();
        }
        return this.scheduleStrategy;
    }

    @Override
    public String toString() {
        if (this.taskInfo == null) {
            this.taskInfo = "FileTaskResultHandle-" + stepInstance.getTaskInstanceId() + "-" + stepInstance.getId();
        }
        return this.taskInfo;
    }
}
