package a311.college.vo.school;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 专业预测返回结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "专业预测结果")
public class SchoolMajorVO {

    @Schema(description = "专业名")
    private String majorName;

    @Schema(description = "专业id")
    private Integer majorId;

    @Schema(description = "首选科目")
    private String firstChoice;

    @Schema(description = "次选科目")
    private String otherChoice;

    @Schema(description = "特殊需求")
    private String special;

    @Schema(description = "难度分类")
    private Integer category;

    @Schema(description = "最低分数")
    private Integer minScore;

    @Schema(description = "最低位次")
    private Integer minRanking;

}
