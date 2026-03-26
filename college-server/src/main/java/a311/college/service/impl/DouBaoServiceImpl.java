package a311.college.service.impl;

import a311.college.agent.AgentMessageVO;
import a311.college.agent.AgentUtil;
import a311.college.constant.deepseek.DouBaoConstant;
import a311.college.constant.error.ErrorConstant;
import a311.college.constant.redis.DouBaoRedisKey;
import a311.college.dto.ai.MajorAIRequestDTO;
import a311.college.dto.ai.SchoolAIRequestDTO;
import a311.college.dto.ai.UserAIRequestDTO;
import a311.college.dto.volunteer.AnalyseDTO;
import a311.college.entity.agent.ChatRecord;
import a311.college.entity.volunteer.Volunteer;
import a311.college.exception.DouBaoAPIErrorException;
import a311.college.mapper.agent.ChatRecordMapper;
import a311.college.mapper.major.MajorMapper;
import a311.college.mapper.school.SchoolMapper;
import a311.college.mapper.volunteer.VolunteerMapper;
import a311.college.service.DouBaoService;
import a311.college.thread.ThreadLocalUtil;
import a311.college.vo.ai.UserAIMessageVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;


import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DouBaoServiceImpl implements DouBaoService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final SchoolMapper schoolMapper;

    private final MajorMapper majorMapper;

    private final VolunteerMapper volunteerMapper;

    private final ChatRecordMapper chatRecordMapper;

    private final AgentUtil agentUtil;

    public DouBaoServiceImpl(RedisTemplate<String, Object> redisTemplate, SchoolMapper schoolMapper,
                             MajorMapper majorMapper, VolunteerMapper volunteerMapper,
                             ChatRecordMapper chatRecordMapper, AgentUtil agentUtil) {
        this.redisTemplate = redisTemplate;
        this.schoolMapper = schoolMapper;
        this.majorMapper = majorMapper;
        this.volunteerMapper = volunteerMapper;
        this.chatRecordMapper = chatRecordMapper;
        this.agentUtil = agentUtil;
    }

    /**
     * 将回答的markdown格式转换为HTML格式
     *
     * @param markdown markdown格式
     * @return HTML格式
     */
    public String markDown2HTML(String markdown) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(document).replaceAll("\n", "");
    }

    /**
     * AI回答用户问题
     *
     * @param request 用户AI请求
     * @return UserAIMessageVO
     */
    public UserAIMessageVO response(UserAIRequestDTO request) {
        // 1.初始化AI
        initUserMessageHistory();
        // 2.将用户消息添加到Redis
        try {
            addMessage(request.getMessage());
        } catch (Exception e) {
            log.error(ErrorConstant.REDIS_CONNECTION_ERROR, e);
        }
        // 3.获取用户对话消息历史
        JSONArray messages = buildHistoryMessageArray();
        // 4.豆包API请求体构建
        JSONObject requestBody = new JSONObject();
        // 4.1选择模型
        requestBody.put("model", DouBaoConstant.MODEL_NAME);
        // 4.2封装消息
        requestBody.put("messages", messages);
        // 4.3设置模型温度
        requestBody.put("temperature", 0.3);
        // 4.4不开启流式输出
        requestBody.put("stream", false);
        // 5.设置请求客户端
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        // 6.请求构建
        Request DouBaoRequest = new Request.Builder()
                .url(DouBaoConstant.API_URL)
                .addHeader("Authorization", "Bearer " + DouBaoConstant.API_KEY)
                .addHeader("Content-Type", DouBaoConstant.PARSE_SET)
                .post(RequestBody.create(
                        requestBody.toJSONString(),
                        MediaType.parse(DouBaoConstant.PARSE_SET)))
                .build();
        // 7.发起请求并接收响应
        try (Response response = client.newCall(DouBaoRequest).execute()) {
            // 7.1响应失败，封装错误信息返回
            if (!response.isSuccessful()) {
                log.error("API请求失败，状态码：{}", response.code());
                return errorResponse();
            }
            // 7.2响应成功，获取响应体
            JSONObject responseJson = null;
            if (response.body() != null) {
                responseJson = JSON.parseObject(response.body().string());
            }
            // 7.3解析响应体
            String answer = extractAnswer(responseJson);
            // 7.4将这一次回答添加到redis，作为对话历史
            addMessage(new UserAIMessageVO(DouBaoConstant.ROLE_ASSISTANT, answer));
            // 7.5封装UserAIMessageVO返回
            return new UserAIMessageVO(DouBaoConstant.ROLE_ASSISTANT, markDown2HTML(StringEscapeUtils.escapeHtml4(answer)));
        } catch (IOException e) {
            log.error("API调用异常", e);
            throw new DouBaoAPIErrorException(ErrorConstant.DOUBAO_SERVICE_ERROR);
        }
    }

    /**
     * 创建用户消息Key
     *
     * @return Key
     */
    private String buildUserMessageKey() {
        return DouBaoRedisKey.DOUBAO_HISTORY_KEY + ThreadLocalUtil.getCurrentId();
    }

    /**
     * 初始化用户对话历史
     */
    private void initUserMessageHistory() {
        String key = buildUserMessageKey();
        if (!redisTemplate.hasKey(key)) {
            // 1.如果没有对话历史将进行初始化
            JSONObject systemMessage = new JSONObject()
                    .fluentPut("role", DouBaoConstant.ROLE_SYSTEM)
                    .fluentPut("content", DouBaoConstant.INIT_CONSTANT);
            // 2.将该用户初始化内容加入Redis
            redisTemplate.opsForList().rightPush(key, systemMessage.toJSONString());
            redisTemplate.expire(key, DouBaoRedisKey.DOUBAO_HISTORY_TTL, TimeUnit.HOURS);
        }
    }

    /**
     * 添加消息到Redis
     *
     * @param message 消息
     */
    private void addMessage(UserAIMessageVO message) {
        JSONObject msg = new JSONObject()
                .fluentPut("role", message.getRole())
                .fluentPut("content", message.getContent());
        redisTemplate.opsForList().rightPush(buildUserMessageKey(), msg.toJSONString());
    }

    /**
     * 构建用户历史对话数组
     *
     * @return JSONArray 用户的历史对话
     */
    private JSONArray buildHistoryMessageArray() {
        // 1.从Redis中取出用户的历史对话
        List<Object> messages = redisTemplate.opsForList().range(buildUserMessageKey(), 0, -1);
        // 2.将历史对话封装为JSONArray返回
        return Optional.ofNullable(messages)
                .map(list -> list.stream()
                        .map(String::valueOf)
                        .map(JSON::parseObject)
                        .collect(Collectors.toCollection(JSONArray::new)))
                .orElseGet(JSONArray::new);
    }

    /**
     * 解析响应体
     *
     * @param response 响应体
     * @return String 回答
     */
    private String extractAnswer(JSONObject response) {
        try {
            return response.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (Exception e) {
            log.error(ErrorConstant.RESPONSE_PRASE_ERROR, e);
            return ErrorConstant.RESPONSE_PRASE_ERROR;
        }
    }

    /**
     * 请求错误，封装错误信息
     *
     * @return UserAIMessageVO 错误信息
     */
    private UserAIMessageVO errorResponse() {
        return new UserAIMessageVO(DouBaoConstant.ROLE_ASSISTANT, ErrorConstant.DOUBAO_SERVICE_ERROR);
    }

//    /**
//     * 请求AI获取学校信息
//     *
//     * @param schoolAIRequestDTO 大学AI请求DTO
//     * @return SchoolAIMessageVO 大学AI请求VO
//     */
//    @Override
//    public SchoolAIMessageVO schoolInformation(SchoolAIRequestDTO schoolAIRequestDTO) {
//        // 1.获取需要请求的学校名
//        String schoolName = schoolMapper.selectBySchoolId(schoolAIRequestDTO.getSchoolId()).getSchoolName();
//        // 2.封装问题
//        String question = "请为我介绍" + schoolName + "的详细信息";
//        Request request = buildRequest(question);
//        // 3.发起请求并获取回答
//        String answer = executeRequest(request);
//        log.info(DouBaoConstant.ROLE_ASSISTANT + "{}", answer);
//        return new SchoolAIMessageVO(DouBaoConstant.ROLE_ASSISTANT, markDown2HTML(StringEscapeUtils.escapeHtml4(answer)));
//    }

    /**
     * 请求agent获取学校信息
     *
     * @param schoolAIRequestDTO 大学agent请求DTO
     * @return AgentMessageVO agent消息VO
     */
    @Override
    public AgentMessageVO schoolInformation(SchoolAIRequestDTO schoolAIRequestDTO) {
        // 1.获取需要请求的学校名
        String schoolName = schoolMapper.selectBySchoolId(schoolAIRequestDTO.getSchoolId()).getSchoolName();
        // 2.封装提示词
        String prompt = "请为我介绍" + schoolName + "的详细信息";
        // 3.发起请求并获取回答
        String answer = agentUtil.simpleChat(prompt);
        return new AgentMessageVO(DouBaoConstant.ROLE_ASSISTANT, markDown2HTML(StringEscapeUtils.escapeHtml4(answer)));
    }

//    /**
//     * 请求AI获取专业信息
//     *
//     * @param majorAIRequestDTO 专业AI请求DTO
//     * @return MajorAIMessageVO 专业AI请求VO
//     */
//    @Override
//    public MajorAIMessageVO majorInformation(MajorAIRequestDTO majorAIRequestDTO) {
//        // 1.获取需要请求的专业名并封装问题
//        String majorName = majorMapper.selectById(majorAIRequestDTO.getMajorId()).getMajorName();
//        String question = "请为我介绍" + majorName + "这个专业";
//        // 2.构建请求
//        Request request = buildRequest(question);
//        // 3.发起请求并获取回答
//        String answer = executeRequest(request);
//        log.info(DouBaoConstant.ROLE_ASSISTANT + "{}", answer);
//        return new MajorAIMessageVO(DouBaoConstant.ROLE_ASSISTANT, markDown2HTML(StringEscapeUtils.escapeHtml4(answer)));
//    }

    /**
     * 请求agent获取专业信息
     *
     * @param majorAIRequestDTO 专业agent请求DTO
     * @return AgentMessageVO agent消息VO
     */
    @Override
    public AgentMessageVO majorInformation(MajorAIRequestDTO majorAIRequestDTO) {
        // 1.获取需要请求的专业名并封装问题
        String majorName = majorMapper.selectById(majorAIRequestDTO.getMajorId()).getMajorName();
        // 2.封装提示词
        String prompt = "请为我介绍" + majorName + "这个专业的详细信息";
        // 3.发起请求并获取回答
        String answer = agentUtil.chat(prompt);
        return new AgentMessageVO(DouBaoConstant.ROLE_ASSISTANT, markDown2HTML(StringEscapeUtils.escapeHtml4(answer)));
    }

    /**
     * 分析用户志愿表
     *
     * @param analyseDTO 分析志愿表DTO
     * @return UserAIMessageVO
     */
    @Override
    public UserAIMessageVO analyseVolunteerTable(AnalyseDTO analyseDTO) {
        // 1.获取用户志愿表
        List<Volunteer> volunteerList = volunteerMapper.selectVolunteers(analyseDTO.getTableId());
        // 2.封装请求问题
        String question = "我是" + analyseDTO.getYear() + "年参加高考的" + analyseDTO.getProvince() + "考生，我的高考成绩是" +
                analyseDTO.getGrade() + "我的高考位次是" + analyseDTO.getRanking() + "这是我模拟填报的志愿表：\n" +
                volunteerList.toString() + "\n请为我分析是否合理，并提出一些建议";
        Request request = buildRequest(question);
        // 3.发起请求并获取回答
        String answer = executeRequest(request);
        log.info(DouBaoConstant.ROLE_ASSISTANT + "{}", answer);
        return new UserAIMessageVO(DouBaoConstant.ROLE_ASSISTANT, markDown2HTML(StringEscapeUtils.escapeHtml4(answer)));
    }

    /**
     * 构建API请求
     *
     * @param question 请求问题
     * @return Request 请求
     */
    private Request buildRequest(String question) {
        // 1.将问题封装为JSON数组
        JSONArray message = new JSONArray();
        JSONObject initMessage = new JSONObject();
        initMessage.put("role", DouBaoConstant.ROLE_ASSISTANT);
        initMessage.put("content", question);
        message.add(initMessage);
        // 2.构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", DouBaoConstant.MODEL_NAME);
        requestBody.put("messages", message);
        requestBody.put("temperature", 0.3);
        requestBody.put("stream", false);
        // 3.构建新的请求，请求DeepSeekAPI
        return new Request.Builder()
                .url(DouBaoConstant.API_URL)
                .addHeader("Authorization", "Bearer " + DouBaoConstant.API_KEY)
                .addHeader("Content-Type", DouBaoConstant.PARSE_SET)
                .post(RequestBody.create(
                        requestBody.toJSONString(),
                        MediaType.parse(DouBaoConstant.PARSE_SET)))
                .build();
    }

    /**
     * 请求DeepSeekAPI并封装回答
     *
     * @param request 请求
     * @return answer 回答
     */
    private String executeRequest(Request request) {
        // 1.获取请求客户端
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        // 2.发起请求并接收响应
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // 2.1响应失败，记录错误日志并返回
                log.info(DouBaoConstant.RESPONSE_ERROR_CONSTANT);
                throw new DouBaoAPIErrorException(DouBaoConstant.RESPONSE_ERROR_CONSTANT);
            }
            // 2.2获取响应体
            JSONObject responseJson = null;
            if (response.body() != null) {
                responseJson = JSON.parseObject(response.body().string());
            }
            // 2.3解析响应体获取回答并返回
            return extractAnswer(responseJson);
        } catch (IOException e) {
            // 2.4请求失败则报错
            log.info(DouBaoConstant.REQUEST_ERROR_CONSTANT);
            throw new DouBaoAPIErrorException(DouBaoConstant.REQUEST_ERROR_CONSTANT);
        }
    }

    /**
     * 保存会话记录
     *
     * @param record 会话记录
     */
    @Override
    public void saveRecord(ChatRecord record) {
        chatRecordMapper.saveRecord(record);
    }

    @Override
    public List<ChatRecord> getUserChatRecordList(Long userId) {
        return chatRecordMapper.findRecordByUserId(userId);
    }


    public ChatRecord getChatRecordDetail(String conversationId) {
        return chatRecordMapper.findRecordByConversationId(conversationId);
    }

}