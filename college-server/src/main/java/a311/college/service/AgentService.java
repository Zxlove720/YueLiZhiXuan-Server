package a311.college.service;

import a311.college.agent.AgentMessageVO;
import a311.college.dto.ai.MajorAIRequestDTO;
import a311.college.dto.ai.SchoolAIRequestDTO;
import a311.college.dto.ai.UserAIRequestDTO;
import a311.college.dto.volunteer.AnalyseDTO;
import a311.college.entity.agent.ChatRecord;
import a311.college.vo.ai.UserAIMessageVO;

import java.util.List;

public interface AgentService {

//    UserAIMessageVO response(UserAIRequestDTO request);

    AgentMessageVO schoolInformation(SchoolAIRequestDTO schoolAIRequestDTO);

    AgentMessageVO majorInformation(MajorAIRequestDTO majorAIRequestDTO);

    AgentMessageVO analyseVolunteerTable(AnalyseDTO analyseDTO);

    void saveRecord(ChatRecord record);

    List<ChatRecord> getUserChatRecordList(Long userId);

    ChatRecord getChatRecordDetail(String conversationId);

    /**
     * 会话归属校验：按会话id + 用户id 联查
     *
     * @param conversationId 会话id
     * @param userId         用户id
     * @return 会话记录，null 表示该会话不属于该用户
     */
    ChatRecord findOwnedRecord(String conversationId, Long userId);

}
