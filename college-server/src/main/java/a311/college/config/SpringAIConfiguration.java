package a311.college.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * SpringAI配置类
 */
@Configuration
public class SpringAIConfiguration {

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

    /**
     * 创建ChatClient
     *
     * @param chatModel ChatModel实体
     * @return ChatClient
     */
    @Bean
    public ChatClient chatClient(DeepSeekChatModel chatModel, ChatMemory chatMemory) {
        // 使用SpringAI的核心是ChatClient，ChatClient需要一个Model，就是想要使用的模型，SpringAI封装好了常见的模型，可以直接使用
        return ChatClient
                // 创建ChatClient工厂
                .builder(chatModel)
                // 设置System背景信息，现在只需要在创建ChatClient的时候指定System背景信息即可，不需要每次发送请求的时候都封装到Message中
                .defaultSystem("你的名字叫豆包")
                // 通过Spring AOP给大模型调用添加环绕通知，进行日志记录
                // 添加日志记录Advisor
                // 添加会话记忆Advisor
                .defaultAdvisors(SimpleLoggerAdvisor.builder().build(), MessageChatMemoryAdvisor.builder(chatMemory).build())
                // 构建ChatClient实例
                .build();
    }

}
