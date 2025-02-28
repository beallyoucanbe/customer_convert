package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smart.sso.server.model.CustomerBase;
import com.smart.sso.server.primary.mapper.CustomerBaseMapper;
import com.smart.sso.server.primary.mapper.TelephoneRecordMapper;
import com.smart.sso.server.model.CommunicationContent;
import com.smart.sso.server.model.CustomerFeatureFromLLM;
import com.smart.sso.server.model.TelephoneRecord;
import com.smart.sso.server.model.TelephoneRecordStatics;
import com.smart.sso.server.model.VO.ChatDetail;
import com.smart.sso.server.model.VO.ChatHistoryVO;
import com.smart.sso.server.service.ConfigService;
import com.smart.sso.server.service.TelephoneRecordService;

import com.smart.sso.server.util.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class TelephoneRecordServiceImpl implements TelephoneRecordService {

    @Autowired
    private TelephoneRecordMapper recordMapper;
    @Autowired
    private CustomerBaseMapper customerBaseMapper;
    @Autowired
    private TelephoneRecordMapper telephoneRecordMapper;
    @Autowired
    private ConfigService configService;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public CustomerFeatureFromLLM getCustomerFeatureFromLLM(String customerId, String activityId) {

        CustomerFeatureFromLLM customerFeatureFromLLM = new CustomerFeatureFromLLM();

        QueryWrapper<TelephoneRecord> queryWrapper = new QueryWrapper<>();
        // 按照沟通时间倒序排列
        queryWrapper.eq("customer_id", customerId);
        queryWrapper.eq("activity_id", activityId);
        queryWrapper.orderBy(true, false, "communication_time");
        List<TelephoneRecord> records = recordMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(records)) {
            return null;
        }
        customerFeatureFromLLM.setCommunicationTime(records.get(0).getCommunicationTime());
        // 对该客户下的所有的通话记录进行总结
        for (TelephoneRecord record : records) {
            if (Objects.isNull(customerFeatureFromLLM.getLatestTimeTelephone()) && record.getCommunicationType().equals("phone")){
                customerFeatureFromLLM.setLatestTimeTelephone(record.getCommunicationTime());
            }
            if (record.getCommunicationType().equals("phone")){
                customerFeatureFromLLM.setFirstTimeTelephone(record.getCommunicationTime());
            }
            //客户的资金体量
            if (!CollectionUtils.isEmpty(record.getFundsVolume())) {
                CommunicationContent communicationContent = record.getFundsVolume().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getFundsVolume())) {
                    customerFeatureFromLLM.setFundsVolume(communicationContent);
                    customerFeatureFromLLM.getFundsVolume().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getFundsVolume().getQuestion()) ||
                            customerFeatureFromLLM.getFundsVolume().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getFundsVolume().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getFundsVolume().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getFundsVolume().getAnswerText()) ||
                            customerFeatureFromLLM.getFundsVolume().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getFundsVolume().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getFundsVolume().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getFundsVolume().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getFundsVolume().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getFundsVolume().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //客户有没有时间
            if (!CollectionUtils.isEmpty(record.getHasTime())) {
                CommunicationContent communicationContent = record.getHasTime().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getHasTime())) {
                    customerFeatureFromLLM.setHasTime(communicationContent);
                    customerFeatureFromLLM.getHasTime().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getHasTime().getQuestion()) ||
                            customerFeatureFromLLM.getHasTime().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getHasTime().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getHasTime().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getHasTime().getAnswerText()) ||
                            customerFeatureFromLLM.getHasTime().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getHasTime().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getHasTime().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getHasTime().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getHasTime().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getHasTime().setAnswerCallId(record.getCallId());
                    }
                }
            }

            //服务介绍1
            if (!CollectionUtils.isEmpty(record.getIntroduceService_1())) {
                CommunicationContent communicationContent = record.getIntroduceService_1().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getIntroduceService_1())) {
                    customerFeatureFromLLM.setIntroduceService_1(communicationContent);
                    customerFeatureFromLLM.getIntroduceService_1().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getIntroduceService_1().getQuestion()) ||
                            customerFeatureFromLLM.getIntroduceService_1().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getIntroduceService_1().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getIntroduceService_1().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getIntroduceService_1().getAnswerText()) ||
                            customerFeatureFromLLM.getIntroduceService_1().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getIntroduceService_1().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getIntroduceService_1().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getIntroduceService_1().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getIntroduceService_1().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getIntroduceService_1().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //服务介绍2
            if (!CollectionUtils.isEmpty(record.getIntroduceService_2())) {
                CommunicationContent communicationContent = record.getIntroduceService_2().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getIntroduceService_2())) {
                    customerFeatureFromLLM.setIntroduceService_2(communicationContent);
                    customerFeatureFromLLM.getIntroduceService_2().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getIntroduceService_2().getQuestion()) ||
                            customerFeatureFromLLM.getIntroduceService_2().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getIntroduceService_2().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getIntroduceService_2().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getIntroduceService_2().getAnswerText()) ||
                            customerFeatureFromLLM.getIntroduceService_2().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getIntroduceService_2().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getIntroduceService_2().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getIntroduceService_2().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getIntroduceService_2().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getIntroduceService_2().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //服务介绍3
            if (!CollectionUtils.isEmpty(record.getIntroduceService_3())) {
                CommunicationContent communicationContent = record.getIntroduceService_3().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getIntroduceService_3())) {
                    customerFeatureFromLLM.setIntroduceService_3(communicationContent);
                    customerFeatureFromLLM.getIntroduceService_3().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getIntroduceService_3().getQuestion()) ||
                            customerFeatureFromLLM.getIntroduceService_3().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getIntroduceService_3().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getIntroduceService_3().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getIntroduceService_3().getAnswerText()) ||
                            customerFeatureFromLLM.getIntroduceService_3().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getIntroduceService_3().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getIntroduceService_3().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getIntroduceService_3().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getIntroduceService_3().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getIntroduceService_3().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //服务介绍4
            if (!CollectionUtils.isEmpty(record.getIntroduceService_4())) {
                CommunicationContent communicationContent = record.getIntroduceService_4().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getIntroduceService_4())) {
                    customerFeatureFromLLM.setIntroduceService_4(communicationContent);
                    customerFeatureFromLLM.getIntroduceService_4().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getIntroduceService_4().getQuestion()) ||
                            customerFeatureFromLLM.getIntroduceService_4().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getIntroduceService_4().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getIntroduceService_4().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getIntroduceService_4().getAnswerText()) ||
                            customerFeatureFromLLM.getIntroduceService_4().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getIntroduceService_4().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getIntroduceService_4().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getIntroduceService_4().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getIntroduceService_4().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getIntroduceService_4().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //服务介绍5
            if (!CollectionUtils.isEmpty(record.getIntroduceService_5())) {
                CommunicationContent communicationContent = record.getIntroduceService_5().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getIntroduceService_5())) {
                    customerFeatureFromLLM.setIntroduceService_5(communicationContent);
                    customerFeatureFromLLM.getIntroduceService_5().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getIntroduceService_5().getQuestion()) ||
                            customerFeatureFromLLM.getIntroduceService_5().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getIntroduceService_5().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getIntroduceService_5().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getIntroduceService_5().getAnswerText()) ||
                            customerFeatureFromLLM.getIntroduceService_5().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getIntroduceService_5().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getIntroduceService_5().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getIntroduceService_5().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getIntroduceService_5().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getIntroduceService_5().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //提醒1
            if (!CollectionUtils.isEmpty(record.getRemindService_1())) {
                CommunicationContent communicationContent = record.getRemindService_1().get(0);
                if (!StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                        !communicationContent.getAnswerText().equals("无")) {
                    communicationContent.setCallId(record.getCallId());
                    communicationContent.setTs(record.getCommunicationTime().format(formatter));
                    customerFeatureFromLLM.getRemindService_1().add(communicationContent);
                }
            }
            //提醒2
            if (!CollectionUtils.isEmpty(record.getRemindService_2())) {
                CommunicationContent communicationContent = record.getRemindService_2().get(0);
                if (!StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                        !communicationContent.getAnswerText().equals("无")) {
                    communicationContent.setCallId(record.getCallId());
                    communicationContent.setTs(record.getCommunicationTime().format(formatter));
                    customerFeatureFromLLM.getRemindService_2().add(communicationContent);
                }
            }
            //提醒3
            if (!CollectionUtils.isEmpty(record.getRemindService_3())) {
                CommunicationContent communicationContent = record.getRemindService_3().get(0);
                if (!StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                        !communicationContent.getAnswerText().equals("无")) {
                    communicationContent.setCallId(record.getCallId());
                    communicationContent.setTs(record.getCommunicationTime().format(formatter));
                    customerFeatureFromLLM.getRemindService_3().add(communicationContent);
                }
            }
            //提醒4
            if (!CollectionUtils.isEmpty(record.getRemindService_4())) {
                CommunicationContent communicationContent = record.getRemindService_4().get(0);
                if (!StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                        !communicationContent.getAnswerText().equals("无")) {
                    communicationContent.setCallId(record.getCallId());
                    communicationContent.setTs(record.getCommunicationTime().format(formatter));
                    customerFeatureFromLLM.getRemindService_4().add(communicationContent);
                }
            }
            //提醒5
            if (!CollectionUtils.isEmpty(record.getRemindService_5())) {
                CommunicationContent communicationContent = record.getRemindService_5().get(0);
                if (!StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                        !communicationContent.getAnswerText().equals("无")) {
                    communicationContent.setCallId(record.getCallId());
                    communicationContent.setTs(record.getCommunicationTime().format(formatter));
                    customerFeatureFromLLM.getRemindService_5().add(communicationContent);
                }
            }
            //客户当前持仓或关注的股票
            if (!CollectionUtils.isEmpty(record.getCurrentStocks())) {
                CommunicationContent communicationContent = record.getCurrentStocks().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getCurrentStocks())) {
                    customerFeatureFromLLM.setCurrentStocks(communicationContent);
                    customerFeatureFromLLM.getCurrentStocks().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCurrentStocks().getQuestion()) ||
                            customerFeatureFromLLM.getCurrentStocks().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getCurrentStocks().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getCurrentStocks().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCurrentStocks().getAnswerText()) ||
                            customerFeatureFromLLM.getCurrentStocks().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getCurrentStocks().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getCurrentStocks().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getCurrentStocks().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getCurrentStocks().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getCurrentStocks().setExplanation(communicationContent.getExplanation());
                        customerFeatureFromLLM.getCurrentStocks().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //客户的交易风格
            if (!CollectionUtils.isEmpty(record.getTradingStyle())) {
                CommunicationContent communicationContent = record.getTradingStyle().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getTradingStyle())) {
                    customerFeatureFromLLM.setTradingStyle(communicationContent);
                    customerFeatureFromLLM.getTradingStyle().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getTradingStyle().getQuestion()) ||
                            customerFeatureFromLLM.getTradingStyle().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getTradingStyle().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getTradingStyle().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getTradingStyle().getAnswerText()) ||
                            customerFeatureFromLLM.getTradingStyle().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getTradingStyle().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getTradingStyle().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getTradingStyle().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getTradingStyle().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getTradingStyle().setExplanation(communicationContent.getExplanation());
                        customerFeatureFromLLM.getTradingStyle().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //客户的股龄
            if (!CollectionUtils.isEmpty(record.getStockMarketAge())) {
                CommunicationContent communicationContent = record.getStockMarketAge().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getStockMarketAge())) {
                    customerFeatureFromLLM.setStockMarketAge(communicationContent);
                    customerFeatureFromLLM.getStockMarketAge().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockMarketAge().getQuestion()) ||
                            customerFeatureFromLLM.getStockMarketAge().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getStockMarketAge().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getStockMarketAge().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockMarketAge().getAnswerText()) ||
                            customerFeatureFromLLM.getStockMarketAge().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getStockMarketAge().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getStockMarketAge().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getStockMarketAge().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getStockMarketAge().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getStockMarketAge().setExplanation(communicationContent.getExplanation());
                        customerFeatureFromLLM.getStockMarketAge().setAnswerCallId(record.getCallId());
                    }
                }
            }

            //提醒查看交付课直播
            if (!CollectionUtils.isEmpty(record.getDeliveryRemindLive())) {
                CommunicationContent communicationContent = record.getDeliveryRemindLive().get(0);
                if (!StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                        !communicationContent.getAnswerText().equals("无")) {
                    communicationContent.setCallId(record.getCallId());
                    communicationContent.setTs(record.getCommunicationTime().format(formatter));
                    customerFeatureFromLLM.getDeliveryRemindLive().add(communicationContent);
                }
            }
            //提醒查看交付课回放
            if (!CollectionUtils.isEmpty(record.getDeliveryRemindPlayback())) {
                CommunicationContent communicationContent = record.getDeliveryRemindPlayback().get(0);
                if (!StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                        !communicationContent.getAnswerText().equals("无")) {
                    communicationContent.setCallId(record.getCallId());
                    communicationContent.setTs(record.getCommunicationTime().format(formatter));
                    customerFeatureFromLLM.getDeliveryRemindPlayback().add(communicationContent);
                }
            }

            //交付期沟通
            if (!CollectionUtils.isEmpty(record.getHomework())) {
                CommunicationContent communicationContent = record.getHomework().get(0);
                if (!StringUtils.isEmpty(communicationContent.getQuestion()) &&
                        !communicationContent.getQuestion().equals("无")) {
                    communicationContent.setCallId(record.getCallId());
                    communicationContent.setTs(record.getCommunicationTime().format(formatter));
                    customerFeatureFromLLM.getHomework().add(communicationContent);
                }
            }

            //客户对老师的认可度
            if (!CollectionUtils.isEmpty(record.getTeacherApproval())) {
                CommunicationContent communicationContent = record.getTeacherApproval().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getTeacherApproval())) {
                    customerFeatureFromLLM.setTeacherApproval(communicationContent);
                    customerFeatureFromLLM.getTeacherApproval().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getTeacherApproval().getQuestion()) ||
                            customerFeatureFromLLM.getTeacherApproval().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getTeacherApproval().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getTeacherApproval().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getTeacherApproval().getAnswerText()) ||
                            customerFeatureFromLLM.getTeacherApproval().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getTeacherApproval().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getTeacherApproval().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getTeacherApproval().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getTeacherApproval().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getTeacherApproval().setAnswerCallId(record.getCallId());
                    }
                }
            }

            //客户对课程的认可度1
            if (!CollectionUtils.isEmpty(record.getCourseMaster_1())) {
                CommunicationContent communicationContent = record.getCourseMaster_1().get(0);
                communicationContent.setTs(record.getCommunicationTime().format(formatter));
                if (Objects.isNull(customerFeatureFromLLM.getCourseMaster_1())) {
                    customerFeatureFromLLM.setCourseMaster_1(communicationContent);
                    customerFeatureFromLLM.getCourseMaster_1().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCourseMaster_1().getQuestion()) ||
                            customerFeatureFromLLM.getCourseMaster_1().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getCourseMaster_1().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getCourseMaster_1().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCourseMaster_1().getAnswerText()) ||
                            customerFeatureFromLLM.getCourseMaster_1().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getCourseMaster_1().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getCourseMaster_1().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getCourseMaster_1().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getCourseMaster_1().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getCourseMaster_1().setAnswerCallId(record.getCallId());
                    }
                }
            }

            if (!CollectionUtils.isEmpty(record.getCourseMaster_2())) {
                CommunicationContent communicationContent = record.getCourseMaster_2().get(0);
                communicationContent.setTs(record.getCommunicationTime().format(formatter));
                if (Objects.isNull(customerFeatureFromLLM.getCourseMaster_2())) {
                    customerFeatureFromLLM.setCourseMaster_2(communicationContent);
                    customerFeatureFromLLM.getCourseMaster_2().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCourseMaster_2().getQuestion()) ||
                            customerFeatureFromLLM.getCourseMaster_2().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getCourseMaster_2().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getCourseMaster_2().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCourseMaster_2().getAnswerText()) ||
                            customerFeatureFromLLM.getCourseMaster_2().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getCourseMaster_2().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getCourseMaster_2().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getCourseMaster_2().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getCourseMaster_2().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getCourseMaster_2().setAnswerCallId(record.getCallId());
                    }
                }
            }

            if (!CollectionUtils.isEmpty(record.getCourseMaster_3())) {
                CommunicationContent communicationContent = record.getCourseMaster_3().get(0);
                communicationContent.setTs(record.getCommunicationTime().format(formatter));
                if (Objects.isNull(customerFeatureFromLLM.getCourseMaster_3())) {
                    customerFeatureFromLLM.setCourseMaster_3(communicationContent);
                    customerFeatureFromLLM.getCourseMaster_3().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCourseMaster_3().getQuestion()) ||
                            customerFeatureFromLLM.getCourseMaster_3().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getCourseMaster_3().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getCourseMaster_3().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCourseMaster_3().getAnswerText()) ||
                            customerFeatureFromLLM.getCourseMaster_3().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getCourseMaster_3().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getCourseMaster_3().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getCourseMaster_3().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getCourseMaster_3().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getCourseMaster_3().setAnswerCallId(record.getCallId());
                    }
                }
            }

            if (!CollectionUtils.isEmpty(record.getCourseMaster_4())) {
                CommunicationContent communicationContent = record.getCourseMaster_4().get(0);
                communicationContent.setTs(record.getCommunicationTime().format(formatter));
                if (Objects.isNull(customerFeatureFromLLM.getCourseMaster_4())) {
                    customerFeatureFromLLM.setCourseMaster_4(communicationContent);
                    customerFeatureFromLLM.getCourseMaster_4().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCourseMaster_4().getQuestion()) ||
                            customerFeatureFromLLM.getCourseMaster_4().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getCourseMaster_4().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getCourseMaster_4().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCourseMaster_4().getAnswerText()) ||
                            customerFeatureFromLLM.getCourseMaster_4().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getCourseMaster_4().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getCourseMaster_4().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getCourseMaster_4().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getCourseMaster_4().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getCourseMaster_4().setAnswerCallId(record.getCallId());
                    }
                }
            }

            if (!CollectionUtils.isEmpty(record.getCourseMaster_5())) {
                CommunicationContent communicationContent = record.getCourseMaster_5().get(0);
                communicationContent.setTs(record.getCommunicationTime().format(formatter));
                if (Objects.isNull(customerFeatureFromLLM.getCourseMaster_5())) {
                    customerFeatureFromLLM.setCourseMaster_5(communicationContent);
                    customerFeatureFromLLM.getCourseMaster_5().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCourseMaster_5().getQuestion()) ||
                            customerFeatureFromLLM.getCourseMaster_5().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getCourseMaster_5().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getCourseMaster_5().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCourseMaster_5().getAnswerText()) ||
                            customerFeatureFromLLM.getCourseMaster_5().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getCourseMaster_5().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getCourseMaster_5().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getCourseMaster_5().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getCourseMaster_5().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getCourseMaster_5().setAnswerCallId(record.getCallId());
                    }
                }
            }

            if (!CollectionUtils.isEmpty(record.getCourseMaster_6())) {
                CommunicationContent communicationContent = record.getCourseMaster_6().get(0);
                communicationContent.setTs(record.getCommunicationTime().format(formatter));
                if (Objects.isNull(customerFeatureFromLLM.getCourseMaster_6())) {
                    customerFeatureFromLLM.setCourseMaster_6(communicationContent);
                    customerFeatureFromLLM.getCourseMaster_6().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCourseMaster_6().getQuestion()) ||
                            customerFeatureFromLLM.getCourseMaster_6().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getCourseMaster_6().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getCourseMaster_6().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCourseMaster_6().getAnswerText()) ||
                            customerFeatureFromLLM.getCourseMaster_6().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getCourseMaster_6().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getCourseMaster_6().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getCourseMaster_6().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getCourseMaster_6().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getCourseMaster_6().setAnswerCallId(record.getCallId());
                    }
                }
            }

            if (!CollectionUtils.isEmpty(record.getCourseMaster_7())) {
                CommunicationContent communicationContent = record.getCourseMaster_7().get(0);
                communicationContent.setTs(record.getCommunicationTime().format(formatter));
                if (Objects.isNull(customerFeatureFromLLM.getCourseMaster_7())) {
                    customerFeatureFromLLM.setCourseMaster_7(communicationContent);
                    customerFeatureFromLLM.getCourseMaster_7().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCourseMaster_7().getQuestion()) ||
                            customerFeatureFromLLM.getCourseMaster_7().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getCourseMaster_7().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getCourseMaster_7().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCourseMaster_7().getAnswerText()) ||
                            customerFeatureFromLLM.getCourseMaster_7().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getCourseMaster_7().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getCourseMaster_7().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getCourseMaster_7().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getCourseMaster_7().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getCourseMaster_7().setAnswerCallId(record.getCallId());
                    }
                }
            }
            // 客户是否要求退款
            if (!CollectionUtils.isEmpty(record.getCustomerRequireRefund())) {
                CommunicationContent communicationContent = record.getCustomerRequireRefund().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getCustomerRequireRefund())) {
                    customerFeatureFromLLM.setCustomerRequireRefund(communicationContent);
                    customerFeatureFromLLM.getCustomerRequireRefund().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCustomerRequireRefund().getAnswerText()) ||
                            customerFeatureFromLLM.getCustomerRequireRefund().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getCustomerRequireRefund().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getCustomerRequireRefund().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getCustomerRequireRefund().setAnswerCallId(record.getCallId());
                    }
                }
            }
        }
        return customerFeatureFromLLM;
    }

    @Override
    public ChatDetail getChatDetail(String customerId, String activityId, String callId) {
        QueryWrapper<TelephoneRecord> queryWrapper = new QueryWrapper<>();
        // 按照沟通时间倒序排列
        queryWrapper.eq("customer_id", customerId);
        queryWrapper.eq("activity_id", activityId);
        queryWrapper.eq("call_id", callId);
        queryWrapper.orderBy(false, false, "communication_time");
        TelephoneRecord record = recordMapper.selectOne(queryWrapper);
        if (Objects.isNull(record)) {
            return null;
        }
        ChatDetail chatDetail = new ChatDetail();
        chatDetail.setId(record.getId());
        chatDetail.setCommunicationTime(record.getCommunicationTime());
        chatDetail.setCommunicationDuration(record.getCommunicationDuration());

        String filePath = "/data/customer-convert/callback/files/" + record.getCallId(); // 文件路径
        List<ChatDetail.Message> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            String lineTitle = null;
            while ((line = br.readLine()) != null &&
                    line.split(" ").length >= 2 &&
                    (line.contains("2024-") || line.contains("2025-"))) {
                lineTitle = line;
                break;
            }
            while (!StringUtils.isEmpty(lineTitle)) {
                ChatDetail.Message message = new ChatDetail.Message();
                message.setRole(lineTitle.substring(0, lineTitle.indexOf(" ")));
                message.setTime(lineTitle.substring(lineTitle.indexOf(" ") + 1));
                lineTitle = null;
                StringBuilder content = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    if (line.split(" ").length >= 2 && (line.contains("2024-") || line.contains("2025-"))) {
                        lineTitle = line;
                        message.setContent(content.substring(0, content.length() - 1));
                        result.add(message);
                        break;
                    } else {
                        content.append(line).append("\n");
                    }
                }
                if (StringUtils.isEmpty(lineTitle)) {
                    message.setContent(content.substring(0, content.length() - 1));
                    result.add(message);
                }
            }
        } catch (IOException e) {
            log.error("读取文件失败：", e);
        }
        chatDetail.setMessages(result);
        return chatDetail;
    }

    @Override
    public List<ChatHistoryVO> getChatHistory(String customerId, String activityId) {
        QueryWrapper<TelephoneRecord> queryWrapper = new QueryWrapper<>();
        // 按照沟通时间倒序排列
        queryWrapper.eq("customer_id", customerId);
        queryWrapper.eq("activity_id", activityId);
        queryWrapper.orderBy(true, false, "communication_time");
        List<TelephoneRecord> records = recordMapper.selectList(queryWrapper);

        List<ChatHistoryVO> result = new ArrayList<>();
        if (!CollectionUtils.isEmpty(records)) {
            for (TelephoneRecord record : records) {
                ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
                chatHistoryVO.setId(record.getCallId());
                chatHistoryVO.setCommunicationTime(record.getCommunicationTime());
                chatHistoryVO.setCommunicationDuration(record.getCommunicationDuration());
                chatHistoryVO.setType(record.getCommunicationType());
                ChatHistoryVO.ChatHistoryInfo basic = new ChatHistoryVO.ChatHistoryInfo();
                chatHistoryVO.setBasic(basic);
                result.add(chatHistoryVO);
            }
        }
        return result;
    }

    @Override
    public Boolean syncCustomerInfo() {
        // 筛选时间
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        List<TelephoneRecordStatics> customerRecordList = getCustomerIdUpdate(dateTime);
        if (CollectionUtils.isEmpty(customerRecordList)) {
            return Boolean.TRUE;
        }
        for (TelephoneRecordStatics record : customerRecordList) {
            syncCustomerInfoFromRecord(record.getCustomerId(), record.getActivityId());
        }
        return Boolean.TRUE;
    }

    @Override
    public void refreshCommunicationRounds() {
        String activityId = configService.getCurrentActivityId();
        List<TelephoneRecordStatics> customerIdList = recordMapper.selectTelephoneRecordStatics(activityId);
        if (CollectionUtils.isEmpty(customerIdList)) {
            return;
        }
        for (TelephoneRecordStatics item : customerIdList) {
            CustomerBase customerBase = customerBaseMapper.selectByCustomerIdAndCampaignId(item.getCustomerId(), item.getActivityId());
            if (item.getFirstCommunicationTime().isBefore(customerBase.getCreateTime())) {
                customerBaseMapper.updateCommunicationRoundsCreatetime(item.getCustomerId(), activityId, item.getTotalCalls(), item.getLatestCommunicationTime(), item.getFirstCommunicationTime());
            } else {
                customerBaseMapper.updateCommunicationRounds(item.getCustomerId(), activityId, item.getTotalCalls(), item.getLatestCommunicationTime());
            }
        }
    }

    @Override
    public int getCommunicationTimeCurrentDay(String customerId, LocalDateTime communicationTime) {
        QueryWrapper<TelephoneRecord> queryWrapperInfo = new QueryWrapper<>();
        LocalDateTime startOfDay = communicationTime.toLocalDate().atStartOfDay();
        queryWrapperInfo.eq("customer_id", customerId);
        queryWrapperInfo.gt("communication_time", startOfDay);
        // 查看该客户当天的通话时间长度
        List<TelephoneRecord> telephoneRecordList = telephoneRecordMapper.selectList(queryWrapperInfo);
        int communicationDurationSum = 0;
        if (!CollectionUtils.isEmpty(telephoneRecordList)) {
            for (TelephoneRecord item : telephoneRecordList) {
                communicationDurationSum += item.getCommunicationDuration();
            }
        }
        return communicationDurationSum;
    }

    @Override
    public List<TelephoneRecordStatics> getCustomerIdUpdate(LocalDateTime dateTime) {
        return recordMapper.selectTelephoneRecordStaticsRecent(dateTime);
    }

    @Override
    public int getCommunicationCountFromTime(String customerId, LocalDateTime dateTime) {
        QueryWrapper<TelephoneRecord> queryWrapperInfo = new QueryWrapper<>();
        queryWrapperInfo.eq("customer_id", customerId);
        queryWrapperInfo.gt("communication_time", dateTime);
        List<TelephoneRecord> telephoneRecordList = telephoneRecordMapper.selectList(queryWrapperInfo);
        if (CollectionUtils.isEmpty(telephoneRecordList)) {
            return 0;
        } else {
            return telephoneRecordList.size();
        }
    }

    @Override
    public TelephoneRecordStatics getCommunicationRound(String customerId, String activityId) {
        return recordMapper.selectTelephoneRecordStaticsOne(customerId, activityId);
    }

    @Override
    public CustomerBase syncCustomerInfoFromRecord(String customerId, String activityId) {
        CustomerBase customerBase = customerBaseMapper.selectByCustomerIdAndCampaignId(customerId, activityId);
        // info 表存在记录，并且客户名称不为空，说明已经同步过信息，跳过
        if (Objects.nonNull(customerBase) && !StringUtils.isEmpty(customerBase.getCustomerName())) {
            return customerBase;
        }
        Map<String, String> activityIdNames = configService.getActivityIdNames();
        TelephoneRecord telephoneRecord = telephoneRecordMapper.selectOneTelephoneRecord(customerId, activityId);
        // record 也没有，不同步
        if (Objects.isNull(telephoneRecord)) {
            return null;
        }
        // info 表不存在记录，新建一条
        if (Objects.isNull(customerBase)) {
            customerBase = new CustomerBase();
            customerBase.setId(CommonUtils.generatePrimaryKey());
            customerBase.setCustomerName(telephoneRecord.getCustomerName());
            customerBase.setCustomerId(telephoneRecord.getCustomerId());
            customerBase.setOwnerName(telephoneRecord.getOwnerName());
            customerBase.setOwnerId(telephoneRecord.getOwnerId());
            customerBase.setActivityId(telephoneRecord.getActivityId());
            customerBase.setActivityName(activityIdNames.containsKey(telephoneRecord.getActivityId()) ? activityIdNames.get(telephoneRecord.getActivityId()) : telephoneRecord.getActivityId());
            customerBase.setUpdateTimeTelephone(LocalDateTime.now());
            customerBaseMapper.insert(customerBase);
        }
        return customerBase;
    }

    @Override
    public boolean existId(String id) {
        TelephoneRecord telephoneRecord = telephoneRecordMapper.selectById(id);
        return Objects.nonNull(telephoneRecord);
    }
}
