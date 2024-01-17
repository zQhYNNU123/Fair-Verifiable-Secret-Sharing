package com.example.smartcontractfvss;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;


@DataType
@Data
public class UserInfo {

    @NotBlank(message = "key不能为空！")
    @Property
    String key;

    @NotBlank(message = "ID不能为空！")
    @Property
    String ID;

    @NotBlank(message = "姓名不能为空！")
    @Length(max = 30, message = "name不能超过30个字符！")
    @Property
    String name;

    @NotBlank(message = "性别不能为空！")
    @Property
    String sex;

    @NotBlank(message = "出生年月不能为空！")
    @Property
    String birthday;

    @Property
    @Length(max = 11, message = "电话号码最长不能超过11位！")
    String phone;

}
