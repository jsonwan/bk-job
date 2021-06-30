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

package com.tencent.bk.job.backup.archive.impl;

import com.tencent.bk.job.backup.archive.AbstractArchivist;
import com.tencent.bk.job.backup.dao.ExecuteArchiveDAO;
import com.tencent.bk.job.backup.dao.JobExecuteDAO;
import com.tencent.bk.job.backup.service.ArchiveProgressService;
import org.jooq.generated.tables.StepInstanceFile;
import org.jooq.generated.tables.records.StepInstanceFileRecord;

import java.io.IOException;
import java.util.List;

/**
 * @since 18/3/2021 20:13
 */
public class StepInstanceFileArchivist extends AbstractArchivist<StepInstanceFileRecord> {

    public StepInstanceFileArchivist(JobExecuteDAO jobExecuteDAO,
                                     ExecuteArchiveDAO executeArchiveDAO,
                                     ArchiveProgressService archiveProgressService) {
        this.jobExecuteDAO = jobExecuteDAO;
        this.executeArchiveDAO = executeArchiveDAO;
        this.archiveProgressService = archiveProgressService;
        this.deleteIdStepSize = 10_000;
        this.setTableName("step_instance_file");
    }

    @Override
    public List<StepInstanceFileRecord> listRecord(Long start, Long stop) {
        return jobExecuteDAO.listStepInstanceFile(start, stop);
    }

    @Override
    protected int batchInsert(List<StepInstanceFileRecord> recordList) throws IOException {
        return executeArchiveDAO.batchInsert(jobExecuteDAO.getStepInstanceFileFields(), recordList, 500);
    }

    @Override
    protected int deleteRecord(Long start, Long stop) {
        return jobExecuteDAO.deleteStepInstanceFile(start, stop);
    }

    @Override
    protected long getFirstInstanceId() {
        return jobExecuteDAO.getFirstInstanceId(StepInstanceFile.STEP_INSTANCE_FILE,
            StepInstanceFile.STEP_INSTANCE_FILE.STEP_INSTANCE_ID);
    }
}
