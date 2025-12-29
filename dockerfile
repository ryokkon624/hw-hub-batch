# ---- build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 依存解決キャッシュを効かせるために先にGradle周りだけコピー
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle* settings.gradle* ./

# ここで一度 dependency を引かせる（失敗してもOKにしない。ちゃんと失敗させる）
RUN chmod +x ./gradlew && ./gradlew --no-daemon -v

# 残りをコピーしてビルド
COPY . .
RUN ./gradlew --no-daemon clean bootJar

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# セキュリティ的に非root推奨
RUN useradd -m appuser
USER appuser

# bootJar（plainじゃない方）を1つだけコピーする
# build/libs に複数 jar があるので "-plain" を除外してコピー
COPY --from=build /workspace/build/libs/*-SNAPSHOT.jar /app/app.jar

# Spring Boot
EXPOSE 8080

# ECS/ローカルで引数を渡せるように ENTRYPOINT にする
ENTRYPOINT ["java","-jar","/app/app.jar"]
