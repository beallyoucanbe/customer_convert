package com.smart.sso.server.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smart.sso.server.model.Events;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.primary.mapper.EventsMapper;
import com.smart.sso.server.service.EventService;
import com.smart.sso.server.util.CommonUtils;
import com.smart.sso.server.util.DateUtil;
import com.smart.sso.server.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EventServiceImpl implements EventService {

    @Autowired
    private EventsMapper eventsMapper;
    private static String recordTitle = "[{\"key\":\"client\",\"label\":\"客户端\"},{\"key\":\"event_type\",\"label\":\"行为类型\"},{\"key\":\"event_time\",\"label\":\"访问时间\"},{\"key\":\"action_content\",\"label\":\"访问内容\"},{\"key\":\"action_section\",\"label\":\"访问栏目\"},{\"key\":\"event_duration\",\"label\":\"访问时长\"}]";

    @Override
    public CustomerFeatureResponse.CourseContent getDeliveryCourseListenContent(String userId) {
        // 交付课事件
        String deliveryCourseEvent = "";
        String deliveryCourseActionType = "";
        // 获取听课次数
        int total = eventsMapper.getCountByUserIdAndEventNameActionType(Integer.parseInt(userId), deliveryCourseEvent, deliveryCourseActionType);
        // 获取听课次数
        int process = eventsMapper.getCountByUserIdAndEventNameActionType(Integer.parseInt(userId), deliveryCourseEvent, deliveryCourseActionType);
        List<Events> events = eventsMapper.getEventsByUserIdAndEventNameActionType(Integer.parseInt(userId), deliveryCourseEvent, deliveryCourseActionType);

        CustomerFeatureResponse.CourseContent deliveryCourseListenContent = new CustomerFeatureResponse.CourseContent();
        deliveryCourseListenContent.setTotal(total);
        deliveryCourseListenContent.setProcess(process);
        deliveryCourseListenContent.setRecords(getRecordContent(events));
        return deliveryCourseListenContent;
    }

    @Override
    public CustomerFeatureResponse.CourseContent getMarketingCourseListenContent(String userId) {
        // 营销课事件
        String marketingCourseEvent = "";
        String marketingCourseActionType = "";
        // 获取听课次数
        int total = eventsMapper.getCountByUserIdAndEventNameActionType(Integer.parseInt(userId), marketingCourseEvent, marketingCourseActionType);
        // 获取听课次数
        int process = eventsMapper.getCountByUserIdAndEventNameActionType(Integer.parseInt(userId), marketingCourseEvent, marketingCourseActionType);
        List<Events> events = eventsMapper.getEventsByUserIdAndEventNameActionType(Integer.parseInt(userId), marketingCourseEvent, marketingCourseActionType);

        CustomerFeatureResponse.CourseContent deliveryCourseListenContent = new CustomerFeatureResponse.CourseContent();
        deliveryCourseListenContent.setTotal(total);
        deliveryCourseListenContent.setProcess(process);
        deliveryCourseListenContent.setRecords(getRecordContent(events));
        return deliveryCourseListenContent;
    }

    @Override
    public CustomerFeatureResponse.FrequencyContent getVisitFreqContent(String userId, LocalDateTime customerCreateTime) {
        // 直播/圈子访问事件
        String visitEvent = "visit";
        String visitActionType = "kgs";
        CustomerFeatureResponse.FrequencyContent visitFreqContent = new CustomerFeatureResponse.FrequencyContent();
        // 这里需要计算访问频次，计算规则
        List<Events> events = eventsMapper.getEventsByUserIdAndEventNameActionType(Integer.parseInt(userId), visitEvent, visitActionType);
        if (CollectionUtils.isEmpty(events)) {
            return visitFreqContent;
        }
        // 访问总时间(转换为分钟)
        int eventDurationSum =  events.stream().mapToInt(Events::getEventDuration).sum() / 60;
        // 计算频次
        int days = CommonUtils.calculateDaysDifference(customerCreateTime);
        // 这里计算平均每天多少分钟
        double fre = (double) eventDurationSum/days;
        String formattedResult = String.format("%.1f", fre);
        visitFreqContent.setValue(Double.parseDouble(formattedResult));
        visitFreqContent.setRecords(getRecordContent(events));
        return visitFreqContent;
    }

    @Override
    public CustomerFeatureResponse.FrequencyContent getFunctionFreqContent(String userId) {
        // 功能指标使用事件
        String functionEvent = "";
        String functionActionType = "";
        // 这里需要计算访问频次，计算规则
        List<Events> events = eventsMapper.getEventsByUserIdAndEventNameActionType(Integer.parseInt(userId), functionEvent, functionActionType);
        CustomerFeatureResponse.FrequencyContent functionFreqContent = new CustomerFeatureResponse.FrequencyContent();
        functionFreqContent.setValue("");
        functionFreqContent.setRecords(getRecordContent(events));
        return functionFreqContent;
    }

    public CustomerFeatureResponse.RecordContent getRecordContent(List<Events> events) {
        CustomerFeatureResponse.RecordContent recordContent = new CustomerFeatureResponse.RecordContent();
        List<CustomerFeatureResponse.RecordTitle> columns = JsonUtil.readValue(recordTitle, new TypeReference<List<CustomerFeatureResponse.RecordTitle>>() {
        });
        recordContent.setColumns(columns);
        recordContent.setData(convertEventDTOFromEvent(events));
        return recordContent;
    }

    private List<Map<String, Object>> convertEventDTOFromEvent(List<Events> events){
        if (CollectionUtils.isEmpty(events)){
            return null;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Events item : events){
            Map<String, Object> dto = new HashMap<>();
            dto.put("client", item.getClassType());
//            dto.put("event_type", item.getEventName());
            dto.put("event_type", "浏览");
            dto.put("event_time", DateUtil.getFormatTime(item.getEventTime()));
            dto.put("event_duration", CommonUtils.getTimeStringWithMinute(item.getEventDuration()));
            dto.put("action_section", item.getActionType());
            dto.put("action_content", item.getActionContent());
            result.add(dto);
        }
        return result;
    }

}
