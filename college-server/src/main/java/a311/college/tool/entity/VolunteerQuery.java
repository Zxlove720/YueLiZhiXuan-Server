package a311.college.tool.entity;

import a311.college.dto.user.VolunteerPageDTO;
import a311.college.enumeration.ProvinceEnum;
import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 志愿推荐工具调用查询参数（仅包含 AI 需感知的字段）
 */
@Data
public class VolunteerQuery {

    @ToolParam(description = "用户所在省份")
    private ProvinceEnum province;

    @ToolParam(required = false, description = "用户首选科目")
    private String firstChoice;

    @ToolParam(description = "用户高考成绩")
    private Integer grade;

    @ToolParam(description = "用户高考位次")
    private Integer ranking;

    @ToolParam(required = false, description = "用户期望查询方式（1为按学校、2为按专业）")
    private Integer category;

    @ToolParam(required = false, description = "用户期望填志愿策略（0=保底、1=稳妥、2=冲刺，null=不过滤）")
    private Integer schoolType;

    public VolunteerPageDTO toVolunteerPageDTO() {
        VolunteerPageDTO dto = new VolunteerPageDTO();
        dto.setProvince(this.province);
        dto.setFirstChoice(this.firstChoice);
        dto.setGrade(this.grade);
        dto.setRanking(this.ranking);
        dto.setCategory(this.category);
        dto.setSchoolType(this.schoolType);
        dto.setPage(1);
        dto.setPageSize(10);
        return dto;
    }

}
