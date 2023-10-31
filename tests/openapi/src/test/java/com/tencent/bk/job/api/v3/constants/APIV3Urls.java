package com.tencent.bk.job.api.v3.constants;

/**
 * V3 版本 API URL
 */
public interface APIV3Urls {
    String CREATE_SCRIPT = "/api/job/v3/job-manage/create_script";
    String GET_SCRIPT_LIST = "/api/job/v3/job-manage/get_script_list";
    String UPDATE_SCRIPT_BASIC = "/api/job/v3/job-manage/update_script_basic";
    String DELETE_SCRIPT = "/api/job/v3/job-manage/delete_script";
    String CREATE_SCRIPT_VERSION = "/api/job/v3/job-manage/create_script_version";
    String GET_SCRIPT_VERSION_LIST = "/api/job/v3/job-manage/get_script_version_list";
    String GET_SCRIPT_VERSION_DETAIL = "/api/job/v3/job-manage/get_script_version_detail";
    String UPDATE_SCRIPT_VERSION = "/api/job/v3/job-manage/update_script_version";
    String DELETE_SCRIPT_VERSION = "/api/job/v3/job-manage/delete_script_version";
    String PUBLISH_SCRIPT_VERSION = "/api/job/v3/job-manage/publish_script_version";
    String DISABLE_SCRIPT_VERSION = "/api/job/v3/job-manage/disable_script_version";

    String FAST_EXECUTE_SCRIPT = "/api/job/v3/job-execute/get_script_list";
    String FAST_TRANSFER_FILE = "/api/job/v3/job-execute/get_script_list";
    String EXECUTE_JOB_PLAN = "/api/job/v3/job-execute/get_script_list";
}
