package com.smart.sso.server.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.service.ConfigService;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
@Slf4j
public class MyCommandLineRunner implements CommandLineRunner {

    @Autowired
    private ConfigService configService;

    @Autowired
    private CustomerInfoService customerInfoService;

    @Override
    public void run(String... args) {
        String fileName = "test.txt";
        Resource resource = new ClassPathResource(fileName);
        String aa = "{\"basic\":{\"funds_volume\":{\"customer_conclusion\":{\"sales_manual_tag\":\"less_five_w\"}}}}";
        String bb = "{\"basic\":{\"funds_volume\":{\"customer_conclusion\":{\"sales_manual_tag\":\"five_to_ten_w\"}}}}";
        String cc = "{\"basic\":{\"funds_volume\":{\"customer_conclusion\":{\"sales_manual_tag\":\"great_ten_w\"}}}}";
        CustomerFeatureResponse newCustomerFeatureaa = JsonUtil.readValue(aa, new TypeReference<CustomerFeatureResponse>() {
        });
        CustomerFeatureResponse newCustomerFeaturebb = JsonUtil.readValue(bb, new TypeReference<CustomerFeatureResponse>() {
        });
        CustomerFeatureResponse newCustomerFeaturecc = JsonUtil.readValue(cc, new TypeReference<CustomerFeatureResponse>() {
        });

        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String customerId = line.split("\t")[0];
                line = line.split("\t")[1];
                int num = 0;
                line = line.replace("w", "").replace(">", "").replace("<", "");
                if (line.equals("1-10")){
                    continue;
                } else {
                    if (line.contains("-")){
                        String[] ttt = line.split("-");
                        num = (Integer.parseInt(ttt[0].trim()) + Integer.parseInt(ttt[1].trim()))/2;
                    } else {
                        num = Integer.parseInt(line.trim());
                    }
                }

                CustomerFeatureResponse thisOne = null;
                if (num < 5) {
                    thisOne = newCustomerFeatureaa;
                } else if (num >= 5 && num < 10) {
                    thisOne = newCustomerFeaturebb;
                } else if (num >= 10) {
                    thisOne = newCustomerFeaturecc;
                } else {
                    System.out.println("special num");
                }
                CustomerFeatureResponse customerFeature = customerInfoService.queryCustomerFeatureById(customerId, "373");
                if (Objects.isNull(customerFeature)){
                    System.out.println(customerId + "在系统中不存在，跳过");
                    continue;
                }
                // 不为空，直接退出
                try {
                    if (!StringUtils.isEmpty(customerFeature.getBasic().getFundsVolume().getCustomerConclusion().getCompareValue())){
                        continue;
                    } else {
                        customerInfoService.modifyCustomerFeatureById(customerId, "373", thisOne);
                        System.out.println(customerId + "处理成功");
                    }
                } catch (NullPointerException e) {
                    try {
                        customerInfoService.modifyCustomerFeatureById(customerId, "373", thisOne);
                        System.out.println(customerId + "处理成功");
                    } catch (Exception ex) {
                        log.error("数据不在可识别的范围内", ex);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.error("客户配置初始化");
        configService.refreshCustomerConfig();
    }
}
