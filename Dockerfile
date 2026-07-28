FROM eclipse-temurin:25-jre-alpine

LABEL maintainer="portfolio-manager-team"

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m -Dspring.profiles.active=prod"
ENV SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/portfolio_db"
ENV SPRING_DATASOURCE_USERNAME="root"
ENV SPRING_DATASOURCE_PASSWORD=""

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/auth/me || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
