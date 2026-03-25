package a311.college.config.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class AgentChatMemory implements ChatMemoryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AgentChatMemory(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @NotNull
    @Override
    public List<String> findConversationIds() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT conversation_id FROM tb_agent_chat_memory",
                String.class
        );
    }

    @NotNull
    @Override
    public List<Message> findByConversationId(@NotNull String conversationId) {
        return jdbcTemplate.query(
                "SELECT content FROM tb_agent_chat_memory WHERE conversation_id = ? ORDER BY `timestamp`, id",
                (rs, rowNum) -> deserialize(rs.getString("content")),
                conversationId
        );
    }

    @Override
    @Transactional
    public void saveAll(@NotNull String conversationId, List<Message> messages) {
        deleteByConversationId(conversationId);
        Timestamp now = Timestamp.from(Instant.now());
        for (Message message : messages) {
            jdbcTemplate.update(
                    "INSERT INTO tb_agent_chat_memory (conversation_id, content, `timestamp`) VALUES (?, ?, ?)",
                    conversationId, serialize(message), now
            );
        }
    }

    @Override
    public void deleteByConversationId(@NotNull String conversationId) {
        jdbcTemplate.update(
                "DELETE FROM tb_agent_chat_memory WHERE conversation_id = ?",
                conversationId
        );
    }

    private String serialize(Message message) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "type", message.getMessageType().name(),
                    "content", message.getText()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize message", e);
        }
    }

    private Message deserialize(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String type = node.get("type").asText();
            String content = node.get("content").asText();
            return toMessage(MessageType.valueOf(type), content);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize message", e);
        }
    }

    private Message toMessage(MessageType type, String content) {
        return switch (type) {
            case USER -> new UserMessage(content);
            case ASSISTANT -> new AssistantMessage(content);
            case SYSTEM -> new SystemMessage(content);
            default -> throw new IllegalArgumentException("Unsupported message type: " + type);
        };
    }
}
