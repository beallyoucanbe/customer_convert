package com.smart.sso.server.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smart.sso.server.model.EventDTO;
import com.smart.sso.server.model.Events;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.primary.mapper.EventsMapper;
import com.smart.sso.server.service.EventService;
import com.smart.sso.server.util.DateUtil;
import com.smart.sso.server.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class EventServiceImpl implements EventService {

    @Autowired
    private EventsMapper eventsMapper;
    private static String recordTitle = "[{\"key\":\"client\",\"label\":\"客户端\"},{\"key\":\"event_type\",\"label\":\"行为类型\"},{\"key\":\"event_time\",\"label\":\"访问时间\"},{\"key\":\"action_content\",\"label\":\"访问内容\"},{\"key\":\"action_section\",\"label\":\"访问栏目\"},{\"key\":\"event_duration\",\"label\":\"访问时长\"}]";

    @Override
    public CustomerFeatureResponse.RecordContent getRecordContent(String userId, String eventName) {
        CustomerFeatureResponse.RecordContent recordContent = new CustomerFeatureResponse.RecordContent();
        List<Events> events = eventsMapper.getEventsByUserIdAndEventName(Integer.parseInt(userId), eventName);
        List<CustomerFeatureResponse.RecordTitle> columns = JsonUtil.readValue(recordTitle, new TypeReference<List<CustomerFeatureResponse.RecordTitle>>() {
        });
        recordContent.setColumns(columns);
        recordContent.setData(convertEventDTOFromEvent(events));
        return recordContent;
    }

    public List<Events> getEventsByUserIdAndEventName(String userId, String eventName) {
        return eventsMapper.getEventsByUserIdAndEventName(Integer.parseInt(userId), eventName);
    }

    private List<EventDTO> convertEventDTOFromEvent(List<Events> events){
        if (CollectionUtils.isEmpty(events)){
            return null;
        }
        List<EventDTO> result = new ArrayList<>();
        for (Events item : events){
            EventDTO dto = new EventDTO();
            dto.setClient(item.getClassType());
            dto.setEventType(item.getEventName());
            dto.setEventTime(DateUtil.getFormatTime(item.getEventTime()));
            dto.setEventDuration(item.getEventDuration());
            dto.setActionSection(item.getActionType());
            dto.setActionContent(item.getActionContent());
            result.add(dto);
        }
        return result;
    }

}
