FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
# 腾讯云等国内环境拉 Maven Central 很慢，构建阶段走阿里云公共仓库
RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml <<'XML'
<settings>
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>*</mirrorOf>
      <name>Aliyun Maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
XML
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/education-learning-platform-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
