package a311.college.service;

import a311.college.agent.AgentMessageVO;
import a311.college.dto.user.VolunteerPageDTO;
import a311.college.dto.volunteer.AddVolunteerDTO;
import a311.college.dto.volunteer.AnalyseDTO;
import a311.college.entity.volunteer.Volunteer;
import a311.college.entity.volunteer.VolunteerTable;
import a311.college.result.PageResult;
import a311.college.vo.ai.UserAIMessageVO;
import a311.college.vo.volunteer.SchoolVolunteer;

import java.util.List;

public interface VolunteerService {

    PageResult<SchoolVolunteer> showVolunteer(VolunteerPageDTO volunteerPageDTO);

    void createVolunteerTable(VolunteerTable volunteerTable);

    void deleteVolunteerTable(int tableId);

    void clearVolunteerTable(Integer tableId);

    void updateVolunteerTableName(VolunteerTable volunteerTable);

    List<VolunteerTable> showVolunteerTable(long userId);

    void addVolunteer(AddVolunteerDTO addVolunteerDTO);

    void deleteVolunteer(Volunteer volunteer);

    void deleteShowVolunteer(Volunteer volunteer);

    List<Volunteer> selectVolunteer(int tableId);

    AgentMessageVO analyseVolunteerTable(AnalyseDTO analyseDTO);

}
