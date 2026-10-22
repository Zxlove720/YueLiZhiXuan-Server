package a311.college.mapper.user;

import a311.college.annotation.AutoFill;
import a311.college.entity.major.Major;
import a311.college.entity.school.School;
import a311.college.entity.user.User;
import a311.college.enumeration.OperationType;
import a311.college.vo.school.CommentVO;
import org.apache.ibatis.annotations.*;

import java.util.List;


/**
 * 用户相关Mapper
 */
@Mapper
public interface UserMapper {

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return User实体对象
     */
    @Select("select * from tb_user where phone = #{phone}")
    User selectByPhone(String phone);

    /**
     * 根据用户id查询用户
     *
     * @param id 用户id
     * @return User实体对象
     */
    @Select("select * from tb_user where id = #{id}")
    User selectById(Long id);

    /**
     * 根据id删除用户
     *
     * @param id 用户id
     */
    @Delete("delete from tb_user where id = #{id}")
    void deleteById(Long id);

    /**
     * 注销账号时清理该用户的全部业务数据，避免遗留孤儿行。
     * 覆盖：院校收藏、专业收藏、评论、志愿、志愿表、AI 会话记录。
     *
     * @param id 用户id
     */
    @Delete("delete from tb_fav_school where user_id = #{id}")
    void deleteFavSchoolsByUser(Long id);

    @Delete("delete from tb_fav_major where user_id = #{id}")
    void deleteFavMajorsByUser(Long id);

    @Delete("delete from tb_comment where user_id = #{id}")
    void deleteCommentsByUser(Long id);

    @Delete("delete from tb_volunteer where user_id = #{id}")
    void deleteVolunteersByUser(Long id);

    @Delete("delete from tb_volunteer_table where user_id = #{id}")
    void deleteVolunteerTablesByUser(Long id);

    @Delete("delete from tb_agent_chat_record where user_id = #{id}")
    void deleteAgentRecordsByUser(Long id);

    /**
     * 用户注册
     *
     * @param user User实体对象
     */
    @AutoFill(OperationType.INSERT)
    @Insert("insert into tb_user(username, password, phone, head, year, province, firstChoice, subjects," +
            "  grade, ranking) values " +
            "(#{username}, #{password}, #{phone}, #{head}, #{year}, #{province}, #{firstChoice}, #{subjects}," +
            " #{grade}, #{ranking})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void register(User user);

    /**
     * 更新用户信息
     *
     * @param user User实体对象
     */
    void update(User user);

    /**
     * 用户修改密码
     *
     * @param newPassword 新密码
     * @param phone       手机号
     */
    @AutoFill(OperationType.UPDATE)
    @Update("update tb_user set password = #{newPassword} where phone = #{phone}")
    void editPassword(String newPassword, String phone);

    /**
     * 查询用户收藏学校表
     *
     * @param userId 用户id
     * @return 用户收藏学校表
     */
    @Select("select * from tb_school where school_id in (select tb_fav_school.school_id from tb_fav_school where user_id = #{userId})")
    List<School> getUserFavoriteSchool(Long userId);

    /**
     * 查询用户收藏专业表
     *
     * @param userId 用户id
     * @return 用户收藏专业表
     */
    List<Major> getUserFavoriteMajor(Long userId);

    /**
     * 分页查询用户学校评论
     *
     * @param id 用户id
     * @return List<CommentVO>
     */
    @Select(("select comment_id, school_id, school_name, comment, add_time from tb_comment where user_id = #{id} and school_id is not null"))
    List<CommentVO> selectSchoolComment(Long id);

    /**
     * 分页查询用户专业评论
     *
     * @param id 用户id
     * @return List<CommentVO>
     */
    @Select(("select comment_id, major_id, major_name, comment, add_time from tb_comment where user_id = #{id} and major_id is not null"))
    List<CommentVO> selectMajorComment(Long id);

    /**
     * 删除当前登录用户自己的评论（限定 user_id，防止越权删除他人评论）
     *
     * @param commentId 评论id
     * @param userId 当前登录用户id
     */
    @Delete("delete from tb_comment where comment_id = #{commentId} and user_id = #{userId}")
    void deleteComment(Integer commentId, Long userId);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return User实体对象
     */
    @Select("select id from tb_user where username = #{username}")
    User selectByUsername(String username);

    @Select("select * from tb_user")
    List<User> getAllUser();

}
