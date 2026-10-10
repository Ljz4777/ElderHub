package com.it.elderhub.dto;

import lombok.Data;

/**
 * 床位调换请求对象
 */
@Data
public class BedTransferDTO {

    // 客户ID
    private Integer customerId;

    // 旧床位ID
    private Integer oldBedId;

    // 目标床位ID
    private Integer newBedId;

    // 调换说明
    private String bedDetails;
}
