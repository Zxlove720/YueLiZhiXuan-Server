package a311.college.constant.redis;

public class ToolContextRedisKey {

    /** 志愿推荐摘要列表（供 LLM 读取的精简数据） */
    public static final String TOOL_VOLUNTEER_SUMMARY_KEY = "college:tool:volunteer:summary:";

    /** 志愿推荐完整数据（按 index 存储，供 addVolunteer 工具使用） */
    public static final String TOOL_VOLUNTEER_DETAIL_KEY = "college:tool:volunteer:detail:";

    public static final Integer TOOL_VOLUNTEER_LIST_TTL = 3600;

    public static final String TOOL_TABLE_ID_KEY = "college:tool:tableId:conversation:";

    public static final Integer TOOL_TABLE_ID_TTL = 3600;

}
