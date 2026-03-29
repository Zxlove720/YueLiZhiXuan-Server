package a311.college.service.impl;

import a311.college.agent.AgentMessageVO;
import a311.college.dto.user.VolunteerPageDTO;
import a311.college.dto.volunteer.AddVolunteerDTO;
import a311.college.dto.volunteer.AnalyseDTO;
import a311.college.entity.volunteer.Volunteer;
import a311.college.entity.volunteer.VolunteerTable;
import a311.college.exception.ReAdditionException;
import a311.college.exception.volunteer.VolunteerException;
import a311.college.mapper.volunteer.VolunteerMapper;
import a311.college.result.PageResult;
import a311.college.service.AgentService;
import a311.college.service.VolunteerService;
import a311.college.thread.ThreadLocalUtil;
import a311.college.vo.volunteer.SchoolVolunteer;
import a311.college.vo.volunteer.ScoreLine;
import a311.college.vo.volunteer.VolunteerVO;
import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VolunteerServiceImpl implements VolunteerService {

    private final VolunteerMapper volunteerMapper;

    private final AgentService douBaoService;

    public VolunteerServiceImpl(VolunteerMapper volunteerMapper, AgentService douBaoService) {
        this.volunteerMapper = volunteerMapper;
        this.douBaoService = douBaoService;
    }

    /**
     * 志愿展示
     *
     * @param volunteerPageDTO 用户志愿分页查询DTO
     * @return PageResult<SchoolVolunteer>
     */
    @Override
    public PageResult<SchoolVolunteer> showVolunteer(VolunteerPageDTO volunteerPageDTO) {
        // 第一步：只查总数（轻量 COUNT 查询）
        long total = volunteerMapper.countVolunteerSchools(volunteerPageDTO);
        if (total == 0) {
            return new PageResult<>(0L, Collections.emptyList());
        }
        // 第二步：只查当前页的学校ID（LIMIT/OFFSET 分页，返回极少数据）
        int offset = (volunteerPageDTO.getPage() - 1) * volunteerPageDTO.getPageSize();
        List<Integer> schoolIds = volunteerMapper.selectPagedSchoolIds(volunteerPageDTO, offset);
        if (schoolIds.isEmpty()) {
            return new PageResult<>(total, Collections.emptyList());
        }
        // 第三步：只查这几所学校的完整数据（IN 查询，数据量极小）
        List<SchoolVolunteer> schoolVolunteerList = volunteerMapper.selectVolunteersBySchoolIds(schoolIds, volunteerPageDTO);

        int userRanking = volunteerPageDTO.getRanking();
        int userGrade = volunteerPageDTO.getGrade();
        Integer schoolType = volunteerPageDTO.getSchoolType();
        Set<Integer> addedMajorIds = new HashSet<>(volunteerMapper.selectAddedMajorIds(volunteerPageDTO.getTableId()));

        // 第四步：处理当前页数据（只处理一页的学校，非常快）
        for (SchoolVolunteer school : schoolVolunteerList) {
            List<VolunteerVO> distinctList = school.getVolunteerVOList().stream()
                    .collect(Collectors.toMap(
                            VolunteerVO::getMajorName,
                            Function.identity(),
                            (existing, replacement) -> replacement,
                            LinkedHashMap::new
                    ))
                    .values().stream().toList();
            school.setVolunteerVOList(distinctList);
            for (VolunteerVO volunteerVO : distinctList) {
                Integer minRanking = volunteerVO.getScoreLineList().get(0).getMinRanking();
                volunteerVO.setCategory(calculateCategory(minRanking, userRanking));
                volunteerVO.setIsAdd(addedMajorIds.contains(volunteerVO.getMajorId()));
                for (ScoreLine scoreLine : volunteerVO.getScoreLineList()) {
                    scoreLine.setScoreThanMe(userGrade - scoreLine.getMinScore());
                    scoreLine.setRankingThanMe(userRanking - scoreLine.getMinRanking());
                }
            }
        }
        // 第五步：schoolType 精确筛选（SQL 已按范围过滤，此处做最终确认）
        List<SchoolVolunteer> resultList;
        if (schoolType != null) {
            resultList = new ArrayList<>();
            for (SchoolVolunteer school : schoolVolunteerList) {
                List<VolunteerVO> matchedMajors = school.getVolunteerVOList().stream()
                        .filter(vo -> schoolType.equals(vo.getCategory()))
                        .collect(Collectors.toList());
                if (!matchedMajors.isEmpty()) {
                    school.setVolunteerVOList(matchedMajors);
                    resultList.add(school);
                }
            }
        } else {
            resultList = schoolVolunteerList;
        }
        return new PageResult<>(total, resultList);
    }

    /**
     * 创建志愿表
     *
     * @param volunteerTable 志愿表
     */
    @Override
    public void createVolunteerTable(VolunteerTable volunteerTable) {
        String tableName = volunteerTable.getTableName();
        long userId = ThreadLocalUtil.getCurrentId();
        if (volunteerMapper.checkVolunteerTable(tableName, userId) != 0) {
            log.info("重名的志愿表");
            throw new VolunteerException("重名的志愿表");
        }
        volunteerTable.setUserId(ThreadLocalUtil.getCurrentId());
        volunteerTable.setCreateTime(LocalDateTime.now());
        volunteerMapper.createVolunteerTable(volunteerTable);
    }

    /**
     * 删除志愿表
     *
     * @param tableId 志愿表id
     */
    @Override
    public void deleteVolunteerTable(int tableId) {
        volunteerMapper.deleteVolunteerTable(tableId);
        // 删除志愿表的时候，表中的所有志愿都要一起删除
        volunteerMapper.clearVolunteerTable(tableId);
    }

    /**
     * 清空志愿表
     *
     * @param tableId 志愿表id
     */
    @Override
    public void clearVolunteerTable(Integer tableId) {
        volunteerMapper.clearVolunteerTable(tableId);
    }


    /**
     * 更新志愿表
     *
     * @param volunteerTable 志愿表
     */
    @Override
    public void updateVolunteerTableName(VolunteerTable volunteerTable) {
        volunteerMapper.updateVolunteerTableName(volunteerTable);
    }

    /**
     * 查询用户志愿表
     *
     * @param userId 用户id
     * @return List<VolunteerTable>
     */
    @Override
    public List<VolunteerTable> showVolunteerTable(long userId) {
        List<VolunteerTable> volunteerTables = volunteerMapper.selectTables(userId);
        for (VolunteerTable volunteerTable : volunteerTables) {
            volunteerTable.setCount(volunteerMapper.getTableCount(volunteerTable.getTableId()));
        }
        return volunteerTables;
    }

    /**
     * 计算专业分类逻辑
     *
     * @param minRanking 专业历年最低分
     * @param ranking    用户分数
     * @return 0保底 1稳妥 2冲刺
     */
    private int calculateCategory(int minRanking, int ranking) {
        if (minRanking >= ranking - 3000 && minRanking <= ranking + 5000) {
            // 该专业为稳
            return 1;
        } else if (minRanking < ranking - 3000 && minRanking >= ranking - 7500) {
            // 该专业为冲
            return 2;
        } else if (minRanking > ranking + 5000) {
            // 该专业为保
            return 0;
        }
        return -1;
    }

    /**
     * 添加志愿
     *
     * @param addVolunteerDTO 添加志愿
     */
    @Override
    public void addVolunteer(AddVolunteerDTO addVolunteerDTO) {
        // 1.判断当前志愿表是否已被填满
        Integer count = volunteerMapper.getSum(addVolunteerDTO.getTableId());
        if (count == null) {
            count = 0;
        }
        if (count >= 96) {
            // 1.2此时该志愿表已经填满，直接返回
            log.error("该志愿表已满");
            throw new VolunteerException("该志愿表已满，请选择其他志愿表");
        }
        // 2.准备添加志愿到志愿表中
        int majorId = addVolunteerDTO.getMajorId();
        Long userId = ThreadLocalUtil.getCurrentId();
        // 2.1判断该志愿是否已经被添加到志愿表中
        if (volunteerMapper.checkVolunteer(majorId, addVolunteerDTO.getTableId()) != 0) {
            // 2.2该志愿已经被添加过了，不允许添加
            throw new ReAdditionException("重复添加");
        }
        // 3.封装Volunteer实体类
        Volunteer volunteer = volunteerMapper.selectSchoolMajorById(majorId);
        BeanUtil.copyProperties(addVolunteerDTO, volunteer);
        volunteer.setUserId(userId);
        volunteer.setCount(count + 1);
        // 4.添加volunteer
        volunteerMapper.addVolunteer(volunteer);
    }

    /**
     * 删除志愿
     *
     * @param volunteer 志愿
     */
    @Override
    public void deleteVolunteer(Volunteer volunteer) {
        Integer count = volunteerMapper.getVolunteerCount(volunteer.getVolunteerId());
        Integer tableId = volunteerMapper.getTableId(volunteer.getVolunteerId());
        volunteerMapper.updateCount(tableId, count);
        volunteerMapper.deleteVolunteer(volunteer);
    }

    /**
     * 删除展示志愿
     *
     * @param volunteer 志愿
     */
    @Override
    public void deleteShowVolunteer(Volunteer volunteer) {
        Integer count = volunteerMapper.getCount(volunteer.getMajorId(), volunteer.getTableId());
        volunteerMapper.updateCount(volunteer.getTableId(), count);
        volunteerMapper.deleteShowVolunteer(volunteer.getMajorId(), volunteer.getTableId());
    }

    /**
     * 查看志愿表内容
     *
     * @param tableId 志愿表id
     * @return List<Volunteer>
     */
    @Override
    public List<Volunteer> selectVolunteer(int tableId) {
        return volunteerMapper.selectVolunteers(tableId);
    }

    /**
     * AI智能分析志愿表
     *
     * @param analyseDTO 分析DTO
     * @return AgentMessageVO
     */
    @Override
    public AgentMessageVO analyseVolunteerTable(AnalyseDTO analyseDTO) {
        return douBaoService.analyseVolunteerTable(analyseDTO);
    }

}
