package com.example.smartcontractfvss;
import com.alibaba.fastjson.JSON;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import java.util.logging.Level;

@Contract(
        name = "UserContract",
        transactionSerializer = "com.example.smartcontractfvss.ValidationJSONTransactionSerializer",
        info = @Info(
                title = "User contract",
                description = "user contract",
                version = "0.0.1-SNAPSHOT",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "https://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "f.carr@example.com",
                        name = "User contract",
                        url = "https://hyperledger.example.com")))

@Log
//定义UserContract类，并实现了ContractInterface接口
public class UserContract implements ContractInterface {

    @Transaction//增加用户信息
    public static UserInfo regUser(Context ctx, UserInfo userInfo){
        ChaincodeStub stub = ctx.getStub(); //通过ctx与区块链网络进行交互

        if (userInfo.getKey() == null) {
            throw new IllegalArgumentException("userInfo.getKey() cannot be null");
        }

        String user = stub.getStringState(userInfo.getKey()); //调用getStringState从区块链状态数据库中获取键对应的值，存储在user对象中

        String state = String.valueOf(stub.getState(userInfo.getKey())); //添加对getState方法的调用

        if (StringUtils.isNotBlank(user)){
            String errorMessage = String.format("用户 %s 已经存在",userInfo.getKey());
            log.log(Level.ALL,errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        //将userInfo对象转换位JSON字符，并存储在区块链状态数据库中的键userInfo.getKey()下
        stub.putStringState(userInfo.getKey(), JSON.toJSONString(userInfo));
        // 添加日志语句
        log.info("用户"+ userInfo.getKey()+"信息已存储：" +JSON.toJSONString(userInfo));
        return userInfo;
    }

    @Transaction//删除用户信息
    public void deleteUser(Context ctx, String key){
        ChaincodeStub stub = ctx.getStub();

        String user = stub.getStringState(key);
        if (StringUtils.isBlank(user)){
            String errorMessage = String.format("用户 %s 不存在",key);
            log.log(Level.ALL,errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        stub.delState(key); //删除用户信息
    }


    @Transaction //修改用户信息
    public static void updateUser(Context ctx, UserInfo userInfo) {
        ChaincodeStub stub = ctx.getStub();

        if (userInfo.getKey() == null) {
            throw new IllegalArgumentException("用户信息为空！");
        }

        String storedUser = stub.getStringState(userInfo.getKey());

        if (StringUtils.isBlank(storedUser)) {
            String errorMessage = String.format("用户 %s 不存在", userInfo.getKey());
            log.log(Level.ALL, errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        // 更新用户信息
        stub.putStringState(userInfo.getKey(), JSON.toJSONString(userInfo));
        log.info("用户"+ userInfo.getKey()+"的信息已更新为：" + JSON.toJSONString(userInfo));

    }


    @Transaction //查询用户信息
    public UserInfo queryUser(Context ctx,String key){
        ChaincodeStub stub = ctx.getStub();

        String user = stub.getStringState(key);

        if (StringUtils.isBlank(user)){
            String errorMessage = String.format("用户 %s 不存在",key);
            log.log(Level.ALL,errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        return JSON.parseObject(user,UserInfo.class);
    }



}
