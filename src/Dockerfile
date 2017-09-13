FROM maven:3.5.0-jdk-8 as ontonet
ADD . /ontonethub-src

WORKDIR /ontonethub-src/indexing
RUN ["mvn", "clean", "install"]

RUN ["mv", "target/indexing-genericrdf-0.1.jar", "../api/src/main/resources/executables/indexing-genericrdf.jar"]

WORKDIR /ontonethub-src
RUN ["mvn", "clean", "install"]

FROM tomcat:8
WORKDIR /usr/local/tomcat/webapps/
RUN ["rm", "-rf", "stanbol*"]
COPY --from=ontonet /ontonethub-src/war/target/stanbol.war .
