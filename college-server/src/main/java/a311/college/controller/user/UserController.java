package a311.college.controller.user;

import a311.college.constant.API.APIConstant;
import a311.college.dto.query.PageQueryDTO;
import a311.college.dto.user.*;
import a311.college.dto.login.UserLoginDTO;
import a311.college.dto.login.RefreshTokenDTO;
import a311.college.entity.major.Major;
import a311.college.entity.school.School;
import a311.college.entity.user.User;
import a311.college.result.LoginResult;
import a311.college.result.PageResult;
import a311.college.result.Result;
import a311.college.service.UserService;
import a311.college.thread.ThreadLocalUtil;
import a311.college.vo.school.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户请求控制器
 */
@Slf4j
@RestController
@RequestMapping("/users")
@Tag(name = APIConstant.USER_SERVICE)
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户登录
     * 用户的手机号 + 密码登录
     *
     * @param userLoginDTO 用户登录数据DTO
     * @return Result<LoginResult>
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginResult> login(@RequestBody UserLoginDTO userLoginDTO) {
        String phone = userLoginDTO.getPhone();
        log.info("手机号为'{}'的用户正在登录", phone);
        return Result.success(userService.login(userLoginDTO));
    }

    /**
     * 用户名检查
     * 注册时检查用户名是否可用
     *
     * @param userUsernameCheckDTO 用户名检查DTO
     * @return Integer 0该用户名不可用 1该用户名可用
     */
    @PostMapping("/checkUsername")
    @Operation(summary = "注册时检查用户名是否可用")
    public Result<Integer> checkUsername(@RequestBody UserUsernameCheckDTO userUsernameCheckDTO) {
        return Result.success(userService.checkUsername(userUsernameCheckDTO));
    }

    /**
     * 手机号检查
     * 注册时检查手机号是否可用
     *
     * @param userPhoneCheckDTO 手机号检查DTO
     * @return Integer 0该手机号不可用 1该手机号可用
     */
    @PostMapping("/checkPhone")
    @Operation(summary = "注册时检查手机号是否可用")
    public Result<Integer> checkPhone(@RequestBody UserPhoneCheckDTO userPhoneCheckDTO) {
        return Result.success(userService.checkPhone(userPhoneCheckDTO));
    }

    /**
     * 用户注册
     *
     * @param userDTO 用户DTO
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<LoginResult> register(@RequestBody UserDTO userDTO) {
        log.info("新用户注册...");
        return Result.success(userService.register(userDTO));
    }

    /**
     * 用户发送验证码修改密码
     *
     * @param userCodeDTO 验证码DTO
     * @return Result<String> 验证码
     */
    @PostMapping("/editCode")
    @Operation(summary = "用户发送验证码修改密码")
    public Result<String> sendEditCode(@RequestBody UserCodeDTO userCodeDTO) {
        log.info("向手机号为'{}'的用户发送验证码，其正在修改密码", userCodeDTO.getPhone());
        return Result.success(userService.sendEditCode(userCodeDTO));
    }

    /**
     * 用户修改密码
     *
     * @param userEditPasswordDTO 密码修改DTO
     * @return Result<LoginResult>
     */
    @PostMapping("/edit")
    @Operation(summary = "用户修改密码")
    public Result<LoginResult> editPassword(@RequestBody UserEditPasswordDTO userEditPasswordDTO) {
        log.info("用户'{}'正在修改密码", ThreadLocalUtil.getCurrentId());
        return Result.success(userService.editPassword(userEditPasswordDTO));
    }

    /**
     * 刷新令牌
     * <p>
     * 访问令牌（JWT）过期后，客户端携带刷新令牌请求新的双令牌。
     * 每次刷新会轮换刷新令牌，旧刷新令牌随即失效。
     * </p>
     *
     * @param refreshTokenDTO 刷新令牌DTO
     * @return Result<LoginResult>
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌")
    public Result<LoginResult> refresh(@RequestBody RefreshTokenDTO refreshTokenDTO) {
        log.info("用户正在刷新令牌");
        return Result.success(userService.refresh(refreshTokenDTO));
    }

    /**
     * 用户退出
     *
     * @return Result<Void>
     */
    @PostMapping("/layout")
    @Operation(summary = "用户登出")
    public Result<Void> layout() {
        log.info("用户'{}'退出登录", ThreadLocalUtil.getCurrentId());
        userService.layout();
        return Result.success();
    }

    /**
     * 用户个人主页
     *
     * @return Result<UserVO>
     */
    @PostMapping("/me")
    @Operation(summary = "查看用户个人主页")
    public Result<User> me() {
        Long userId = ThreadLocalUtil.getCurrentId();
        log.info("用户'{}'正在查看个人页面", userId);
        return Result.success(userService.showMe(userId));
    }

    /**
     * 分页展示用户收藏学校
     *
     * @return Result<List < BriefSchoolInfoVO>>
     */
    @PostMapping("/showSchool")
    @Operation(summary = "分页展示用户收藏学校")
    public Result<PageResult<School>> showSchool(@RequestBody PageQueryDTO pageQueryDTO) {
        log.info("展示用户'{}'收藏学校", ThreadLocalUtil.getCurrentId());
        return Result.success(userService.showFavoriteSchool(pageQueryDTO));
    }

    /**
     * 分页展示用户收藏专业
     *
     * @return Result<List < BriefSchoolInfoVO>>
     */
    @PostMapping("/showMajor")
    @Operation(summary = "分页展示用户收藏专业")
    public Result<PageResult<Major>> showMajor(@RequestBody PageQueryDTO pageQueryDTO) {
        log.info("展示用户'{}'收藏专业", ThreadLocalUtil.getCurrentId());
        return Result.success(userService.showFavoriteMajor(pageQueryDTO));
    }

    /**
     * 分页查询用户学校评论
     *
     * @param pageQueryDTO 分页查询DTO
     * @return Result<PageResult<CommentVO>> 评论列表
     */
    @PostMapping("/schoolComment")
    @Operation(summary = "分页查询用户评论")
    public Result<PageResult<CommentVO>> showSchoolComment(@RequestBody PageQueryDTO pageQueryDTO) {
        log.info("用户'{}'正在查看学校评论", ThreadLocalUtil.getCurrentId());
        return Result.success(userService.showSchoolComment(pageQueryDTO));
    }

    /**
     * 分页查询用户专业评论
     *
     * @param pageQueryDTO 分页查询DTO
     * @return Result<PageResult<CommentVO>> 评论列表
     */
    @PostMapping("/majorComment")
    @Operation(summary = "分页查询用户评论")
    public Result<PageResult<CommentVO>> showMajorComment(@RequestBody PageQueryDTO pageQueryDTO) {
        log.info("用户'{}'正在查看专业评论", ThreadLocalUtil.getCurrentId());
        return Result.success(userService.showMajorComment(pageQueryDTO));
    }

    /**
     * 删除用户评论
     *
     * @param userCommentDTO 用户评论DTO
     * @return Result<Void>
     */
    @PostMapping("/deleteComment")
    @Operation(summary = "删除用户评论")
    public Result<Void> deleteComment(@RequestBody UserCommentDTO userCommentDTO) {
        userService.deleteComment(userCommentDTO);
        return Result.success();
    }

    /**
     * 修改用户信息
     *
     * @param userDTO 用户DTO
     * @return Result<Void>
     */
    @PostMapping("/update")
    @Operation(summary = "修改用户信息")
    public Result<Void> updateUser(@RequestBody UserDTO userDTO) {
        log.info("用户'{}'正在修改信息...", userDTO.getId());
        userService.update(userDTO);
        return Result.success();
    }

    /**
     * 用户注销发送验证码
     *
     * @param userCodeDTO 验证码DTO
     * @return code 验证码
     */
    @PostMapping("/deleteCode")
    @Operation(summary = "用户注销发送验证码")
    public Result<String> sendDeleteCode(@RequestBody UserCodeDTO userCodeDTO) {
        log.info("用户'{}'正在进行注销操作", ThreadLocalUtil.getCurrentId());
        return Result.success(userService.sendDeleteCode(userCodeDTO));
    }

    /**
     * 用户注销
     *
     * @param userDeleteDTO 用户注销DTO
     */
    @PostMapping("/delete")
    @Operation(summary = "用户注销")
    public Result<Void> deleteUser(@RequestBody UserDeleteDTO userDeleteDTO) {
        log.info("手机号为'{}'的用户正在注销", userDeleteDTO.getPhone());
        userService.deleteUser(userDeleteDTO);
        return Result.success();
    }

}
