package a311.college.service;

import a311.college.dto.query.PageQueryDTO;
import a311.college.dto.user.*;
import a311.college.dto.login.UserLoginDTO;
import a311.college.dto.login.RefreshTokenDTO;
import a311.college.entity.major.Major;
import a311.college.entity.school.School;
import a311.college.entity.user.User;
import a311.college.result.LoginResult;
import a311.college.result.PageResult;
import a311.college.vo.school.CommentVO;

/**
 * 用户相关服务
 */
public interface UserService {

    LoginResult login(UserLoginDTO userLoginDTO);

    Integer checkUsername(UserUsernameCheckDTO userUsernameCheckDTO);

    Integer checkPhone(UserPhoneCheckDTO userPhoneCheckDTO);

    LoginResult register(UserDTO userDTO);

    String sendEditCode(UserCodeDTO userCodeDTO);

    LoginResult editPassword(UserEditPasswordDTO userEditPasswordDTO);

    LoginResult refresh(RefreshTokenDTO refreshTokenDTO);

    void layout();

    User showMe(Long id);

    PageResult<School> showFavoriteSchool(PageQueryDTO pageQueryDTO);

    PageResult<Major> showFavoriteMajor(PageQueryDTO pageQueryDTO);

    PageResult<CommentVO> showSchoolComment(PageQueryDTO pageQueryDTO);

    PageResult<CommentVO> showMajorComment(PageQueryDTO pageQueryDTO);

    void deleteComment(UserCommentDTO userCommentDTO);

    void update(UserDTO userDTO);

    String sendDeleteCode(UserCodeDTO userCodeDTO);

    void deleteUser(UserDeleteDTO userDeleteDTO);


}
