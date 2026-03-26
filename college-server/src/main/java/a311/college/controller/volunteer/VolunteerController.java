package a311.college.controller.volunteer;

import a311.college.agent.AgentMessageVO;
import a311.college.constant.API.APIConstant;
import a311.college.dto.user.VolunteerPageDTO;
import a311.college.dto.volunteer.AddVolunteerDTO;
import a311.college.dto.volunteer.AnalyseDTO;
import a311.college.entity.volunteer.Volunteer;
import a311.college.entity.volunteer.VolunteerTable;
import a311.college.result.PageResult;
import a311.college.result.Result;
import a311.college.service.VolunteerService;
import a311.college.thread.ThreadLocalUtil;
import a311.college.vo.ai.UserAIMessageVO;
import a311.college.vo.volunteer.SchoolVolunteer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 志愿控制器
 */
@Slf4j
@RestController
@RequestMapping("/volunteers")
@Tag(name = APIConstant.VOLUNTEER_SERVICE)
public class VolunteerController {

    private final VolunteerService volunteerService;

    @Autowired
    public VolunteerController(VolunteerService volunteerService) {
        this.volunteerService = volunteerService;
    }

    /**
     * 志愿展示
     *
     * @param volunteerPageDTO 志愿分页查询DTO
     * @return Result<PageResult<SchoolVolunteer>>
     */
    @PostMapping("/showVolunteer")
    @Operation(summary = "展示志愿")
    public Result<PageResult<SchoolVolunteer>> showVolunteer(@RequestBody VolunteerPageDTO volunteerPageDTO) {
        return Result.success(volunteerService.showVolunteer(volunteerPageDTO));
    }

    /**
     * 创建志愿表
     *
     * @param volunteerTable 志愿表实体类
     * @return Result<Void>
     */
    @PostMapping("/create")
    @Operation(summary = "创建志愿表")
    public Result<Void> createVolunteerTable(@RequestBody VolunteerTable volunteerTable) {
        log.info("创建志愿表");
        volunteerService.createVolunteerTable(volunteerTable);
        return Result.success();
    }

    /**
     * 删除志愿表
     *
     * @param volunteerTable 志愿表实体对象
     * @return Result<Void>
     */
    @PostMapping("/delete")
    @Operation(summary = "删除志愿表")
    public Result<Void> deleteVolunteerTable(@RequestBody VolunteerTable volunteerTable) {
        log.info("删除志愿表");
        volunteerService.deleteVolunteerTable(volunteerTable.getTableId());
        return Result.success();
    }

    /**
     * 清空志愿表
     *
     * @param volunteerTable 志愿表
     */
    @PostMapping("/clear")
    @Operation(summary = "清空志愿表")
    public Result<Void> clearVolunteerTable(@RequestBody VolunteerTable volunteerTable) {
        log.info("清空志愿表");
        volunteerService.clearVolunteerTable(volunteerTable.getTableId());
        return Result.success();
    }

    /**
     * 修改志愿表名字
     *
     * @param volunteerTable 志愿表实体对象
     * @return Result<Void>
     */
    @PostMapping("/update")
    @Operation(summary = "修改志愿表名字")
    public Result<Void> updateVolunteerTableName(@RequestBody VolunteerTable volunteerTable) {
        log.info("修改志愿表名字");
        volunteerService.updateVolunteerTableName(volunteerTable);
        return Result.success();
    }

    /**
     * 查询用户创建的志愿表
     *
     * @return Result<List<Volunteer>>
     */
    @PostMapping("/showTables")
    @Operation(summary = "查询用户创建的志愿表")
    public Result<List<VolunteerTable>> showVolunteerTable() {
        log.info("查询用户创建的志愿表");
        return Result.success(volunteerService.showVolunteerTable(ThreadLocalUtil.getCurrentId()));
    }

    /**
     * 添加志愿
     *
     * @param addVolunteerDTO 添加志愿DTO
     */
    @PostMapping("/addVolunteer")
    @Operation(summary = "添加志愿")
    public Result<Void> addVolunteer(@RequestBody AddVolunteerDTO addVolunteerDTO) {
        volunteerService.addVolunteer(addVolunteerDTO);
        return Result.success();
    }

    /**
     * 删除志愿
     *
     * @param volunteer 志愿
     */
    @PostMapping("/deleteVolunteer")
    @Operation(summary = "删除志愿")
    public Result<Void> deleteVolunteer(@RequestBody Volunteer volunteer) {
        volunteerService.deleteVolunteer(volunteer);
        return Result.success();
    }

    /**
     * 删除展示志愿
     *
     * @param volunteer 志愿
     */
    @PostMapping("/deleteShowVolunteer")
    @Operation(summary = "删除展示志愿")
    public Result<Void> deleteShowVolunteer(@RequestBody Volunteer volunteer) {
        volunteerService.deleteShowVolunteer(volunteer);
        return Result.success();
    }

    /**
     * 查询志愿表
     *
     * @param volunteerTable 志愿表
     * @return Result<List<Volunteer>>
     */
    @PostMapping("/volunteers")
    @Operation(summary = "查询志愿表")
    public Result<List<Volunteer>> selectVolunteer(@RequestBody VolunteerTable volunteerTable) {
        log.info("查询志愿表");
        return Result.success(volunteerService.selectVolunteer(volunteerTable.getTableId()));
    }

    /**
     * AI分析志愿表
     *
     * @param analyseDTO 分析DTO
     * @return Result<AgentMessageVO>
     */
    @PostMapping("/analyse")
    @Operation(summary = "AI分析志愿表")
    public Result<AgentMessageVO> analyseVolunteerTable(@RequestBody AnalyseDTO analyseDTO) {
        log.info("志愿表分析");
        return Result.success(volunteerService.analyseVolunteerTable(analyseDTO));
    }
}
