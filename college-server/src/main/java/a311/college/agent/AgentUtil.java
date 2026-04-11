package a311.college.agent;

import a311.college.entity.agent.ChatRecord;
import a311.college.service.AgentService;
import a311.college.thread.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Flux;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 大模型相关工具类
 */
@Slf4j
@Component
public class AgentUtil {

    // 带对话记忆的chatClient，主要用于用户和agent交互
    private final ChatClient agentChatClient;

    // 没有对话记忆的chatClient，主要用于请求大模型获取大学、专业的信息
    private final ChatClient simpleChatClient;

    // 对话记忆Repository，用于获取对话记忆
    private final ChatMemoryRepository chatMemoryRepository;

    // 大模型和本地数据库相关服务
    private final AgentService agentService;

    public AgentUtil(@Qualifier(value = "agentChatClient") ChatClient agentChatClient,
                     @Qualifier(value = "simpleChatClient") ChatClient simpleChatClient,
                     AgentService agentService,
                     ChatMemoryRepository chatMemoryRepository) {
        this.agentChatClient = agentChatClient;
        this.simpleChatClient = simpleChatClient;
        this.agentService = agentService;
        this.chatMemoryRepository = chatMemoryRepository;
    }

    /**
     * 请求大模型获取信息
     * <p>
     * 无需会话记忆，无需深度思考，只需要最简单的一次性提问，回答即可
     * </p>
     *
     * @param prompt 提示词
     * @return 大模型返回的信息
     */
    public String chatForInformation(String prompt) {
        return simpleChatClient
                .prompt(prompt)
                .call()
                .content();
    }

    /**
     * 和大模型对话并工具调用
     * <p>
     * 用户和大模型对话，支持会话记忆和工具调用。
     * </p>
     *
     * @param prompt         提示词
     * @param conversationId 对话id
     * @return Flux<String>  流式输出
     */
    public Flux<String> chatWithAgent(String prompt, @NotNull String conversationId) {
        // 在 HTTP 请求线程上捕获 userId，后续工具调用会切换到 boundedElastic 线程，ThreadLocal 不会传递
        Long userId = ThreadLocalUtil.getCurrentId();
        // 判断是否需要创建对话记录ChatRecord
        ChatRecord chatRecordDetail = agentService.getChatRecordDetail(conversationId);
        if (chatRecordDetail == null) {
            log.info("conversationId：{}在数据库中不存在，需要创建会话记录", conversationId);
            // 构造会话记录
            ChatRecord chatRecord = new ChatRecord();
            chatRecord.setConversationId(conversationId);
            chatRecord.setUserId(userId);
            chatRecord.setTitle(prompt.length() >= 10 ? prompt.substring(0, 10): prompt);
            chatRecord.setCreateTime(LocalDateTime.now());
            // 保存会话记录
            agentService.saveRecord(chatRecord);
        }
        return agentChatClient
                .prompt(prompt)
                // ChatMemory是根据conversationId来区分不同会话的，所以说为了区分不同对话，需要在发送请求的时候携带会话id
                .advisors(advisorSpec -> advisorSpec
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                // 将 userId 注入工具上下文，工具执行时从 ToolContext 中取出，规避 ThreadLocal 跨线程丢失问题
                // 将 conversationId 注入工具上下文，因为ToolContext是一个只读的Map，所以说不能直接往里面写不同工具调用后返回的数据
                // 所以说需要传递 conversationId 区分不同对话，然后将工具调用返回的数据保存到Redis中当作上下文存储
                .toolContext(new HashMap<>(Map.of("userId", userId, "conversationId", conversationId)))
                .stream()
                .content();
    }

//    /**
//     * 处理DeepSeek模型深度思考内容
//     * <p>
//     * 处理DeepSeek模型深度思考的内容，其他模型调用chatResponse方法返回的是AssistantMessage类型，需要转换为DeepSeek模型
//     * 专用的DeepSeekAssistantMessage类型，并从中获取ReasoningContent，ReasoningContent就是思考内容
//     * DeepSeek 模型已废弃
//     * </p>
//     *
//     * @param chatResponse ChatResponse 模型响应
//     * @return String 深度思考内容
//     */
//    private String handlerReasoningContent(ChatResponse chatResponse) {
//        // 需要把AssistantMessage类型转换为DeepSeekAssistantMessage类型
//        DeepSeekAssistantMessage deepSeekAssistantMessage = (DeepSeekAssistantMessage) chatResponse.getResult().getOutput();
//        String reasoningContent = deepSeekAssistantMessage.getReasoningContent();
//        if (reasoningContent != null && !reasoningContent.isBlank()) {
//            // 如果推理结果存在，则将其包裹上<think></think>标签，方便前端处理
//            return "<think>" + reasoningContent + "</think>";
//        }
//        // 没有推理结果，直接返回文本
//        String text = deepSeekAssistantMessage.getText();
//        // getText() 可能返回 null 或空白，统一过滤，避免无效 SSE 事件穿透 mapNotNull
//        if (text == null || text.isBlank()) {
//            return null;
//        }
//        return text;
//    }

    /**
     * 获取对话记录
     * <p>
     * 根据用户id获取用户对话记录
     * </p>
     *
     * @return List<ChatRecord>
     */
    public List<ChatRecord> getChatRecord() {
        return agentService.getUserChatRecordList(ThreadLocalUtil.getCurrentId());
    }

    /**
     * 获取对话历史
     * <p>
     * 根据chatId获取某次对话历史
     * </p>
     *
     * @param chatId 对话id
     * @return List<AgentMessageVO>
     */
    public List<AgentMessageVO> getChatHistory(@PathVariable String chatId) {
        return chatMemoryRepository.findByConversationId(chatId).stream().map(AgentMessageVO::new).toList();
    }

}
