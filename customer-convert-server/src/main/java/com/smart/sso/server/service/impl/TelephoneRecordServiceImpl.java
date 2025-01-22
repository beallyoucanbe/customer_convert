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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


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
                    communicationContent.setTs(sdf.format(record.getCommunicationTime()));
                    customerFeatureFromLLM.getRemindService_1().add(communicationContent);
                }
            }
            //提醒2
            if (!CollectionUtils.isEmpty(record.getRemindService_2())) {
                CommunicationContent communicationContent = record.getRemindService_2().get(0);
                if (!StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                        !communicationContent.getAnswerText().equals("无")) {
                    communicationContent.setCallId(record.getCallId());
                    communicationContent.setTs(sdf.format(record.getCommunicationTime()));
                    customerFeatureFromLLM.getRemindService_2().add(communicationContent);
                }
            }
            //提醒3
            if (!CollectionUtils.isEmpty(record.getRemindService_3())) {
                CommunicationContent communicationContent = record.getRemindService_3().get(0);
                if (!StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                        !communicationContent.getAnswerText().equals("无")) {
                    communicationContent.setCallId(record.getCallId());
                    communicationContent.setTs(sdf.format(record.getCommunicationTime()));
                    customerFeatureFromLLM.getRemindService_3().add(communicationContent);
                }
            }
            //提醒4
            if (!CollectionUtils.isEmpty(record.getRemindService_4())) {
                CommunicationContent communicationContent = record.getRemindService_4().get(0);
                if (!StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                        !communicationContent.getAnswerText().equals("无")) {
                    communicationContent.setCallId(record.getCallId());
                    communicationContent.setTs(sdf.format(record.getCommunicationTime()));
                    customerFeatureFromLLM.getRemindService_4().add(communicationContent);
                }
            }
            //提醒5
            if (!CollectionUtils.isEmpty(record.getRemindService_5())) {
                CommunicationContent communicationContent = record.getRemindService_5().get(0);
                if (!StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                        !communicationContent.getAnswerText().equals("无")) {
                    communicationContent.setCallId(record.getCallId());
                    communicationContent.setTs(sdf.format(record.getCommunicationTime()));
                    customerFeatureFromLLM.getRemindService_5().add(communicationContent);
                }
            }
            //客户的赚钱欲望
            if (!CollectionUtils.isEmpty(record.getEarningDesire())) {
                CommunicationContent communicationContent = record.getEarningDesire().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getEarningDesire())) {
                    customerFeatureFromLLM.setEarningDesire(communicationContent);
                    customerFeatureFromLLM.getEarningDesire().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getEarningDesire().getQuestion()) ||
                            customerFeatureFromLLM.getEarningDesire().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getEarningDesire().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getEarningDesire().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getEarningDesire().getAnswerText()) ||
                            customerFeatureFromLLM.getEarningDesire().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getEarningDesire().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getEarningDesire().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getEarningDesire().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getEarningDesire().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getEarningDesire().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //客户对软件功能的清晰度
            if (!CollectionUtils.isEmpty(record.getSoftwareFunctionClarity())) {
                CommunicationContent communicationContent = record.getSoftwareFunctionClarity().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getSoftwareFunctionClarity())) {
                    customerFeatureFromLLM.setSoftwareFunctionClarity(communicationContent);
                    customerFeatureFromLLM.getSoftwareFunctionClarity().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSoftwareFunctionClarity().getQuestion()) ||
                            customerFeatureFromLLM.getSoftwareFunctionClarity().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSoftwareFunctionClarity().getAnswerText()) ||
                            customerFeatureFromLLM.getSoftwareFunctionClarity().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //客户对选股方法的认可度
            if (!CollectionUtils.isEmpty(record.getStockSelectionMethod())) {
                CommunicationContent communicationContent = record.getStockSelectionMethod().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getStockSelectionMethod())) {
                    customerFeatureFromLLM.setStockSelectionMethod(communicationContent);
                    customerFeatureFromLLM.getStockSelectionMethod().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockSelectionMethod().getQuestion()) ||
                            customerFeatureFromLLM.getStockSelectionMethod().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getStockSelectionMethod().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getStockSelectionMethod().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockSelectionMethod().getAnswerText()) ||
                            customerFeatureFromLLM.getStockSelectionMethod().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getStockSelectionMethod().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getStockSelectionMethod().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getStockSelectionMethod().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getStockSelectionMethod().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getStockSelectionMethod().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //客户对自身问题及影响的认可度
            if (!CollectionUtils.isEmpty(record.getSelfIssueRecognition())) {
                CommunicationContent communicationContent = record.getSelfIssueRecognition().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getSelfIssueRecognition())) {
                    customerFeatureFromLLM.setSelfIssueRecognition(communicationContent);
                    customerFeatureFromLLM.getSelfIssueRecognition().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSelfIssueRecognition().getQuestion()) ||
                            customerFeatureFromLLM.getSelfIssueRecognition().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getSelfIssueRecognition().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getSelfIssueRecognition().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSelfIssueRecognition().getAnswerText()) ||
                            customerFeatureFromLLM.getSelfIssueRecognition().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getSelfIssueRecognition().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getSelfIssueRecognition().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getSelfIssueRecognition().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getSelfIssueRecognition().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getSelfIssueRecognition().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //客户对软件价值的认可度
            if (!CollectionUtils.isEmpty(record.getSoftwareValueApproval())) {
                CommunicationContent communicationContent = record.getSoftwareValueApproval().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getSoftwareValueApproval())) {
                    customerFeatureFromLLM.setSoftwareValueApproval(communicationContent);
                    customerFeatureFromLLM.getSoftwareValueApproval().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSoftwareValueApproval().getQuestion()) ||
                            customerFeatureFromLLM.getSoftwareValueApproval().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getSoftwareValueApproval().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getSoftwareValueApproval().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSoftwareValueApproval().getAnswerText()) ||
                            customerFeatureFromLLM.getSoftwareValueApproval().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getSoftwareValueApproval().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getSoftwareValueApproval().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getSoftwareValueApproval().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getSoftwareValueApproval().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getSoftwareValueApproval().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //客户对购买软件的态度
            if (!CollectionUtils.isEmpty(record.getSoftwarePurchaseAttitude())) {
                CommunicationContent communicationContent = record.getSoftwarePurchaseAttitude().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getSoftwarePurchaseAttitude())) {
                    customerFeatureFromLLM.setSoftwarePurchaseAttitude(communicationContent);
                    customerFeatureFromLLM.getSoftwarePurchaseAttitude().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSoftwarePurchaseAttitude().getQuestion()) ||
                            customerFeatureFromLLM.getSoftwarePurchaseAttitude().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getSoftwarePurchaseAttitude().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getSoftwarePurchaseAttitude().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSoftwarePurchaseAttitude().getAnswerText()) ||
                            customerFeatureFromLLM.getSoftwarePurchaseAttitude().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getSoftwarePurchaseAttitude().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getSoftwarePurchaseAttitude().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getSoftwarePurchaseAttitude().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getSoftwarePurchaseAttitude().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getSoftwarePurchaseAttitude().setAnswerCallId(record.getCallId());
                    }
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
            //客户为什么买这些股票
            if (!CollectionUtils.isEmpty(record.getStockPurchaseReason())) {
                CommunicationContent communicationContent = record.getStockPurchaseReason().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getStockPurchaseReason())) {
                    customerFeatureFromLLM.setStockPurchaseReason(communicationContent);
                    customerFeatureFromLLM.getStockPurchaseReason().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockPurchaseReason().getQuestion()) ||
                            customerFeatureFromLLM.getStockPurchaseReason().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getStockPurchaseReason().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getStockPurchaseReason().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockPurchaseReason().getAnswerText()) ||
                            customerFeatureFromLLM.getStockPurchaseReason().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getStockPurchaseReason().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getStockPurchaseReason().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getStockPurchaseReason().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getStockPurchaseReason().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getStockPurchaseReason().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //客户怎么决定的买卖这些股票的时机
            if (!CollectionUtils.isEmpty(record.getTradeTimingDecision())) {
                CommunicationContent communicationContent = record.getTradeTimingDecision().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getTradeTimingDecision())) {
                    customerFeatureFromLLM.setTradeTimingDecision(communicationContent);
                    customerFeatureFromLLM.getTradeTimingDecision().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getTradeTimingDecision().getQuestion()) ||
                            customerFeatureFromLLM.getTradeTimingDecision().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getTradeTimingDecision().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getTradeTimingDecision().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getTradeTimingDecision().getAnswerText()) ||
                            customerFeatureFromLLM.getTradeTimingDecision().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getTradeTimingDecision().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getTradeTimingDecision().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getTradeTimingDecision().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getTradeTimingDecision().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getTradeTimingDecision().setAnswerCallId(record.getCallId());
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
            //客户的学习能力
            if (!CollectionUtils.isEmpty(record.getLearningAbility())) {
                CommunicationContent communicationContent = record.getLearningAbility().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getLearningAbility())) {
                    customerFeatureFromLLM.setLearningAbility(communicationContent);
                    customerFeatureFromLLM.getLearningAbility().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getLearningAbility().getQuestion()) ||
                            customerFeatureFromLLM.getLearningAbility().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getLearningAbility().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getLearningAbility().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getLearningAbility().getAnswerText()) ||
                            customerFeatureFromLLM.getLearningAbility().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getLearningAbility().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getLearningAbility().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getLearningAbility().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getLearningAbility().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getLearningAbility().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //业务员有结合客户的股票举例
            if (!CollectionUtils.isEmpty(record.getIllustrateBasedStock())) {
                CommunicationContent communicationContent = record.getIllustrateBasedStock().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getIllustrateBasedStock())) {
                    customerFeatureFromLLM.setIllustrateBasedStock(communicationContent);
                    customerFeatureFromLLM.getIllustrateBasedStock().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getIllustrateBasedStock().getQuestion()) ||
                            customerFeatureFromLLM.getIllustrateBasedStock().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getIllustrateBasedStock().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getIllustrateBasedStock().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getIllustrateBasedStock().getAnswerText()) ||
                            customerFeatureFromLLM.getIllustrateBasedStock().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getIllustrateBasedStock().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getIllustrateBasedStock().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getIllustrateBasedStock().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getIllustrateBasedStock().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getIllustrateBasedStock().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //业务员有基于客户交易风格做针对性的功能介绍
            if (!CollectionUtils.isEmpty(record.getTradeStyleIntroduce())) {
                CommunicationContent communicationContent = record.getTradeStyleIntroduce().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getTradeStyleIntroduce())) {
                    customerFeatureFromLLM.setTradeStyleIntroduce(communicationContent);
                    customerFeatureFromLLM.getTradeStyleIntroduce().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getTradeStyleIntroduce().getQuestion()) ||
                            customerFeatureFromLLM.getTradeStyleIntroduce().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getTradeStyleIntroduce().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getTradeStyleIntroduce().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getTradeStyleIntroduce().getAnswerText()) ||
                            customerFeatureFromLLM.getTradeStyleIntroduce().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getTradeStyleIntroduce().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getTradeStyleIntroduce().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getTradeStyleIntroduce().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getTradeStyleIntroduce().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getTradeStyleIntroduce().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //业务员有点评客户的选股方法
            if (!CollectionUtils.isEmpty(record.getStockPickMethodReview())) {
                CommunicationContent communicationContent = record.getStockPickMethodReview().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getStockPickMethodReview())) {
                    customerFeatureFromLLM.setStockPickMethodReview(communicationContent);
                    customerFeatureFromLLM.getStockPickMethodReview().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockPickMethodReview().getQuestion()) ||
                            customerFeatureFromLLM.getStockPickMethodReview().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getStockPickMethodReview().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getStockPickMethodReview().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockPickMethodReview().getAnswerText()) ||
                            customerFeatureFromLLM.getStockPickMethodReview().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getStockPickMethodReview().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getStockPickMethodReview().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getStockPickMethodReview().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getStockPickMethodReview().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getStockPickMethodReview().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //业务员有点评客户的选股时机
            if (!CollectionUtils.isEmpty(record.getStockPickTimingReview())) {
                CommunicationContent communicationContent = record.getStockPickTimingReview().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getStockPickTimingReview())) {
                    customerFeatureFromLLM.setStockPickTimingReview(communicationContent);
                    customerFeatureFromLLM.getStockPickTimingReview().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockPickTimingReview().getQuestion()) ||
                            customerFeatureFromLLM.getStockPickTimingReview().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getStockPickTimingReview().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getStockPickTimingReview().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockPickTimingReview().getAnswerText()) ||
                            customerFeatureFromLLM.getStockPickTimingReview().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getStockPickTimingReview().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getStockPickTimingReview().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getStockPickTimingReview().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getStockPickTimingReview().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getStockPickTimingReview().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //业务员有对客户的问题做量化放大
            if (!CollectionUtils.isEmpty(record.getCustomerIssuesQuantified())) {
                CommunicationContent communicationContent = record.getCustomerIssuesQuantified().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getCustomerIssuesQuantified())) {
                    customerFeatureFromLLM.setCustomerIssuesQuantified(communicationContent);
                    customerFeatureFromLLM.getCustomerIssuesQuantified().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCustomerIssuesQuantified().getQuestion()) ||
                            customerFeatureFromLLM.getCustomerIssuesQuantified().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getCustomerIssuesQuantified().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getCustomerIssuesQuantified().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCustomerIssuesQuantified().getAnswerText()) ||
                            customerFeatureFromLLM.getCustomerIssuesQuantified().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getCustomerIssuesQuantified().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getCustomerIssuesQuantified().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getCustomerIssuesQuantified().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getCustomerIssuesQuantified().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getCustomerIssuesQuantified().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //业务员有对软件的价值做量化放大
            if (!CollectionUtils.isEmpty(record.getSoftwareValueQuantified())) {
                CommunicationContent communicationContent = record.getSoftwareValueQuantified().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getSoftwareValueQuantified())) {
                    customerFeatureFromLLM.setSoftwareValueQuantified(communicationContent);
                    customerFeatureFromLLM.getSoftwareValueQuantified().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSoftwareValueQuantified().getQuestion()) ||
                            customerFeatureFromLLM.getSoftwareValueQuantified().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getSoftwareValueQuantified().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getSoftwareValueQuantified().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSoftwareValueQuantified().getAnswerText()) ||
                            customerFeatureFromLLM.getSoftwareValueQuantified().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getSoftwareValueQuantified().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getSoftwareValueQuantified().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getSoftwareValueQuantified().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getSoftwareValueQuantified().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getSoftwareValueQuantified().setAnswerCallId(record.getCallId());
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
                // 软件功能清晰度
                if (!CollectionUtils.isEmpty(record.getSoftwareFunctionClarity())) {
                    CommunicationContent communicationContent = record.getSoftwareFunctionClarity().get(0);
                    String answerTag = communicationContent.getAnswerTag();
                    if (!StringUtils.isEmpty(answerTag) &&
                            (("是".equals(answerTag) ||
                                    "有购买意向".equals(answerTag) ||
                                    "认可".equals(answerTag) ||
                                    "清晰".equals(answerTag)))) {
                        basic.setSoftwareFunctionClarity(Boolean.TRUE);
                    } else if (!StringUtils.isEmpty(answerTag) &&
                            (("否".equals(answerTag) ||
                                    "无购买意向".equals(answerTag) ||
                                    "不认可".equals(answerTag) ||
                                    "不清晰".equals(answerTag)))) {
                        basic.setSoftwareFunctionClarity(Boolean.FALSE);
                    }
                }
                // 选股方法的认可度
                if (!CollectionUtils.isEmpty(record.getStockSelectionMethod())) {
                    CommunicationContent communicationContent = record.getStockSelectionMethod().get(0);
                    String answerTag = communicationContent.getAnswerTag();
                    if (!StringUtils.isEmpty(answerTag) &&
                            (("是".equals(answerTag) ||
                                    "有购买意向".equals(answerTag) ||
                                    "认可".equals(answerTag) ||
                                    "清晰".equals(answerTag)))) {
                        basic.setStockSelectionMethod(Boolean.TRUE);
                    } else if (!StringUtils.isEmpty(answerTag) &&
                            (("否".equals(answerTag) ||
                                    "无购买意向".equals(answerTag) ||
                                    "不认可".equals(answerTag) ||
                                    "不清晰".equals(answerTag)))) {
                        basic.setStockSelectionMethod(Boolean.FALSE);
                    }
                }
                // 自身问题及影响的认可度
                if (!CollectionUtils.isEmpty(record.getSelfIssueRecognition())) {
                    CommunicationContent communicationContent = record.getSelfIssueRecognition().get(0);
                    String answerTag = communicationContent.getAnswerTag();
                    if (!StringUtils.isEmpty(answerTag) &&
                            (("是".equals(answerTag) ||
                                    "有购买意向".equals(answerTag) ||
                                    "认可".equals(answerTag) ||
                                    "清晰".equals(answerTag)))) {
                        basic.setSelfIssueRecognition(Boolean.TRUE);
                    } else if (!StringUtils.isEmpty(answerTag) &&
                            (("否".equals(answerTag) ||
                                    "无购买意向".equals(answerTag) ||
                                    "不认可".equals(answerTag) ||
                                    "不清晰".equals(answerTag)))) {
                        basic.setSelfIssueRecognition(Boolean.FALSE);
                    }
                }
                // 软件价值的认可度
                if (!CollectionUtils.isEmpty(record.getSoftwareValueApproval())) {
                    CommunicationContent communicationContent = record.getSoftwareValueApproval().get(0);
                    String answerTag = communicationContent.getAnswerTag();
                    if (!StringUtils.isEmpty(answerTag) &&
                            (("是".equals(answerTag) ||
                                    "有购买意向".equals(answerTag) ||
                                    "认可".equals(answerTag) ||
                                    "清晰".equals(answerTag)))) {
                        basic.setSoftwareValueApproval(Boolean.TRUE);
                    } else if (!StringUtils.isEmpty(answerTag) &&
                            (("否".equals(answerTag) ||
                                    "无购买意向".equals(answerTag) ||
                                    "不认可".equals(answerTag) ||
                                    "不清晰".equals(answerTag)))) {
                        basic.setSoftwareValueApproval(Boolean.FALSE);
                    }
                }
                // 客户对软件购买的态度
                if (!CollectionUtils.isEmpty(record.getSoftwarePurchaseAttitude())) {
                    CommunicationContent communicationContent = record.getSoftwarePurchaseAttitude().get(0);
                    String answerTag = communicationContent.getAnswerTag();
                    if (!StringUtils.isEmpty(answerTag) &&
                            (("是".equals(answerTag) ||
                                    "有购买意向".equals(answerTag) ||
                                    "认可".equals(answerTag) ||
                                    "清晰".equals(answerTag)))) {
                        basic.setSoftwarePurchaseAttitude(Boolean.TRUE);
                    } else if (!StringUtils.isEmpty(answerTag) &&
                            (("否".equals(answerTag) ||
                                    "无购买意向".equals(answerTag) ||
                                    "不认可".equals(answerTag) ||
                                    "不清晰".equals(answerTag)))) {
                        basic.setSoftwarePurchaseAttitude(Boolean.FALSE);
                    }
                }
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
