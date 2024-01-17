package com.example.smartcontractfvss;


import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hyperledger.fabric.contract.annotation.Serializer;
import org.hyperledger.fabric.contract.execution.JSONTransactionSerializer;
import org.hyperledger.fabric.contract.metadata.TypeSchema;
import org.hyperledger.fabric.shim.ChaincodeException;
import java.util.Map;
import java.util.Set;

@Log
@Serializer(target = Serializer.TARGET.TRANSACTION)

//ValidationJSONTransactionSerializer类通过extends来或继承JSONTransactionSerializer类
public class ValidationJSONTransactionSerializer extends JSONTransactionSerializer {

    static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();//创建类对象

    @Override
    public Object fromBuffer(byte[] buffer, TypeSchema ts){
        //从父类中获取对象
        Object obj = super.fromBuffer(buffer,ts);
        //打印请求参数,执行参数校验的日志

        log.info(String.format("对请求参数执行参数校验 %s", ReflectionToStringBuilder.toString(obj, ToStringStyle.JSON_STYLE)));

        Set<ConstraintViolation<Object>> constraintViolations = VALIDATOR.validate(obj);//执行对象的参数校验
        if(CollectionUtils.isNotEmpty(constraintViolations)){
            //如果参数校验错误，将错误信息收集和组织成map
            Map<String ,String> err = Maps.newHashMapWithExpectedSize(constraintViolations.size());
            for (ConstraintViolation<Object> cv : constraintViolations){
                err.put(cv.getPropertyPath().toString(), cv.getMessage());
            }
            String errMsg = String.format("参数校验不通过，错误信息 %s", JSON.toJSONString(err));

            log.info(errMsg); //打印错误的日志信息，然后抛出异常
            throw new ChaincodeException(errMsg);
        }

        return obj;
    }

}
