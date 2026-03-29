//package a311.college.controller.agent;
//
//import a311.college.agent.AgentMessageVO;
//import a311.college.entity.agent.ChatRecord;
//import a311.college.result.Result;
//import a311.college.service.AgentService;
//import a311.college.thread.ThreadLocalUtil;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.memory.ChatMemory;
//import org.springframework.ai.chat.memory.ChatMemoryRepository;
//import org.springframework.ai.chat.model.ChatResponse;
//import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
//import org.springframework.web.bind.annotation.*;
//import reactor.core.publisher.Flux;
//
//import javax.validation.constraints.NotNull;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Slf4j
//@RestController
//@RequestMapping("/test/ai")
//public class SpringAITestController {
//
//    private final ChatClient chatClient;
//
//    private final AgentService douBaoService;
//
//    private final ChatMemoryRepository chatMemoryRepository;
//
//
//    public SpringAITestController(ChatClient chatClient, AgentService douBaoService, ChatMemoryRepository chatMemoryRepository) {
//        this.chatClient = chatClient;
//        this.douBaoService = douBaoService;
//        this.chatMemoryRepository = chatMemoryRepository;
//    }
//
//    /**
//     * SpringAI实现对话功能
//     * <p>
//     *     最简单的实现，没有对话记忆；没有流式输出，单纯就是把一次请求大模型API的操作进行了封装
//     * </p>
//     * @param prompt 用户提示词
//     * @return Result<String> 大模型生成结果
//     */
//    @GetMapping("/chat-nonstream")
//    public Result<String> chatNonStream(@RequestParam(defaultValue = "你好") String prompt) {
//        return Result.success(chatClient
//                // 传入提示词
//                .prompt(prompt)
//                // 调用，使用非流式输出
//                .call()
//                // 返回响应内容
//                .content());
//    }
//
//    /**
//     * SpringAI实现对话功能
//     * <p>
//     *     项目实际使用实现，支持对话记忆并存储至数据库，支持日志，流式输出；支持深度思考
//     * </p>
//     *
//     * @param prompt 用户提示词
//     * @return Result<Flux<String>> 大模型生成结果
//     */
//    @RequestMapping("/chat")
//    public Flux<String> chat(@RequestParam(defaultValue = "你好") String prompt, @RequestParam @NotNull String chatId) {
////        // 常规使用 非深度思考
////        return chatClient
////                // 传入提示词
////                .prompt(prompt)
////                // 调用，使用非流式输出
////                .stream()
////                // 返回响应内容
////                .content();
//
//        // DeepSeek深度思考特殊处理
//        // 常规模型返回的是AssistantMessage类型，但是DeepSeek返回的是DeepSeekAssistantMessage类型，而思考内容在reasoning_content中，需要特殊处理
//        Long userId = ThreadLocalUtil.getCurrentId();
//        String conversationId = chatId + "-" + userId;
//        ChatRecord chatRecordDetail = douBaoService.getChatRecordDetail(conversationId);
//        if (chatRecordDetail == null) {
//            log.info("conversationId：{}在数据库中不存在，需要创建会话记录", conversationId);
//            // 构造会话记录
//            ChatRecord chatRecord = new ChatRecord();
//            chatRecord.setConversationId(conversationId);
//            chatRecord.setUserId(userId);
//            chatRecord.setTitle(userId + "对话");
//            chatRecord.setCreateTime(LocalDateTime.now());
//            // 保存会话记录
//            douBaoService.saveRecord(chatRecord);
//        }
//        return chatClient
//                .prompt(prompt)
//                // ChatMemory是根据conversationId来区分不同会话的，所以说为了区分不同对话，需要在发送请求的时候携带会话id
//                .advisors(advisorSpec -> advisorSpec
//                        .param(ChatMemory.CONVERSATION_ID, conversationId))
//                .stream()
//                .chatResponse()
//                .mapNotNull(this::handlerReasoningContent);
//    }
//
//    /**
//     * 处理DeepSeek模型深度思考内容
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
//        return deepSeekAssistantMessage.getText();
//    }
//
//    /**
//     * 获取会话记录
//     *
//     * @return Result<List<ChatRecord>>
//     */
//    @GetMapping("/record")
//    public Result<List<ChatRecord>> getChatRecord() {
//        return Result.success(douBaoService.getUserChatRecordList(ThreadLocalUtil.getCurrentId()));
//    }
//
//    /**
//     * 获取对话历史
//     *
//     * @param conversationId 对话id
//     * @return Result<List<AgentMessageVO>>
//     */
//    @GetMapping("/{conversationId}")
//    public Result<List<AgentMessageVO>> getChatHistory(@PathVariable String conversationId) {
//        return Result.success(chatMemoryRepository.findByConversationId(conversationId).stream().map(AgentMessageVO::new).toList());
//    }
//
//
//}
