FROM maven:3.5.0-jdk-8
ADD . /ontonethub-src

WORKDIR /ontonethub-src/indexing
RUN ["mvn", "clean", "install"]

RUN ["mv", "target/indexing-genericrdf-0.1.jar", "../api/src/main/resources/executables/indexing-genericrdf.jar"]

WORKDIR /ontonethub-src
RUN ["mvn", "clean", "install"]
