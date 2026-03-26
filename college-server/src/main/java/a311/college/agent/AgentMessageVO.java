package a311.college.agent;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.Message;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "agent响应VO")
public class AgentMessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "对话角色")
    private String role;

    @Schema(description = "对话内容")
    private String content;

    public AgentMessageVO(Message message) {
        this.role = switch (message.getMessageType()) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            case TOOL -> "tool";
            default -> "unknown";
        };
        this.content = message.getText();
    }

}
