FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B || true
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=builder /app/target/*.jar app.jar
ENV JAVA_OPTS="-Xms128m -Xmx256m"
EXPOSE 8086
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
