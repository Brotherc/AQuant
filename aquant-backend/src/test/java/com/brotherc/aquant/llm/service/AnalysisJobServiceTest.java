package com.brotherc.aquant.llm.service;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.common.utils.UserContext;
import com.brotherc.aquant.llm.entity.AnalysisJob;
import com.brotherc.aquant.llm.entity.AnalysisJobStatus;
import com.brotherc.aquant.llm.repository.AnalysisJobEventRepository;
import com.brotherc.aquant.llm.repository.AnalysisJobPromptSnapshotRepository;
import com.brotherc.aquant.llm.repository.AnalysisJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/** 分析作业生命周期操作测试。 */
class AnalysisJobServiceTest {

    private final AnalysisJobRepository jobRepository = mock(AnalysisJobRepository.class);
    private final AnalysisJobEventRepository eventRepository = mock(AnalysisJobEventRepository.class);
    private final AnalysisJobPromptSnapshotRepository promptSnapshotRepository = mock(AnalysisJobPromptSnapshotRepository.class);
    private final PromptTemplateService promptTemplateService = mock(PromptTemplateService.class);
    private final PythonAnalysisClient pythonClient = mock(PythonAnalysisClient.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AnalysisJobService service = new AnalysisJobService(
            jobRepository,
            eventRepository,
            promptSnapshotRepository,
            promptTemplateService,
            pythonClient,
            new ObjectMapper(),
            executor,
            transactionTemplate,
            200L);

    @BeforeEach
    void setUpTransactionTemplate() {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        executor.shutdownNow();
    }

    @Test
    void deleteTerminalJobRemovesEventsAndJobForOwner() {
        AnalysisJob job = job("job-1", AnalysisJobStatus.SUCCEEDED, 7L);
        job.setPythonJobId("python-job-1");
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        UserContext.set(7L, "tester");

        service.delete("job-1");

        var order = inOrder(pythonClient, transactionTemplate, eventRepository,
                promptSnapshotRepository, jobRepository);
        order.verify(pythonClient).delete("python-job-1");
        order.verify(transactionTemplate).executeWithoutResult(any());
        order.verify(eventRepository).deleteByJobId("job-1");
        order.verify(promptSnapshotRepository).deleteByJobId("job-1");
        order.verify(jobRepository).delete(job);
    }

    @Test
    void deleteKeepsJavaRecordsWhenPythonDeleteFails() {
        AnalysisJob job = job("job-sync-fail", AnalysisJobStatus.SUCCEEDED, 7L);
        job.setPythonJobId("python-sync-fail");
        when(jobRepository.findById("job-sync-fail")).thenReturn(Optional.of(job));
        doThrow(new IllegalStateException("python unavailable"))
                .when(pythonClient).delete("python-sync-fail");
        UserContext.set(7L, "tester");

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.delete("job-sync-fail"));

        assertEquals(ExceptionEnum.ANALYSIS_JOB_DELETE_SYNC_FAILED.getCode(), exception.getCode());
        verify(eventRepository, never()).deleteByJobId(anyString());
        verify(promptSnapshotRepository, never()).deleteByJobId(anyString());
        verify(jobRepository, never()).delete(any(AnalysisJob.class));
        verifyNoInteractions(transactionTemplate);
    }

    @Test
    void deleteLegacyTerminalJobWithoutPythonIdOnlyCleansJavaRecords() {
        AnalysisJob job = job("job-legacy", AnalysisJobStatus.FAILED, 7L);
        when(jobRepository.findById("job-legacy")).thenReturn(Optional.of(job));
        UserContext.set(7L, "tester");

        service.delete("job-legacy");

        verifyNoInteractions(pythonClient);
        verify(eventRepository).deleteByJobId("job-legacy");
        verify(promptSnapshotRepository).deleteByJobId("job-legacy");
        verify(jobRepository).delete(job);
    }

    @Test
    void deleteRejectsRunningJobAndKeepsHistory() {
        AnalysisJob job = job("job-2", AnalysisJobStatus.RUNNING, 7L);
        when(jobRepository.findById("job-2")).thenReturn(Optional.of(job));
        UserContext.set(7L, "tester");

        assertThrows(IllegalStateException.class, () -> service.delete("job-2"));

        verifyNoInteractions(pythonClient);
        verify(eventRepository, never()).deleteByJobId(anyString());
        verify(promptSnapshotRepository, never()).deleteByJobId(anyString());
        verify(jobRepository, never()).delete(any(AnalysisJob.class));
    }

    @Test
    void deleteRejectsJobOwnedByAnotherUser() {
        AnalysisJob job = job("job-3", AnalysisJobStatus.FAILED, 8L);
        when(jobRepository.findById("job-3")).thenReturn(Optional.of(job));
        UserContext.set(7L, "tester");

        assertThrows(IllegalArgumentException.class, () -> service.delete("job-3"));

        verifyNoInteractions(pythonClient);
        verify(eventRepository, never()).deleteByJobId(anyString());
        verify(promptSnapshotRepository, never()).deleteByJobId(anyString());
        verify(jobRepository, never()).delete(any(AnalysisJob.class));
    }

    @Test
    void deleteRejectsLegacyJobWithoutOwner() {
        AnalysisJob job = job("job-4", AnalysisJobStatus.SUCCEEDED, null);
        when(jobRepository.findById("job-4")).thenReturn(Optional.of(job));
        UserContext.set(7L, "tester");

        assertThrows(IllegalArgumentException.class, () -> service.delete("job-4"));

        verifyNoInteractions(pythonClient);
        verify(eventRepository, never()).deleteByJobId(anyString());
        verify(promptSnapshotRepository, never()).deleteByJobId(anyString());
        verify(jobRepository, never()).delete(any(AnalysisJob.class));
    }

    private static AnalysisJob job(String id, AnalysisJobStatus status, Long ownerId) {
        AnalysisJob job = new AnalysisJob();
        job.setId(id);
        job.setStatus(status);
        job.setCreatedBy(ownerId);
        return job;
    }
}
