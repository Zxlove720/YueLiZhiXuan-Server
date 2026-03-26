package a311.college.controller.school;


import a311.college.agent.AgentMessageVO;
import a311.college.constant.API.APIConstant;
import a311.college.dto.ai.SchoolAIRequestDTO;
import a311.college.dto.query.school.*;
import a311.college.dto.school.*;
import a311.college.dto.user.UserSearchDTO;
import a311.college.entity.school.School;
import a311.college.service.DouBaoService;
import a311.college.entity.school.SchoolMajor;
import a311.college.result.PageResult;
import a311.college.result.Result;
import a311.college.service.SchoolService;
import a311.college.thread.ThreadLocalUtil;
import a311.college.vo.major.HotMajorVO;
import a311.college.vo.school.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 大学请求控制器
 */
@Slf4j
@RestController
@RequestMapping("/schools")
@Tag(name = APIConstant.SCHOOL_SERVICE)
public class SchoolController {

    private final SchoolService schoolService;

    private final DouBaoService douBaoService;

    @Autowired
    public SchoolController(SchoolService schoolService, DouBaoService douBaoService) {
        this.schoolService = schoolService;
        this.douBaoService = douBaoService;
    }

    /**
     * 大学信息分页查询
     *
     * @param schoolPageQueryDTO 大学分页查询DTO
     * @return Result<PageResult < School>>
     */
    @PostMapping("/page")
    @Operation(summary = "学校列表分页查询")
    public Result<PageResult<School>> schoolPageSelect(@RequestBody SchoolPageQueryDTO schoolPageQueryDTO) {
        log.info("大学分页查询...查询参数为：第{}页，每页{}条", schoolPageQueryDTO.getPage(), schoolPageQueryDTO.getPageSize());
        PageResult<School> pageResult = schoolService.pageSelect(schoolPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 学校专业分页查询
     *
     * @param schoolMajorPageQueryDTO 学校专业分页查询DTO
     * @return Result<PageResult < SchoolMajor>>
     */
    @PostMapping("/majors")
    @Operation(summary = "学校专业分页查询")
    public Result<PageResult<SchoolMajor>> schoolMajorPageSelect(@RequestBody SchoolMajorPageQueryDTO schoolMajorPageQueryDTO) {
        log.info("大学专业分页查询...查询参数为：第{}页，每页{}条", schoolMajorPageQueryDTO.getPage(), schoolMajorPageQueryDTO.getPageSize());
        PageResult<SchoolMajor> pageResult = schoolService.pageSelectMajor(schoolMajorPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据用户搜索内容进行搜索提示
     *
     * @param userSearchDTO 用户搜索DTO
     * @return Result<SearchVO>
     */
    @PostMapping("/searchList")
    @Operation(summary = "根据用户搜索内容进行搜索提示")
    public Result<SearchVO> searchList(@RequestBody UserSearchDTO userSearchDTO) {
        log.info("用户正在搜索：{}", userSearchDTO.getMessage());
        SearchVO searchVO = schoolService.searchList(userSearchDTO);
        return Result.success(searchVO);
    }

    /**
     * 用户搜索学校
     *
     * @param schoolPageQueryDTO 学校分页查询DTO
     * @return Result<PageResult<School>>
     */
    @PostMapping("/search")
    @Operation(summary = "用户搜索学校")
    public Result<PageResult<School>> search(@RequestBody SchoolPageQueryDTO schoolPageQueryDTO) {
        log.info("学校搜索...查询参数为：第{}页，每页{}条", schoolPageQueryDTO.getPage(), schoolPageQueryDTO.getPageSize());
        return Result.success(schoolService.search(schoolPageQueryDTO));
    }

    /**
     * 查询学校具体信息
     *
     * @param schoolDTO 学校DTO
     * @return DetailedSchoolVO 学校具体信息VO
     */
    @PostMapping("/detail")
    @Operation(summary = "根据学校id查询学校具体信息")
    public Result<DetailedSchoolVO> getDetailSchool(@RequestBody SchoolDTO schoolDTO) {
        DetailedSchoolVO detailedSchoolVO = schoolService.getDetailSchool(schoolDTO);
        return Result.success(detailedSchoolVO);
    }

    /**
     * 用户收藏学校
     *
     * @param schoolDTO 学校DTO
     */
    @PostMapping("/addSchool")
    @Operation(summary = "用户收藏学校")
    public Result<Void> addFavoriteSchool(@RequestBody SchoolDTO schoolDTO) {
        log.info("用户'{}'收藏了'{}'学校", ThreadLocalUtil.getCurrentId(), schoolDTO.getSchoolId());
        schoolService.addFavoriteSchool(schoolDTO);
        return Result.success();
    }

    /**
     * 用户删除收藏
     *
     * @param schoolDTO 学校DTO
     */
    @PostMapping("/deleteSchool")
    @Operation(summary = "用户删除收藏")
    public Result<Void> deleteFavoriteSchool(@RequestBody SchoolDTO schoolDTO) {
        log.info("用户'{}'删除收藏'{}'学校", ThreadLocalUtil.getCurrentId(), schoolDTO.getSchoolId());
        schoolService.deleteFavoriteSchool(schoolDTO);
        return Result.success();
    }

    /**
     * 根据学校分数获取分数相近学校
     *
     * @param schoolDTO 学校DTO
     * @return Result<List < School>>
     */
    @PostMapping("/close")
    @Operation(summary = "根据学校分数获取分数相近学校")
    public Result<List<School>> scoreCloseSchool(@RequestBody SchoolDTO schoolDTO) {
        log.info("用户'{}'获取和'{}'学校分数相近的学校", schoolDTO.getUserId(), schoolDTO.getSchoolId());
        return Result.success(schoolService.getCloseSchool(schoolDTO));
    }

    /**
     * 录取预测
     *
     * @param forecastDTO 录取预测DTO
     * @return SchoolForecastVO 录取预测VO
     */
    @PostMapping("/forecast")
    @Operation(summary = "录取预测")
    public Result<SchoolForecastVO> forecast(@RequestBody ForecastDTO forecastDTO) {
        log.info("用户'{}'正在做录取预测", ThreadLocalUtil.getCurrentId());
        return Result.success(schoolService.forecast(forecastDTO));
    }

    /**
     * 获取某学校的历年分数线
     *
     * @param yearScoreQueryDTO 分数线查询DTO
     * @return Result<List < SchoolYearScoreVO>>
     */
    @PostMapping("/schoolScoreLine")
    @Operation(summary = "获取某学校的历年分数线")
    public Result<List<SchoolYearScoreVO>> getSchoolScoreLine(@RequestBody YearScoreQueryDTO yearScoreQueryDTO) {
        return Result.success(schoolService.schoolScoreLine(yearScoreQueryDTO));
    }

    /**
     * 获取某学校的专业分数线
     *
     * @param yearScoreQueryDTO 分数线查询DTO
     * @return Result<PageResult < MajorYearScoreVO>>
     */
    @PostMapping("/majorScoreLine")
    @Operation(summary = "获取某学校的专业分数线")
    public Result<PageResult<MajorYearScoreVO>> getMajorScoreLine(@RequestBody YearScoreQueryDTO yearScoreQueryDTO) {
        return Result.success(schoolService.majorScoreLine(yearScoreQueryDTO));
    }

    /**
     * 用户评价学校
     *
     * @param addCommentDTO 用户评价DTO
     */
    @PostMapping("/comment")
    @Operation(summary = "用户评价学校")
    public Result<Void> addSchoolComment(@RequestBody AddCommentDTO addCommentDTO) {
        log.info("用户'{}'发表对于'{}'大学的评论", ThreadLocalUtil.getCurrentId(), addCommentDTO.getSchoolId());
        schoolService.addSchoolComment(addCommentDTO);
        return Result.success();
    }

    /**
     * 展示大学评论区
     *
     * @param commentDTO 大学评论区查询DTO
     * @return Result<PageResult < CommentVO>>
     */
    @PostMapping("/showComment")
    @Operation(summary = "展示大学评论区")
    public Result<List<CommentVO>> pageQuerySchoolComment(@RequestBody CommentDTO commentDTO) {
        log.info("查看大学'{}'的评价", commentDTO.getSchoolId());
        return Result.success(schoolService.showComment(commentDTO));
    }

    /**
     * 获取本省热门本科院校
     *
     * @param provinceQueryDTO 省份查询DTO
     * @return Result<List < SchoolSceneryVO>>
     */
    @PostMapping("/hotSchool1")
    @Operation(summary = "获取本省热门本科院校")
    public Result<List<SchoolSceneryVO>> getSchool1(@RequestBody ProvinceQueryDTO provinceQueryDTO) {
        log.info("获取本省热门本科院校");
        return Result.success(schoolService.getSchool1(provinceQueryDTO));
    }

    /**
     * 获取本省热门专科院校
     *
     * @param provinceQueryDTO 省份查询DTO
     * @return Result<List < SchoolSceneryVO>>
     */
    @PostMapping("/hotSchool2")
    @Operation(summary = "获取本省热门专科院校")
    public Result<List<SchoolSceneryVO>> getSchool2(@RequestBody ProvinceQueryDTO provinceQueryDTO) {
        log.info("获取本省热门专科院校");
        return Result.success(schoolService.getSchool2(provinceQueryDTO));
    }

    /**
     * 获取外省热门本科院校
     *
     * @param provinceQueryDTO 省份查询DTO
     * @return Result<List < SchoolSceneryVO>>
     */
    @PostMapping("/hotSchool3")
    @Operation(summary = "获取外省热门本科院校")
    public Result<List<SchoolSceneryVO>> getSchool3(@RequestBody ProvinceQueryDTO provinceQueryDTO) {
        log.info("获取外省热门本科院校");
        return Result.success(schoolService.getSchool3(provinceQueryDTO));
    }

    /**
     * 获取外省热门专科院校
     *
     * @param provinceQueryDTO 省份查询DTO
     * @return Result<List < SchoolSceneryVO>>
     */
    @PostMapping("/hotSchool4")
    @Operation(summary = "获取外省热门专科院校")
    public Result<List<SchoolSceneryVO>> getSchool4(@RequestBody ProvinceQueryDTO provinceQueryDTO) {
        log.info("获取外省热门专科院校");
        return Result.success(schoolService.getSchool4(provinceQueryDTO));
    }

    /**
     * 获取热门专业（本科）
     *
     * @return Result<List < HotMajorVO>>
     */
    @PostMapping("/hotMajor1")
    @Operation(summary = "获取热门专业（本科）")
    public Result<List<HotMajorVO>> hotMajor() {
        log.info("获取热门专业（本科）");
        List<HotMajorVO> hotMajorVOList = schoolService.getHotMajor();
        return Result.success(hotMajorVOList);
    }

    /**
     * 获取热门专业（专科）
     *
     * @return Result<List < HotMajorVO>>
     */
    @PostMapping("/hotMajor2")
    @Operation(summary = "获取热门专业（专科）")
    public Result<List<HotMajorVO>> hotMajorProfessional() {
        log.info("获取热门专业（专科）");
        List<HotMajorVO> hotMajorVOList = schoolService.getHotMajorProfessional();
        return Result.success(hotMajorVOList);
    }

    /**
     * 获取热门院校排行榜
     *
     * @return Result<List < SchoolVO>>
     */
    @PostMapping("/hotRank")
    @Operation(summary = "获取热门院校排行榜")
    public Result<List<School>> hotSchool() {
        log.info("获取热门院校");
        List<School> schoolSimpleVOList = schoolService.getHotRank();
        return Result.success(schoolSimpleVOList);
    }

    /**
     * 获取强基计划学校
     *
     * @return Result<List < SchoolVO>>
     */
    @PostMapping("/basic")
    @Operation(summary = "获取强基计划学校")
    public Result<List<School>> basicsSchool() {
        log.info("获取强基计划学校");
        return Result.success(schoolService.getBasicSchool());
    }

    /**
     * 请求AI获取学校信息
     *
     * @param schoolAIRequestDTO 大学AI请求 DTO
     * @return Result<SchoolAIMessageVO>
     */
    @PostMapping("/information")
    @Operation(summary = "请求AI获取学校信息")
    public Result<AgentMessageVO> schoolAIRequest(@RequestBody SchoolAIRequestDTO schoolAIRequestDTO) {
        log.info("正在请求'{}'学校的信息", schoolAIRequestDTO.getSchoolId());
        AgentMessageVO AgentMessageVO = douBaoService.schoolInformation(schoolAIRequestDTO);
        return Result.success(AgentMessageVO);
    }

}
