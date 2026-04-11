package a311.college.service.impl;

import a311.college.constant.redis.MajorRedisKey;
import a311.college.constant.user.UserErrorConstant;
import a311.college.dto.major.MajorDTO;
import a311.college.dto.query.major.MajorPageQueryDTO;
import a311.college.dto.query.major.MajorSchoolPageQueryDTO;
import a311.college.dto.query.major.ProfessionalClassQueryDTO;
import a311.college.dto.query.major.SubjectCategoryQueryDTO;
import a311.college.dto.query.school.CommentDTO;
import a311.college.dto.school.AddCommentDTO;
import a311.college.entity.major.Major;
import a311.college.entity.major.ProfessionalClass;
import a311.college.entity.major.SubjectCategory;
import a311.college.entity.school.School;
import a311.college.exception.CommentIllegalException;
import a311.college.exception.PageQueryException;
import a311.college.exception.ReAdditionException;
import a311.college.filter.FinderUtil;
import a311.college.mapper.major.MajorMapper;
import a311.college.mapper.school.SchoolMapper;
import a311.college.result.PageResult;
import a311.college.service.MajorService;
import a311.college.thread.ThreadLocalUtil;
import a311.college.vo.major.DetailMajorVO;
import a311.college.vo.school.CommentVO;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class MajorServiceImpl implements MajorService {

    private final MajorMapper majorMapper;

    private final SchoolMapper schoolMapper;

    @Autowired
    public MajorServiceImpl(MajorMapper majorMapper, SchoolMapper schoolMapper) {
        this.majorMapper = majorMapper;
        this.schoolMapper = schoolMapper;
    }

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    /**
     * 查询学科门类
     *
     * @param subjectCategoryQueryDTO 学科门类查询DTO
     * @return List<SubjectCategory>
     */
    @Override
    public List<SubjectCategory> getSubjectCategory(SubjectCategoryQueryDTO subjectCategoryQueryDTO) {
        List<SubjectCategory> subjectCategories = majorMapper.selectSubjectCategory(subjectCategoryQueryDTO);
        for (SubjectCategory subjectCategory : subjectCategories) {
            subjectCategory.setProfessionalClassList(majorMapper.selectProfessionalClass(subjectCategory.getSubjectCategoryId()));
        }
        return subjectCategories;
    }

    /**
     * 获取专业类别
     *
     * @param professionalClassQueryDTO 专业类别查询DTO
     * @return List<ProfessionalClass>
     */
    @Override
    public List<ProfessionalClass> getProfessionalClass(ProfessionalClassQueryDTO professionalClassQueryDTO) {
        //return majorMapper.selectProfessionalClass(professionalClassQueryDTO);
        return null;
    }

    /**
     * 专业分页查询
     *
     * @param majorPageQueryDTO 专业分页查询DTO
     * @return PageResult<Major>
     */
    @Override
    public PageResult<Major> majorPageQuery(MajorPageQueryDTO majorPageQueryDTO) {
        // 判断是否命中缓存
        // 拼装Key
        String key = MajorRedisKey.MAJOR_CACHE_KEY + majorPageQueryDTO.getProfessionalClassId() + ":";
        String cachedJson = redisTemplate.opsForValue().get(key);
        if (cachedJson != null && !cachedJson.isEmpty()) {
            log.info("缓存命中");
            // 2.2手动进行分页并返回
            List<Major> majorCache = JSON.parseArray(cachedJson, Major.class);
            return manualPage(majorCache, majorPageQueryDTO.getPage(), majorPageQueryDTO.getPageSize());
        }
        try (Page<Major> page = PageHelper.startPage(majorPageQueryDTO.getPage(), majorPageQueryDTO.getPageSize())) {
            majorMapper.pageQueryMajors(majorPageQueryDTO);
            // 获取专业记录数
            long total = page.getTotal();
            // 获取总记录
            List<Major> result = page.getResult();
            return new PageResult<>(total, result);
        } catch (Exception e) {
            log.error("专业分页查询失败，报错为：{}", e.getMessage());
            throw new PageQueryException(e.getMessage());
        }
    }

    /**
     * 人工分页
     *
     * @param filterCache 过滤后的学校缓存
     * @param page        查询页码
     * @param pageSize    每页大小
     * @return PageResult<SchoolVO>
     */
    private PageResult<Major> manualPage(List<Major> filterCache, Integer page, Integer pageSize) {
        // 1.获取记录总数
        int total = filterCache.size();
        // 2.获取起始页码
        int start = (page - 1) * pageSize;
        if (start >= total) return new PageResult<>((long) total, Collections.emptyList());
        // 3.获取结束页码
        int end = Math.min(start + pageSize, total);
        // 4.分页并返回
        List<Major> pageData = filterCache.subList(start, end);
        return new PageResult<>((long) total, pageData);
    }

    /**
     * 热门专业信息缓存预热
     */
    @Override
    public void cacheMajor() {
        // 1. 定义学科门类分类表
        List<Integer> hotProfessionalClass = Arrays.asList(1, 2, 3, 4, 6, 9, 14, 34, 37, 56);
        // 2. 缓存热门学科门类中的专业
        for (Integer professionalClass : hotProfessionalClass) {
            // 2.1 统一的键名格式
            String key = MajorRedisKey.MAJOR_CACHE_KEY + professionalClass + ":";
            try {
                // 2.2 查询数据库
                // 删除旧数据
                redisTemplate.delete(key);
                // 获取专业专业
                List<Major> majorList = majorMapper.selectAllMajor(professionalClass);
                if (!majorList.isEmpty()) {
                    redisTemplate.opsForValue().set(key, JSON.toJSONString(majorList));
                    log.info("专业分类 {} 缓存预热成功，共 {} 条数据", professionalClass, majorList.size());
                } else {
                    log.warn("专业分类{}无数据，跳过缓存预热", professionalClass);
                }

            } catch (Exception e) {
                log.error("学科门类 {} 缓存预热失败: {}", professionalClass, e.getMessage(), e);
            }
        }
    }

    /**
     * 查询专业具体信息
     *
     * @param majorDTO 专业DTO
     * @return DetailMajorVO
     */
    @Override
    public DetailMajorVO getDetailMajor(MajorDTO majorDTO) {
        // 1.根据id查询到对应专业
        Major major = majorMapper.selectById(majorDTO.getMajorId());
        // 2.封装专业详情对象
        DetailMajorVO detailMajorVO = new DetailMajorVO();
        BeanUtil.copyProperties(major, detailMajorVO);
        // 3.构造专业满意度集合
        List<Double> satisfaction = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            // 3.1设置随机满意度
            double randomSatisfaction = Math.round(RandomUtil.randomDouble(3.7, 5.0) * 10) / 10.0;
            satisfaction.add(randomSatisfaction);
        }
        // 4.构造专业就业率集合
        List<String> employmentRate = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            // 4.1构造随机就业率集合
            int start = RandomUtil.randomInt(75, 81);
            int end = RandomUtil.randomInt(81, 93);
            String rate = start + "%" + "~" + end + "%";
            employmentRate.add(rate);
        }
        // 5.返回专业详情对象
        detailMajorVO.setSatisfaction(satisfaction);
        detailMajorVO.setEmploymentRate(employmentRate);
        // 判断专业是否被收藏
        detailMajorVO.setFavorite(majorMapper.checkFavorite(majorDTO) == 1);
        return detailMajorVO;
    }

    /**
     * 用户收藏专业
     *
     * @param majorDTO 专业DTO
     */
    @Override
    public void addFavoriteMajor(MajorDTO majorDTO) {
        long userId = ThreadLocalUtil.getCurrentId();
        majorDTO.setUserId(userId);
        if (majorMapper.checkMajorDistinct(majorDTO) != 0) {
            throw new ReAdditionException(UserErrorConstant.RE_ADDITION);
        }
        majorMapper.addFavoriteMajor(majorDTO);
    }

    /**
     * 用户删除收藏
     *
     * @param majorDTO 大学DTO
     */
    @Override
    public void deleteFavoriteMajor(MajorDTO majorDTO) {
        long userId = ThreadLocalUtil.getCurrentId();
        majorDTO.setUserId(userId);
        majorMapper.deleteFavoriteMajor(majorDTO);
    }

    /**
     * 查询某专业开设学校
     *
     * @param majorSchoolPageQueryDTO 专业分页查询DTO
     * @return List<School>
     */
    @Override
    public PageResult<School> querySchools(MajorSchoolPageQueryDTO majorSchoolPageQueryDTO) {
        try (Page<School> page = PageHelper.startPage(majorSchoolPageQueryDTO.getPage(), majorSchoolPageQueryDTO.getPageSize())) {
            schoolMapper.selectMajorSchool(majorSchoolPageQueryDTO);
            return new PageResult<>(page.getTotal(), page.getResult());
        } catch (Exception e) {
            log.error("学校分页查询失败，报错为：{}", e.getMessage());
            throw new PageQueryException(e.getMessage());
        }
    }

    /**
     * 用户评价专业
     *
     * @param addCommentDTO 用户评价DTO
     */
    @Override
    public void addMajorComment(AddCommentDTO addCommentDTO) {
        FinderUtil finderUtil = new FinderUtil();
        // 进行敏感词判断
        if (finderUtil.containsSensitiveWord(addCommentDTO.getComment())) {
            log.error("输入的内容含有敏感词");
            throw new CommentIllegalException("输入内容含有敏感词");
        }
        addCommentDTO.setUserId(ThreadLocalUtil.getCurrentId());
        addCommentDTO.setMajorName(majorMapper.selectById(addCommentDTO.getMajorId()).getMajorName());
        majorMapper.addComment(addCommentDTO);
    }

    /**
     * 分页查询用户评价
     *
     * @param commentDTO 评论区分页查询DTO
     * @return List<CommentVO>
     */
    @Override
    public List<CommentVO> showComment(CommentDTO commentDTO) {
        return majorMapper.selectComment(commentDTO.getMajorId());
    }


}
