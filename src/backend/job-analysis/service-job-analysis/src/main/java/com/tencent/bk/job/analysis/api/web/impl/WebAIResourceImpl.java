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

package com.tencent.bk.job.analysis.api.web.impl;

import com.tencent.bk.job.analysis.api.web.WebAIResource;
import com.tencent.bk.job.analysis.model.dto.AIChatHistoryDTO;
import com.tencent.bk.job.analysis.model.web.req.AIAnalyzeErrorReq;
import com.tencent.bk.job.analysis.model.web.req.AICheckScriptReq;
import com.tencent.bk.job.analysis.model.web.req.AIGeneralChatReq;
import com.tencent.bk.job.analysis.model.web.resp.AIAnswer;
import com.tencent.bk.job.analysis.model.web.resp.AIChatRecord;
import com.tencent.bk.job.analysis.model.web.resp.ClearChatHistoryResp;
import com.tencent.bk.job.analysis.model.web.resp.UserInput;
import com.tencent.bk.job.analysis.service.ai.AIAnalyzeErrorService;
import com.tencent.bk.job.analysis.service.ai.AIChatHistoryService;
import com.tencent.bk.job.analysis.service.ai.AICheckScriptService;
import com.tencent.bk.job.analysis.service.ai.ChatService;
import com.tencent.bk.job.analysis.service.ai.impl.AIConfigService;
import com.tencent.bk.job.analysis.service.ai.impl.AIMessageI18nService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Primary
@RestController("jobAnalysisWebAIResource")
@Slf4j
public class WebAIResourceImpl implements WebAIResource {

    private final AIConfigService aiConfigService;
    private final ChatService chatService;
    private final AICheckScriptService aiCheckScriptService;
    private final AIAnalyzeErrorService aiAnalyzeErrorService;
    private final AIChatHistoryService aiChatHistoryService;
    private final AIMessageI18nService aiMessageI18nService;

    @Autowired
    public WebAIResourceImpl(AIConfigService aiConfigService,
                             ChatService chatService,
                             AICheckScriptService aiCheckScriptService,
                             AIAnalyzeErrorService aiAnalyzeErrorService,
                             AIChatHistoryService aiChatHistoryService,
                             AIMessageI18nService aiMessageI18nService) {
        this.aiConfigService = aiConfigService;
        this.chatService = chatService;
        this.aiCheckScriptService = aiCheckScriptService;
        this.aiAnalyzeErrorService = aiAnalyzeErrorService;
        this.aiChatHistoryService = aiChatHistoryService;
        this.aiMessageI18nService = aiMessageI18nService;
    }

    @Override
    public Response<Map<String, Object>> getAIConfig(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId) {
        return Response.buildSuccessResp(aiConfigService.getAIConfig());
    }

    @Override
    public Response<List<AIChatRecord>> getLatestChatHistoryList(String username,
                                                                 AppResourceScope appResourceScope,
                                                                 String scopeType,
                                                                 String scopeId,
                                                                 Integer start,
                                                                 Integer length) {
        List<AIChatHistoryDTO> chatRecordList = chatService.getLatestChatHistoryList(username, start, length);
        List<AIChatRecord> aiChatRecordList = new ArrayList<>();
        aiChatRecordList.add(getGreetingRecord());
        aiChatRecordList.addAll(
            chatRecordList.stream().map(AIChatHistoryDTO::toAIChatRecord).collect(Collectors.toList())
        );
        return Response.buildSuccessResp(aiChatRecordList);
    }

    private AIChatRecord getGreetingRecord() {
        AIChatRecord greetingRecord = new AIChatRecord();
        greetingRecord.setUserInput(new UserInput("", 0L));
        greetingRecord.setAiAnswer(getGreetingAIAnswer());
        return greetingRecord;
    }

    private AIAnswer getGreetingAIAnswer() {
        return new AIAnswer(
            "0",
            null,
            aiMessageI18nService.getAIGreetingMessage(),
            0L
        );
    }

    @Override
    public Response<AIAnswer> generalChat(String username,
                                          AppResourceScope appResourceScope,
                                          String scopeType,
                                          String scopeId,
                                          AIGeneralChatReq req) {
        AIAnswer aiAnswer = chatService.chatWithAI(username, req.getContent());
        return Response.buildSuccessResp(aiAnswer);
    }

    @Override
    public Response<AIAnswer> checkScript(String username,
                                          AppResourceScope appResourceScope,
                                          String scopeType,
                                          String scopeId,
                                          AICheckScriptReq req) {
        AIAnswer aiAnswer = aiCheckScriptService.check(username, req.getType(), req.getContent());
        return Response.buildSuccessResp(aiAnswer);
    }

    @Override
    public Response<AIAnswer> analyzeError(String username,
                                           AppResourceScope appResourceScope,
                                           String scopeType,
                                           String scopeId,
                                           AIAnalyzeErrorReq req) {
        AIAnswer aiAnswer = aiAnalyzeErrorService.analyze(username, appResourceScope.getAppId(), req);
        return Response.buildSuccessResp(aiAnswer);
    }

    @Override
    public Response<ClearChatHistoryResp> clearChatHistory(String username,
                                                           AppResourceScope appResourceScope,
                                                           String scopeType,
                                                           String scopeId) {
        int deletedTotalCount = aiChatHistoryService.softDeleteChatHistory(username);
        return Response.buildSuccessResp(new ClearChatHistoryResp(deletedTotalCount));
    }

}
