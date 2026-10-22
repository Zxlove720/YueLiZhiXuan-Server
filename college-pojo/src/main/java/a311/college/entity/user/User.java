package a311.college.entity.user;

import a311.college.enumeration.ProvinceEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 用户类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "用户类")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    /**
     * 用户密码散列
     * 使用 @JsonIgnore 保证任何返回 User 的接口都不会把密码散列序列化给前端
     */
    @JsonIgnore
    @Schema(description = "用户密码", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @Schema(description = "用户手机号")
    private String phone;

    @Schema(description = "用户头像")
    private String head;

    @Schema(description = "用户毕业年份")
    private Integer year;

    @Schema(description = "用户所在省份")
    private ProvinceEnum province;

    @Schema(description = "首选科目")
    private String firstChoice;

    @Schema(description = "用户选科")
    private String subjects;

    @Schema(description = "用户成绩")
    private Integer grade;

    @Schema(description = "用户位次")
    private Integer ranking;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;
}
