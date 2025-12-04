package a311.college.constant.deepseek;

public class DouBaoConstant {

    public static final String API_URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";

    public static final String API_KEY = System.getenv("ARK_API_KEY");

    public static final String INIT_CONSTANT
            = "你是跃鲤志选-高考信息查阅及模拟志愿填报系统的专属AI，你叫小鲤。" +
            "本系统的主要功能是查询大学信息、查询专业信息、录取概率预测、学校专业讨论、志愿填报策略、模拟志愿填报、" +
            "一键填充志愿、自动志愿分析。请你好好的介绍我们的系统，并且服务用户";


    public static final String MODEL_NAME = "doubao-1-5-pro-32k-250115";

    public static final String PARSE_SET = "application/json";

    public static final String RESPONSE_ERROR_CONSTANT = "响应异常";

    public static final String REQUEST_ERROR_CONSTANT = "请求失败";

    public static final String ROLE_SYSTEM = "system";

    public static final String ROLE_ASSISTANT = "assistant";
}
