FROM bkjob/os:0.0.2

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="3.13.0"

## 安装JDK
RUN mkdir -p /data && \
    cd /data/ &&\
    curl -OL https://github.com/Tencent/TencentKona-8/releases/download/8.0.21-GA/TencentKona8.0.21.b1_jdk_linux-x86_64_8u442.tar.gz &&\
    tar -xzf TencentKona8.0.21.b1_jdk_linux-x86_64_8u442.tar.gz &&\
    rm -f TencentKona8.0.21.b1_jdk_linux-x86_64_8u442.tar.gz
ENV JAVA_HOME=/data/TencentKona-8.0.21-442
ENV PATH=${JAVA_HOME}/bin:$PATH
ENV CLASSPATH=.:${JAVA_HOME}/lib

## 安装在线诊断工具arthas
RUN mkdir -p /data/tools && \
    cd /data/tools && \
    curl -OL https://github.com/alibaba/arthas/releases/download/arthas-all-4.0.5/arthas-bin.zip && \
    yum install -y unzip && \
    unzip arthas-bin.zip arthas-boot.jar && \
    rm -rf arthas-bin* && \
    echo 'alias arthas="JAVA_TOOL_OPTIONS=;java -jar /data/tools/arthas-boot.jar"' >> ~/.bashrc
