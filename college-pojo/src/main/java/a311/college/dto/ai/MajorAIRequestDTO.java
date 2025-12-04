package a311.college.dto.ai;

import a311.college.vo.ai.MajorAIMessageVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "专业AI请求")
public class MajorAIRequestDTO {

    @Schema(description = "专业ID")
    private Integer majorId;

    @Schema(description = "请求消息")
    private MajorAIMessageVO message;

    @Schema(description = "模型温度")
    private Double temperature = 0.3;

}
