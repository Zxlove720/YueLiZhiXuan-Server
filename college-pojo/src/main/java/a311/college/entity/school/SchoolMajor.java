package a311.college.entity.school;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 学校专业类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "大学专业类")
public class SchoolMajor implements Serializable {

    @Schema(description = "专业名称")
    private String majorName;

    @Schema(description = "专业id")
    private Integer majorId;

    @Schema(description = "所属批次")
    private String batchName;

    @Schema(description = "首选科目")
    private String firstChoice;

    @Schema(description = "次选科目")
    private String otherChoice;

    @Schema(description = "特殊要求")
    private String special;

    @Schema(description = "最低分数")
    private Integer minScore;

    @Schema(description = "最低位次")
    private Integer minRanking;

}
