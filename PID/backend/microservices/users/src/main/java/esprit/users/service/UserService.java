package esprit.users.service;

import esprit.users.dto.SigninRequest;
import esprit.users.dto.SignupRequest;
import esprit.users.dto.UserProfileUpdateRequest;
import esprit.users.entity.User;

import java.util.List;

public interface UserService {

    User createUser(User user);

    User signup(SignupRequest request);

    User signin(SigninRequest request);

    User googleSignin(String idToken);

    User facebookSignin(String accessToken);

    void requestPasswordReset(String email);

    void resetPassword(String token, String newPassword);

    User updateUser(Long id, User user);

    User updateUserProfile(Long id, UserProfileUpdateRequest request);

    void deleteUser(Long id);

    User getUserById(Long id);

    List<User> getAllUsers();
}

