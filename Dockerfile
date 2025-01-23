## define an alias
#FROM it-docker.pkg.devops.sinochem.com/huashujupingtai/sdm/openjdk:8-jdk-alpine-sdm as build
#
#COPY . /src
#WORKDIR /src
#
#ARG type="basic"
#ENV package_type=$type
#
#RUN <<EOF
#    mvn clean package -P${package_type} -DskipTests
#    mvn package assembly:single
#    ./shrink_package.sh
#EOF

FROM it-docker.pkg.devops.sinochem.com/huashujupingtai/sdm/openjdk:8-jdk-alpine-sdm
LABEL maintainer="wgzhao <wgzhao@gmail.com>"
LABEL version="4.2.3"
LABEL description="Addax is a versatile open-source ETL tool that can seamlessly transfer data between various RDBMS and NoSQL databases, making it an ideal solution for data migration."

#COPY --from=build  /src/target/addax/addax-* /opt/addax/

COPY /target/addax/addax-* /opt/addax/

WORKDIR /opt/addax

RUN chmod 755 /opt/addax/bin/*

