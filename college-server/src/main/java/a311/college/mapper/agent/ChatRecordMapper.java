package a311.college.mapper.agent;

import a311.college.entity.agent.ChatRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatRecordMapper {

    /**
     * 根据用户id获取所有会话记录
     *
     * @param userId 用户id
     * @return List<ChatRecord> 会话记录
     */
    @Select("SELECT conversation_id, title, create_time FROM tb_agent_chat_record WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<ChatRecord> findRecordByUserId(@Param("userId") Long userId);


    /**
     * 根据会话id获取当前会话记录
     *
     * @param conversationId 会话id
     * @return List<ChatRecord> 会话记录
     */
    @Select("SELECT conversation_id FROM tb_agent_chat_record WHERE conversation_id = #{conversationId}")
    ChatRecord findRecordByConversationId(@Param("conversationId") String conversationId);

    /**
     * 按会话id + 用户id 联查，用于会话归属校验（防止越权读写他人会话）
     *
     * @param conversationId 会话id
     * @param userId         当前登录用户id
     * @return 会话记录，null 表示该会话不属于该用户
     */
    @Select("SELECT conversation_id, user_id FROM tb_agent_chat_record WHERE conversation_id = #{conversationId} AND user_id = #{userId}")
    ChatRecord findOwnedRecord(@Param("conversationId") String conversationId, @Param("userId") Long userId);

    /**
     * 新增会话记录
     *
     * @param record 会话记录
     */
    @Insert("insert into tb_agent_chat_record(user_id, conversation_id, title, create_time) " +
            "values (#{userId}, #{conversationId}, #{title}, #{createTime})")
    void saveRecord(ChatRecord record);

}
