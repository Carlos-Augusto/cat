FROM maven:3.6-openjdk-8 as build
COPY . /cat
WORKDIR /cat
RUN ./buildHttp.sh /cat/bin

FROM openjdk:8-jre-slim
EXPOSE 8080/tcp
COPY --from=build /cat/bin /cat
WORKDIR /cat
ENTRYPOINT ./catalina.sh
