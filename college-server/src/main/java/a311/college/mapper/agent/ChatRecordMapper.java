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
    @Select("SELECT conversation_id FROM tb_agent_chat_record WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<ChatRecord> findRecordByUserId(@Param("userId") Long userId);


    @Insert("insert into tb_agent_chat_record(user_id, conversation_id, title, create_time) " +
            "set user_id = #{userId}, conversation_id = #{conversationId}, title = #{title}, create_time = #{createTime}")
    void saveRecord(ChatRecord record);

}
