package a311.college.dto.query.school;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 院校分数线查询DTO
 */
@Data
@Schema(description = "院校分数线查询DTO")
public class YearScoreQueryDTO {

    @Schema(description = "学校id")
    private Integer schoolId;

    @Schema(description = "招生省份")
    private String province;

    @Schema(description = "首选科目")
    private String firstChoice;

    @Schema(description = "招生年份")
    private Integer year;

    @Schema(description = "起始页码")
    private Integer page = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    /** 服务端钳制分页参数：页码 ≥ 1、每页 1~100，防止超大 LIMIT 拖垮数据库 */
    public void setPage(Integer page) {
        this.page = (page == null || page < 1) ? 1 : page;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), 100);
    }
}
