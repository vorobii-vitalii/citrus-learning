package org.citrus.learn.integration_tests.utils;

import java.util.Map;
import java.util.stream.Collectors;

public class JavaOptionsCreator {

	public static String createOptions(Map<String, String> map) {
		return map.entrySet()
				.stream()
				.map(e -> String.format("-D%s=%s", e.getKey(), e.getValue()))
				.collect(Collectors.joining(" "));
	}

}