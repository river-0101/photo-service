# 빌드 스테이지
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# pom.xml, SDK JAR 복사
COPY pom.xml .
COPY libs ./libs

# SDK JAR을 로컬 Maven 저장소에 설치 후 의존성 다운로드 (캐싱)
RUN mvn install:install-file \
    -Dfile=libs/logncrash-java-sdk3-4.0.0.jar \
    -DgroupId=com.toast.java \
    -DartifactId=logncrash-java-sdk3 \
    -Dversion=4.0.0 \
    -Dpackaging=jar \
    -DgeneratePom=true && \
    mvn dependency:go-offline -B

COPY src ./src

# Maven 빌드 (테스트 스킵)
RUN mvn clean package -DskipTests

# 실행 스테이지
FROM --platform=linux/amd64 eclipse-temurin:17-jre
WORKDIR /app

# 빌드된 JAR 복사
COPY --from=build /app/target/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 환경변수 (실행 시 주입)
ENV SPRING_PROFILES_ACTIVE=prod
ENV DB_HOST=""
ENV DB_PORT="3306"
ENV DB_NAME="photoservice"
ENV DB_USERNAME=""
ENV DB_PASSWORD=""
ENV OBJECT_STORAGE_ENDPOINT=""
ENV OBJECT_STORAGE_REGION=""
ENV OBJECT_STORAGE_ACCESS_KEY=""
ENV OBJECT_STORAGE_SECRET_KEY=""
ENV OBJECT_STORAGE_BUCKET_NAME=""
ENV JWT_SECRET=""
ENV LOG_CRASH_APPKEY=""

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]