package com.gyx.controller;

import com.gyx.common.Result;
import com.gyx.entity.ResumeRecord;
import com.gyx.service.ResumeRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resume")
public class ResumeRecordController {

    @Autowired
    private ResumeRecordService resumeRecordService;

    @PostMapping("/save")
    public Result<?> save(@RequestBody ResumeRecord record) {
        resumeRecordService.save(record);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody ResumeRecord record) {
        record.setId(id);
        resumeRecordService.update(record);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        resumeRecordService.delete(id);
        return Result.success(null);
    }

    @GetMapping("/{id}")
    public Result<ResumeRecord> detail(@PathVariable Long id) {
        return Result.success(resumeRecordService.getById(id));
    }

    @GetMapping("/list")
    public Result<List<ResumeRecord>> list() {
        return Result.success(resumeRecordService.listByUser());
    }
}