package a311.college.controller.agent;

import a311.college.agent.AgentMessageVO;
import a311.college.agent.AgentUtil;
import a311.college.entity.agent.ChatRecord;
import a311.college.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/agent")
public class AgentController {

    private final AgentUtil agentUtil;

    public AgentController(AgentUtil agentUtil) {
        this.agentUtil = agentUtil;
    }

//    /**
//     * 回答用户问题
//     *
//     * @param request 用户请求
//     * @return Result<Void>
//     */
//    @PostMapping("/answer")
//    @Operation(summary = "DeepSeekApi回答问题")
//    public Result<UserAIMessageVO> responseQuestion(@RequestBody UserAIRequestDTO request) {
//        return Result.success(douBaoService.response(request));
//    }

    /**
     * SpringAI实现对话功能
     *
     * @param prompt 用户提示词
     * @return Result<Flux<String>> 大模型生成结果
     */
    @RequestMapping("/chat")
    public Flux<String> chat(@RequestParam(defaultValue = "你好") String prompt, @RequestParam @NotNull String chatId) {
        return agentUtil.chatWithThink(prompt, chatId);
    }

    /**
     * 获取会话记录
     *
     * @return Result<List<ChatRecord>>
     */
    @GetMapping("/record")
    public Result<List<ChatRecord>> getChatRecord() {
        return Result.success(agentUtil.getChatRecord());
    }

    /**
     * 获取对话历史
     *
     * @param chatId 对话id
     * @return Result<List<AgentMessageVO>>
     */
    @GetMapping("/{chatId}")
    public Result<List<AgentMessageVO>> getChatHistory(@PathVariable String chatId) {
        return Result.success(agentUtil.getChatHistory(chatId));
    }

}
