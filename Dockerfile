FROM openjdk:11-jdk
VOLUME /tmp
ADD . /smalltalk/
WORKDIR /smalltalk/
RUN ./mvnw compile
CMD ["./bin/dispatcher", "start", "--import", "org.tinystruct.system.NettyHttpServer", "--server-port", "777"]
EXPOSE 777
