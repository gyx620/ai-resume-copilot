package com.gyx.service;

import com.gyx.entity.ResumeRecord;

import java.util.List;

public interface ResumeRecordService {

    void save(ResumeRecord record);

    void update(ResumeRecord record);

    void delete(Long id);

    ResumeRecord getById(Long id);

    List<ResumeRecord> listByUser();
}