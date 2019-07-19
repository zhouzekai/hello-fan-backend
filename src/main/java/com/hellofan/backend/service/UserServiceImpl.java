package com.hellofan.backend.service;


import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import com.hellofan.backend.dto.UserInfo;
import com.hellofan.backend.mapper.UserExtMapper;
import com.hellofan.backend.mapper.generator.UserMapper;
import com.hellofan.backend.model.generator.User;
import com.hellofan.backend.model.generator.UserExample;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.*;


@Service
public class UserServiceImpl implements UserService {

//    @Autowired
//    private StringRedisTemplate s;
//
//    @Autowired
//    private MongoTemplate mongoTemplate;

    @Autowired
    UserExtMapper userExtMapper;

    @Autowired
    UserMapper userMapper;

    /*
     *调用腾讯云发送验证码到用户
     *
     */

    /**
     * 存放验证码的hashmap,key为手机号,value为验证码
     */
    Map<String, String> codeMap = new HashMap<>();


    @Override
    public Boolean sendMsg(String countryCode, String phoneNum, String flag) {
        Boolean isSend = true;
        //随机生成的验证码
        String randomcode = (int) ((Math.random() * 9 + 1) * 1000) + "";
        System.out.println("randomcode=" + randomcode);

        // 短信应用SDK AppID
        int appid = 1400209835; // 1400开头
        // 短信应用SDK AppKey
        String appkey = "7ea3f772ec147d95aa3f0d5ff492f85a";
        try {
            //调用腾讯云接口发送验证码
            SmsSingleSender ssender = new SmsSingleSender(appid, appkey);
            SmsSingleSenderResult result = ssender.send(0, "86", phoneNum,
                    "【HelloFan你好繁】您的验证码是：" + randomcode, "", "");
            System.out.println("issendsuccess" + result);
            //将验证码保存到Redis中
//            s.opsForValue().set(phoneNum,randomcode
//                    ,300, TimeUnit.SECONDS
//            );
            //验证码保存
            codeMap.put(phoneNum, randomcode);
            //5min后移除验证码
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    codeMap.remove(phoneNum);
                }
            }, 300000);
        } catch (Exception e) {
            // HTTP响应码错误
            isSend = false;
            e.printStackTrace();
        } finally {
            return isSend;
        }
    }

    @Override
    public Boolean verify(String phoneNum, String code) {
        //从Redis中获取验证码
//        String trueCode=s.opsForValue().get(phoneNum);
//        return code.equals(trueCode);
        return code.equals(codeMap.get(phoneNum));
    }

    @Override
    public void register(User user) {
        userExtMapper.insertUser(user);
    }

    @Override
    public boolean isNameRepeat(String name) {

        int temp = userExtMapper.findUserByName(name);
        if (temp == 0)
            return false;
        else
            return true;
    }

    @Override
    public boolean isPhoneNumRepeat(String phoneNum) {
        int temp = userExtMapper.findUserByPhoneNum(phoneNum);
        if (temp == 0)
            return false;
        else
            return true;
    }

    /**
     * @param userName 既可以是用户名也可以是手机号，如果是11位数字优先当作手机号处理
     * @param password
     * @return false检验错误 检验成功返回用户名
     */
    @Override
    public String verifyUserInfo(String userName, String password) {
        //如果用户信息为空 肯定是验证错误
        if (userName == null || password == null || userName.equals("") || password.equals("")) {
            return "false";
        } else {
            int result = 0;
            //如果用户是手机号登陆
            result = userExtMapper.verifyUserInfoByPhoneNum(userName, password);
            if (result != 1) {  //如果用手机号验证不了登陆 则用用户名验证登陆
                result = userExtMapper.verifyUserInfoByName(userName, password);
                if (result != 1) {
                    return "false";
                } else {//登陆成功
                    return userName;
                }
            }
            if (result != 1) {
                return "false";
            } else
                return userExtMapper.findUserNameByPhone(userName);
        }
    }

    @Override
    public boolean updatePassword(String phoneNum, String password) {
        userExtMapper.updateByPhoneNum(phoneNum, password);
        return false;
    }

    @Override
    public void deleteRandomCode(String phoneNum) {
        try {
            codeMap.remove(phoneNum);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //randomcode=null;
    }

//    @Override
//    public void update(User user) {
//        //如果数据存在就更新
//        Update update=new Update();
//        if(user.getDeposit()!=null)
//            update.set("deposit",user.getDeposit());
//        if(user.getStatus()!=null)
//            update.set("Status",user.getStatus());
//        if(user.getName()!=null)
//            update.set("Name",user.getName());
//        if(user.getIdNum()!=null)
//            update.set("IdNum",user.getIdNum());
//
//        mongoTemplate.updateFirst(
//                new Query(Criteria.where("phoneNum").is(user.getPhoneNum())),  //查询语句
//                update,        //要更新的值
//                user.getClass()
//        );
//    }

    @Override
    public Date getUpdateTime(String userName) {
        // return studyPlanMapper.
        return userExtMapper.getUpdateTime(userName);
    }

    @Override
    public boolean updateSharedPreferences(User user) {
        try {
            userExtMapper.updateSharedPreferences(user);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public User getSharedPreferences(String userName) {
        return userExtMapper.getSharedPreferences(userName);
    }

    @Override
    public UserInfo getUserInfo(String userName) {
        UserExample userExample = new UserExample();
        userExample.createCriteria().andUserNameEqualTo(userName);
        List<User> users = userMapper.selectByExample(userExample);
        if (users != null && users.size() != 0) {
            User user = users.get(0);
            UserInfo userInfo = new UserInfo();
            BeanUtils.copyProperties(user, userInfo);
            return userInfo;
        }
        return null;
    }

    @Override
    public String saveUserInfo(UserInfo userInfo) {
        try {
            UserExample userExample = new UserExample();
            String userName = userInfo.getUserName();
            userExample.createCriteria().andUserNameEqualTo(userName);
            User user = new User();
            BeanUtils.copyProperties(userInfo, user);
            userMapper.updateByExampleSelective(user, userExample);
            return "success";
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return "fail";
        }
    }

}
