package a311.college.dto.login;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 刷新令牌 DTO
 * <p>
 * 客户端在访问令牌过期后，携带刷新令牌请求新的访问令牌。
 * </p>
 */
@Data
@Schema(description = "刷新令牌DTO")
public class RefreshTokenDTO implements Serializable {

    @Schema(description = "刷新令牌")
    private String refreshToken;

}
