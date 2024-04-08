FROM adoptopenjdk/openjdk11:alpine-nightly-slim
VOLUME /tmp
ADD . /smalltalk/
WORKDIR /smalltalk/
RUN ./mvnw compile
CMD ["./bin/dispatcher", "start", "--import", "org.tinystruct.system.TomcatServer", "--server-port", "777"]
EXPOSE 777
