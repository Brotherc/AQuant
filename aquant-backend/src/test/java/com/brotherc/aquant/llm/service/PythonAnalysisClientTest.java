package com.brotherc.aquant.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

class PythonAnalysisClientTest {

    private static final String BASE_URL = "http://127.0.0.1:8000";

    @Test
    void deleteAcceptsSuccessfulPythonResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        PythonAnalysisClient client = new PythonAnalysisClient(restTemplate, new ObjectMapper(), BASE_URL);
        server.expect(once(), requestTo(BASE_URL + "/v1/analysis/jobs/python-1"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("{\"success\":true,\"data\":{\"deleted\":true}}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> client.delete("python-1"));
        server.verify();
    }

    @Test
    void deleteTreatsPythonNotFoundAsIdempotentSuccess() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        PythonAnalysisClient client = new PythonAnalysisClient(restTemplate, new ObjectMapper(), BASE_URL);
        server.expect(requestTo(BASE_URL + "/v1/analysis/jobs/missing"))
                .andRespond(withResourceNotFound());

        assertDoesNotThrow(() -> client.delete("missing"));
        server.verify();
    }

    @Test
    void deleteRejectsBusinessFailureAndConflict() {
        RestTemplate businessRestTemplate = new RestTemplate();
        MockRestServiceServer businessServer = MockRestServiceServer.bindTo(businessRestTemplate).build();
        PythonAnalysisClient businessClient = new PythonAnalysisClient(
                businessRestTemplate, new ObjectMapper(), BASE_URL);
        businessServer.expect(requestTo(BASE_URL + "/v1/analysis/jobs/rejected"))
                .andRespond(withSuccess("{\"success\":false}", MediaType.APPLICATION_JSON));

        assertThrows(IllegalStateException.class, () -> businessClient.delete("rejected"));
        businessServer.verify();

        RestTemplate conflictRestTemplate = new RestTemplate();
        MockRestServiceServer conflictServer = MockRestServiceServer.bindTo(conflictRestTemplate).build();
        PythonAnalysisClient conflictClient = new PythonAnalysisClient(
                conflictRestTemplate, new ObjectMapper(), BASE_URL);
        conflictServer.expect(requestTo(BASE_URL + "/v1/analysis/jobs/running"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThrows(IllegalStateException.class, () -> conflictClient.delete("running"));
        conflictServer.verify();
    }

    @Test
    void deleteRejectsNetworkFailure() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        PythonAnalysisClient client = new PythonAnalysisClient(restTemplate, new ObjectMapper(), BASE_URL);
        server.expect(requestTo(BASE_URL + "/v1/analysis/jobs/unavailable"))
                .andRespond(request -> {
                    throw new ResourceAccessException("connection refused");
                });

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> client.delete("unavailable"));

        assertTrue(exception.getMessage().contains("Python 分析服务删除作业失败"));
        server.verify();
    }
}
