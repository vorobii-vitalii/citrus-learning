package org.citrus.learn.positionsservice.integration_tests.utils;

import java.nio.file.Paths;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

public class LocalJavaContainer<SELF extends LocalJavaContainer<SELF>> extends GenericContainer<SELF> {

	public LocalJavaContainer() {
		super(new ImageFromDockerfile()
				.withFileFromPath("/app.jar", Paths.get(MountableFile.forHostPath("./build/libs/app.jar").getResolvedPath()))
				.withDockerfileFromBuilder(dockerfileBuilder -> {
					dockerfileBuilder.from("openjdk:21-jdk");
					dockerfileBuilder.add("./app.jar", "./app.jar");
					dockerfileBuilder.entryPoint("sh", "-c", "java ${JAVA_OPTS} -jar /app.jar ${0} ${@}");
				}));
	}

}