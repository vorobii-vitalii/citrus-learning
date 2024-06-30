package org.citrus.learn.spring;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;

class TestIntegrationFlowRegistrar {

	IntegrationFlowContext integrationFlowContext = mock(IntegrationFlowContext.class);

	IntegrationFlowRegistrar integrationFlowRegistrar = new IntegrationFlowRegistrar(integrationFlowContext);

	IntegrationFlow integrationFlow = mock(IntegrationFlow.class);

	IntegrationFlowContext.IntegrationFlowRegistrationBuilder integrationFlowRegistrationBuilder =
			mock(IntegrationFlowContext.IntegrationFlowRegistrationBuilder.class);

	IntegrationFlowContext.IntegrationFlowRegistration integrationFlowRegistration =
			mock(IntegrationFlowContext.IntegrationFlowRegistration.class);

	@Test
	void shouldRegisterFlowInContextAndStartIt() {
		when(integrationFlowContext.registration(integrationFlow)).thenReturn(integrationFlowRegistrationBuilder);
		when(integrationFlowRegistrationBuilder.register()).thenReturn(integrationFlowRegistration);
		integrationFlowRegistrar.register(integrationFlow);
		verify(integrationFlowRegistration).start();
	}
}
