package a311.college.service.impl;

import a311.college.constant.error.ErrorConstant;
import a311.college.constant.redis.SchoolRedisKey;
import a311.college.constant.user.UserErrorConstant;
import a311.college.controller.school.constant.SchoolConstant;
import a311.college.dto.query.school.*;
import a311.college.dto.school.*;
import a311.college.dto.user.UserSearchDTO;
import a311.college.entity.major.Major;
import a311.college.entity.school.School;
import a311.college.entity.school.SchoolMajor;
import a311.college.enumeration.school.*;
import a311.college.exception.CommentIllegalException;
import a311.college.exception.PageQueryException;
import a311.college.exception.ReAdditionException;
import a311.college.filter.FinderUtil;
import a311.college.mapper.major.MajorMapper;
import a311.college.mapper.resource.ResourceMapper;
import a311.college.mapper.school.SchoolMapper;
import a311.college.result.PageResult;
import a311.college.service.SchoolService;
import a311.college.thread.ThreadLocalUtil;
import a311.college.vo.major.HotMajorVO;
import a311.college.vo.major.MajorSimpleVO;
import a311.college.vo.school.*;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 学校相关服务实现类
 */
@Slf4j
@Service
public class SchoolServiceImpl implements SchoolService {

    private final SchoolMapper schoolMapper;

    private final ResourceMapper resourceMapper;

    private final MajorMapper majorMapper;

    @Autowired
    public SchoolServiceImpl(SchoolMapper schoolMapper, ResourceMapper resourceMapper, MajorMapper majorMapper) {
        this.schoolMapper = schoolMapper;
        this.resourceMapper = resourceMapper;
        this.majorMapper = majorMapper;
    }

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    /**
     * 学校信息分页查询
     *
     * @param schoolPageQueryDTO 学校分页查询DTO
     * @return PageResult<DetailedSchoolVO>
     */
    @Override
    public PageResult<School> pageSelect(SchoolPageQueryDTO schoolPageQueryDTO) {
        // 1.根据查询条件，判断是否命中热点学校缓存
        List<String> rank = schoolPageQueryDTO.getRankList();
        String rankList = "";
        if (rank != null) {
            rankList = schoolPageQueryDTO.getRankList().toString().
                    replaceAll("\\[", "").replace("]", "");
            if (rankList.contains("中央部委")) {
                String cached = redisTemplate.opsForValue().get(SchoolRedisKey.CENTER_CACHE_KEY);
                if (cached != null && !cached.isEmpty()) {
                    log.info("缓存命中中央部委学校");
                    List<School> schoolCache = JSON.parseArray(cached, School.class);
                    List<School> filterCache = filterHot(schoolCache, schoolPageQueryDTO);
                    return manualPage(filterCache, schoolPageQueryDTO.getPage(), schoolPageQueryDTO.getPageSize());
                }
            }
            if (rankList.contains("C9联盟")) {
                String cached = redisTemplate.opsForValue().get(SchoolRedisKey.C9_CACHE_KEY);
                if (cached != null && !cached.isEmpty()) {
                    log.info("缓存命中C9联盟");
                    List<School> schoolCache = JSON.parseArray(cached, School.class);
                    List<School> filterCache = filterHot(schoolCache, schoolPageQueryDTO);
                    return manualPage(filterCache, schoolPageQueryDTO.getPage(), schoolPageQueryDTO.getPageSize());
                }
            }
            if (rankList.contains("国防七子")) {
                String cached = redisTemplate.opsForValue().get(SchoolRedisKey.DEFENSE_CACHE_KEY);
                if (cached != null && !cached.isEmpty()) {
                    log.info("缓存命中国防七子");
                    List<School> schoolCache = JSON.parseArray(cached, School.class);
                    List<School> filterCache = filterHot(schoolCache, schoolPageQueryDTO);
                    return manualPage(filterCache, schoolPageQueryDTO.getPage(), schoolPageQueryDTO.getPageSize());
                }
            }
            log.info("缓存没有命中特殊学校，查看缓存是否命中热点地区");
        }
        // 2.根据查询条件，判断是否命中热点地区学校缓存
        // 2.1封装key
        String key = SchoolRedisKey.SCHOOL_CACHE_KEY + schoolPageQueryDTO.getProvince() + ":";
        String cachedJson = redisTemplate.opsForValue().get(key);
        if (cachedJson != null && !cachedJson.isEmpty()) {
            // 2.2成功从缓存中获取数据
            log.info("缓存命中");
            // 2.3根据查询条件进行过滤
            List<School> schoolCache = JSON.parseArray(cachedJson, School.class);
            List<School> filterCache = filterSchools(schoolCache, schoolPageQueryDTO, rankList);
            // 2.4手动进行分页并返回
            return manualPage(filterCache, schoolPageQueryDTO.getPage(), schoolPageQueryDTO.getPageSize());
        }
        // 3.缓存未命中，进行分页查询查询数据
        log.info("缓存未命中，开启分页查询");
        try (Page<School> page = PageHelper.startPage(schoolPageQueryDTO.getPage(), schoolPageQueryDTO.getPageSize())) {
            schoolMapper.schoolPageQuery(schoolPageQueryDTO);
            // 3.1获取总记录条数
            long total = page.getTotal();
            // 3.2获取总记录并返回
            List<School> result = page.getResult();
            return new PageResult<>(total, result);
        } catch (Exception e) {
            log.error("大学信息分页查询失败，报错为：{}", e.getMessage());
            throw new PageQueryException(ErrorConstant.SCHOOL_PAGE_QUERY_ERROR);
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
    private PageResult<School> manualPage(List<School> filterCache, Integer page, Integer pageSize) {
        // 1.获取记录总数
        int total = filterCache.size();
        // 2.获取起始页码
        int start = (page - 1) * pageSize;
        if (start >= total) return new PageResult<>((long) total, Collections.emptyList());
        // 3.获取结束页码
        int end = Math.min(start + pageSize, total);
        // 4.分页并返回
        List<School> pageData = filterCache.subList(start, end);
        return new PageResult<>((long) total, pageData);
    }

    /**
     * 从缓存中过滤热门地区学校数据
     *
     * @param schoolCache        学校缓存
     * @param schoolPageQueryDTO 学校分页查询DTO
     * @return List<SchoolVO>    过滤后学校列表
     */
    private List<School> filterSchools(List<School> schoolCache, SchoolPageQueryDTO schoolPageQueryDTO, String rankList) {
        return schoolCache.stream()
                .filter(s -> schoolPageQueryDTO.getRankList() == null || s.getRankList().contains(rankList))
                .filter(s -> schoolPageQueryDTO.getProvince() == null || s.getSchoolProvince().getName().contains(schoolPageQueryDTO.getProvince()))
                .collect(Collectors.toList());
    }

    /**
     * 从缓存中过滤热门学校数据
     *
     * @param schoolCache        学校缓存
     * @param schoolPageQueryDTO 学校分页查询DTO
     * @return List<School>      过滤后学校列表
     */
    private List<School> filterHot(List<School> schoolCache, SchoolPageQueryDTO schoolPageQueryDTO) {
        return schoolCache.stream()
                .filter(s -> schoolPageQueryDTO.getProvince() == null || s.getSchoolProvince().getName().contains(schoolPageQueryDTO.getProvince()))
                .collect(Collectors.toList());
    }

    /**
     * 学校信息缓存预热
     * 添加热点地区的学校到缓存
     */
    @Override
    public void cacheSchool() {
        // 0. 定义热点地区列表
        List<String> hotAreas = Arrays.asList("北京", "上海", "广东", "重庆", "天津", "浙江", "江苏", "陕西", "四川", "湖北");
        for (String area : hotAreas) {
            // 0.1 统一键名格式
            String key = SchoolRedisKey.SCHOOL_CACHE_KEY + area + ":";
            try {
                // 1. 查询数据库
                List<School> school = schoolMapper.selectAllSchoolByProvince(area);
                // 2. 删除旧缓存（避免残留旧数据）
                redisTemplate.delete(key);
                // 3. 批量插入新数据（使用JSON字符串存储）
                if (!school.isEmpty()) {
                    redisTemplate.opsForValue().set(key, JSON.toJSONString(school));
                    log.info("热点地区 {} 缓存预热成功，共 {} 条数据", area, school.size());
                } else {
                    log.warn("热点地区 {} 无数据，跳过缓存预热", area);
                }
            } catch (Exception e) {
                log.error("热点地区 {} 缓存预热失败: {}", area, e.getMessage(), e);
            }
        }
    }

    /**
     * 缓存热门学校
     */
    public void cacheHot() {
        try {
            List<School> schoolList = new ArrayList<>();
            for (C9Enum c9 : C9Enum.values()) {
                School school = schoolMapper.querySchoolName(c9.toString());
                if (school != null) {
                    schoolList.add(school);
                }
            }
            addCache(schoolList, SchoolRedisKey.C9_CACHE_KEY);
            log.info("C9高校缓存完成，一共{}所", schoolList.size());
            schoolList.clear();

            for (CenterEnum center : CenterEnum.values()) {
                School school = schoolMapper.querySchoolName(center.toString());
                if (school != null) {
                    schoolList.add(school);
                }
            }
            addCache(schoolList, SchoolRedisKey.CENTER_CACHE_KEY);
            log.info("中央部委直属高校缓存完成，一共{}所", schoolList.size());
            schoolList.clear();

            for (Defense7Enum defense : Defense7Enum.values()) {
                School school = schoolMapper.querySchoolName(defense.toString());
                if (school != null) {
                    schoolList.add(school);
                }
            }
            addCache(schoolList, SchoolRedisKey.DEFENSE_CACHE_KEY);
            log.info("国防7子缓存完成，一共{}所", schoolList.size());
            schoolList.clear();

        } catch (Exception e) {
            log.error("热门学校缓存预热失败: {}", e.getMessage());
        }
    }

    /**
     * 缓存热门学校
     *
     * @param schoolList 学校列表
     * @param key        键
     */
    private void addCache(List<School> schoolList, String key) {
        // 1.删除缓存的旧数据
        redisTemplate.delete(key);
        // 2.缓存新的热门学校数据（使用JSON字符串存储）
        if (!schoolList.isEmpty()) {
            redisTemplate.opsForValue().set(key, JSON.toJSONString(schoolList));
            log.info("{}缓存成功", key);
        } else {
            log.warn("没有该类型的热门学校，缓存失败");
        }
    }

    /**
     * 学校专业分页查询
     *
     * @param schoolMajorPageQueryDTO 学校专业分页查询DTO
     * @return PageResult<SchoolMajor>
     */
    @Override
    public PageResult<SchoolMajor> pageSelectMajor(SchoolMajorPageQueryDTO schoolMajorPageQueryDTO) {
        try (Page<SchoolMajor> page = PageHelper.startPage(schoolMajorPageQueryDTO.getPage(), schoolMajorPageQueryDTO.getPageSize())) {
            schoolMapper.schoolMajorPageQuery(schoolMajorPageQueryDTO);
            long total = page.getTotal();
            List<SchoolMajor> result = page.getResult();
            return new PageResult<>(total, result);
        } catch (Exception e) {
            log.error("大学专业分页查询失败，报错为：{}", e.getMessage());
            throw new PageQueryException(ErrorConstant.SCHOOL_MAJOR_PAGE_QUERY_ERROR);
        }
    }

    /**
     * 用户搜索提示
     *
     * @param userSearchDTO 用户搜索DTO
     * @return SearchVO
     */
    @Override
    public SearchVO searchList(UserSearchDTO userSearchDTO) {
        if (userSearchDTO.getMessage() == null) {
            return new SearchVO();
        }
        // 获取用户搜索内容
        String message = userSearchDTO.getMessage();
        // 1.根据用户搜索内容搜索学校
        List<School> schoolList = schoolMapper.searchSchool(message);
        // 2.根据用户搜索内容搜索专业
        List<Major> majorList = majorMapper.searchMajor(message);
        // 3.判断是否有匹配搜索内容的学校
        if (schoolList != null) {
            log.info("用户搜索到学校信息");
            // 3.2成功匹配到学校数据，对其进行处理
            for (School school : schoolList) {
                String[] split = school.getRankList().split(",");
                StringBuilder rank = new StringBuilder(split[0]);
                if (split.length == 3) {
                    rank.append(",").append(split[1]).append(",").append(split[2]);
                }
                if (split.length > 3) {
                    rank.append(",").append(split[2]).append(",").append(split[3]);
                }
                school.setRankList(rank.toString());
            }
        }
        // 4.返回搜索结果
        return new SearchVO(schoolList, majorList);
    }

    /**
     * 用户搜索学校
     *
     * @param schoolPageQueryDTO 学校分页查询DTO
     * @return PageResult<School>
     */
    @Override
    public PageResult<School> search(SchoolPageQueryDTO schoolPageQueryDTO) {
        try (Page<School> page = PageHelper.startPage(schoolPageQueryDTO.getPage(), schoolPageQueryDTO.getPageSize())) {
            schoolMapper.selectSchoolBySchoolName(schoolPageQueryDTO.getSchoolName());
            List<School> result = page.getResult();
            if (result.isEmpty()) {
                throw new RuntimeException("输入错误");
            }
            return new PageResult<>(page.getTotal(), page.getResult());
        } catch (Exception e) {
            log.error(ErrorConstant.SCHOOL_SEARCH_ERROR);
            throw new PageQueryException(ErrorConstant.SCHOOL_SEARCH_ERROR);
        }
    }

    /**
     * 查询学校具体信息
     *
     * @param schoolDTO 学校查询DTO
     * @return DetailedSchoolVO 学校具体信息
     */
    @Override
    public DetailedSchoolVO getDetailSchool(SchoolDTO schoolDTO) {
        // 1.获取学校信息
        DetailedSchoolVO detailedSchoolVO = schoolMapper.selectDetailBySchoolId(schoolDTO.getSchoolId());
        // 2.处理学校排名信息
        String rankItem = detailedSchoolVO.getRankItem();
        String rankInfo = detailedSchoolVO.getRankInfo();
        Map<String, String> rank = new HashMap<>();
        String[] splitRankItem = rankItem.split(",");
        String[] splitRankInfo = rankInfo.split(",");
        for (int i = 0; i < splitRankItem.length; i++) {
            rank.put(splitRankItem[i], splitRankInfo[i]);
        }
        detailedSchoolVO.setRank(rank);
        // 3.返回随机校园风光
        // 3.1获取所有照片
        List<String> imageList = resourceMapper.getAllImages();
        // 3.2从所有照片中随机选取6张
        List<String> images = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            int index = RandomUtil.randomInt(0, 150);
            images.add(imageList.get(index));
        }
        // 3.3返回随机6张校园风光
        detailedSchoolVO.setImages(images);
        int schoolId = schoolDTO.getSchoolId();
        switch (schoolId) {
            case 1038 -> detailedSchoolVO.setImages(SchoolConstant.get1038());
            case 119 -> detailedSchoolVO.setImages(SchoolConstant.get119());
            case 934 -> detailedSchoolVO.setImages(SchoolConstant.get934());
            case 184 -> detailedSchoolVO.setImages(SchoolConstant.get184());
        }
        // 4.随机校园配置
        if (detailedSchoolVO.getScore() > 60) {
            // 4.1该学校属于好学校
            detailedSchoolVO.setEquipment(highScoreSchool());
        } else if (detailedSchoolVO.getRankList().contains("民办")) {
            // 4.2该学校属于有钱的学校
            detailedSchoolVO.setEquipment(richSchool());
        } else {
            // 4.3该学校属于一般学校
            detailedSchoolVO.setEquipment(commonSchool());
        }
        // 5.为该学校封装展示专业
        // 5.1获取专业
        List<MajorSimpleVO> majorSimpleVOList = schoolMapper.selectMajor(detailedSchoolVO.getSchoolId());
        // 5.2调整专业格式
        for (MajorSimpleVO majorSimpleVO : majorSimpleVOList) {
            majorSimpleVO.setMajorName(majorSimpleVO.getMajorName());
        }
        // 5.3封装专业列表
        detailedSchoolVO.setMajors(majorSimpleVOList);
        // 6.判断该学校是否被用户收藏
        schoolDTO.setUserId(ThreadLocalUtil.getCurrentId());
        detailedSchoolVO.setFavorite(schoolMapper.checkSchoolDistinct(schoolDTO) == 1);
        return detailedSchoolVO;
    }

    /**
     * 为好学校设置学校设施
     */
    private Map<String, Integer> highScoreSchool() {
        Map<String, Integer> equipment = new HashMap<>();
        equipment.put("one", 4);
        equipment.put("two", 1);
        equipment.put("three", RandomUtil.randomInt(5, 16));
        equipment.put("four", 1);
        equipment.put("five", 1);
        equipment.put("six", 1);
        equipment.put("seven", 1);
        equipment.put("eight", 1);
        equipment.put("nine", 1);
        equipment.put("ten", RandomUtil.randomInt(5, 11));
        return equipment;
    }

    /**
     * 为有钱的学校设置学校设施
     */
    private Map<String, Integer> richSchool() {
        Map<String, Integer> equipment = new HashMap<>();
        equipment.put("one", 4);
        equipment.put("two", 1);
        equipment.put("three", RandomUtil.randomInt(3, 7));
        equipment.put("four", 1);
        equipment.put("five", 1);
        equipment.put("six", 1);
        equipment.put("seven", 1);
        equipment.put("eight", 1);
        equipment.put("nine", 1);
        equipment.put("ten", RandomUtil.randomInt(2, 5));
        return equipment;
    }

    /**
     * 为一般学校设置学校设施
     */
    private Map<String, Integer> commonSchool() {
        Map<String, Integer> equipment = new HashMap<>();
        Integer[] room = {4, 6};
        equipment.put("one", RandomUtil.randomEle(room));
        Integer[] flag = {0, 1};
        equipment.put("two", RandomUtil.randomEle(flag));
        equipment.put("three", RandomUtil.randomInt(5, 11));
        equipment.put("four", 1);
        equipment.put("five", 1);
        equipment.put("six", RandomUtil.randomEle(flag));
        equipment.put("seven", 1);
        equipment.put("eight", 0);
        equipment.put("nine", RandomUtil.randomEle(flag));
        equipment.put("ten", RandomUtil.randomInt(3, 7));
        return equipment;
    }

    /**
     * 用户收藏学校
     *
     * @param schoolDTO 学校DTO
     */
    @Override
    public void addFavoriteSchool(SchoolDTO schoolDTO) {
        long userId = ThreadLocalUtil.getCurrentId();
        schoolDTO.setUserId(userId);
        if (schoolMapper.checkSchoolDistinct(schoolDTO) != 0) {
            throw new ReAdditionException(UserErrorConstant.RE_ADDITION);
        }
        schoolMapper.addFavoriteSchool(schoolDTO);
    }

    /**
     * 用户删除收藏
     *
     * @param schoolDTO 学校DTO
     */
    @Override
    public void deleteFavoriteSchool(SchoolDTO schoolDTO) {
        long userId = ThreadLocalUtil.getCurrentId();
        schoolDTO.setUserId(userId);
        schoolMapper.deleteFavoriteSchool(schoolDTO);
    }

    /**
     * 根据学校分数获取分数相近学校
     *
     * @param schoolDTO 学校DTO
     * @return List<School>
     */
    @Override
    public List<School> getCloseSchool(SchoolDTO schoolDTO) {
        School school = schoolMapper.selectBySchoolId(schoolDTO.getSchoolId());
        Integer score = school.getScore();
        List<School> schoolVOList = schoolMapper.selectCloseSchool(score);
        for (School closeSchool : schoolVOList) {
            String[] split = closeSchool.getRankList().split(",");
            closeSchool.setRankList(split[0] + "," + split[1] + "," + split[2]);
        }
        return schoolVOList;
    }

    /**
     * 录取预测
     *
     * @param forecastDTO 录取预测DTO
     * @return SchoolForecastVO 录取预测VO
     */
    @Override
    public SchoolForecastVO forecast(ForecastDTO forecastDTO) {
        // 1.列出三种难度的专业
        int minimum = 0;
        int stable = 0;
        int rush = 0;
        // 2.处理专业信息
        // 2.1获得专业信息
        List<SchoolMajor> schoolMajorList = schoolMapper.selectAllSchoolMajor(forecastDTO);
        // 2.2将SchoolMajor对象封装为SchoolMajorVO对象返回
        List<SchoolMajorVO> schoolMajorVOList = new ArrayList<>();
        for (SchoolMajor schoolMajor : schoolMajorList) {
            // 2.3构造SchoolMajorVo对象
            SchoolMajorVO schoolMajorVO = new SchoolMajorVO();
            // 2.4属性拷贝
            BeanUtil.copyProperties(schoolMajor, schoolMajorVO);
            // 2.5默认根据用户的位次进行预测
            Integer userRanking = forecastDTO.getRanking();
            if (userRanking != null) {
                int majorRanking = schoolMajor.getMinRanking();
                if (majorRanking >= userRanking - 3000 && majorRanking <= userRanking + 5000) {
                    // 2.6该专业为稳
                    schoolMajorVO.setCategory(1);
                    stable++;
                } else if (majorRanking < userRanking - 3000 && majorRanking >= userRanking - 5000) {
                    // 2.7该专业为冲
                    schoolMajorVO.setCategory(2);
                    rush++;
                } else if (majorRanking > userRanking + 5000) {
                    // 2.8该专业为保
                    schoolMajorVO.setCategory(0);
                    minimum++;
                }
            } else if (forecastDTO.getGrade() != null) {
                int majorScore = schoolMajor.getMinScore();
                int userGrade = forecastDTO.getGrade();
                if (majorScore <= userGrade + 10 && majorScore >= userGrade - 10) {
                    // 该专业为稳
                    schoolMajorVO.setCategory(1);
                    stable++;
                } else if (majorScore > userGrade + 10 && majorScore <= userGrade + 30) {
                    // 该专业为冲
                    schoolMajorVO.setCategory(2);
                    rush++;
                } else if (majorScore < userGrade - 10 && majorScore >= userGrade - 30) {
                    // 该专业为保
                    schoolMajorVO.setCategory(0);
                    minimum++;
                }
            }
            // 2.9将有分类的专业加入到SchoolMajorVOList中返回
            if (schoolMajorVO.getCategory() != null) {
                schoolMajorVOList.add(schoolMajorVO);
            } else {
                schoolMajorVO.setCategory(3);
                schoolMajorVOList.add(schoolMajorVO);
            }
        }
        int sigma = 15;
        // 3.构建ForecastVO结果
        SchoolForecastVO schoolForecastVO = new SchoolForecastVO();
        // 3.1封装可选专业
        schoolForecastVO.setSelectableMajor(minimum + stable + rush);
        // 3.2封装专业列表
        schoolForecastVO.setMajorForecastList(schoolMajorVOList);
        // 3.3计算录取概率
        double chance = calculateProbability(forecastDTO.getGrade(), forecastDTO.getRanking(), schoolMajorList.get(0).getMinScore(),
                schoolMajorList.get(0).getMinRanking(), sigma);
        int forecast = (int) Math.round(((stable + minimum + 0.2 * rush) / schoolMajorList.size()) * 100);
        schoolForecastVO.setChance(forecast == 0 ? 1 : forecast);
        // 3.4封装不同策略的专业个数
        schoolForecastVO.setMinimum(minimum);
        schoolForecastVO.setStable(stable);
        schoolForecastVO.setRush(rush);
        return schoolForecastVO;
    }


    /**
     * 计算高考录取概率（正态分布模型）
     *
     * @param studentScore  学生分数
     * @param studentRank   学生位次
     * @param majorMinScore 专业历史最低分
     * @param majorMinRank  专业历史最低位次
     * @param sigma         15
     * @return 录取概率
     */
    public static int calculateProbability(
            int studentScore,
            int studentRank,
            int majorMinScore,
            int majorMinRank,
            int sigma) {
        // 步骤1：计算正态分布均值（假设最低分为5%分位点）
        double mu = majorMinScore + 1.645 * sigma; // Z=-1.645对应5%分位
        // 步骤2：计算Z值
        double z = (studentScore - mu) / sigma;
        // 步骤3：计算正态累积概率
        NormalDistribution normalDist = new NormalDistribution(mu, sigma);
        double probability = normalDist.cumulativeProbability(studentScore) * 100;
        // 步骤4：位次校准（最高提升20%）
        if (studentRank < majorMinRank) {
            double rankAdvantage = (double) (majorMinRank - studentRank) / majorMinRank;
            probability += 10 + 10 * rankAdvantage; // 基础10% + 比例提升
            probability = Math.min(probability, 100.0);
        }
        return (int) Math.round(probability);
    }


    /**
     * 获取某学校的历年分数线
     *
     * @param yearScoreDTO 分数线查询DTO
     * @return List<SchoolYearScoreVO>
     */
    @Override
    public List<SchoolYearScoreVO> schoolScoreLine(YearScoreQueryDTO yearScoreDTO) {
        return schoolMapper.selectScoreLineByYear(yearScoreDTO);
    }

    /**
     * 获取某学校专业的历年分数线
     *
     * @param yearScoreQueryDTO 分数线查询DTO
     * @return PageResult<MajorYearScoreVO>
     */
    @Override
    public PageResult<MajorYearScoreVO> majorScoreLine(YearScoreQueryDTO yearScoreQueryDTO) {
        try (Page<MajorYearScoreVO> page = PageHelper.startPage(yearScoreQueryDTO.getPage(), yearScoreQueryDTO.getPageSize())) {
            schoolMapper.selectMajorScoreLine(yearScoreQueryDTO);
            return new PageResult<>(page.getTotal(), page.getResult());
        } catch (Exception e) {
            log.error("查询专业分数线失败，报错为{}", e.getMessage());
            throw new PageQueryException(e.getMessage());
        }
    }

    /**
     * 用户评价大学
     *
     * @param addCommentDTO 用户评价DTO
     */
    @Override
    public void addSchoolComment(AddCommentDTO addCommentDTO) {
        FinderUtil finderUtil = new FinderUtil();
        // 进行敏感词判断
        if (finderUtil.containsSensitiveWord(addCommentDTO.getComment())) {
            log.error(ErrorConstant.SENSITIVE_WORD_ERROR);
            throw new CommentIllegalException(ErrorConstant.SENSITIVE_WORD_ERROR);
        }
        addCommentDTO.setUserId(ThreadLocalUtil.getCurrentId());
        addCommentDTO.setSchoolName(schoolMapper.selectBySchoolId(addCommentDTO.getSchoolId()).getSchoolName());
        schoolMapper.addComment(addCommentDTO);
    }

    /**
     * 查询用户评价
     *
     * @param commentDTO 大学DTO
     * @return List<CommentVO>
     */
    @Override
    public List<CommentVO> showComment(CommentDTO commentDTO) {
        return schoolMapper.selectComment(commentDTO.getSchoolId());
    }

    /**
     * 处理学校数据
     *
     * @param schoolSceneryVOList 学校风光VOList
     */
    private void trimData(List<SchoolSceneryVO> schoolSceneryVOList) {
        for (SchoolSceneryVO schoolSceneryVO : schoolSceneryVOList) {
            String rankString = schoolSceneryVO.getRankList();
            String[] rankList = rankString.split(",");
            if (rankList.length > 3) {
                if (rankList.length == 4) {
                    schoolSceneryVO.setRankList(rankList[0] + "," + rankList[1] + "," + rankList[3]);
                } else {
                    schoolSceneryVO.setRankList(rankList[0] + "," + rankList[3] + "," + rankList[4]);
                }
            }
            if (schoolSceneryVO.getRankItem() == null) {
                continue;
            }
            String rankItem = schoolSceneryVO.getRankItem();
            String rankInfo = schoolSceneryVO.getRankInfo();
            Map<String, String> rank = new HashMap<>();
            String[] splitRankItem = rankItem.split(",");
            String[] splitRankInfo = rankInfo.split(",");
            for (int i = 0; i < splitRankItem.length; i++) {
                rank.put(splitRankItem[i], splitRankInfo[i]);
            }
            schoolSceneryVO.setRank(rank);
            schoolSceneryVO.setRankItem(null);
            schoolSceneryVO.setRankInfo(null);
        }
    }

    /**
     * 获取本省热门本科院校
     *
     * @param provinceQueryDTO 省份查询DTO
     * @return List<SchoolSceneryVO>
     */
    @Override
    public List<SchoolSceneryVO> getSchool1(ProvinceQueryDTO provinceQueryDTO) {
        // 1.先精确查询到本省最好的学校
        SchoolSceneryVO bestSchool = schoolMapper.selectUniqueSchool(provinceQueryDTO.getProvince().getBestSchool());
        List<SchoolMajor> majorList = schoolMapper.selectBestMajor(bestSchool.getSchoolId(), provinceQueryDTO.getProvince().getName());
        // 1.1封装专业列表
        bestSchool.setMajors(majorList);
        // 2.再查询其他学校
        List<SchoolSceneryVO> schoolSceneryVOList = schoolMapper.selectSchoolByProvince
                (provinceQueryDTO.getProvince().getName(), bestSchool.getSchoolName());
        schoolSceneryVOList.add(bestSchool);
        // 3.处理数据
        trimData(schoolSceneryVOList);
        return schoolSceneryVOList;
    }

    /**
     * 获取本省热门专科院校
     *
     * @param provinceQueryDTO 省份查询DTO
     * @return List<SchoolSceneryVO>
     */
    @Override
    public List<SchoolSceneryVO> getSchool2(ProvinceQueryDTO provinceQueryDTO) {
        // 1.先精确查询到本省最好的专科
        SchoolSceneryVO bestProfessional = schoolMapper.selectUniqueSchool(provinceQueryDTO.getProvince().getBestProfessional());
        List<SchoolMajor> majorList = schoolMapper.selectBestMajor(bestProfessional.getSchoolId(), provinceQueryDTO.getProvince().getName());
        // 1.1封装专业列表
        bestProfessional.setMajors(majorList);
        // 2.再查询其他学校
        List<SchoolSceneryVO> schoolSceneryVOList = schoolMapper.selectProfessionalByProvince(provinceQueryDTO.getProvince().getName());
        schoolSceneryVOList.add(bestProfessional);
        // 3.处理数据
        trimData(schoolSceneryVOList);
        return schoolSceneryVOList;
    }

    /**
     * 获取外省热门本科院校
     *
     * @param provinceQueryDTO 省份查询DTO
     * @return List<SchoolSceneryVO>
     */
    @Override
    public List<SchoolSceneryVO> getSchool3(ProvinceQueryDTO provinceQueryDTO) {
        // 1.先精确查询到外省最好的本科
        SchoolSceneryVO bestSchool = schoolMapper.selectOtherProvinceSchool(provinceQueryDTO.getProvince().getBestSchool());
        List<SchoolMajor> majorList = schoolMapper.selectBestMajor(bestSchool.getSchoolId(), provinceQueryDTO.getProvince().getName());
        // 1.1封装专业列表
        bestSchool.setMajors(majorList);
        // 2.再查询其他学校
        List<SchoolSceneryVO> schoolSceneryVOList = schoolMapper.selectWithoutProvince(provinceQueryDTO.getProvince().getName(), bestSchool.getSchoolName());
        schoolSceneryVOList.add(bestSchool);
        // 3.处理数据
        trimData(schoolSceneryVOList);
        return schoolSceneryVOList;
    }

    /**
     * 获取外省热门专科院校
     *
     * @param provinceQueryDTO 省份查询DTO
     * @return List<SchoolSceneryVO>
     */
    @Override
    public List<SchoolSceneryVO> getSchool4(ProvinceQueryDTO provinceQueryDTO) {
        // 1.先精确查询到外省最好的专科
        SchoolSceneryVO bestProfessional = schoolMapper.selectOtherProvinceProfessional(provinceQueryDTO.getProvince().getBestProfessional());
        List<SchoolMajor> majorList = schoolMapper.selectBestMajor(bestProfessional.getSchoolId(), provinceQueryDTO.getProvince().getName());
        // 1.1封装专业列表
        bestProfessional.setMajors(majorList);
        // 2.再查询其他学校
        List<SchoolSceneryVO> schoolSceneryVOList = schoolMapper.selectWithoutProvinceProfessional(provinceQueryDTO.getProvince().getName(), bestProfessional.getSchoolName());
        schoolSceneryVOList.add(bestProfessional);
        // 3.处理数据
        trimData(schoolSceneryVOList);
        return schoolSceneryVOList;
    }

    /**
     * 获取热门专业（本科）
     *
     * @return List<HotMajorVO>
     */
    @Override
    public List<HotMajorVO> getHotMajor() {
        return SchoolConstant.getHotMajor();
    }

    /**
     * 获取热门专业（专科）
     *
     * @return List<HotMajorVO>
     */
    @Override
    public List<HotMajorVO> getHotMajorProfessional() {
        return SchoolConstant.getHotProfessionalMajor();
    }

    /**
     * 获取热门院校排行榜
     *
     * @return List<SchoolVO>
     */
    @Override
    public List<School> getHotRank() {
        return SchoolConstant.getHotRank();
    }

    /**
     * 获取强基计划学校
     *
     * @return List<SchoolVO>
     */
    @Override
    public List<School> getBasicSchool() {
        return schoolMapper.selectBasicSchool();
    }

}
