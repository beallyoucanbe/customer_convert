package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smart.sso.server.model.*;
import com.smart.sso.server.primary.mapper.CustomerBaseMapper;
import com.smart.sso.server.primary.mapper.TelephoneRecordMapper;
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
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public CustomerFeatureFromLLM getCustomerFeatureFromLLM(String customerId, String activityId) {

        CustomerFeatureFromLLM customerFeatureFromLLM = new CustomerFeatureFromLLM();

        QueryWrapper<TelephoneRecord> queryWrapper = new QueryWrapper<>();
        // 按照沟通时间倒序排列
        queryWrapper.eq("customer_id", customerId);
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
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getFundsVolume().getAnswerText()) ||
                            customerFeatureFromLLM.getFundsVolume().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getFundsVolume().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getFundsVolume().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getFundsVolume().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //客户的仓位
            if (!CollectionUtils.isEmpty(record.getStockPosition())) {
                CommunicationContent communicationContent = record.getStockPosition().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getStockPosition())) {
                    customerFeatureFromLLM.setStockPosition(communicationContent);
                    customerFeatureFromLLM.getStockPosition().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getStockPosition().getAnswerText()) ||
                            customerFeatureFromLLM.getStockPosition().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getStockPosition().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getStockPosition().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getStockPosition().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //客户的回复
            if (!CollectionUtils.isEmpty(record.getCustomerResponse())) {
                CommunicationContent communicationContent = record.getCustomerResponse().get(0);
                if (!StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                        !communicationContent.getAnswerText().equals("无")) {
                    communicationContent.setCallId(record.getCallId());
                    communicationContent.setTs(record.getCommunicationTime().format(formatter));
                    customerFeatureFromLLM.getCustomerResponse().add(communicationContent);
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
            //购买过类似的产品
            if (!CollectionUtils.isEmpty(record.getPurchaseSimilarProduct())) {
                CommunicationContent communicationContent = record.getPurchaseSimilarProduct().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getPurchaseSimilarProduct())) {
                    customerFeatureFromLLM.setPurchaseSimilarProduct(communicationContent);
                    customerFeatureFromLLM.getPurchaseSimilarProduct().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getPurchaseSimilarProduct().getQuestion()) ||
                            customerFeatureFromLLM.getPurchaseSimilarProduct().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getPurchaseSimilarProduct().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getPurchaseSimilarProduct().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getPurchaseSimilarProduct().getAnswerText()) ||
                            customerFeatureFromLLM.getPurchaseSimilarProduct().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getPurchaseSimilarProduct().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getPurchaseSimilarProduct().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getPurchaseSimilarProduct().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getPurchaseSimilarProduct().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getPurchaseSimilarProduct().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //会员票是否买入
            if (!CollectionUtils.isEmpty(record.getMemberStocksBuy())) {
                CommunicationContent communicationContent = record.getMemberStocksBuy().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getMemberStocksBuy())) {
                    customerFeatureFromLLM.setMemberStocksBuy(communicationContent);
                    customerFeatureFromLLM.getMemberStocksBuy().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getMemberStocksBuy().getQuestion()) ||
                            customerFeatureFromLLM.getMemberStocksBuy().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getMemberStocksBuy().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getMemberStocksBuy().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getMemberStocksBuy().getAnswerText()) ||
                            customerFeatureFromLLM.getMemberStocksBuy().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getMemberStocksBuy().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getMemberStocksBuy().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getMemberStocksBuy().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getMemberStocksBuy().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getMemberStocksBuy().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //会员票涨跌
            if (!CollectionUtils.isEmpty(record.getMemberStocksPrice())) {
                CommunicationContent communicationContent = record.getMemberStocksPrice().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getMemberStocksPrice())) {
                    customerFeatureFromLLM.setMemberStocksPrice(communicationContent);
                    customerFeatureFromLLM.getMemberStocksPrice().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getMemberStocksPrice().getQuestion()) ||
                            customerFeatureFromLLM.getMemberStocksPrice().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getMemberStocksPrice().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getMemberStocksPrice().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getMemberStocksPrice().getAnswerText()) ||
                            customerFeatureFromLLM.getMemberStocksPrice().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getMemberStocksPrice().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getMemberStocksPrice().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getMemberStocksPrice().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getMemberStocksPrice().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getMemberStocksPrice().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //福利票是否买入
            if (!CollectionUtils.isEmpty(record.getWelfareStocksBuy())) {
                CommunicationContent communicationContent = record.getWelfareStocksBuy().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getWelfareStocksBuy())) {
                    customerFeatureFromLLM.setWelfareStocksBuy(communicationContent);
                    customerFeatureFromLLM.getWelfareStocksBuy().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getWelfareStocksBuy().getQuestion()) ||
                            customerFeatureFromLLM.getWelfareStocksBuy().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getWelfareStocksBuy().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getWelfareStocksBuy().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getWelfareStocksBuy().getAnswerText()) ||
                            customerFeatureFromLLM.getWelfareStocksBuy().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getWelfareStocksBuy().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getWelfareStocksBuy().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getWelfareStocksBuy().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getWelfareStocksBuy().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getWelfareStocksBuy().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //福利票涨跌
            if (!CollectionUtils.isEmpty(record.getWelfareStocksPrice())) {
                CommunicationContent communicationContent = record.getWelfareStocksPrice().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getWelfareStocksPrice())) {
                    customerFeatureFromLLM.setWelfareStocksPrice(communicationContent);
                    customerFeatureFromLLM.getWelfareStocksPrice().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getWelfareStocksPrice().getQuestion()) ||
                            customerFeatureFromLLM.getWelfareStocksPrice().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getWelfareStocksPrice().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getWelfareStocksPrice().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getWelfareStocksPrice().getAnswerText()) ||
                            customerFeatureFromLLM.getWelfareStocksPrice().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getWelfareStocksPrice().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getWelfareStocksPrice().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getWelfareStocksPrice().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getWelfareStocksPrice().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getWelfareStocksPrice().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //是否咨询实战班
            if (!CollectionUtils.isEmpty(record.getConsultingPracticalClass())) {
                CommunicationContent communicationContent = record.getConsultingPracticalClass().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getConsultingPracticalClass())) {
                    customerFeatureFromLLM.setConsultingPracticalClass(communicationContent);
                    customerFeatureFromLLM.getConsultingPracticalClass().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getConsultingPracticalClass().getQuestion()) ||
                            customerFeatureFromLLM.getConsultingPracticalClass().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getConsultingPracticalClass().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getConsultingPracticalClass().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getConsultingPracticalClass().getAnswerText()) ||
                            customerFeatureFromLLM.getConsultingPracticalClass().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getConsultingPracticalClass().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getConsultingPracticalClass().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getConsultingPracticalClass().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getConsultingPracticalClass().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getConsultingPracticalClass().setExplanation(communicationContent.getExplanation());
                        customerFeatureFromLLM.getConsultingPracticalClass().setAnswerCallId(record.getCallId());
                    }
                }
            }
            //客户学习请教
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

            //客户是否愿意继续跟进股票
            if (!CollectionUtils.isEmpty(record.getContinueFollowingStock())) {
                CommunicationContent communicationContent = record.getContinueFollowingStock().get(0);
                if (Objects.isNull(customerFeatureFromLLM.getContinueFollowingStock())) {
                    customerFeatureFromLLM.setContinueFollowingStock(communicationContent);
                    customerFeatureFromLLM.getContinueFollowingStock().setCallId(record.getCallId());
                } else {
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getContinueFollowingStock().getQuestion()) ||
                            customerFeatureFromLLM.getContinueFollowingStock().getQuestion().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getQuestion()) &&
                            !communicationContent.getQuestion().equals("无") &&
                            !communicationContent.getQuestion().equals("null")) {
                        customerFeatureFromLLM.getContinueFollowingStock().setQuestion(communicationContent.getQuestion());
                        customerFeatureFromLLM.getContinueFollowingStock().setQuestionCallId(record.getCallId());
                    }
                    if ((StringUtils.isEmpty(customerFeatureFromLLM.getContinueFollowingStock().getAnswerText()) ||
                            customerFeatureFromLLM.getContinueFollowingStock().getAnswerText().equals("无")) &&
                            !StringUtils.isEmpty(communicationContent.getAnswerText()) &&
                            !communicationContent.getAnswerText().equals("无")) {
                        customerFeatureFromLLM.getContinueFollowingStock().setAnswerText(communicationContent.getAnswerText());
                        customerFeatureFromLLM.getContinueFollowingStock().setAnswerTag(communicationContent.getAnswerTag());
                        customerFeatureFromLLM.getContinueFollowingStock().setDoubtText(communicationContent.getDoubtText());
                        customerFeatureFromLLM.getContinueFollowingStock().setDoubtTag(communicationContent.getDoubtTag());
                        customerFeatureFromLLM.getContinueFollowingStock().setExplanation(communicationContent.getExplanation());
                        customerFeatureFromLLM.getContinueFollowingStock().setAnswerCallId(record.getCallId());
                    }
                }
            }

            //客户对软件价值认可
            if (!CollectionUtils.isEmpty(record.getSoftwareValueApproval())) {
                CommunicationContent communicationContent = record.getSoftwareValueApproval().get(0);
                communicationContent.setTs(record.getCommunicationTime().format(formatter));
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
            //客户对软件购买态度
            if (!CollectionUtils.isEmpty(record.getSoftwarePurchaseAttitude())) {
                CommunicationContent communicationContent = record.getSoftwarePurchaseAttitude().get(0);
                communicationContent.setTs(record.getCommunicationTime().format(formatter));
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

        String filePath = "/home/haiyangu1/hsw/files/" + record.getCallId(); // 文件路径
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
            CustomerBase customerBase = customerBaseMapper.selectByCustomerId(item.getCustomerId());
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
        CustomerBase customerBase = customerBaseMapper.selectByCustomerId(customerId);
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
