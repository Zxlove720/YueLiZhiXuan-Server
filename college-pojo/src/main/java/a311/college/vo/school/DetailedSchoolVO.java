package a311.college.vo.school;

import a311.college.vo.major.MajorSimpleVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;


/**
 * 大学详细信息VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "大学信息VO")
public class DetailedSchoolVO implements Serializable {

    @Schema(description = "大学id")
    private Integer schoolId;

    @Schema(description = "大学校徽")
    private String schoolHead;

    @Schema(description = "大学名称")
    private String schoolName;

    @Schema(description = "所在省份")
    private String schoolProvince;

    @Schema(description = "学校地址")
    private String schoolAddress;

    @Schema(description = "大学等级标签列表")
    private String rankList;

    @Schema(description = "学校综合得分")
    private Integer score;

    @Schema(description = "软科2025中国大学排名（主榜），501=500+，null=未上榜")
    private Integer ruankeRank;

    @Schema(description = "排名项目")
    private String rankItem;

    @Schema(description = "排名信息")
    private String rankInfo;

    @Schema(description = "大学具体排名")
    private Map<String, String> rank;

    @Schema(description = "大学官方网站")
    private String website;

    @Schema(description = "大学官方电话")
    private String phoneNumber;

    @Schema(description = "大学官方邮箱")
    private String email;

    @Schema(description = "校园风光")
    private List<String> images;

    @Schema(description = "开设专业")
    private List<MajorSimpleVO> majors;

    @Schema(description = "校园配置")
    private Map<String, Integer> equipment;

    @Schema(description = "是否被收藏")
    private Boolean favorite;

}