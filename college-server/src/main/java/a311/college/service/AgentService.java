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

}
