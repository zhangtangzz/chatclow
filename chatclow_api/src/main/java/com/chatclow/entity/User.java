package com.chatclow.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表：chatclow_ai_user
 */

@Data //LomBok:自动生成gentter/setter
@TableName("chatclow_ai_user")  //告诉MyBatis-Plus表名是什么
public class User {
    //主键自增
    @TableId(type = IdType.AUTO)
    private  Long id;

    //用户名
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3,max = 20, message = "用户名长度必须在3-20个字符之间")
    private String username;

    //邮箱
    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    //密码
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在6-32位之间")
    private  String password;

    //角色：1=普通用户，2=管理员
    private Integer role;

    //创建时间
    @TableField(fill = FieldFill.INSERT)
    private  LocalDateTime createdDt;


}
