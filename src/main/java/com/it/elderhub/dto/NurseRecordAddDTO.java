package com.it.elderhub.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NurseRecordAddDTO {
    private Integer customerId;
    private Integer itemId;
    private LocalDateTime nursingTime; // 护理执行时间
    private String nursingContent;     // 护理内容描述
    private Integer nursingCount;      // 本次执行次数
    private Integer userId;            // 执行护理人员ID (管家录入时由后端自动获取)
}
