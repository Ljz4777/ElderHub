package com.it.elderhub.vo;

import lombok.Data;
import java.util.Date;

@Data
public class UserVO {
    private Integer id;
    private String username;
    private String nickname;
    private Integer sex;
    private String phoneNumber;
    private String email;
    private Integer roleId;
    private String roleName;
    private Date createTime;
}
