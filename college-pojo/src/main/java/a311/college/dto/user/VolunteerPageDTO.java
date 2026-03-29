package a311.college.dto.user;

import a311.college.enumeration.ProvinceEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户志愿查询DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户志愿查询DTO")
public class VolunteerPageDTO {

    @Schema(description = "志愿表id")
    private Integer tableId;

    @Schema(description = "用户所在省份")
    private ProvinceEnum province;

    @Schema(description = "首选科目")
    private String firstChoice;

    @Schema(description = "用户成绩")
    private Integer grade;

    @Schema(description = "用户位次")
    private Integer ranking;

    @Schema(description = "查询方式（1为按学校、2为按专业）")
    private Integer category;

    @Schema(description = "学校类型过滤（0=保底、1=稳妥、2=冲刺，null=不过滤）")
    private Integer schoolType;

    @Schema(description = "起始页码")
    private Integer page = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

}
