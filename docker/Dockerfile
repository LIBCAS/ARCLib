# syntax = docker/dockerfile:experimental

## BUILD image ##
FROM maven:3-eclipse-temurin-17 AS builder

# Install unzip for inspecting the extracted layers (optional)
RUN apt-get update && apt-get install -y unzip

WORKDIR /build

# Copy files
COPY ./domainbase ./domainbase
COPY ./formatLibrary ./formatLibrary
COPY ./system ./system

# COPY config files
COPY ./pom.xml ./pom.xml

RUN mvn clean package -DskipTests=true

# extract layers
RUN --mount=type=cache,id=maven-arclib-be-build,target=/build/be-build/build \
    java -Djarmode=layertools -jar ./system/target/arclib.jar extract

## RUN Image ##
FROM eclipse-temurin:17

# Install dependencies for ClamAV and DROID
RUN apt-get update && \
    apt-get install -y clamav clamav-daemon unzip

# Update ClamAV database
RUN freshclam

RUN wget https://github.com/digital-preservation/droid/releases/download/droid-6.8.0/droid-binary-6.8.0-bin.zip -P /opt/

RUN unzip /opt/droid-binary-6.8.0-bin.zip -d /opt/droid

RUN sed -i '/# Run DROID:/a JAVA_TOOL_OPTIONS=""' /opt/droid/droid.sh

RUN cp /opt/droid/droid.sh /opt/droid/droid

# Make ClamAV and DROID executables accessible via PATH
ENV PATH="/opt/droid:${PATH}"

WORKDIR /app

RUN mkdir -p /app/fileStorage/prod1

COPY --from=builder /build/dependencies /app
COPY --from=builder /build/snapshot-dependencies /app
COPY --from=builder /build/spring-boot-loader /app
COPY --from=builder /build/application /app
CMD ["java", "org.springframework.boot.loader.launch.JarLauncher"]
