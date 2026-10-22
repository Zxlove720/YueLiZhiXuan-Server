package a311.college.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页查询DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页查询DTO")
public class PageQueryDTO {

    @Schema(description = "查询页码")
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
