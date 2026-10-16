package com.gyx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gyx.entity.ResumeRecord;
import com.gyx.exception.BusinessException;
import com.gyx.mapper.ResumeRecordMapper;
import com.gyx.service.ResumeRecordService;
import com.gyx.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ResumeRecordServiceImpl implements ResumeRecordService {

    @Autowired
    private ResumeRecordMapper resumeMapper;

    @Override
    public void save(ResumeRecord record) {
        Long userId = UserContext.getUserId();
        log.info("保存简历: userId={}, fileName={}", userId, record.getFileName());
        record.setUserId(userId);
        resumeMapper.insert(record);
        log.info("简历保存成功: resumeId={}", record.getId());
    }

    @Override
    public void update(ResumeRecord record) {
        Long userId = UserContext.getUserId();
        ResumeRecord exist = resumeMapper.selectById(record.getId());
        if (exist == null) {
            throw new BusinessException(404, "简历不存在");
        }
        if (!Objects.equals(exist.getUserId(), userId)) {
            log.warn("越权更新: resumeId={}, owner={}, requester={}", record.getId(), exist.getUserId(), userId);
            throw new BusinessException(403, "无权修改该简历");
        }
        record.setUserId(userId);
        resumeMapper.updateById(record);
    }

    @Override
    public void delete(Long id) {
        Long userId = UserContext.getUserId();
        log.info("删除简历: resumeId={}, userId={}", id, userId);

        ResumeRecord exist = resumeMapper.selectById(id);
        if (exist == null) {
            log.warn("删除失败-记录不存在: resumeId={}", id);
            throw new BusinessException(404, "简历不存在 (id=" + id + ")");
        }
        if (!Objects.equals(exist.getUserId(), userId)) {
            log.warn("越权删除: resumeId={}, owner={}, requester={}", id, exist.getUserId(), userId);
            throw new BusinessException(403, "无权删除该简历");
        }

        resumeMapper.deleteById(id);
        log.info("简历删除成功: resumeId={}", id);
    }

    @Override
    public ResumeRecord getById(Long id) {
        ResumeRecord record = resumeMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "简历不存在");
        }
        if (!Objects.equals(record.getUserId(), UserContext.getUserId())) {
            throw new BusinessException(403, "无权查看该简历");
        }
        return record;
    }

    @Override
    public List<ResumeRecord> listByUser() {
        Long userId = UserContext.getUserId();
        log.info("查询简历列表: userId={}", userId);
        if (userId == null) {
            log.warn("当前用户未登录，返回空列表");
            return List.of();
        }
        return resumeMapper.selectList(
                new LambdaQueryWrapper<ResumeRecord>()
                        .eq(ResumeRecord::getUserId, userId)
        );
    }
}
