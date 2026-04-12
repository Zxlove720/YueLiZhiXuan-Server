package a311.college.vo.volunteer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面向 LLM 的志愿推荐精简摘要
 * <p>
 * 仅包含 LLM 向用户展示推荐时所需的最少字段，
 * 完整数据通过 index 在 Redis 中查找。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerSummary {

    /** 全局唯一序号，LLM 用此序号引用具体志愿 */
    private Integer index;

    private String schoolName;

    private String majorName;

    /** 冲稳保：0=保底 1=稳妥 2=冲刺 */
    private Integer category;

    /** 最近一年最低分 */
    private Integer latestMinScore;

    /** 最低分与用户分差（正数=用户更高） */
    private Integer scoreThanMe;

    /** 最低位次与用户位次差（正数=用户位次更好） */
    private Integer rankingThanMe;

    private String categoryLabel() {
        if (category == null) return "未知";
        return switch (category) {
            case 0 -> "保底";
            case 1 -> "稳妥";
            case 2 -> "冲刺";
            default -> "未知";
        };
    }

    @Override
    public String toString() {
        return String.format("序号%d: %s - %s [%s] 最低分%d(差值%+d分) 位次差%+d",
                index, schoolName, majorName, categoryLabel(),
                latestMinScore, scoreThanMe, rankingThanMe);
    }
}
