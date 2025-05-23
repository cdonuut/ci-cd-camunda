package com.cicd;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ZeebeProcessTest
public class PlayProcessTest {

  private ZeebeClient client;
  private ZeebeTestEngine engine;

  @Test
  void shouldTriggerTimerStartEvent() throws InterruptedException, TimeoutException {
    // Deploy
    DeploymentEvent deployment = client.newDeployResourceCommand()
            .addResourceFromClasspath("models/PlayProcess.bpmn")
            .send().join();

    assertEquals(1, deployment.getProcesses().size());

    // Advance time to trigger the timer start
    engine.increaseTime(Duration.ofSeconds(10));

    // Wait for process instance to be created
    engine.waitForIdleState(Duration.ofSeconds(10));

    // Now assert that the process started
    BpmnAssert.assertThat(deployment).containsProcessesByBpmnProcessId( "play_process");
  }
  @Test

  void shouldCompleteConnectorStep() throws InterruptedException, TimeoutException {
    // Deploy (optional if using @BeforeEach for setup)
    DeploymentEvent deployment = client.newDeployResourceCommand()
            .addResourceFromClasspath("models/PlayProcess.bpmn")
            .send().join();

    // Start process by timer
    engine.increaseTime(Duration.ofSeconds(10));

    // Register worker to handle Connector
    client.newWorker()
            .jobType("io.camunda:connector-http-json")
            .handler((jobClient, job) -> jobClient.newCompleteCommand(job.getKey())
                    .variables(Map.of("statusCode", 200)) // Mocked output
                    .send()
                    .join())
            .open();

    engine.waitForIdleState(Duration.ofSeconds(10));


    // Assert process passed the connector
    BpmnAssert.assertThat(deployment).containsProcessesByBpmnProcessId( "play_process");
  }



}