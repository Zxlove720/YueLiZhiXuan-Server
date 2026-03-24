package a311.college.controller.doubao;

import a311.college.dto.ai.UserAIRequestDTO;
import a311.college.entity.agent.ChatRecord;
import a311.college.result.Result;
import a311.college.service.DouBaoService;
import a311.college.thread.ThreadLocalUtil;
import a311.college.vo.ai.UserAIMessageVO;
import cn.hutool.core.lang.UUID;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ai")
public class DouBaoController {

    private final ChatClient chatClient;

    private final DouBaoService douBaoService;

    @Autowired
    public DouBaoController(DouBaoService douBaoService, ChatClient chatClient) {
        this.douBaoService = douBaoService;
        this.chatClient = chatClient;
    }

    /**
     * 回答用户问题
     *
     * @param request 用户请求
     * @return Result<Void>
     */
    @PostMapping("/answer")
    @Operation(summary = "DeepSeekApi回答问题")
    public Result<UserAIMessageVO> responseQuestion(@RequestBody UserAIRequestDTO request) {
        return Result.success(douBaoService.response(request));
    }

    /**
     * SpringAI实现对话功能
     *
     * @param prompt 用户提示词
     * @return Result<String> 大模型生成结果
     */
    @GetMapping("/chat-nonstream")
    public Result<String> chatNonStream(@RequestParam(defaultValue = "你好") String prompt) {
        return Result.success(chatClient
                // 传入提示词
                .prompt(prompt)
                // 调用，使用非流式输出
                .call()
                // 返回响应内容
                .content());
    }

    /**
     * SpringAI实现对话功能
     *
     * @param prompt 用户提示词
     * @return Result<Flux<String>> 大模型生成结果
     */
    @RequestMapping("/chat")
    public Flux<String> chat(@RequestParam(defaultValue = "你好") String prompt) {
        log.info("进入chat Stream方法");
//        // 常规使用 非深度思考
//        return chatClient
//                // 传入提示词
//                .prompt(prompt)
//                // 调用，使用非流式输出
//                .stream()
//                // 返回响应内容
//                .content();

        // DeepSeek深度思考特殊处理
        // 常规模型返回的是AssistantMessage类型，但是DeepSeek返回的是DeepSeekAssistantMessage类型，而思考内容在reasoning_content中，需要特殊处理

        // 保存会话记录
        ChatRecord chatRecord = new ChatRecord();
        Long userId = ThreadLocalUtil.getCurrentId();
        String conversationId = UUID.randomUUID().toString() + userId;
        chatRecord.setConversationId(conversationId);
        chatRecord.setUserId(userId);
        chatRecord.setTitle(userId + "对话");
        chatRecord.setCreateTime(LocalDateTime.now());
        douBaoService.saveRecord(chatRecord);
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

    /**
     * 获取会话记录
     *
     * @return Result<List<ChatRecord>>
     */
    @GetMapping("/record")
    public Result<List<ChatRecord>> getChatRecord() {
        return Result.success(douBaoService.getChatRecord(ThreadLocalUtil.getCurrentId()));
    }

}
