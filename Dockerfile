FROM bellsoft/liberica-openjdk-alpine:17-cds

WORKDIR /app

COPY target/snmp-service-1.0.0-main.jar snmp-srv-1.0.0.jar
COPY src/main/resources/config.json config.json
COPY src/main/resources/logback.xml logback.xml

EXPOSE 8888

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dlogback.configurationFile=logback.xml -jar snmp-srv-1.0.0.jar"]

