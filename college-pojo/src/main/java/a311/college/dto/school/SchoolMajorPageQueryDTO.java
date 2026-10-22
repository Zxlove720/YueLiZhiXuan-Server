package a311.college.dto.school;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 大学专业分页查询DTO
 */
@Data
public class SchoolMajorPageQueryDTO {

    @Schema(description = "大学id")
    private Integer schoolId;

    @Schema(description = "首选科目")
    private String firstChoice;

    @Schema(description = "查询页码")
    private Integer page;

    @Schema(description = "每页大小")
    private Integer pageSize;

    /** 服务端钳制分页参数：页码 ≥ 1、每页 1~100，防止超大 LIMIT 拖垮数据库 */
    public void setPage(Integer page) {
        this.page = (page == null || page < 1) ? 1 : page;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), 100);
    }
}
