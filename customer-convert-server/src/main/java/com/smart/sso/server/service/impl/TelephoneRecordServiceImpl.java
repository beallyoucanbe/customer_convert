package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smart.sso.server.model.CommunicationFreqContent;
import com.smart.sso.server.model.CustomerInfo;
import com.smart.sso.server.primary.mapper.CustomerInfoMapper;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private CustomerInfoMapper customerInfoMapper;
    @Autowired
    private TelephoneRecordMapper telephoneRecordMapper;
    @Autowired
    private ConfigService configService;


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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getFundsVolume().getQuestion()) || customerFeatureFromLLM.getFundsVolume().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getFundsVolume().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getFundsVolume().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getFundsVolume().getAnswerText()) || customerFeatureFromLLM.getFundsVolume().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getFundsVolume().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getFundsVolume().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getFundsVolume().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getFundsVolume().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getFundsVolume().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //客户的赚钱欲望
            if (!CollectionUtils.isEmpty(record.getEarningDesire())) {
                CommunicationContent communicationContent = record.getEarningDesire().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getEarningDesire())) {
                    customerFeatureFromLLM.setEarningDesire(communicationContent);
                    customerFeatureFromLLM.getEarningDesire().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getEarningDesire().getQuestion()) || customerFeatureFromLLM.getEarningDesire().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getEarningDesire().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getEarningDesire().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getEarningDesire().getAnswerText()) || customerFeatureFromLLM.getEarningDesire().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSoftwareFunctionClarity().getQuestion()) || customerFeatureFromLLM.getSoftwareFunctionClarity().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSoftwareFunctionClarity().getAnswerText()) || customerFeatureFromLLM.getSoftwareFunctionClarity().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setAsked(communicationContent.getAsked());
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockSelectionMethod().getQuestion()) || customerFeatureFromLLM.getStockSelectionMethod().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getStockSelectionMethod().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getStockSelectionMethod().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockSelectionMethod().getAnswerText()) || customerFeatureFromLLM.getStockSelectionMethod().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSelfIssueRecognition().getQuestion()) || customerFeatureFromLLM.getSelfIssueRecognition().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getSelfIssueRecognition().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getSelfIssueRecognition().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSelfIssueRecognition().getAnswerText()) || customerFeatureFromLLM.getSelfIssueRecognition().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSoftwareValueApproval().getQuestion()) || customerFeatureFromLLM.getSoftwareValueApproval().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getSoftwareValueApproval().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getSoftwareValueApproval().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSoftwareValueApproval().getAnswerText()) || customerFeatureFromLLM.getSoftwareValueApproval().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSoftwarePurchaseAttitude().getQuestion()) || customerFeatureFromLLM.getSoftwarePurchaseAttitude().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getSoftwarePurchaseAttitude().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getSoftwarePurchaseAttitude().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSoftwarePurchaseAttitude().getAnswerText()) || customerFeatureFromLLM.getSoftwarePurchaseAttitude().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCurrentStocks().getQuestion()) || customerFeatureFromLLM.getCurrentStocks().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getCurrentStocks().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getCurrentStocks().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCurrentStocks().getAnswerText()) || customerFeatureFromLLM.getCurrentStocks().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getCurrentStocks().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getCurrentStocks().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getCurrentStocks().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getCurrentStocks().setDoubtTag(communicationContent.getDoubtTag());
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockPurchaseReason().getQuestion()) || customerFeatureFromLLM.getStockPurchaseReason().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getStockPurchaseReason().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getStockPurchaseReason().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockPurchaseReason().getAnswerText()) || customerFeatureFromLLM.getStockPurchaseReason().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getTradeTimingDecision().getQuestion()) || customerFeatureFromLLM.getTradeTimingDecision().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getTradeTimingDecision().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getTradeTimingDecision().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getTradeTimingDecision().getAnswerText()) || customerFeatureFromLLM.getTradeTimingDecision().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getTradingStyle().getQuestion()) || customerFeatureFromLLM.getTradingStyle().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getTradingStyle().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getTradingStyle().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getTradingStyle().getAnswerText()) || customerFeatureFromLLM.getTradingStyle().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getTradingStyle().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getTradingStyle().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getTradingStyle().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getTradingStyle().setDoubtTag(communicationContent.getDoubtTag());
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockMarketAge().getQuestion()) || customerFeatureFromLLM.getStockMarketAge().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getStockMarketAge().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getStockMarketAge().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockMarketAge().getAnswerText()) || customerFeatureFromLLM.getStockMarketAge().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getStockMarketAge().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getStockMarketAge().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getStockMarketAge().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getStockMarketAge().setDoubtTag(communicationContent.getDoubtTag());
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getLearningAbility().getQuestion()) || customerFeatureFromLLM.getLearningAbility().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getLearningAbility().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getLearningAbility().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getLearningAbility().getAnswerText()) || customerFeatureFromLLM.getLearningAbility().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getIllustrateBasedStock().getQuestion()) || customerFeatureFromLLM.getIllustrateBasedStock().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getIllustrateBasedStock().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getIllustrateBasedStock().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getIllustrateBasedStock().getAnswerText()) || customerFeatureFromLLM.getIllustrateBasedStock().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getTradeStyleIntroduce().getQuestion()) || customerFeatureFromLLM.getTradeStyleIntroduce().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getTradeStyleIntroduce().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getTradeStyleIntroduce().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getTradeStyleIntroduce().getAnswerText()) || customerFeatureFromLLM.getTradeStyleIntroduce().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockPickMethodReview().getQuestion()) || customerFeatureFromLLM.getStockPickMethodReview().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getStockPickMethodReview().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getStockPickMethodReview().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockPickMethodReview().getAnswerText()) || customerFeatureFromLLM.getStockPickMethodReview().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockPickTimingReview().getQuestion()) || customerFeatureFromLLM.getStockPickTimingReview().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getStockPickTimingReview().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getStockPickTimingReview().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockPickTimingReview().getAnswerText()) || customerFeatureFromLLM.getStockPickTimingReview().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCustomerIssuesQuantified().getQuestion()) || customerFeatureFromLLM.getCustomerIssuesQuantified().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getCustomerIssuesQuantified().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getCustomerIssuesQuantified().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCustomerIssuesQuantified().getAnswerText()) || customerFeatureFromLLM.getCustomerIssuesQuantified().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSoftwareValueQuantified().getQuestion()) || customerFeatureFromLLM.getSoftwareValueQuantified().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getSoftwareValueQuantified().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getSoftwareValueQuantified().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getSoftwareValueQuantified().getAnswerText()) || customerFeatureFromLLM.getSoftwareValueQuantified().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getSoftwareValueQuantified().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getSoftwareValueQuantified().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getSoftwareValueQuantified().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getSoftwareValueQuantified().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getSoftwareValueQuantified().setAnswerCallId(record.getCallId());
                    }
                }
            }

            if (record.getCommunicationDuration() >= 30) {
                //客户学习请教次数
                CommunicationFreqContent customerLearningFre = customerFeatureFromLLM.getCustomerLearning();
                customerLearningFre.setCommunicationCount(customerLearningFre.getCommunicationCount() + 1);
                customerLearningFre.setCommunicationTime(customerLearningFre.getCommunicationTime() + record.getCommunicationDuration());
                if (!CollectionUtils.isEmpty(record.getCustomerLearning())) {
                    CommunicationContent communicationContent = record.getCustomerLearning().get(0);
                    if (!StringUtils.isEmpty(communicationContent.getAnswerTag())) {
                        customerLearningFre.setRemindCount(customerLearningFre.getRemindCount() + Integer.parseInt(communicationContent.getAnswerTag()));
                        customerLearningFre.getFrequencyItemList().add(new CommunicationFreqContent.FrequencyItem(
                                record.getCallId(),
                                customerFeatureFromLLM.getCommunicationTime(),
                                Integer.parseInt(communicationContent.getAnswerTag()),
                                communicationContent.getAnswerText()));
                    }
                }

                //业务员互动次数
                CommunicationFreqContent ownerInteractionLearningFre = customerFeatureFromLLM.getOwnerInteraction();
                ownerInteractionLearningFre.setCommunicationCount(ownerInteractionLearningFre.getCommunicationCount() + 1);
                ownerInteractionLearningFre.setCommunicationTime(ownerInteractionLearningFre.getCommunicationTime() + record.getCommunicationDuration());
                if (!CollectionUtils.isEmpty(record.getOwnerInteraction())) {
                    CommunicationContent communicationContent = record.getOwnerInteraction().get(0);
                    if (!StringUtils.isEmpty(communicationContent.getAnswerTag())) {
                        ownerInteractionLearningFre.setRemindCount(ownerInteractionLearningFre.getRemindCount() + Integer.parseInt(communicationContent.getAnswerTag()));
                        ownerInteractionLearningFre.getFrequencyItemList().add(new CommunicationFreqContent.FrequencyItem(
                                record.getCallId(),
                                customerFeatureFromLLM.getCommunicationTime(),
                                Integer.parseInt(communicationContent.getAnswerTag()),
                                communicationContent.getAnswerText()));
                    }
                }
            }

            // 客户是否愿意继续沟通
            if (!CollectionUtils.isEmpty(record.getCustomerContinueCommunicate())) {
                CommunicationContent communicationContent = record.getCustomerContinueCommunicate().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getCustomerContinueCommunicate())) {
                    customerFeatureFromLLM.setCustomerContinueCommunicate(communicationContent);
                    customerFeatureFromLLM.getCustomerContinueCommunicate().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getCustomerContinueCommunicate().getAnswerText()) || customerFeatureFromLLM.getCustomerContinueCommunicate().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) && !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getCustomerContinueCommunicate().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getCustomerContinueCommunicate().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getCustomerContinueCommunicate().setAnswerCallId(record.getCallId());
                    }
                }
            }

            // 业务员正确包装系列课
            if (!CollectionUtils.isEmpty(record.getOwnerPackagingCourse())) {
                CommunicationContent communicationContent = record.getOwnerPackagingCourse().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getOwnerPackagingCourse()) &&
                        !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                        !communicationContent.getQuestion().equals("无") &&
                        !communicationContent.getQuestion().equals("null")) {
                    communicationContent.setAnswerTag("是");
                    communicationContent.setAnswerText(communicationContent.getQuestion());
                    communicationContent.setAnswerCallId(record.getCallId());
                    communicationContent.setQuestionCallId(record.getCallId());
                    customerFeatureFromLLM.setOwnerPackagingCourse(communicationContent);
                    customerFeatureFromLLM.getOwnerPackagingCourse().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getOwnerPackagingCourse().getQuestion()) || customerFeatureFromLLM.getOwnerPackagingCourse().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getOwnerPackagingCourse().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getOwnerPackagingCourse().setAnswerTag("是");
                        customerFeatureFromLLM.getOwnerPackagingCourse().setAnswerText(communicationContent.getQuestion());
                        customerFeatureFromLLM.getOwnerPackagingCourse().setAnswerCallId(communicationContent.getQuestion());
                        customerFeatureFromLLM.getOwnerPackagingCourse().setQuestionCallId(record.getCallId());
                    }
                }
            }

            // 业务员正确包装功能
            if (!CollectionUtils.isEmpty(record.getOwnerPackagingFunction())) {
                CommunicationContent communicationContent = record.getOwnerPackagingFunction().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getOwnerPackagingFunction()) &&
                        !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                    communicationContent.setAnswerTag("是");
                    communicationContent.setAnswerText(communicationContent.getQuestion());
                    communicationContent.setAnswerCallId(record.getCallId());
                    communicationContent.setQuestionCallId(record.getCallId());
                    customerFeatureFromLLM.setOwnerPackagingFunction(communicationContent);
                    customerFeatureFromLLM.getOwnerPackagingFunction().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getOwnerPackagingFunction().getQuestion()) || customerFeatureFromLLM.getOwnerPackagingFunction().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getOwnerPackagingFunction().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getOwnerPackagingFunction().setAnswerTag("是");
                        customerFeatureFromLLM.getOwnerPackagingFunction().setAnswerText(communicationContent.getQuestion());
                        customerFeatureFromLLM.getOwnerPackagingFunction().setAnswerCallId(record.getCallId());
                        customerFeatureFromLLM.getOwnerPackagingFunction().setQuestionCallId(record.getCallId());
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
        chatDetail.setType(record.getCommunicationType());

        String filePath = "/opt/customer-convert/callback/files/" + record.getCallId(); // 文件路径
        List<ChatDetail.Message> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                ChatDetail.Message message = new ChatDetail.Message();
                if (line.split(" ").length >= 2 && (line.contains("2024") || line.contains("2025"))) {
                    message.setRole(line.substring(0, line.indexOf(" ")));
                    message.setTime(line.substring(line.indexOf(" ") + 1, line.length()));
                    if ((line = br.readLine()) != null) {
                        message.setContent(line);
                        result.add(message);
                    }
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
                    if (!StringUtils.isEmpty(answerTag) && (("是".equals(answerTag) || "有购买意向".equals(answerTag) || "认可".equals(answerTag) || "清晰".equals(answerTag)))) {
                        basic.setSoftwareFunctionClarity(Boolean.TRUE);
                    } else if (!StringUtils.isEmpty(answerTag) && (("否".equals(answerTag) || "无购买意向".equals(answerTag) || "不认可".equals(answerTag) || "不清晰".equals(answerTag)))) {
                        basic.setSoftwareFunctionClarity(Boolean.FALSE);
                    }
                }
                // 选股方法的认可度
                if (!CollectionUtils.isEmpty(record.getStockSelectionMethod())) {
                    CommunicationContent communicationContent = record.getStockSelectionMethod().get(0);
                    String answerTag = communicationContent.getAnswerTag();
                    if (!StringUtils.isEmpty(answerTag) && (("是".equals(answerTag) || "有购买意向".equals(answerTag) || "认可".equals(answerTag) || "清晰".equals(answerTag)))) {
                        basic.setStockSelectionMethod(Boolean.TRUE);
                    } else if (!StringUtils.isEmpty(answerTag) && (("否".equals(answerTag) || "无购买意向".equals(answerTag) || "不认可".equals(answerTag) || "不清晰".equals(answerTag)))) {
                        basic.setStockSelectionMethod(Boolean.FALSE);
                    }
                }
                // 自身问题及影响的认可度
                if (!CollectionUtils.isEmpty(record.getSelfIssueRecognition())) {
                    CommunicationContent communicationContent = record.getSelfIssueRecognition().get(0);
                    String answerTag = communicationContent.getAnswerTag();
                    if (!StringUtils.isEmpty(answerTag) && (("是".equals(answerTag) || "有购买意向".equals(answerTag) || "认可".equals(answerTag) || "清晰".equals(answerTag)))) {
                        basic.setSelfIssueRecognition(Boolean.TRUE);
                    } else if (!StringUtils.isEmpty(answerTag) && (("否".equals(answerTag) || "无购买意向".equals(answerTag) || "不认可".equals(answerTag) || "不清晰".equals(answerTag)))) {
                        basic.setSelfIssueRecognition(Boolean.FALSE);
                    }
                }
                // 软件价值的认可度
                if (!CollectionUtils.isEmpty(record.getSoftwareValueApproval())) {
                    CommunicationContent communicationContent = record.getSoftwareValueApproval().get(0);
                    String answerTag = communicationContent.getAnswerTag();
                    if (!StringUtils.isEmpty(answerTag) && (("是".equals(answerTag) || "有购买意向".equals(answerTag) || "认可".equals(answerTag) || "清晰".equals(answerTag)))) {
                        basic.setSoftwareValueApproval(Boolean.TRUE);
                    } else if (!StringUtils.isEmpty(answerTag) && (("否".equals(answerTag) || "无购买意向".equals(answerTag) || "不认可".equals(answerTag) || "不清晰".equals(answerTag)))) {
                        basic.setSoftwareValueApproval(Boolean.FALSE);
                    }
                }
                // 客户对软件购买的态度
                if (!CollectionUtils.isEmpty(record.getSoftwarePurchaseAttitude())) {
                    CommunicationContent communicationContent = record.getSoftwarePurchaseAttitude().get(0);
                    String answerTag = communicationContent.getAnswerTag();
                    if (!StringUtils.isEmpty(answerTag) && (("是".equals(answerTag) || "有购买意向".equals(answerTag) || "认可".equals(answerTag) || "清晰".equals(answerTag)))) {
                        basic.setSoftwarePurchaseAttitude(Boolean.TRUE);
                    } else if (!StringUtils.isEmpty(answerTag) && (("否".equals(answerTag) || "无购买意向".equals(answerTag) || "不认可".equals(answerTag) || "不清晰".equals(answerTag)))) {
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
            customerInfoMapper.updateCommunicationRounds(item.getCustomerId(), activityId, item.getTotalCalls(), item.getLatestCommunicationTime());
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
        String activityId = configService.getCurrentActivityId();
        return recordMapper.selectTelephoneRecordStaticsRecent(activityId, dateTime);
    }

    @Override
    public TelephoneRecordStatics getCommunicationRound(String customerId, String activityId) {
        return recordMapper.selectTelephoneRecordStaticsOne(customerId, activityId);
    }

    @Override
    public CustomerInfo syncCustomerInfoFromRecord(String customerId, String activityId) {
        CustomerInfo customerInfo = customerInfoMapper.selectByCustomerIdAndCampaignId(customerId, activityId);
        // info 表存在记录，并且客户名称不为空，说明已经同步过信息，跳过
        if (Objects.nonNull(customerInfo) && !StringUtils.isEmpty(customerInfo.getCustomerName())) {
            return customerInfo;
        }
        Map<String, String> activityIdNames = configService.getActivityIdNames();
        TelephoneRecord telephoneRecord = telephoneRecordMapper.selectOneTelephoneRecord(customerId, activityId);
        // record 也没有，不同步
        if (Objects.isNull(telephoneRecord)) {
            return null;
        }
        // info 表不存在记录，新建一条
        if (Objects.isNull(customerInfo)) {
            customerInfo = new CustomerInfo();
            customerInfo.setId(CommonUtils.generatePrimaryKey());
            customerInfo.setCustomerName(telephoneRecord.getCustomerName());
            customerInfo.setCustomerId(telephoneRecord.getCustomerId());
            customerInfo.setOwnerName(telephoneRecord.getOwnerName());
            customerInfo.setOwnerId(telephoneRecord.getOwnerId());
            customerInfo.setActivityId(telephoneRecord.getActivityId());
            customerInfo.setActivityName(activityIdNames.containsKey(telephoneRecord.getActivityId()) ? activityIdNames.get(telephoneRecord.getActivityId()) : telephoneRecord.getActivityId());
            customerInfo.setUpdateTimeTelephone(LocalDateTime.now());
            customerInfoMapper.insert(customerInfo);
        } else {
            // info 表存在记录，需要同步名称等关键信息
            customerInfo.setCustomerName(telephoneRecord.getCustomerName());
            customerInfo.setOwnerName(telephoneRecord.getOwnerName());
            customerInfo.setOwnerId(telephoneRecord.getOwnerId());
            customerInfo.setActivityName(activityIdNames.containsKey(telephoneRecord.getActivityId()) ? activityIdNames.get(telephoneRecord.getActivityId()) : telephoneRecord.getActivityId());
            customerInfo.setUpdateTimeTelephone(LocalDateTime.now());
            customerInfoMapper.updateById(customerInfo);
        }
        return customerInfo;
    }

    @Override
    public List<String> getOwnerHasTeleToday(String activityId) {
        // 获取当前时间的 LocalDateTime 对象
        LocalDateTime currentTime = LocalDateTime.now();
        // 获取当天日期的开始时间（00:00:00）的 LocalDateTime 对象
        LocalDateTime startOfDay = currentTime.toLocalDate().atStartOfDay();
        return recordMapper.selectOwnerHasTele(activityId, startOfDay, currentTime);
    }

    @Override
    public List<TelephoneRecord> getOwnerTelephoneRecordToday(String ownerId) {
        // 获取当前时间的 LocalDateTime 对象
        LocalDateTime currentTime = LocalDateTime.now();
        // 获取当天日期的开始时间（00:00:00）的 LocalDateTime 对象
        LocalDateTime startOfDay = currentTime.toLocalDate().atStartOfDay();
        return recordMapper.selectOwnerTelephoneRecord(ownerId, startOfDay, currentTime);
    }

    @Override
    public List<String> getOwnerHasTeleYesterday(String activityId) {
        // 获取当前时间
        LocalDateTime currentTime = LocalDateTime.now();
        // 获取昨天的日期
        LocalDateTime yesterday = currentTime.minusDays(1);
        // 获取昨天的开始时间（00:00:00）
        LocalDateTime startOfYesterday = yesterday.toLocalDate().atStartOfDay();
        // 获取昨天的结束时间（23:59:59）
        LocalDateTime endOfYesterday = yesterday.toLocalDate().atStartOfDay().plusDays(1).minusSeconds(1);
        return recordMapper.selectOwnerHasTele(activityId, startOfYesterday, endOfYesterday);
    }

    @Override
    public List<TelephoneRecord> getOwnerTelephoneRecordYesterday(String ownerId) {
        // 获取当前时间
        LocalDateTime currentTime = LocalDateTime.now();
        // 获取昨天的日期
        LocalDateTime yesterday = currentTime.minusDays(1);
        // 获取昨天的开始时间（00:00:00）
        LocalDateTime startOfYesterday = yesterday.toLocalDate().atStartOfDay();
        // 获取昨天的结束时间（23:59:59）
        LocalDateTime endOfYesterday = yesterday.toLocalDate().atStartOfDay().plusDays(1).minusSeconds(1);
        return recordMapper.selectOwnerTelephoneRecord(ownerId, startOfYesterday, endOfYesterday);
    }

    @Override
    public List<String> selectCustomerExceed8Hour(String activityId) {
        return recordMapper.selectCustomerExceed8Hour(activityId);
    }
}
