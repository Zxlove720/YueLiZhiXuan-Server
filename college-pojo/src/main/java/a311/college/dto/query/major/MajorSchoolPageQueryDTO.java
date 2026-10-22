package a311.college.dto.query.major;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 开设某专业学校分页查询DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "开设某专业学校分页查询DTO")
public class MajorSchoolPageQueryDTO {

    @Schema(description = "专业名称")
    private String majorName;

    @Schema(description = "省份名")
    private String province;

    @Schema(description = "大学等级（类型）")
    private List<String> rankList;

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
