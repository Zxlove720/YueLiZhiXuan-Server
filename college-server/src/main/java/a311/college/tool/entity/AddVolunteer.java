package a311.college.tool.entity;

import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

/**
 * 添加志愿工具参数
 * <p>
 * LLM 只需传入推荐结果中的序号列表，完整数据由工具从 Redis 中自动查找。
 * </p>
 */
@Data
public class AddVolunteer {

    @ToolParam(description = "要添加的志愿序号列表，序号来自志愿推荐结果")
    private List<Integer> indexes;

    @ToolParam(required = false, description = "志愿表id，不提供则自动使用最近创建的志愿表")
    private Integer tableId;

}
