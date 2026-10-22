package a311.college.service.impl;

import a311.college.auth.TokenPair;
import a311.college.auth.TokenService;
import a311.college.constant.user.UserErrorConstant;
import a311.college.dto.login.RefreshTokenDTO;
import a311.college.dto.login.UserLoginDTO;
import a311.college.dto.query.PageQueryDTO;
import a311.college.dto.user.*;
import a311.college.entity.major.Major;
import a311.college.entity.school.School;
import a311.college.entity.user.User;
import a311.college.exception.*;
import a311.college.filter.FinderUtil;
import a311.college.mapper.user.UserMapper;
import a311.college.constant.redis.UserRedisKey;
import a311.college.regex.RegexUtils;
import a311.college.result.LoginResult;
import a311.college.result.PageResult;
import a311.college.service.UserService;
import a311.college.thread.ThreadLocalUtil;
import a311.college.vo.school.CommentVO;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 用户相关服务
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    /**
     * 旧版本 MD5 密码散列格式（32 位十六进制字符串）
     * 用于识别存量数据，在用户成功登录时将其无缝升级为 BCrypt 散列。
     */
    private static final Pattern LEGACY_MD5_HASH_PATTERN = Pattern.compile("^[0-9a-f]{32}$");

    private final UserMapper userMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final TokenService tokenService;

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper,
                           StringRedisTemplate stringRedisTemplate,
                           TokenService tokenService,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户登录
     * 手机号 + 密码
     *
     * @param userLoginDTO 封装用户登录数据的DTO
     * @return LoginResult 登录返回结果
     */
    @Override
    public LoginResult login(UserLoginDTO userLoginDTO) {
        // 1.获取用户的手机号和密码
        String phone = userLoginDTO.getPhone();
        String password = userLoginDTO.getPassword();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 1.1手机号格式不合法，登录失败
            throw new LoginFailedException(UserErrorConstant.PHONE_NUMBER_ERROR);
        }
        // 2.根据手机号查询用户
        User user = userMapper.selectByPhone(phone);
        // 3.处理异常情况
        if (user == null) {
            // 3.1手机号对应的用户不存在，登录失败
            throw new LoginFailedException(UserErrorConstant.ACCOUNT_NOT_FOUND);
        }
        // 3.2密码比对
        if (!verifyPassword(password, user)) {
            // 3.3密码错误，登录失败
            throw new LoginFailedException(UserErrorConstant.PASSWORD_ERROR);
        }
        // 4.密码校验通过，登录成功
        return loginSuccessful(user);
    }

    /**
     * 校验用户密码，并顺带完成“旧 MD5 散列 → BCrypt 散列”的存量数据升级
     * <p>
     * 优先按 BCrypt 校验；若数据库中仍是旧版 MD5 散列且校验通过，
     * 则立即用 BCrypt 重新散列并写回数据库（登录期间完成，用户无感知）。
     * </p>
     *
     * @param rawPassword 用户输入的明文密码
     * @param user        用户实体（散列升级时需要其手机号定位数据行）
     * @return true 表示密码校验通过
     */
    private boolean verifyPassword(String rawPassword, User user) {
        String storedPassword = user.getPassword();
        // 1.优先按 BCrypt 校验
        if (passwordEncoder.matches(rawPassword, storedPassword)) {
            return true;
        }
        // 2.兼容旧版 MD5 散列：校验通过则无缝升级为 BCrypt
        if (storedPassword != null && LEGACY_MD5_HASH_PATTERN.matcher(storedPassword).matches()) {
            String legacyHash = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
            if (legacyHash.equals(storedPassword)) {
                log.info("检测到旧版MD5密码散列，已校验通过，正在升级为BCrypt");
                userMapper.editPassword(passwordEncoder.encode(rawPassword), user.getPhone());
                return true;
            }
        }
        return false;
    }

    /**
     * 用户登录成功，签发双令牌并返回登录结果
     *
     * @param user 用户
     * @return LoginResult 登录返回结果
     */
    private LoginResult loginSuccessful(User user) {
        LoginResult result = new LoginResult();
        // 开启新会话：签发双令牌，并把会话起始标记写入 Redis（绝对过期计时起点）
        TokenPair tokenPair = tokenService.startSession(user.getId());
        result.setAccessToken(tokenPair.accessToken());
        result.setRefreshToken(tokenPair.refreshToken());
        // 封装用户信息（不返回密码）
        result.setHead(user.getHead());
        result.setUsername(user.getUsername());
        result.setPhone(user.getPhone());
        result.setYear(user.getYear());
        result.setProvince(user.getProvince());
        result.setSubjects(user.getSubjects());
        result.setGrade(user.getGrade());
        result.setRanking(user.getRanking());
        return result;
    }

    /**
     * 用户名检查
     *
     * @param userUsernameCheckDTO 用户名检查DTO
     * @return Integer 0该用户名不可用 1该用户名可用
     */
    @Override
    public Integer checkUsername(UserUsernameCheckDTO userUsernameCheckDTO) {
        User user = userMapper.selectByUsername(userUsernameCheckDTO.getUsername());
        if (user != null) {
            // 该用户名已经存在，不能注册
            return 0;
        }
        return 1;
    }

    /**
     * 手机号检查
     *
     * @param userPhoneCheckDTO 手机号检查DTO
     * @return Integer 0该手机号不可用 1该手机号可用
     */
    @Override
    public Integer checkPhone(UserPhoneCheckDTO userPhoneCheckDTO) {
        User user = userMapper.selectByPhone(userPhoneCheckDTO.getPhone());
        if (user != null) {
            // 该手机号已经存在，不能注册
            return 0;
        }
        return 1;
    }

    /**
     * 用户注册
     *
     * @param userDTO userDTO
     * @return LoginResult 登录返回结果
     */
    @Override
    public LoginResult register(UserDTO userDTO) {
        FinderUtil finderUtil = new FinderUtil();
        if (finderUtil.containsSensitiveWord(userDTO.getUsername())) {
            throw new CommentIllegalException("用户名中含有敏感词");
        }
        User user = new User();
        // 1.1进行属性拷贝
        BeanUtil.copyProperties(userDTO, user);
        // 1.2使用BCrypt对用户密码进行散列后存储
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        String subjects = user.getSubjects();
        user.setFirstChoice(subjects != null && !subjects.isBlank() ? subjects.split(",")[0] : "物理");
        // 1.3数据库中 grade/ranking 为 NOT NULL，客户端未填写时兜底为 0，避免注册插入失败
        if (user.getGrade() == null) {
            user.setGrade(0);
        }
        if (user.getRanking() == null) {
            user.setRanking(0);
        }
        userMapper.register(user);
        return loginSuccessful(user);
    }

    /**
     * 发送验证码
     *
     * @param phone  手机号
     * @param preKey 键前缀
     * @return code 验证码
     */
    private String code(String phone, String preKey) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果手机号不合法，返回错误信息
            throw new LoginFailedException(UserErrorConstant.PHONE_NUMBER_ERROR);
        }
        // 1.发送频控：同一手机号 60 秒内只允许发送一条，防止接口被刷
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
                UserRedisKey.USER_CODE_LIMIT_KEY + phone, "1",
                UserRedisKey.USER_CODE_SEND_INTERVAL, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(acquired)) {
            throw new LoginFailedException("发送过于频繁，请稍后再试");
        }
        // 3.手机号合法，生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 4.将验证码保存至redis（写入即带过期，避免残留）
        stringRedisTemplate.opsForValue().set(preKey + phone, code,
                UserRedisKey.USER_CODE_TTL, TimeUnit.SECONDS);
        // 5.发送验证码（短信功能待完成）
        //TODO 后期如果有机会可以将其改为真实的发送手机验证码
        log.info("发送短信验证码成功，验证码为：{}", code);
        log.info(UserRedisKey.CODE_TIME_MESSAGE);
        // 6.响应结果
        return code;
    }

    /**
     * 验证码统一校验：连续失败锁定 + 验证通过后立即作废（一次性使用）。
     * 防止 60 秒有效窗口内重放同一验证码，以及无限次试错爆破。
     *
     * @param preKey 验证码键前缀
     * @param phone  手机号
     * @param code   用户提交的验证码
     */
    private void verifyAndConsumeCode(String preKey, String phone, String code) {
        String failKey = UserRedisKey.USER_CODE_FAIL_KEY + phone;
        String fails = stringRedisTemplate.opsForValue().get(failKey);
        if (fails != null && Integer.parseInt(fails) >= UserRedisKey.USER_CODE_FAIL_LIMIT) {
            throw new PasswordEditFailedException("验证码错误次数过多，请10分钟后再试");
        }
        String cacheCode = stringRedisTemplate.opsForValue().get(preKey + phone);
        if (code == null || !code.equals(cacheCode)) {
            Long count = stringRedisTemplate.opsForValue().increment(failKey);
            if (count != null && count == 1) {
                stringRedisTemplate.expire(failKey, UserRedisKey.USER_CODE_FAIL_TTL, TimeUnit.SECONDS);
            }
            throw new PasswordEditFailedException(UserErrorConstant.CODE_ERROR);
        }
        // 校验通过：验证码立即作废（一次性），同时清空失败计数
        stringRedisTemplate.delete(preKey + phone);
        stringRedisTemplate.delete(failKey);
    }

    /**
     * 发送验证码修改密码
     *
     * @param userCodeDTO 验证码DTO
     * @return code 验证码
     */
    @Override
    public String sendEditCode(UserCodeDTO userCodeDTO) {
        return code(userCodeDTO.getPhone(), UserRedisKey.USER_EDIT_CODE_KEY);
    }

    /**
     * 用户修改密码
     * <p>
     * 验证码校验通过后，使用 BCrypt 散列新密码并写库；随后吊销旧会话
     * （删除刷新令牌、将当前访问令牌加入黑名单），最后签发新令牌返回，
     * 防止改密后旧登录状态继续可用。
     * </p>
     *
     * @param userEditPasswordDTO 修改密码DTO
     * @return LoginResult 新的登录结果
     */
    @Override
    public LoginResult editPassword(UserEditPasswordDTO userEditPasswordDTO) {
        String phone = userEditPasswordDTO.getPhone();
        String code = userEditPasswordDTO.getCode();
        // 1.判断手机号是否合法
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 1.1.如果手机号不合法，返回错误信息
            throw new PasswordEditFailedException(UserErrorConstant.PHONE_NUMBER_ERROR);
        }
        // 2.验证码校验（失败锁定 + 通过后立即作废，防止窗口期内重放）
        verifyAndConsumeCode(UserRedisKey.USER_EDIT_CODE_KEY, phone, code);
        // 3.手机号和验证码比对成功，使用BCrypt散列新密码并写库
        userMapper.editPassword(passwordEncoder.encode(userEditPasswordDTO.getNewPassword()), phone);
        // 4.找回密码是匿名流程（无登录态），必须按手机号定位目标账号并吊销其全部会话：
        // 否则盗号者改密后，被盗账号的旧会话依然有效
        User user = userMapper.selectByPhone(phone);
        if (user != null) {
            tokenService.revokeAllSessions(user.getId(), ThreadLocalUtil.getCurrentToken());
        }
        return loginSuccessful(user);
    }

    /**
     * 刷新令牌：用刷新令牌换取新的双令牌（刷新令牌轮换）
     *
     * @param refreshTokenDTO 刷新令牌DTO
     * @return LoginResult 新的登录结果
     */
    @Override
    public LoginResult refresh(RefreshTokenDTO refreshTokenDTO) {
        TokenPair tokenPair = tokenService.refreshTokens(refreshTokenDTO.getRefreshToken());
        User user = userMapper.selectById(tokenPair.userId());
        if (user == null) {
            // 刷新令牌对应的用户已被注销
            throw new TokenInvalidException("刷新令牌无效或已过期，请重新登录");
        }
        LoginResult result = new LoginResult();
        result.setAccessToken(tokenPair.accessToken());
        result.setRefreshToken(tokenPair.refreshToken());
        result.setHead(user.getHead());
        result.setUsername(user.getUsername());
        result.setPhone(user.getPhone());
        result.setYear(user.getYear());
        result.setProvince(user.getProvince());
        result.setSubjects(user.getSubjects());
        result.setGrade(user.getGrade());
        result.setRanking(user.getRanking());
        return result;
    }

    /**
     * 用户登出
     * <p>
     * 删除 Redis 中的刷新令牌，并把当前访问令牌加入黑名单，
     * 使本次登录会话彻底失效。
     * </p>
     */
    @Override
    public void layout() {
        tokenService.revokeSession(ThreadLocalUtil.getCurrentId(), ThreadLocalUtil.getCurrentToken());
    }

    /**
     * 展示用户主页
     *
     * @param id 用户id
     * @return User
     */
    @Override
    public User showMe(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 展示用户收藏学校
     *
     * @return List<BriefSchoolInfoVO>
     */
    @Override
    public PageResult<School> showFavoriteSchool(PageQueryDTO pageQueryDTO) {
        Long userId = ThreadLocalUtil.getCurrentId();
        try (Page<School> page = PageHelper.startPage(pageQueryDTO.getPage(), pageQueryDTO.getPageSize())) {
            List<School> schoolList = userMapper.getUserFavoriteSchool(userId);
            return new PageResult<>(page.getTotal(), schoolList);
        } catch (Exception e) {
            log.error("用户收藏学校分页查询失败，报错为：{}", e.getMessage());
            throw new PageQueryException(UserErrorConstant.USER_SCHOOL_ERROR);
        }
    }

    /**
     * 展示用户收藏专业
     *
     * @return PageResult<Major>
     */
    @Override
    public PageResult<Major> showFavoriteMajor(PageQueryDTO pageQueryDTO) {
        Long userId = ThreadLocalUtil.getCurrentId();
        try (Page<Major> page = PageHelper.startPage(pageQueryDTO.getPage(), pageQueryDTO.getPageSize())) {
            List<Major> majorList = userMapper.getUserFavoriteMajor(userId);
            return new PageResult<>(page.getTotal(), majorList);
        } catch (Exception e) {
            log.error("用户收藏专业分页查询失败，报错为：{}", e.getMessage());
            throw new PageQueryException(UserErrorConstant.USER_MAJOR_ERROR);
        }
    }

    /**
     * 查看用户评论
     *
     * @return List<String> 用户评论列表
     */
    @Override
    public PageResult<CommentVO> showSchoolComment(PageQueryDTO pageQueryDTO) {
        try (Page<CommentVO> page = PageHelper.startPage(pageQueryDTO.getPage(), pageQueryDTO.getPageSize())) {
            List<CommentVO> commentList = userMapper.selectSchoolComment(ThreadLocalUtil.getCurrentId());
            return new PageResult<>(page.getTotal(), commentList);
        } catch (Exception e) {
            log.error("用户学校评论分页查询失败，报错为：{}", e.getMessage());
            throw new PageQueryException(UserErrorConstant.USER_COMMENT_ERROR);
        }
    }

    /**
     * 查看用户评论
     *
     * @return List<String> 用户评论列表
     */
    @Override
    public PageResult<CommentVO> showMajorComment(PageQueryDTO pageQueryDTO) {
        try (Page<CommentVO> page = PageHelper.startPage(pageQueryDTO.getPage(), pageQueryDTO.getPageSize())) {
            List<CommentVO> commentList = userMapper.selectMajorComment(ThreadLocalUtil.getCurrentId());
            return new PageResult<>(page.getTotal(), commentList);
        } catch (Exception e) {
            log.error("用户专业评论分页查询失败，报错为：{}", e.getMessage());
            throw new PageQueryException(UserErrorConstant.USER_COMMENT_ERROR);
        }
    }

    /**
     * 删除用户评论
     *
     * @param userCommentDTO 用户评论DTO
     */
    @Override
    public void deleteComment(UserCommentDTO userCommentDTO) {
        // 限定只删本人评论：任何登录用户不得凭 commentId 越权删除他人评论
        userMapper.deleteComment(userCommentDTO.getCommentId(), ThreadLocalUtil.getCurrentId());
    }


    /**
     * 修改用户信息
     *
     * @param userDTO 用户DTO
     */
    @Override
    public void update(UserDTO userDTO) {
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setId(ThreadLocalUtil.getCurrentId());
        // 选科变化时同步首选科目（subjects 首项），避免预测/推荐按旧首选过滤
        if (userDTO.getSubjects() != null && !userDTO.getSubjects().isBlank()) {
            user.setFirstChoice(userDTO.getSubjects().split(",")[0]);
        }
        userMapper.update(user);
    }

    /**
     * 发送验证码进行注销
     *
     * @param userCodeDTO 验证码DTO
     * @return code 验证码
     */
    @Override
    public String sendDeleteCode(UserCodeDTO userCodeDTO) {
        return code(userCodeDTO.getPhone(), UserRedisKey.USER_DELETE_CODE_KEY);
    }

    /**
     * 用户注销
     *
     * @param userDeleteDTO 用户注销DTO
     */
    @Override
    @Transactional
    public void deleteUser(UserDeleteDTO userDeleteDTO) {
        // 1.获取当前用户id
        long id = ThreadLocalUtil.getCurrentId();
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new OperateNotAllowException("用户不存在");
        }
        // 2.绑定校验：验证码手机号必须与当前登录账号一致，
        // 防止拿任意手机号的验证码注销当前登录的账号
        if (user.getPhone() == null || !user.getPhone().equals(userDeleteDTO.getPhone())) {
            throw new OperateNotAllowException("手机号与当前登录账号不一致");
        }
        // 3.验证码校验（失败锁定 + 通过后立即作废）
        verifyAndConsumeCode(UserRedisKey.USER_DELETE_CODE_KEY, userDeleteDTO.getPhone(), userDeleteDTO.getCode());
        // 4.验证码比对成功，吊销该用户全部会话（注销 = 全端下线）
        tokenService.revokeAllSessions(id, ThreadLocalUtil.getCurrentToken());
        // 5.清理该用户全部业务数据（收藏/评论/志愿/志愿表/AI 记录），避免孤儿数据
        userMapper.deleteFavSchoolsByUser(id);
        userMapper.deleteFavMajorsByUser(id);
        userMapper.deleteCommentsByUser(id);
        userMapper.deleteVolunteersByUser(id);
        userMapper.deleteVolunteerTablesByUser(id);
        userMapper.deleteAgentRecordsByUser(id);
        // 6.注销用户
        userMapper.deleteById(id);
    }

}
