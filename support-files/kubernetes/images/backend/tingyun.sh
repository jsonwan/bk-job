#! /bin/sh
# 听云接入相关脚本

# 记录下当前所在目录，脚本执行结束时回到原目录
currentDir=$(pwd)
trap "cd ${currentDir}"  EXIT TERM

# 从Pod传入的环境变量
# 探针在容器内的安装路径
echo "TINGYUN_AGENT_DIR=${TINGYUN_AGENT_DIR}"
# 探针下载命令
echo "TINGYUN_DOWNLOAD_CMD=${TINGYUN_DOWNLOAD_CMD}"
# collector.addresses
echo "TINGYUN_COLLECTOR_ADDRESSES=${TINGYUN_COLLECTOR_ADDRESSES}"
# 业务系统
echo "TINGYUN_DEFAULT_BUSINESS_SYSTEM=${TINGYUN_DEFAULT_BUSINESS_SYSTEM}"

# 从挂载Secret文件传入的变量
# license_key
TINGYUN_LICENSE_KEY=$(cat /etc/secrets/tingyun/license_key)

# 下载探针
function downLoadAndConfigAgent() {
    if [ ! -d ${TINGYUN_AGENT_DIR} ];then
        mkdir -p ${TINGYUN_AGENT_DIR}
    fi
    cd ${TINGYUN_AGENT_DIR}
    # 下载探针压缩包并解压
    eval ${TINGYUN_DOWNLOAD_CMD}
    standardZipFileName="tingyun-agent-java.zip"
    agentZipFileName=$(ls -1|head -n1)
    mv ${agentZipFileName} ${standardZipFileName}
    unzip ${standardZipFileName} && rm -rf ${standardZipFileName}
    # 修改配置文件
    cd tingyun
    propertiesFileName="tingyun.properties"
    # 暂时无需修改配置项
}

downLoadAndConfigAgent
if [ $? -eq 0 ];then
    export JAVA_OPTS="$JAVA_OPTS \
           -Dtingyun.app_name=${BK_JOB_APP_NAME} \
           -Dtingyun.license_key=${TINGYUN_LICENSE_KEY} \
           -Dtingyun.collector.addresses=${TINGYUN_COLLECTOR_ADDRESSES} \
           -Dtingyun.default_business_system=${TINGYUN_DEFAULT_BUSINESS_SYSTEM} \
           -javaagent:${TINGYUN_AGENT_DIR}/tingyun/tingyun-agent-java.jar"
    echo "将听云探针启动所需的环境变量打入JAVA_OPTS中"
    echo "JAVA_OPTS 更新为：$JAVA_OPTS"
    echo "启动服务及听云探针"
else
    echo -e "听云Java Agent下载及安装失败，服务未启动，可按以下操作：\n1.检查错误，直至恢复后启动探针及服务；\n2.通过values属性关闭探针、启动服务。"
fi
