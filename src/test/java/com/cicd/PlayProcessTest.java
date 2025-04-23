package com.cicd;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ZeebeProcessTest
public class PlayProcessTest {

  private ZeebeClient client;
  private ZeebeTestEngine engine;

  @Test
  void shouldDeployProcess() {
    DeploymentEvent deploymentEvent = client
            .newDeployResourceCommand()
            .addResourceFromClasspath("models/PlayProcess.bpmn")
            .send()
            .join();

    assertTrue(deploymentEvent.getKey() > 0, "Deployment key should be greater than 0");
    assertEquals(1, deploymentEvent.getProcesses().size(), "Should deploy exactly one process");

    BpmnAssert.assertThat(deploymentEvent)
            .containsProcessesByBpmnProcessId("play_process");
  }
}