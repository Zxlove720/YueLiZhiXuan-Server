package a311.college.config;

import a311.college.tool.ToolCalling;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;


/**
 * SpringAI配置类
 */
@Configuration
public class SpringAIConfiguration {

    /**
     * 高考小鲤多轮对话系统提示词
     */
    @Value("classpath:prompt/system.txt")
    private Resource systemPrompt;

    /**
     * 院校/专业/志愿单次信息查询系统提示词
     */
    @Value("classpath:prompt/information.txt")
    private Resource informationPrompt;

    @Value("${college.ai.doubao.base-url}")
    private String doubaoBaseUrl;

    @Value("${college.ai.doubao.api-key}")
    private String doubaoApiKey;

    @Value("${college.ai.doubao.model}")
    private String doubaoModel;

    @Value("${college.ai.doubao.temperature:0.3}")
    private Double doubaoTemperature;

    @Value("${college.ai.qwen.base-url}")
    private String qwenBaseUrl;

    @Value("${college.ai.qwen.api-key}")
    private String qwenApiKey;

    @Value("${college.ai.qwen.model}")
    private String qwenModel;

    @Value("${college.ai.qwen.temperature:0.7}")
    private Double qwenTemperature;

    @Bean("doubaoChatModel")
    public OpenAiChatModel doubaoChatModel() {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(doubaoBaseUrl)
                .apiKey(doubaoApiKey)
                .completionsPath("/chat/completions")
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(doubaoModel)
                .temperature(doubaoTemperature)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }

    @Bean("qwenChatModel")
    public OpenAiChatModel qwenChatModel() {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(qwenBaseUrl)
                .apiKey(qwenApiKey)
                .completionsPath("/chat/completions")
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(qwenModel)
                .temperature(qwenTemperature)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }


//    /**
//     * 自定义 RestClient 超时，覆盖 Spring AI 默认的 10s 超时
//     * Spring AI 的 OpenAiAutoConfiguration 注入 RestClient.Builder 时会应用此 Customizer
//     */
//    @Bean
//    public RestClientCustomizer aiRestClientCustomizer() {
//        return builder -> builder.requestFactory(
//                ClientHttpRequestFactories.get(
//                        ClientHttpRequestFactorySettings.DEFAULTS
//                                .withConnectTimeout(Duration.ofSeconds(30))
//                                .withReadTimeout(Duration.ofSeconds(120))
//                )
//        );
//    }

    /**
     * 创建ChatMemory会话记忆
     * ChatMemory只负责管理会话记忆，并不负责读写记忆，对话记忆的读写是通过ChatMemoryRepository实现的，所以说要将二者结合起来使用
     *
     * @param chatMemoryRepository JDBCChatMemoryRepository 将会话记忆保存到数据库中
     * @return ChatMemory 会话记忆
     */
    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository chatMemoryRepository) {
        // MessageWindowChatMemory 固定窗口大小的会话记忆，相当于在一个窗口中保存会话记忆，当消息数超过窗口最大值时，将删除较旧消息
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(30)
                .build();
    }

//    /**
//     * 创建ChatClient
//     * <p>
//     * 用于用户和agent对话使用的ChatClient，使用DeepSeek模型，支持对话记忆
//     * 因为DeepSeek不同时支持深度思考和工具调用，所以说DeepSeek模型已经废弃！！！
//     * </p>
//     *
//     * @param chatModel ChatModel实体
//     * @return ChatClient
//     */
//    @Bean
//    public ChatClient chatClient(DeepSeekChatModel chatModel, ChatMemory chatMemory, ToolCalling toolCalling) {
//        return ChatClient
//                .builder(chatModel)
//                .defaultSystem(systemPrompt)
//                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), SimpleLoggerAdvisor.builder().build())
//                .defaultTools(toolCalling)
//                .build();
//    }

    /**
     * 创建ChatClient
     * <p>
     * 用于在页面中请求大模型获取信息的chatClient，使用doubao大模型，没有对话记忆，没有深度思考，只能单次对话
     * </p>
     *
     * @param chatModel ChatModel实体
     * @return ChatClient
     */
    @Bean
    public ChatClient simpleChatClient(@Qualifier("doubaoChatModel") OpenAiChatModel chatModel) {
        return ChatClient
                .builder(chatModel)
                .defaultSystem(informationPrompt)
                .defaultAdvisors(SimpleLoggerAdvisor.builder().build())
                .build();
    }

    /**
     * 创建ChatClient
     * <p>
     * 使用Qwen大模型-Qwen3.5-flash，该模型同时具备深度思考和工具调用的能力
     * 该ChatClient支持对话记忆、工具调用等；主要用于用户使用agent对话解决问题
     * </p>
     *
     * @param chatModel   配置好的chatModel实体
     * @param chatMemory  配置好的chatMemory实体
     * @param toolCalling 配置好的tool
     * @return chatClient chatClient实体
     */
    @Bean
    public ChatClient agentChatClient(@Qualifier("qwenChatModel") OpenAiChatModel chatModel, ChatMemory chatMemory, ToolCalling toolCalling) {
        return ChatClient
                .builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), SimpleLoggerAdvisor.builder().build())
                .defaultTools(toolCalling)
                .build();
    }


//    //测试使用的ChatClient，纯净版本，单纯的调用
//    @Bean
//    public ChatClient chatClient(DeepSeekChatModel chatModel) {
//        // 使用SpringAI的核心是ChatClient，ChatClient需要一个Model，就是想要使用的模型，SpringAI封装好了常见的模型，可以直接使用
//        return ChatClient
//                // 创建ChatClient工厂
//                .builder(chatModel)
//                // 设置System背景信息，现在只需要在创建ChatClient的时候指定System背景信息即可，不需要每次发送请求的时候都封装到Message中
//                .defaultSystem("你的名字叫豆包")
//                // 通过Spring AOP给大模型调用添加环绕通知，进行日志记录
//                // 添加日志记录Advisor
//                // 添加会话记忆Advisor
//                .defaultAdvisors(SimpleLoggerAdvisor.builder().build())
//                // 构建ChatClient实例
//                .build();
//    }

}
