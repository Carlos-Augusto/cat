FROM maven:3.8-openjdk-11-slim as build
COPY . /cat
WORKDIR /cat
RUN mkdir bin
RUN ./buildHttp.sh /cat/bin

FROM openjdk:11-jre-slim
EXPOSE 8080/tcp
COPY --from=build /cat/bin /cat
WORKDIR /cat
ENTRYPOINT ./catalina.sh
