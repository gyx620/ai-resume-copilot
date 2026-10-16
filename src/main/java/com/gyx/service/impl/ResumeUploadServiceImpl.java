package com.gyx.service.impl;

import com.gyx.entity.ResumeRecord;
import com.gyx.exception.BusinessException;
import com.gyx.service.ResumeRecordService;
import com.gyx.service.ResumeUploadService;
import com.gyx.utils.PdfUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class ResumeUploadServiceImpl implements ResumeUploadService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private ResumeRecordService resumeRecordService;

    @Override
    public Long upload(MultipartFile file) {
        validatePdf(file);

        String content;
        try (InputStream input = file.getInputStream()) {
            content = PdfUtil.parse(input);
        } catch (IOException e) {
            throw new BusinessException(400, "PDF 文件无法解析，请确认文件未损坏");
        }

        if (content.isBlank()) {
            throw new BusinessException(400, "PDF 中没有可提取的文字，扫描版简历暂不支持");
        }

        ResumeRecord record = new ResumeRecord();
        record.setFileName(file.getOriginalFilename());
        record.setContent(content);
        resumeRecordService.save(record);
        return record.getId();
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的 PDF 文件");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(413, "PDF 文件不能超过 10 MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new BusinessException(400, "只支持上传 PDF 文件");
        }

        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(1024);
            if (!containsPdfSignature(header)) {
                throw new BusinessException(400, "文件内容不是有效的 PDF");
            }
        } catch (IOException e) {
            throw new BusinessException(400, "无法读取上传文件，请重试");
        }
    }

    private boolean containsPdfSignature(byte[] header) {
        for (int offset = 0; offset <= header.length - PDF_SIGNATURE.length; offset++) {
            boolean matches = true;
            for (int index = 0; index < PDF_SIGNATURE.length; index++) {
                if (header[offset + index] != PDF_SIGNATURE[index]) {
                    matches = false;
                    break;
                }
            }
            if (matches) return true;
        }
        return false;
    }
}
