package a311.college.tool;

import a311.college.entity.volunteer.VolunteerTable;
import a311.college.result.PageResult;
import a311.college.service.VolunteerService;
import a311.college.thread.ThreadLocalUtil;
import a311.college.tool.entity.AddVolunteer;
import a311.college.tool.entity.VolunteerQuery;
import a311.college.vo.volunteer.SchoolVolunteer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;


/**
 * 工具调用
 */
@Slf4j
@Component
public class ToolCalling {

    private final VolunteerService volunteerService;

    public ToolCalling(VolunteerService volunteerService) {
        this.volunteerService = volunteerService;
    }

    @Tool(description = "根据条件推荐志愿")
    public PageResult<SchoolVolunteer> getVolunteer(@ToolParam(description = "推荐志愿使用的条件") VolunteerQuery volunteerQuery) {
        log.error("正在调用志愿查询工具");
        return volunteerService.showVolunteer(volunteerQuery.toVolunteerPageDTO());
    }

    @Tool(description = "帮助用户创建志愿表")
    public Integer createVolunteerTable(@ToolParam(description = "志愿表名称") String tableName, ToolContext toolContext) {
        log.error("正在调用志愿表创建工具");
        // 工具运行在 boundedElastic 线程，ThreadLocal 已失效，从 ToolContext 中恢复 userId
        ThreadLocalUtil.setCurrentId((Long) toolContext.getContext().get("userId"));
        VolunteerTable volunteerTable = new VolunteerTable();
        volunteerTable.setTableName(tableName);
        volunteerService.createVolunteerTable(volunteerTable);
        return volunteerTable.getTableId();
    }

    @Tool(description = "在志愿表中添加志愿")
    public void addVolunteer(@ToolParam() AddVolunteer addVolunteer, ToolContext toolContext) {
        log.error("正在调用志愿添加工具");
        ThreadLocalUtil.setCurrentId((Long) toolContext.getContext().get("userId"));
        volunteerService.addVolunteer(addVolunteer.toAddVolunteerDTO());
    }

}
