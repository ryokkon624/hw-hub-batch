FROM eclipse-temurin:21-jre
WORKDIR /app

# 非rootユーザー
RUN useradd -m appuser
USER appuser

# Actions の "Build JAR" で作られた bootJar をコピー
# （jar名が変わる可能性があるなら、workflow側で app.jar に揃えるのが安全）
COPY build/libs/app.jar /app/app.jar

EXPOSE 8080

# バージョン情報（CI/CDから --build-arg APP_VERSION=v1.x.x で注入）
# ローカル開発時は "local" がデフォルト値
# シングルステージビルドのためARGの再宣言は不要
ARG APP_VERSION=local
ENV APP_VERSION=${APP_VERSION}

ENTRYPOINT ["java","-jar","/app/app.jar"]
