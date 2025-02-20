package com.smart.sso.server.service.impl;

import com.smart.sso.server.model.CustomerFeature;
import com.smart.sso.server.model.CustomerInfo;
import com.smart.sso.server.model.FeatureContentSales;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.primary.mapper.CustomerFeatureMapper;
import com.smart.sso.server.primary.mapper.CustomerInfoMapper;
import com.smart.sso.server.service.AnotherService;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.util.DateUtil;
import com.smart.sso.server.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

@Service
@Slf4j
public class AnotherServiceImpl implements AnotherService {

    @Autowired
    private CustomerInfoService customerInfoService;
    @Autowired
    private CustomerFeatureMapper customerFeatureMapper;
    @Autowired
    private CustomerInfoMapper customerInfoMapper;

    @Override
    public void process() {

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("tttt.txt");

        if (inputStream == null) {
            System.out.println("File not found!");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {

                try {
                    String[] strs = line.split("\t");
                    String customerId = strs[0].trim();
                    String funds =  strs[1].trim().replace("w","").replace("<", "").replace(">", "");
                    String value = funds.split("-")[0];
                    int target = Integer.parseInt(value);
                    String result = null;
                    if (target < 5) {
                        result = "less_five_w";
                    } else if (target >= 5 && target <= 10) {
                        result = "five_to_ten_w";
                    } else if (target > 10) {
                        result = "great_ten_w";
                    }

                    FeatureContentSales featureContent = new FeatureContentSales();
                    featureContent.setTag(result);
                    featureContent.setUpdateTime(DateUtil.getCurrentDateTime());

                    CustomerFeatureResponse customerFeature = customerInfoService.queryCustomerFeatureById(customerId, "381");
                    String fundsold = null;
                    try {
                        fundsold = customerFeature.getBasic().getFundsVolume().getCustomerConclusion().getCompareValue().toString();
                    } catch (Exception e) {
                        CustomerInfo customerInfo = customerInfoMapper.selectByCustomerIdAndCampaignId(customerId, "381");
                        if (Objects.isNull(customerInfo)) {
                            continue;
                        }
                        CustomerFeature feature = customerFeatureMapper.selectById(customerInfo.getId());
                        if (Objects.nonNull(feature)){
                            customerFeatureMapper.updateFundsVolumeSalesById(customerInfo.getId(), JsonUtil.serialize(featureContent));
                        } else {
                            feature= new CustomerFeature();
                            feature.setId(customerInfo.getId());
                            feature.setFundsVolumeSales(featureContent);
                            customerFeatureMapper.insert(feature);
                        }
                        fundsold = result;
                    }
                    if (StringUtils.isEmpty(fundsold)) {
                        CustomerInfo customerInfo = customerInfoMapper.selectByCustomerIdAndCampaignId(customerId, "381");
                        if (Objects.isNull(customerInfo)) {
                            continue;
                        }
                        CustomerFeature feature = customerFeatureMapper.selectById(customerInfo.getId());
                        if (Objects.nonNull(feature)){
                            customerFeatureMapper.updateFundsVolumeSalesById(customerInfo.getId(), JsonUtil.serialize(featureContent));
                        } else {
                            feature= new CustomerFeature();
                            feature.setId(customerInfo.getId());
                            feature.setFundsVolumeSales(featureContent);
                            customerFeatureMapper.insert(feature);
                        }
                    }
                } catch (Exception e) {
                    log.error(line, e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
