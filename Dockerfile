FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD . /smalltalk/
WORKDIR /smalltalk/
CMD ["./bin/dispatcher", "start", "--import", "org.tinystruct.system.TomcatServer", "--server-port", "777"]
EXPOSE 777
