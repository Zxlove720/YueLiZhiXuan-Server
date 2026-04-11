package a311.college.tool;

import a311.college.constant.redis.ToolContextRedisKey;
import a311.college.dto.volunteer.AddVolunteerDTO;
import a311.college.entity.volunteer.VolunteerTable;
import a311.college.result.PageResult;
import a311.college.service.VolunteerService;
import a311.college.thread.ThreadLocalUtil;
import a311.college.tool.entity.AddVolunteer;
import a311.college.tool.entity.VolunteerQuery;
import a311.college.vo.volunteer.SchoolVolunteer;
import a311.college.vo.volunteer.ScoreLine;
import a311.college.vo.volunteer.VolunteerSummary;
import a311.college.vo.volunteer.VolunteerVO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 工具调用
 */
@Slf4j
@Component
public class ToolCalling {

    private final VolunteerService volunteerService;

    private final StringRedisTemplate redisTemplate;

    public ToolCalling(VolunteerService volunteerService, StringRedisTemplate redisTemplate) {
        this.volunteerService = volunteerService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 志愿推荐工具
     * <p>
     * 返回精简摘要供 LLM 展示，完整数据按 index 缓存到 Redis，
     * 后续 addVolunteer 通过 index 取完整数据，无需 LLM 搬运字段。
     * </p>
     */
    @Tool(description = "根据条件推荐志愿，返回精简的推荐摘要列表，每条记录包含序号(index)、学校名、专业名、冲稳保类型、分差和位次差")
    public String getVolunteer(@ToolParam(description = "推荐志愿使用的条件") VolunteerQuery volunteerQuery,
                               ToolContext toolContext) {
        log.info("正在调用志愿查询工具");
        String conversationId = toolContext.getContext().get("conversationId").toString();
        String summaryKey = ToolContextRedisKey.TOOL_VOLUNTEER_SUMMARY_KEY + conversationId + ":";
        // 如果本轮对话已有推荐结果缓存，直接返回摘要文本
        String cachedSummary = redisTemplate.opsForValue().get(summaryKey);
        if (cachedSummary != null && !cachedSummary.isEmpty()) {
            log.info("命中摘要缓存，直接返回");
            return cachedSummary;
        }
        // 查询数据库获取完整推荐结果
        PageResult<SchoolVolunteer> pageResult = volunteerService.showVolunteer(volunteerQuery.toVolunteerPageDTO());
        List<SchoolVolunteer> records = pageResult.getRecords();
        if (records == null || records.isEmpty()) {
            return "未找到符合条件的志愿推荐结果，请检查省份、成绩、位次是否准确。";
        }
        // 构建精简摘要 + 按 index 缓存完整数据
        List<VolunteerSummary> summaries = new ArrayList<>();
        String detailKeyPrefix = ToolContextRedisKey.TOOL_VOLUNTEER_DETAIL_KEY + conversationId + ":";
        int globalIndex = 1;
        for (SchoolVolunteer school : records) {
            if (school.getVolunteerVOList() == null) continue;
            for (VolunteerVO vo : school.getVolunteerVOList()) {
                // 取最近一年的分数线作为摘要展示
                ScoreLine latest = (vo.getScoreLineList() != null && !vo.getScoreLineList().isEmpty())
                        ? vo.getScoreLineList().get(0) : null;
                VolunteerSummary summary = new VolunteerSummary();
                summary.setIndex(globalIndex);
                summary.setSchoolName(school.getSchoolName());
                summary.setMajorName(vo.getMajorName());
                summary.setCategory(vo.getCategory());
                summary.setLatestMinScore(latest != null ? latest.getMinScore() : null);
                summary.setScoreThanMe(latest != null ? latest.getScoreThanMe() : null);
                summary.setRankingThanMe(latest != null ? latest.getRankingThanMe() : null);
                summaries.add(summary);
                // 将完整的单条数据缓存到 Redis（按 index 存储）
                // 构建 AddVolunteerDTO 所需的全部字段
                AddVolunteerDTO detailDTO = new AddVolunteerDTO();
                detailDTO.setMajorId(vo.getMajorId());
                detailDTO.setCategory(vo.getCategory());
                if (latest != null) {
                    detailDTO.setYear(latest.getYear());
                    detailDTO.setMinScore(latest.getMinScore());
                    detailDTO.setMinRanking(latest.getMinRanking());
                    detailDTO.setScoreThanMe(latest.getScoreThanMe());
                    detailDTO.setRankingThanMe(latest.getRankingThanMe());
                }
                redisTemplate.opsForValue().set(
                        detailKeyPrefix + globalIndex,
                        JSON.toJSONString(detailDTO),
                        ToolContextRedisKey.TOOL_VOLUNTEER_LIST_TTL, TimeUnit.SECONDS);
                globalIndex++;
            }
        }
        // 将摘要文本缓存到 Redis
        StringBuilder sb = new StringBuilder();
        sb.append("共找到 ").append(summaries.size()).append(" 条推荐志愿：\n");
        for (VolunteerSummary s : summaries) {
            sb.append(s.toString()).append("\n");
        }
        String summaryText = sb.toString();
        redisTemplate.opsForValue().set(summaryKey, summaryText,
                ToolContextRedisKey.TOOL_VOLUNTEER_LIST_TTL, TimeUnit.SECONDS);
        return summaryText;
    }

    @Tool(description = "帮助用户创建志愿表")
    public VolunteerTable createVolunteerTable(@ToolParam(description = "志愿表名称") String tableName, ToolContext toolContext) {
        log.info("正在调用志愿表创建工具");
        ThreadLocalUtil.setCurrentId((Long) toolContext.getContext().get("userId"));
        VolunteerTable volunteerTable = new VolunteerTable();
        volunteerTable.setTableName(tableName);
        volunteerService.createVolunteerTable(volunteerTable);
        String conversationId = toolContext.getContext().get("conversationId").toString();
        String key = ToolContextRedisKey.TOOL_TABLE_ID_KEY + conversationId + ":";
        List<VolunteerTable> volunteerTableList;
        List<VolunteerTable> cacheVolunteerTable = JSON.parseArray(redisTemplate.opsForValue().get(key), VolunteerTable.class);
        if (cacheVolunteerTable == null || cacheVolunteerTable.isEmpty()) {
            volunteerTableList = new ArrayList<>();
        } else {
            volunteerTableList = cacheVolunteerTable;
        }
        volunteerTableList.add(volunteerTable);
        redisTemplate.opsForValue().set(key, JSON.toJSONString(volunteerTableList),
                ToolContextRedisKey.TOOL_VOLUNTEER_LIST_TTL, TimeUnit.SECONDS);
        return volunteerTable;
    }

    /**
     * 志愿添加工具
     * <p>
     * LLM 只需传入序号列表，工具自动从 Redis 取完整数据并添加到志愿表。
     * </p>
     */
    @Tool(description = "将推荐的志愿添加到志愿表中，只需传入志愿推荐结果中的序号即可")
    public String addVolunteer(@ToolParam(description = "要添加的志愿序号列表") AddVolunteer addVolunteer,
                               ToolContext toolContext) {
        log.info("正在调用志愿添加工具");
        ThreadLocalUtil.setCurrentId((Long) toolContext.getContext().get("userId"));
        String conversationId = toolContext.getContext().get("conversationId").toString();

        // 获取 tableId
        Integer tableId = addVolunteer.getTableId();
        if (tableId == null) {
            String tableKey = ToolContextRedisKey.TOOL_TABLE_ID_KEY + conversationId + ":";
            List<VolunteerTable> volunteerTableList = JSON.parseArray(
                    redisTemplate.opsForValue().get(tableKey), VolunteerTable.class);
            if (volunteerTableList != null && !volunteerTableList.isEmpty()) {
                tableId = volunteerTableList.get(volunteerTableList.size() - 1).getTableId();
            } else {
                return "未找到志愿表，请先创建志愿表。";
            }
        }

        String detailKeyPrefix = ToolContextRedisKey.TOOL_VOLUNTEER_DETAIL_KEY + conversationId + ":";
        List<Integer> indexes = addVolunteer.getIndexes();
        if (indexes == null || indexes.isEmpty()) {
            return "未指定要添加的志愿序号。";
        }

        List<String> successList = new ArrayList<>();
        List<String> failList = new ArrayList<>();

        for (Integer idx : indexes) {
            String detailJson = redisTemplate.opsForValue().get(detailKeyPrefix + idx);
            if (detailJson == null || detailJson.isEmpty()) {
                failList.add("序号" + idx + "（未找到对应数据）");
                continue;
            }
            try {
                AddVolunteerDTO dto = JSON.parseObject(detailJson, AddVolunteerDTO.class);
                dto.setTableId(tableId);
                log.info(dto.toString());
                volunteerService.addVolunteer(dto);
                successList.add("序号" + idx);
            } catch (Exception e) {
                failList.add("序号" + idx + "（" + e.getMessage() + "）");
            }
        }

        StringBuilder result = new StringBuilder();
        if (!successList.isEmpty()) {
            result.append("成功添加：").append(String.join("、", successList)).append("。");
        }
        if (!failList.isEmpty()) {
            result.append("添加失败：").append(String.join("、", failList)).append("。");
        }
        return result.toString();
    }

}
