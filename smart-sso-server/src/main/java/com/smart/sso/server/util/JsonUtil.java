package com.smart.sso.server.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtil {
    private static ObjectMapper DEFAULT_OBJECT_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    static {
        // 容忍json中出现未知的列
        DEFAULT_OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 兼容java中的驼峰的字段名命名
        DEFAULT_OBJECT_MAPPER.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    public static <T> T readValue(String content, TypeReference<T> valueTypeRef) {
        try {
            if (content == null) {
                return null;
            }
            return DEFAULT_OBJECT_MAPPER.readValue(content, valueTypeRef);
        } catch (Exception e) {
            log.error("failed to parse content to list. [type={}]", valueTypeRef.getType(), e);
            throw new RuntimeException("failed to read value by specified type", e);
        }
    }

    public static final String summary_string = "{\n" +
                "  \"summary\": {\n" +
                "    \"advantage\": [\n" +
                "      \"高效率的交易流程\",\n" +
                "      \"个性化的客户服务\",\n" +
                "      \"先进的技术支持\"\n" +
                "    ],\n" +
                "    \"questions\": [\n" +
                "      \"如何进一步优化交易流程？\",\n" +
                "      \"客户服务如何更加个性化？\"\n" +
                "    ]\n" +
                "  },\n" +
                "  \"info_explanation\": {\n" +
                "    \"info_collection\": \"客户信息收集完毕，包括基本信息和交易偏好。\",\n" +
                "    \"stock\": true,\n" +
                "    \"trade_based_intro\": false,\n" +
                "    \"trade_style_understanding\": \"客户倾向于长期投资，偏好价值股。\",\n" +
                "    \"stock_pick_review\": true,\n" +
                "    \"stock_timing_review\": false\n" +
                "  },\n" +
                "  \"approval_analysis\": {\n" +
                "    \"method\": {\n" +
                "      \"recognition\": \"approved\",\n" +
                "      \"chats\": [\n" +
                "        {\n" +
                "          \"recognition\": \"approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我对你们的交易方法很满意。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"感谢您的肯定，我们会继续努力。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"recognition\": \"approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我对你们的交易方法很满意。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"感谢您的肯定，我们会继续努力。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"issue\": {\n" +
                "      \"recognition\": \"not_approved\",\n" +
                "      \"chats\": [\n" +
                "        {\n" +
                "          \"recognition\": \"not_approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我对交易成本有些担忧。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"我们会为您提供成本优化方案。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"value\": {\n" +
                "      \"recognition\": \"approved\",\n" +
                "      \"chats\": [\n" +
                "        {\n" +
                "          \"recognition\": \"approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我觉得你们的服务很有价值。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"我们致力于提供高价值服务。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"recognition\": \"approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我觉得你们的服务很有价值。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"我们致力于提供高价值服务。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"recognition\": \"approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我觉得你们的服务很有价值。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"我们致力于提供高价值服务。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"recognition\": \"approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我觉得你们的服务很有价值。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"我们致力于提供高价值服务。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"recognition\": \"approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我觉得你们的服务很有价值。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"我们致力于提供高价值服务。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"recognition\": \"approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我觉得你们的服务很有价值。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"我们致力于提供高价值服务。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"recognition\": \"approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我觉得你们的服务很有价值。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"我们致力于提供高价值服务。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"recognition\": \"approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我觉得你们的服务很有价值。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"我们致力于提供高价值服务。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"recognition\": \"approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我觉得你们的服务很有价值。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"我们致力于提供高价值服务。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"recognition\": \"approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我觉得你们的服务很有价值。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"我们致力于提供高价值服务。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"recognition\": \"approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我觉得你们的服务很有价值。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"我们致力于提供高价值服务。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"recognition\": \"approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我觉得你们的服务很有价值。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"我们致力于提供高价值服务。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"price\": {\n" +
                "      \"recognition\": \"not_approved\",\n" +
                "      \"chats\": [\n" +
                "        {\n" +
                "          \"recognition\": \"not_approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我认为你们的价格有点高。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"我们致力于提供性价比高的服务。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"recognition\": \"not_approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我认为你们的价格有点高。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"我们致力于提供性价比高的服务。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"purchase\": {\n" +
                "      \"recognition\": \"approved\",\n" +
                "      \"chats\": [\n" +
                "        {\n" +
                "          \"recognition\": \"approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我对购买流程很满意。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"感谢您的肯定，我们会继续努力。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"recognition\": \"approved\",\n" +
                "          \"messages\": [\n" +
                "            {\n" +
                "              \"role\": \"customer\",\n" +
                "              \"content\": \"我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。我对购买流程很满意。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"role\": \"sales\",\n" +
                "              \"content\": \"感谢您的肯定，我们会继续努力。\",\n" +
                "              \"time\": \"2018-08-08 12:00:00\"\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}";

}
