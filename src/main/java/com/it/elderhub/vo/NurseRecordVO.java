package com.it.elderhub.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NurseRecordVO {
    private Integer id;
    private Integer customerId;
    private Integer itemId;
    private Integer userId;
    private String nursingName; // 关联查询出的护理项目名称
    private LocalDateTime nursingTime;
    private String nursingContent;
    private Integer nursingCount;
}
