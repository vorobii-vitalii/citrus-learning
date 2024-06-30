package org.citrus.learn.integration_tests.dto;

import java.util.Map;

public record GraphQLRequestWrapper(String query, Map<String, String> variables) {
}
