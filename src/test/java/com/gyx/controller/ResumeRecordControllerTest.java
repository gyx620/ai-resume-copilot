package com.gyx.controller;

import com.gyx.entity.ResumeRecord;
import com.gyx.service.ResumeRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeRecordControllerTest {

    @Mock
    private ResumeRecordService resumeRecordService;

    @InjectMocks
    private ResumeRecordController controller;

    @Test
    void detail_shouldReturnRecord() {
        ResumeRecord record = new ResumeRecord();
        record.setId(1L);
        when(resumeRecordService.getById(1L)).thenReturn(record);

        var result = controller.detail(1L);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    void list_shouldReturnRecords() {
        when(resumeRecordService.listByUser()).thenReturn(List.of(new ResumeRecord()));

        var result = controller.list();
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    void delete_shouldCallService() {
        var result = controller.delete(1L);
        assertEquals(200, result.getCode());
        verify(resumeRecordService).delete(1L);
    }

    @Test
    void update_shouldCallService() {
        ResumeRecord record = new ResumeRecord();
        var result = controller.update(1L, record);
        assertEquals(200, result.getCode());
        verify(resumeRecordService).update(record);
    }
}
