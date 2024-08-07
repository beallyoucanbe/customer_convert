package com.smart.sso.server.service;

import com.smart.sso.server.model.TextMessage;

public interface MessageService {

    String sendMessageToChat(String url, TextMessage message);

    /**
     * 消息内容示例（列表里的值出自“阶段”的值和“客户认可度对应的模型记录”的值：
     *
     * 您刚和客户XXX通完电话，该客户的匹配度较高/中等/较低（不应重点跟进）/未完成判断。
     * 截至本次通话已完成：
     *         1、客户交易风格了解
     *         2、客户认可老师和课程
     *         3、客户理解了软件功能
     *          4、客户认可选股方法
     *          5、客户认可自身问题
     *          6、客户认可软件价值
     *          7、客户确认购买
     *          8、客户完成购买
     * 截至本次通话遗留问题，待下次通话解决：
     *         1、客户对老师和课程不认可
     *         2、客户对软件功能不理解
     *          3、客户对选股方法不认可
     *          4、客户对自身问题不认可
     *          5、客户对软件价值不认可
     *          6、客户拒绝购买
     *
     * 详细内容链接：http://xxxxxxxxx（嵌入天网的该客户详情页链接）
     * @param id
     * @return
     */
    void sendNoticeForSingle(String id);

    void sendNoticeForLeader(String id);

}
