package a311.college.dto.query.major;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 专业查询DTO
 */
@Data
@Schema(description = "专业查询DTO")
public class MajorPageQueryDTO {

    @Schema(description = "专业所属培养层次id")
    private Integer academicLevelId;

    @Schema(description = "专业所属学科门类id")
    private Integer subjectCategoryId;

    @Schema(description = "专业所属专业类别id")
    private Integer professionalClassId;

    @Schema(description = "专业名称")
    private String majorName;

    @Schema(description = "查询页码")
    private Integer page = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 15;

    /** 服务端钳制分页参数：页码 ≥ 1、每页 1~100，防止超大 LIMIT 拖垮数据库 */
    public void setPage(Integer page) {
        this.page = (page == null || page < 1) ? 1 : page;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize == null ? 15 : Math.min(Math.max(pageSize, 1), 100);
    }
}
