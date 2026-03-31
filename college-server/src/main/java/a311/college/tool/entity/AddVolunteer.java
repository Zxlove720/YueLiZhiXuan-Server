package a311.college.tool.entity;

import lombok.Data;
import a311.college.dto.volunteer.AddVolunteerDTO;
import org.springframework.ai.tool.annotation.ToolParam;

@Data
public class AddVolunteer {

    @ToolParam(description = "志愿表id")
    private Integer tableId;

    @ToolParam(description = "专业id")
    private Integer majorId;

    @ToolParam(description = "所属类型")
    private Integer category;

    @ToolParam(description = "招生年份")
    private Integer year;

    @ToolParam(description = "最低分数")
    private Integer minScore;

    @ToolParam(description = "最低位次")
    private Integer minRanking;

    @ToolParam(description = "最低分与用户相比")
    private Integer scoreThanMe;

    @ToolParam(description = "最低位次与用户相比")
    private Integer rankingThanMe;

    public AddVolunteerDTO toAddVolunteerDTO() {
        AddVolunteerDTO dto = new AddVolunteerDTO();
        dto.setTableId(tableId);
        dto.setMajorId(majorId);
        dto.setCategory(category);
        dto.setYear(year);
        dto.setMinScore(minScore);
        dto.setMinRanking(minRanking);
        return dto;
    }

}
