package a311.college.tool;

import a311.college.result.PageResult;
import a311.college.service.VolunteerService;
import a311.college.tool.entity.VolunteerQuery;
import a311.college.vo.volunteer.SchoolVolunteer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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
        log.error("工具调用成功");
        return volunteerService.showVolunteer(volunteerQuery.toVolunteerPageDTO());
    }

}
