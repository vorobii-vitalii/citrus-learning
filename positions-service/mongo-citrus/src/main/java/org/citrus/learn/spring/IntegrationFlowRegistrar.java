package org.citrus.learn.spring;

import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;

public class IntegrationFlowRegistrar {
	private final IntegrationFlowContext integrationFlowContext;

	public IntegrationFlowRegistrar(IntegrationFlowContext integrationFlowContext) {
		this.integrationFlowContext = integrationFlowContext;
	}

	public void register(IntegrationFlow integrationFlow) {
		integrationFlowContext.registration(integrationFlow).register().start();
	}
}
