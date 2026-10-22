package a311.college.result;

import a311.college.enumeration.ProvinceEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * 用户登录后返回的登录结果
 * <p>
 * 包含双令牌（访问令牌 + 刷新令牌）与用户基本信息。
 * 出于安全考虑，不返回用户密码（无论明文还是密文）。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录信息返回")
public class LoginResult implements Serializable {

    @Schema(description = "访问令牌（JWT，有效期较短）")
    private String accessToken;

    @Schema(description = "刷新令牌（存储在Redis，有效期较长，用于换取新的访问令牌）")
    private String refreshToken;

    @Schema(description = "用户头像")
    private String head;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户手机")
    private String phone;

    @Schema(description = "用户毕业年份")
    private Integer year;

    @Schema(description = "用户省份")
    private ProvinceEnum province;

    @Schema(description = "用户选科")
    private String subjects;

    @Schema(description = "用户成绩")
    private Integer grade;

    @Schema(description = "用户位次")
    private Integer ranking;

}
