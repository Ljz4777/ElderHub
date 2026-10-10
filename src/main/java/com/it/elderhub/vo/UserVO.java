package com.it.elderhub.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {
    private Integer id;
    private String nickname;
    private String username;
    private Integer sex;
    private String email;
    private String phoneNumber;
    private Integer roleId;
    private String roleName; // 辅助字段：返回角色名
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}