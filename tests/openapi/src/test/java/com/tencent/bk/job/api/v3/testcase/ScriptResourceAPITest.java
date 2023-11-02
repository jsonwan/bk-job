package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.JobResourceStatusEnum;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.constant.ScriptTypeEnum;
import com.tencent.bk.job.api.model.EsbResp;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.Base64Util;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.util.TestValueGenerator;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbScriptVersionDetailV3DTO;
import com.tencent.bk.job.api.v3.model.request.EsbCreateScriptV3Request;
import com.tencent.bk.job.api.v3.model.request.EsbCreateScriptVersionV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbDeleteScriptV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbDeleteScriptVersionV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbGetScriptVersionListV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbManageScriptVersionV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbUpdateScriptBasicV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbUpdateScriptVersionV3Req;
import io.restassured.common.mapper.TypeRef;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.tencent.bk.job.api.constant.Constant.SHELL_SCRIPT_CONTENT_BASE64;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 业务脚本管理 API 测试
 */
@DisplayName("v3.ScriptResourceAPITest")
class ScriptResourceAPITest {

    private static final List<EsbScriptVersionDetailV3DTO> createdScriptList = new ArrayList<>();

    @AfterAll
    static void tearDown() {
        // 清理测试数据
        if (CollectionUtils.isNotEmpty(createdScriptList)) {
            createdScriptList.forEach(script -> {
                // 清理脚本
                EsbDeleteScriptV3Req req = new EsbDeleteScriptV3Req();
                req.setScopeId(script.getScopeId());
                req.setScopeType(script.getScopeType());
                req.setScriptId(script.getScriptId());
                Operations.deleteScript(req);
                // 清理脚本版本
                EsbDeleteScriptVersionV3Req req1 = new EsbDeleteScriptVersionV3Req();
                req1.setScopeId(script.getScopeId());
                req1.setScopeType(script.getScopeType());
                req1.setScriptId(script.getScriptId());
                req1.setScriptVersionId(script.getId());
                Operations.deleteScriptVersion(req1);
            });
        }
    }

    @Nested
    class CreateTest {
        @Test
        @DisplayName("测试脚本正常创建")
        void testCreateScript() {
            EsbCreateScriptV3Request req = new EsbCreateScriptV3Request();
            req.setContent(SHELL_SCRIPT_CONTENT_BASE64);
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setDescription(TestValueGenerator.generateUniqueStrValue("shell_script_desc", 50));
            req.setName(TestValueGenerator.generateUniqueStrValue("shell_script", 50));
            req.setType(ScriptTypeEnum.SHELL.getValue());
            req.setVersion("v1");
            req.setVersionDesc("v1_desc");

            EsbScriptVersionDetailV3DTO createdScript =
                given()
                    .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                    .body(JsonUtil.toJson(req))
                    .post(APIV3Urls.CREATE_SCRIPT)
                    .then()
                    .spec(ApiUtil.successResponseSpec())
                    .body("data", notNullValue())
                    .body("data.id", greaterThan(0))
                    .body("data.script_id", notNullValue())
                    .body("data.name", equalTo(req.getName()))
                    .body("data.script_language", equalTo(req.getType()))
                    .body("data.bk_scope_type", equalTo(req.getScopeType()))
                    .body("data.bk_scope_id", equalTo(req.getScopeId()))
                    .body("data.content", equalTo(Base64Util.base64DecodeContentToStr(req.getContent())))
                    .body("data.creator", equalTo(TestProps.DEFAULT_TEST_USER))
                    .body("data.create_time", greaterThan(0L))
                    .body("data.last_modify_user", equalTo(TestProps.DEFAULT_TEST_USER))
                    .body("data.last_modify_time", greaterThan(0L))
                    .body("data.version", equalTo(req.getVersion()))
                    .body("data.version_desc", equalTo(req.getVersionDesc()))
                    .body("data.status", equalTo(JobResourceStatusEnum.DRAFT.getValue()))
                    .body("data.description", equalTo(req.getDescription()))
                    .extract()
                    .body()
                    .as(new TypeRef<EsbResp<EsbScriptVersionDetailV3DTO>>() {
                    })
                    .getData();

            createdScriptList.add(createdScript);
        }

        @Test
        @DisplayName("创建脚本异常场景测试-比如参数校验，业务逻辑等")
        void givenInvalidCreateScriptParamThenFail() {
            EsbCreateScriptV3Request req = new EsbCreateScriptV3Request();
            req.setContent(SHELL_SCRIPT_CONTENT_BASE64);
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setDescription(TestValueGenerator.generateUniqueStrValue("shell_script_desc", 50));
            req.setType(ScriptTypeEnum.SHELL.getValue());
            req.setVersion("v1");
            req.setVersionDesc("v1_desc");
            // 脚本名称为空，创建失败
            req.setName(null);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec());

            // 脚本名称有特殊字符，创建失败
            req.setName(TestValueGenerator.generateUniqueStrValue("*<>", 50));
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec());

            req.setName(TestValueGenerator.generateUniqueStrValue("shell_script", 50));
            // 脚本类型非法，创建失败
            req.setType(-1);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec());

            req.setType(ScriptTypeEnum.SHELL.getValue());
            // 脚本内容为空，创建失败
            req.setContent(null);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec());
            // 版本号为空, 创建失败
            req.setVersion(null);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec());

            // 版本号有特殊字符, 创建失败
            req.setVersion("|");
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec());
        }

        @Test
        @DisplayName("测试脚本版本创建")
        void testCreateScriptVersion() {
            EsbCreateScriptVersionV3Req req = new EsbCreateScriptVersionV3Req();
            req.setContent(SHELL_SCRIPT_CONTENT_BASE64);
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setVersion("v2");
            req.setVersionDesc("v2_desc");
            if (CollectionUtils.isEmpty(createdScriptList)){
                testCreateScript();
            }
            req.setScriptId(createdScriptList.get(0).getScriptId());
            EsbScriptVersionDetailV3DTO createdScript =
                given()
                    .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                    .body(JsonUtil.toJson(req))
                    .post(APIV3Urls.CREATE_SCRIPT_VERSION)
                    .then()
                    .spec(ApiUtil.successResponseSpec())
                    .body("data", notNullValue())
                    .body("data.id", greaterThan(0))
                    .body("data.script_id", notNullValue())
                    .body("data.bk_scope_type", equalTo(req.getScopeType()))
                    .body("data.bk_scope_id", equalTo(req.getScopeId()))
                    .body("data.content", equalTo(Base64Util.base64DecodeContentToStr(req.getContent())))
                    .body("data.creator", equalTo(TestProps.DEFAULT_TEST_USER))
                    .body("data.create_time", greaterThan(0L))
                    .body("data.last_modify_user", equalTo(TestProps.DEFAULT_TEST_USER))
                    .body("data.last_modify_time", greaterThan(0L))
                    .body("data.version", equalTo(req.getVersion()))
                    .body("data.version_desc", equalTo(req.getVersionDesc()))
                    .body("data.status", equalTo(JobResourceStatusEnum.DRAFT.getValue()))
                    .extract()
                    .body()
                    .as(new TypeRef<EsbResp<EsbScriptVersionDetailV3DTO>>() {
                    })
                    .getData();
            createdScriptList.add(createdScript);
        }

        @Test
        @DisplayName("创建脚本版本异常场景测试-比如参数校验，业务逻辑等")
        void givenInvalidCreateScriptVersionParamThenFail() {
            EsbCreateScriptVersionV3Req req = new EsbCreateScriptVersionV3Req();
            req.setContent(SHELL_SCRIPT_CONTENT_BASE64);
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setVersion("v2");
            req.setVersionDesc("v2_desc");
            if (CollectionUtils.isEmpty(createdScriptList)){
                testCreateScript();
            }

            // 脚本ID为空，创建失败
            req.setScriptId(null);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.failResponseSpec());
            // 脚本内容为空，创建失败
            req.setScriptId(createdScriptList.get(0).getScriptId());
            req.setContent(null);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.failResponseSpec());
            // 版本号为空，创建失败
            req.setContent(SHELL_SCRIPT_CONTENT_BASE64);
            req.setVersion(null);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.failResponseSpec());
            // 版本号有特殊字符，创建失败
            req.setVersion("v2?");
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.failResponseSpec());
            // 同一脚本下，脚本版本号重复，创建失败
            req.setVersion(createdScriptList.get(0).getVersion());
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.SCRIPT_VERSION_NAME_EXIST));
        }
    }

    @Nested
    class GetTest {
        @Test
        @DisplayName("测试获取脚本版本列表")
        void testGetScriptVersionList() {
            EsbScriptVersionDetailV3DTO scriptVersionDTO;
            if (CollectionUtils.isNotEmpty(createdScriptList)) {
                scriptVersionDTO = createdScriptList.get(0);
            } else {
                scriptVersionDTO = Operations.createScript();
            }
            EsbGetScriptVersionListV3Req req = new EsbGetScriptVersionListV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setScriptId(scriptVersionDTO.getScriptId());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_SCRIPT_VERSION_LIST)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.total", greaterThan(0));
        }
    }

    @Nested
    class UpdateTest {
        // 更新操作
        @Test
        @DisplayName("测试更新脚本基础信息")
        void testUpdateScriptBasic() {
            EsbScriptVersionDetailV3DTO scriptVersionDTO;
            if (CollectionUtils.isNotEmpty(createdScriptList)) {
                scriptVersionDTO = createdScriptList.get(0);
            } else {
                scriptVersionDTO = Operations.createScript();
            }
            EsbUpdateScriptBasicV3Req req = new EsbUpdateScriptBasicV3Req();
            req.setScopeId(scriptVersionDTO.getScopeId());
            req.setScopeType(scriptVersionDTO.getScopeType());
            req.setScriptId(scriptVersionDTO.getScriptId());
            req.setName(TestValueGenerator.generateUniqueStrValue("shell_script", 50));
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.UPDATE_SCRIPT_BASIC)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", equalTo(req.getScriptId()))
                .body("data.name", equalTo(req.getName()))
                .body("data.script_language", greaterThan(0))
                .body("data.bk_scope_type", equalTo(req.getScopeType()))
                .body("data.bk_scope_id", equalTo(req.getScopeId()))
                .body("data.creator", notNullValue())
                .body("data.create_time", greaterThan(0L))
                .body("data.last_modify_user", equalTo(TestProps.DEFAULT_TEST_USER))
                .body("data.last_modify_time", greaterThan(0L));
        }

        @Test
        @DisplayName("测试更新脚本版本信息")
        void testUpdateScriptVersion() {
            EsbScriptVersionDetailV3DTO scriptVersionDTO;
            if (CollectionUtils.isNotEmpty(createdScriptList)) {
                scriptVersionDTO = createdScriptList.get(0);
            } else {
                scriptVersionDTO = Operations.createScript();
            }

            EsbUpdateScriptVersionV3Req req = new EsbUpdateScriptVersionV3Req();
            req.setScopeId(scriptVersionDTO.getScopeId());
            req.setScopeType(scriptVersionDTO.getScopeType());
            req.setScriptId(scriptVersionDTO.getScriptId());
            req.setScriptVersionId(scriptVersionDTO.getId());
            req.setContent(SHELL_SCRIPT_CONTENT_BASE64);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.UPDATE_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", equalTo(req.getScriptVersionId().intValue()))
                .body("data.script_id", equalTo(req.getScriptId()))
                .body("data.name", notNullValue())
                .body("data.script_language", notNullValue())
                .body("data.bk_scope_type", equalTo(req.getScopeType()))
                .body("data.bk_scope_id", equalTo(req.getScopeId()))
                .body("data.content", equalTo(Base64Util.base64DecodeContentToStr(req.getContent())))
                .body("data.creator", notNullValue())
                .body("data.create_time", greaterThan(0L))
                .body("data.last_modify_user", equalTo(TestProps.DEFAULT_TEST_USER))
                .body("data.last_modify_time", greaterThan(0L))
                .body("data.version", notNullValue())
                .body("data.status", notNullValue());
        }
    }

    @Nested
    class OperationTest {
        // 上线、下线等操作
        @Test
        @DisplayName("测试上线脚本版本")
        void testPublishScriptVersion() {
            EsbScriptVersionDetailV3DTO scriptVersionDTO;
            if (CollectionUtils.isNotEmpty(createdScriptList)) {
                scriptVersionDTO = createdScriptList.get(0);
            } else {
                scriptVersionDTO = Operations.createScript();
            }
            EsbManageScriptVersionV3Req req = new EsbManageScriptVersionV3Req();
            req.setScopeId(scriptVersionDTO.getScopeId());
            req.setScopeType(scriptVersionDTO.getScopeType());
            req.setScriptId(scriptVersionDTO.getScriptId());
            req.setScriptVersionId(scriptVersionDTO.getId());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.PUBLISH_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data.status", equalTo(JobResourceStatusEnum.ONLINE.getValue()))
                .body("data.id", equalTo(req.getScriptVersionId().intValue()))
                .body("data.script_id", equalTo(req.getScriptId()));
        }

        @Test
        @DisplayName("测试禁用脚本版本")
        void testDisableScriptVersion() {
            EsbScriptVersionDetailV3DTO scriptVersionDTO;
            if (CollectionUtils.isNotEmpty(createdScriptList)) {
                scriptVersionDTO = createdScriptList.get(0);
            } else {
                scriptVersionDTO = Operations.createScript();
            }
            EsbManageScriptVersionV3Req req = new EsbManageScriptVersionV3Req();
            req.setScopeId(scriptVersionDTO.getScopeId());
            req.setScopeType(scriptVersionDTO.getScopeType());
            req.setScriptId(scriptVersionDTO.getScriptId());
            req.setScriptVersionId(scriptVersionDTO.getId());
            if (scriptVersionDTO.getStatus() == JobResourceStatusEnum.ONLINE.getValue()
                || scriptVersionDTO.getStatus() == JobResourceStatusEnum.OFFLINE.getValue()) {
                given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                    .body(JsonUtil.toJson(req))
                    .post(APIV3Urls.DISABLE_SCRIPT_VERSION)
                    .then()
                    .spec(ApiUtil.successResponseSpec())
                    .body("data.status", equalTo(JobResourceStatusEnum.DISABLED.getValue()))
                    .body("data.script_version_id", equalTo(req.getScriptVersionId().intValue()))
                    .body("data.script_id", equalTo(req.getScriptId()));
            } else {
                given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                    .body(JsonUtil.toJson(req))
                    .post(APIV3Urls.DISABLE_SCRIPT_VERSION)
                    .then()
                    .spec(ApiUtil.failResponseSpec(ErrorCode.UNSUPPORTED_OPERATION));
            }
        }
    }

    @Nested
    class DeleteTest {
        @Test
        @DisplayName("测试脚本删除")
        void testDeleteScript() {
            EsbScriptVersionDetailV3DTO createdScript = Operations.createScript();
            EsbDeleteScriptV3Req req = new EsbDeleteScriptV3Req();
            req.setScopeId(createdScript.getScopeId());
            req.setScopeType(createdScript.getScopeType());
            req.setScriptId(createdScript.getScriptId());

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.DELETE_SCRIPT)
                .then()
                .spec(ApiUtil.successResponseSpec());
        }

        @Test
        @DisplayName("测试脚本版本删除")
        void testDeleteScriptVersion() {
            EsbScriptVersionDetailV3DTO createdScript = Operations.createScript();
            EsbDeleteScriptVersionV3Req req = new EsbDeleteScriptVersionV3Req();
            req.setScopeId(createdScript.getScopeId());
            req.setScopeType(createdScript.getScopeType());
            req.setScriptId(createdScript.getScriptId());
            req.setScriptVersionId(createdScript.getId());

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.DELETE_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.successResponseSpec());
        }
    }


}
