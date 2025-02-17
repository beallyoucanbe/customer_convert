package com.smart.sso.server.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smart.sso.server.model.CourseListenDetail;
import com.smart.sso.server.model.CustomerCharacter;
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

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {

    @Autowired
    private EventsMapper eventsMapper;
    private static String recordTitle = "[{\"key\":\"client\",\"label\":\"客户端\"},{\"key\":\"event_type\",\"label\":\"行为类型\"},{\"key\":\"event_time\",\"label\":\"访问时间\"},{\"key\":\"action_content\",\"label\":\"访问内容\"},{\"key\":\"action_section\",\"label\":\"访问栏目\"},{\"key\":\"event_duration\",\"label\":\"访问时长\"}]";

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public CustomerFeatureResponse.CourseContent getDeliveryCourseListenContent(String userId) {
        // 交付课事件
        String deliveryCourseEvent = "visit";
        String deliveryCourseActionType = "course_watch";
        String deliveryCourseClassType = "服务课";
        // 获取听课次数
        int total = 15;
        CustomerFeatureResponse.CourseContent deliveryCourseListenContent = new CustomerFeatureResponse.CourseContent();
        deliveryCourseListenContent.setTotal(total);
        // 获取听课数据
        List<Events> events = eventsMapper.getEventsByUserIdAndEventNameActionTypeClassType(Integer.parseInt(userId), deliveryCourseEvent, deliveryCourseActionType, deliveryCourseClassType);
        if (!CollectionUtils.isEmpty(events)) {
            Map<String, CourseListenDetail> courseListenDetailMap = new LinkedHashMap<>();
            for (Events item : events) {
                if (!courseListenDetailMap.containsKey(item.getActionContent())) {
                    CourseListenDetail one = new CourseListenDetail();
                    one.setCourseName(item.getActionContent());
                    one.setCourseListenProcess(Integer.parseInt(item.getExt1()));
                    // 听课时长，转化为分钟
                    one.setCourseListenDuration(item.getEventDuration() / 60);
                    one.setCourseListenTime(sdf.format(item.getEventTime()));
                    one.setPlayAll(one.getCourseListenDuration() >= 40);
                    courseListenDetailMap.put(one.getCourseName(), one);
                } else {
                    CourseListenDetail one = courseListenDetailMap.get(item.getActionContent());
                    one.setCourseListenDuration(one.getCourseListenDuration() + item.getEventDuration() / 60);
                    int process = Integer.parseInt(item.getExt1()) + one.getCourseListenProcess();
                    process = Math.min(process, 100);
                    one.setCourseListenProcess(process);
                    one.setPlayAll(one.getCourseListenDuration() >= 40);
                }
            }
            deliveryCourseListenContent.setProcess((int) courseListenDetailMap.values().stream().filter(CourseListenDetail::getPlayAll).count());
            deliveryCourseListenContent.setRecords(getRecordContent(courseListenDetailMap));
        }
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
    public CustomerFeatureResponse.FrequencyContent getVisitLiveFreqContent(String userId, LocalDateTime customerCreateTime) {
        // 直播访问事件
        String visitEvent = "visit";
        String visitActionType = "program";
        String visitActionContent = "沙场点兵";
        CustomerFeatureResponse.FrequencyContent visitFreqContent = new CustomerFeatureResponse.FrequencyContent();
        // 这里需要计算访问频次，计算规则
        List<Events> events = eventsMapper.getEventsByUserIdAndEventNameActionTypeActionContent(Integer.parseInt(userId), visitEvent, visitActionType, visitActionContent);
        if (CollectionUtils.isEmpty(events)) {
            return visitFreqContent;
        }
        // 访问总时间(转换为分钟)
        int eventDurationSum = events.stream().mapToInt(Events::getEventDuration).sum() / 60;
        // 计算频次
        int days = CommonUtils.calculateDaysDifference(customerCreateTime);
        // 这里计算平均每天多少分钟
        double fre = (double) eventDurationSum / days;
        String formattedResult = String.format("%.1f", fre);
        visitFreqContent.setValue(Double.parseDouble(formattedResult));
        visitFreqContent.setRecords(getRecordContent(events));
        return visitFreqContent;
    }

    @Override
    public CustomerFeatureResponse.FrequencyContent getVisitCommunityFreqContent(String userId, LocalDateTime customerCreateTime) {
        // 圈子访问事件
        String visitEvent = "visit";
        String visitActionType = "kgs";
        String visitActionContent = "智能投教圈";
        CustomerFeatureResponse.FrequencyContent visitFreqContent = new CustomerFeatureResponse.FrequencyContent();
        // 这里需要计算访问频次，计算规则
        List<Events> events = eventsMapper.getEventsByUserIdAndEventNameActionTypeActionContent(Integer.parseInt(userId), visitEvent, visitActionType, visitActionContent);
        if (CollectionUtils.isEmpty(events)) {
            return visitFreqContent;
        }
        // 访问总时间(转换为分钟)
        int eventDurationSum = events.stream().mapToInt(Events::getEventDuration).sum() / 60;
        // 计算频次
        int days = CommonUtils.calculateDaysDifference(customerCreateTime);
        // 这里计算平均每天多少分钟
        double fre = (double) eventDurationSum / days;
        String formattedResult = String.format("%.1f", fre);
        visitFreqContent.setValue(Double.parseDouble(formattedResult));
        visitFreqContent.setRecords(getRecordContent(events));
        return visitFreqContent;
    }

    @Override
    public CustomerFeatureResponse.FrequencyContent getFunctionFreqContent(String userId, LocalDateTime customerCreateTime) {
        // 功能指标使用事件
        String functionEvent = "visit";
        String functionActionType = "tool";
        CustomerFeatureResponse.FrequencyContent functionFreqContent = new CustomerFeatureResponse.FrequencyContent();
        // 这里需要计算访问频次，计算规则
        List<Events> events = eventsMapper.getEventsByUserIdAndEventNameActionType(Integer.parseInt(userId), functionEvent, functionActionType);
        if (CollectionUtils.isEmpty(events)) {
            return functionFreqContent;
        }
        events = events.stream().filter(item -> item.getActionContent().contains("主力军情") ||
                        item.getActionContent().contains("热点狙击"))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(events)) {
            return functionFreqContent;
        }
        // 访问总时间(转换为分钟)
        int eventDurationSum = events.stream().mapToInt(Events::getEventDuration).sum() / 60;
        // 计算频次
        int days = CommonUtils.calculateDaysDifference(customerCreateTime);
        // 这里计算平均每天多少分钟
        double fre = (double) eventDurationSum / days;
        String formattedResult = String.format("%.1f", fre);
        functionFreqContent.setValue(Double.parseDouble(formattedResult));
        functionFreqContent.setRecords(getRecordContent(events));
        return functionFreqContent;
    }

    @Override
    public void setDeliveryCourseCharacter(String userId, CustomerCharacter customerCharacter) {
        // 交付课事件
        String deliveryCourseEvent = "visit";
        String deliveryCourseActionType = "course_watch";
        String deliveryCourseClassType = "服务课";
        // 获取听课数据
        List<Events> events = eventsMapper.getEventsByUserIdAndEventNameActionTypeClassType(Integer.parseInt(userId), deliveryCourseEvent, deliveryCourseActionType, deliveryCourseClassType);
        if (CollectionUtils.isEmpty(events)) {
            return;
        }
        Map<String, CourseListenDetail> courseListenDetailMap = new LinkedHashMap<>();
        for (Events item : events) {
            if (!courseListenDetailMap.containsKey(item.getActionContent())) {
                CourseListenDetail one = new CourseListenDetail();
                one.setCourseName(item.getActionContent());
                one.setCourseListenProcess(Integer.parseInt(item.getExt1()));
                // 听课时长，转化为分钟
                one.setCourseListenDuration(item.getEventDuration() / 60);
                one.setCourseListenTime(sdf.format(item.getEventTime()));
                one.setPlayAll(one.getCourseListenDuration() >= 40);
                courseListenDetailMap.put(one.getCourseName(), one);
            } else {
                CourseListenDetail one = courseListenDetailMap.get(item.getActionContent());
                one.setCourseListenDuration(one.getCourseListenDuration() + item.getEventDuration() / 60);
                int process = Integer.parseInt(item.getExt1()) + one.getCourseListenProcess();
                process = Math.min(process, 100);
                one.setCourseListenProcess(process);
                one.setPlayAll(one.getCourseListenDuration() >= 40);
            }
        }

        for (Map.Entry<String, CourseListenDetail> entry : courseListenDetailMap.entrySet()) {
            String courseName = entry.getKey().trim();
            int courseStatus = 0;
            int courseListenDuration = entry.getValue().getCourseListenDuration();
            if (courseListenDuration > 40) {
                courseStatus = 2;
            } else if (courseListenDuration > 5) {
                courseStatus = 1;
            }
            if (courseName.contains("巧用“中线操盘”看多空")) {
                customerCharacter.setCourse1(courseStatus);
            } else if (courseName.contains("巧用“趋势柱线”辨强弱")) {
                customerCharacter.setCourse2(courseStatus);
            } else if (courseName.contains("巧用“两点乾坤”")) {
                customerCharacter.setCourse3(courseStatus);
            }
        }
    }

    public CustomerFeatureResponse.RecordContent getRecordContent(List<Events> events) {
        CustomerFeatureResponse.RecordContent recordContent = new CustomerFeatureResponse.RecordContent();
        List<CustomerFeatureResponse.RecordTitle> columns = JsonUtil.readValue(recordTitle, new TypeReference<List<CustomerFeatureResponse.RecordTitle>>() {
        });
        recordContent.setColumns(columns);
        recordContent.setData(convertEventDTOFromEvent(events));
        return recordContent;
    }

    public CustomerFeatureResponse.RecordContent getRecordContent(Map<String, CourseListenDetail> courseListenDetailMap) {
        CustomerFeatureResponse.RecordContent recordContent = new CustomerFeatureResponse.RecordContent();
        List<CustomerFeatureResponse.RecordTitle> columns = new ArrayList<>();
        columns.add(new CustomerFeatureResponse.RecordTitle("course_name", "课程名称"));
        columns.add(new CustomerFeatureResponse.RecordTitle("course_time", "听课时间"));
        columns.add(new CustomerFeatureResponse.RecordTitle("process", "听课进度"));
        columns.add(new CustomerFeatureResponse.RecordTitle("play_all", "是否完播"));
        recordContent.setColumns(columns);

        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<String, CourseListenDetail> entry : courseListenDetailMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("course_name", entry.getValue().getCourseName());
            item.put("course_time", entry.getValue().getCourseListenTime());
            item.put("process", entry.getValue().getCourseListenProcess());
            item.put("play_all", entry.getValue().getPlayAll());
            data.add(item);
        }
        recordContent.setData(data);
        return recordContent;
    }

    private List<Map<String, Object>> convertEventDTOFromEvent(List<Events> events) {
        if (CollectionUtils.isEmpty(events)) {
            return null;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Events item : events) {
            Map<String, Object> dto = new HashMap<>();
            dto.put("client", item.getExt1());
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
