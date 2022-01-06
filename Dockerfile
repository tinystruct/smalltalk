FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD . /example/
WORKDIR /example/
CMD ["./bin/dispatcher","--import-applications=org.tinystruct.system.TomcatServer", "--server-port=777", "--start-server"]
EXPOSE 777
