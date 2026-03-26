package a311.college.controller.major;

import a311.college.agent.AgentMessageVO;
import a311.college.constant.API.APIConstant;
import a311.college.dto.major.MajorDTO;
import a311.college.dto.query.major.*;
import a311.college.dto.query.school.CommentDTO;
import a311.college.dto.school.AddCommentDTO;
import a311.college.entity.major.ProfessionalClass;
import a311.college.dto.ai.MajorAIRequestDTO;
import a311.college.entity.major.Major;
import a311.college.entity.major.SubjectCategory;
import a311.college.entity.school.School;
import a311.college.result.PageResult;
import a311.college.result.Result;
import a311.college.service.DouBaoService;
import a311.college.service.MajorService;
import a311.college.thread.ThreadLocalUtil;
import a311.college.vo.major.DetailMajorVO;
import a311.college.vo.school.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 专业Controller
 */
@Slf4j
@RestController
@RequestMapping("/majors")
@Tag(name = APIConstant.MAJOR_SERVICE)
public class MajorController {

    private final MajorService majorService;

    private final DouBaoService douBaoService;

    @Autowired
    public MajorController(MajorService majorService, DouBaoService douBaoService) {
        this.majorService = majorService;
        this.douBaoService = douBaoService;
    }

    /**
     * 查询学科门类
     *
     * @param subjectCategoryQueryDTO 学科门类查询DTO
     * @return Result<List<SubjectCategory>>
     */
    @PostMapping("/subject")
    @Operation(summary = "学科门类查询")
    public Result<List<SubjectCategory>> subjectCategoryQuery(@RequestBody SubjectCategoryQueryDTO subjectCategoryQueryDTO) {
        log.info("查询'{}'下的学科门类", subjectCategoryQueryDTO.getAcademicLevelId());
        List<SubjectCategory> subjectCategoryList = majorService.getSubjectCategory(subjectCategoryQueryDTO);
        return Result.success(subjectCategoryList);
    }

    /**
     * 专业类别查询
     *
     * @param professionalClassQueryDTO 专业类别查询DTO
     * @return Result<List<ProfessionalClass>>
     */
    @PostMapping("/professional")
    @Operation(summary = "专业类别查询")
    public Result<List<ProfessionalClass>> professionalClassQuery(@RequestBody ProfessionalClassQueryDTO professionalClassQueryDTO) {
        log.info("查询'{}'学科门类下的专业类别", professionalClassQueryDTO.getSubjectCategoryId());
        List<ProfessionalClass> professionalClassList = majorService.getProfessionalClass(professionalClassQueryDTO);
        return Result.success(professionalClassList);
    }

    /**
     * 专业分页查询
     *
     * @param majorPageQueryDTO 专业分页查询DTO
     * @return Result<PageResult < Major>>
     */
    @PostMapping("/page")
    @Operation(summary = "专业分页查询")
    public Result<PageResult<Major>> majorPageQuery(@RequestBody MajorPageQueryDTO majorPageQueryDTO) {
        log.info("专业分页查询...");
        return Result.success(majorService.majorPageQuery(majorPageQueryDTO));
    }

    /**
     * 请求AI获取专业信息
     *
     * @return Result<AgentMessageVO>
     */
    @PostMapping("/information")
    @Operation(summary = "请求AI获取专业信息")
    public Result<AgentMessageVO> majorAIRequest(@RequestBody MajorAIRequestDTO majorAIRequestDTO) {
        log.info("正在请求'{}'专业的信息", majorAIRequestDTO.getMajorId());
        AgentMessageVO agentMessageVO = douBaoService.majorInformation(majorAIRequestDTO);
        return Result.success(agentMessageVO);
    }

    /**
     * 查询专业具体信息
     *
     * @param majorDTO 专业DTO
     * @return Result<DetailMajorVO>
     */
    @PostMapping("/detail")
    @Operation(summary = "查询专业具体信息")
    public Result<DetailMajorVO> getDetailMajor(@RequestBody MajorDTO majorDTO) {
        log.info("正在查询'{}'专业的详细信息", majorDTO.getMajorId());
        majorDTO.setUserId(ThreadLocalUtil.getCurrentId());
        DetailMajorVO detailMajorVO = majorService.getDetailMajor(majorDTO);
        return Result.success(detailMajorVO);
    }

    /**
     * 用户收藏专业
     *
     * @param majorDTO 专业DTO
     */
    @PostMapping("/addMajor")
    @Operation(summary = "用户收藏专业")
    public Result<Void> addFavoriteMajor(@RequestBody MajorDTO majorDTO) {
        log.info("用户'{}'收藏了'{}'专业", ThreadLocalUtil.getCurrentId(), majorDTO.getMajorId());
        majorService.addFavoriteMajor(majorDTO);
        return Result.success();
    }

    /**
     * 用户删除收藏
     *
     * @param majorDTO 专业DTO
     */
    @PostMapping("/deleteFavorite")
    @Operation(summary = "用户删除收藏专业")
    public Result<Void> deleteFavoriteMajor(@RequestBody MajorDTO majorDTO) {
        log.info("用户'{}'删除收藏'{}'专业", ThreadLocalUtil.getCurrentId(), majorDTO.getMajorId());
        majorService.deleteFavoriteMajor(majorDTO);
        return Result.success();
    }

    /**
     * 查询开设某专业的学校
     *
     * @param majorSchoolPageQueryDTO 开设某专业学校分页查询DTO
     * @return Result<List<School>>
     */
    @PostMapping("/schools")
    @Operation(summary = "查询专业开设院校")
    public Result<PageResult<School>> pageQuerySchool(@RequestBody MajorSchoolPageQueryDTO majorSchoolPageQueryDTO) {
        log.info("查询开设某专业的学校");
        return Result.success(majorService.querySchools(majorSchoolPageQueryDTO));
    }

    /**
     * 用户评价专业
     *
     * @param addCommentDTO 用户评价DTO
     * @return Result<Void>
     */
    @PostMapping("/comment")
    @Operation(summary = "用户评价专业")
    public Result<Void> comment(@RequestBody AddCommentDTO addCommentDTO) {
        log.info("用户'{}'发表对于'{}'专业的评论", ThreadLocalUtil.getCurrentId(), addCommentDTO.getMajorId());
        majorService.addMajorComment(addCommentDTO);
        return Result.success();
    }


    /**
     * 分页展示专业评论区
     *
     * @param commentDTO 评论区分页查询DTO
     * @return Result<PageResult<CommentVO>>
     */
    @PostMapping("/showComment")
    @Operation(summary = "分页展示专业评论区")
    public Result<List<CommentVO>> pageQuerySchoolComment(@RequestBody CommentDTO commentDTO) {
        log.info("查看专业'{}'的评价", commentDTO.getMajorId());
        return Result.success(majorService.showComment(commentDTO));
    }

}
