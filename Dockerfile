FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system rsvp && useradd --system --gid rsvp rsvp \
	&& mkdir -p /app/data/uploads && chown -R rsvp:rsvp /app
COPY --from=build /app/target/rsvp-0.0.1-SNAPSHOT.jar app.jar
USER rsvp
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
