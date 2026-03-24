package a311.college.entity.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话历史记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话历史记录实体")
public class ChatRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话id
     */
    private String id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 会话id
     */
    private String conversationId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 会话创建时间
     */
    private LocalDateTime createTime;

}
