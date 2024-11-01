package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smart.sso.server.mapper.TelephoneRecordMapper;
import com.smart.sso.server.model.CommunicationContent;
import com.smart.sso.server.model.CustomerFeatureFromLLM;
import com.smart.sso.server.model.TelephoneRecord;
import com.smart.sso.server.service.TelephoneRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
public class TelephoneRecordServiceImpl implements TelephoneRecordService {

    @Autowired
    private TelephoneRecordMapper recordMapper;

    @Override
    public CustomerFeatureFromLLM getCustomerFeatureFromLLM(String customerId, String activityId) {

        CustomerFeatureFromLLM customerFeatureFromLLM = new CustomerFeatureFromLLM();

        QueryWrapper<TelephoneRecord> queryWrapper = new QueryWrapper<>();
        // 按照沟通时间倒序排列
        queryWrapper.eq("customer_id", customerId);
        queryWrapper.eq("activity_id", activityId);
        queryWrapper.orderBy(false, false, "communication_time");
        List<TelephoneRecord> records = recordMapper.selectList(queryWrapper);
        // 对该客户下的所有的通话记录进行总结
        for (TelephoneRecord record : records) {
            //客户的资金体量
            if (!CollectionUtils.isEmpty(record.getFundsVolume())) {
                CommunicationContent communicationContent = record.getFundsVolume().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getFundsVolume())) {
                    customerFeatureFromLLM.setFundsVolume(communicationContent);
                    customerFeatureFromLLM.getFundsVolume().setCallId(record.getCallId());
                } else {
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getFundsVolume().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getFundsVolume().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getFundsVolume().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getFundsVolume().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getFundsVolume().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getFundsVolume().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getFundsVolume().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getFundsVolume().setCallId(record.getCallId());
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
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getEarningDesire().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getEarningDesire().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getEarningDesire().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getEarningDesire().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getEarningDesire().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getEarningDesire().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getEarningDesire().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getEarningDesire().setCallId(record.getCallId());
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
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getSoftwareFunctionClarity().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getSoftwareFunctionClarity().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getSoftwareFunctionClarity().setCallId(record.getCallId());
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
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getStockSelectionMethod().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getStockSelectionMethod().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getStockSelectionMethod().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getStockSelectionMethod().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getStockSelectionMethod().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getStockSelectionMethod().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getStockSelectionMethod().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getStockSelectionMethod().setCallId(record.getCallId());
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
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getSelfIssueRecognition().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getSelfIssueRecognition().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getSelfIssueRecognition().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getSelfIssueRecognition().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getSelfIssueRecognition().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getSelfIssueRecognition().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getSelfIssueRecognition().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getSelfIssueRecognition().setCallId(record.getCallId());
                    }
                }
            }
            //客户对软件价值的认可度
            if (!CollectionUtils.isEmpty(record.getSelfIssueRecognition())) {
                CommunicationContent communicationContent = record.getSelfIssueRecognition().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getSelfIssueRecognition())) {
                    customerFeatureFromLLM.setSelfIssueRecognition(communicationContent);
                    customerFeatureFromLLM.getSelfIssueRecognition().setCallId(record.getCallId());
                } else {
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getSelfIssueRecognition().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getSelfIssueRecognition().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getSelfIssueRecognition().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getSelfIssueRecognition().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getSelfIssueRecognition().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getSelfIssueRecognition().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getSelfIssueRecognition().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getSelfIssueRecognition().setCallId(record.getCallId());
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
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getSoftwarePurchaseAttitude().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getSoftwarePurchaseAttitude().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getSoftwarePurchaseAttitude().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getSoftwarePurchaseAttitude().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getSoftwarePurchaseAttitude().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getSoftwarePurchaseAttitude().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getSoftwarePurchaseAttitude().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getSoftwarePurchaseAttitude().setCallId(record.getCallId());
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
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getCurrentStocks().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getCurrentStocks().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getCurrentStocks().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getCurrentStocks().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getCurrentStocks().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getCurrentStocks().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getCurrentStocks().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getCurrentStocks().setCallId(record.getCallId());
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
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getStockPurchaseReason().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getStockPurchaseReason().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getStockPurchaseReason().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getStockPurchaseReason().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getStockPurchaseReason().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getStockPurchaseReason().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getStockPurchaseReason().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getStockPurchaseReason().setCallId(record.getCallId());
                    }
                }
            }
            //客户怎么决定的买卖这些股票的时机
            if (!CollectionUtils.isEmpty(record.getTradeTimingDecision())) {
                CommunicationContent communicationContent = record.getTradeTimingDecision().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getStockPurchaseReason())) {
                    customerFeatureFromLLM.setTradeTimingDecision(communicationContent);
                    customerFeatureFromLLM.getTradeTimingDecision().setCallId(record.getCallId());
                } else {
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getTradeTimingDecision().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getTradeTimingDecision().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getTradeTimingDecision().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getTradeTimingDecision().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getTradeTimingDecision().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getTradeTimingDecision().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getTradeTimingDecision().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getTradeTimingDecision().setCallId(record.getCallId());
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
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getTradingStyle().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getTradingStyle().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getTradingStyle().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getTradingStyle().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getTradingStyle().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getTradingStyle().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getTradingStyle().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getTradingStyle().setCallId(record.getCallId());
                    }
                }
            }
            //客户的股龄
            if (!CollectionUtils.isEmpty(record.getStockMarketAge())) {
                CommunicationContent communicationContent = record.getStockMarketAge().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getStockMarketAge())) {
                    customerFeatureFromLLM.setTradingStyle(communicationContent);
                    customerFeatureFromLLM.getStockMarketAge().setCallId(record.getCallId());
                } else {
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getStockMarketAge().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getStockMarketAge().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getStockMarketAge().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getStockMarketAge().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getStockMarketAge().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getStockMarketAge().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getStockMarketAge().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getStockMarketAge().setCallId(record.getCallId());
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
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getLearningAbility().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getLearningAbility().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getLearningAbility().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getLearningAbility().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getLearningAbility().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getLearningAbility().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getLearningAbility().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getLearningAbility().setCallId(record.getCallId());
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
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getIllustrateBasedStock().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getIllustrateBasedStock().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getIllustrateBasedStock().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getIllustrateBasedStock().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getIllustrateBasedStock().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getIllustrateBasedStock().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getIllustrateBasedStock().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getIllustrateBasedStock().setCallId(record.getCallId());
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
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getTradeStyleIntroduce().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getTradeStyleIntroduce().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getTradeStyleIntroduce().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getTradeStyleIntroduce().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getTradeStyleIntroduce().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getTradeStyleIntroduce().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getTradeStyleIntroduce().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getTradeStyleIntroduce().setCallId(record.getCallId());
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
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getStockPickMethodReview().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getStockPickMethodReview().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getStockPickMethodReview().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getStockPickMethodReview().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getStockPickMethodReview().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getStockPickMethodReview().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getStockPickMethodReview().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getStockPickMethodReview().setCallId(record.getCallId());
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
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getStockPickTimingReview().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getStockPickTimingReview().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getStockPickTimingReview().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getStockPickTimingReview().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getStockPickTimingReview().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getStockPickTimingReview().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getStockPickTimingReview().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getStockPickTimingReview().setCallId(record.getCallId());
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
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getCustomerIssuesQuantified().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getCustomerIssuesQuantified().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getCustomerIssuesQuantified().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getCustomerIssuesQuantified().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getCustomerIssuesQuantified().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getCustomerIssuesQuantified().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getCustomerIssuesQuantified().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getCustomerIssuesQuantified().setCallId(record.getCallId());
                    }
                }
            }
            //业务员有对软件的价值做量化放大
            if (!CollectionUtils.isEmpty(record.getSoftwareValueQuantified())) {
                CommunicationContent communicationContent = record.getSoftwareValueQuantified().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getCustomerIssuesQuantified())) {
                    customerFeatureFromLLM.setSoftwareValueQuantified(communicationContent);
                    customerFeatureFromLLM.getSoftwareValueQuantified().setCallId(record.getCallId());
                } else {
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getSoftwareValueQuantified().getQuestion()) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getSoftwareValueQuantified().setQuestion(communicationContent.getQuestion());
                    }
                    if (StringUtils.isEmpty(customerFeatureFromLLM.getSoftwareValueQuantified().getAnswerText()) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText())) {
                        customerFeatureFromLLM.getSoftwareValueQuantified().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getSoftwareValueQuantified().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getSoftwareValueQuantified().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getSoftwareValueQuantified().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getSoftwareValueQuantified().setCallId(record.getCallId());
                    }
                }
            }



            //如果 funds_volume_model json list 中有一个 question 有值，就是 ‘是’;
//                for (int i = featureContentByModel.size() - 1; i >= 0; i--) {
//                    if (!StringUtils.isEmpty(featureContentByModel.get(i).getQuestion()) &&
//                            !featureContentByModel.get(i).getQuestion().equals("无") &&
//                            !featureContentByModel.get(i).getQuestion().equals("null")) {
//                        featureVO.setInquired("yes");
//                        CustomerFeatureResponse.OriginChat originChat = new CustomerFeatureResponse.OriginChat();
//                        originChat.setContent(featureContentByModel.get(i).getQuestion());
//                        originChat.setId(featureContentByModel.get(i).getCallId());
//                        featureVO.setInquiredOriginChat(originChat);
//                        break;
//                    }
//                }
//                //如果都没有 question 或者 question 都没值，但是有 answer 有值，就是‘不需要’；
//                if (featureVO.getInquired().equals("no")) {
//                    for (int i = featureContentByModel.size() - 1; i >= 0; i--) {
//                        if (!StringUtils.isEmpty(featureContentByModel.get(i).getAnswer()) &&
//                                !featureContentByModel.get(i).getAnswer().equals("无") &&
//                                !featureContentByModel.get(i).getAnswer().equals("null")) {
//                            featureVO.setInquired("no-need");
//                            CustomerFeatureResponse.OriginChat originChat = new CustomerFeatureResponse.OriginChat();
//                            originChat.setContent(featureContentByModel.get(i).getQuestion());
//                            originChat.setId(featureContentByModel.get(i).getCallId());
//                            featureVO.setInquiredOriginChat(originChat);
//                            break;
//                        }
//                    }
//                }
        }
        return customerFeatureFromLLM;
    }
}
