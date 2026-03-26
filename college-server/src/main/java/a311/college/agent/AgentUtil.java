package a311.college.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.validation.constraints.NotNull;

@Slf4j
@Component
public class AgentUtil {

    // 带对话记忆的chatClient，主要用于用户和agent交互
    private final ChatClient chatClient;

    // 没有对话记忆的chatClient，主要用于请求大模型获取大学、专业的信息
    private final ChatClient simpleChatClient;

    public AgentUtil(ChatClient chatClient, @Qualifier(value = "simpleChatClient") ChatClient simpleChatClient) {
        this.chatClient = chatClient;
        this.simpleChatClient = simpleChatClient;
    }

    /**
     * 请求大模型获取信息
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

    public Flux<String> chatWithStream(String prompt, @NotNull String conversationId) {
        return chatClient
                .prompt(prompt)
                // ChatMemory是根据conversationId来区分不同会话的，所以说为了区分不同对话，需要在发送请求的时候携带会话id
                .advisors(advisorSpec -> advisorSpec
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .chatResponse()
                .mapNotNull(this::handlerReasoningContent);
    }

    /**
     * 处理DeepSeek模型深度思考内容
     *
     * @param chatResponse ChatResponse 模型响应
     * @return String 深度思考内容
     */
    private String handlerReasoningContent(ChatResponse chatResponse) {
        // 需要把AssistantMessage类型转换为DeepSeekAssistantMessage类型
        DeepSeekAssistantMessage deepSeekAssistantMessage = (DeepSeekAssistantMessage) chatResponse.getResult().getOutput();
        String reasoningContent = deepSeekAssistantMessage.getReasoningContent();
        if (reasoningContent != null && !reasoningContent.isBlank()) {
            // 如果推理结果存在，则将其包裹上<think></think>标签，方便前端处理
            return "<think>" + reasoningContent + "</think>";
        }
        // 没有推理结果，直接返回文本
        return deepSeekAssistantMessage.getText();
    }

}
