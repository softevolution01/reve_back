FROM eclipse-temurin:21.0.6_7-jdk

WORKDIR /app

COPY pom.xml .
RUN ./mvnw dependency:go-offline -B

COPY src ./src

RUN ./mvnw package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/reve_back-0.0.1-SNAPSHOT.jar"]