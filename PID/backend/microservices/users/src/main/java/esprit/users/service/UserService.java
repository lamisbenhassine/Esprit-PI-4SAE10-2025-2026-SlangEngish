package esprit.users.service;

import esprit.users.dto.SigninRequest;
import esprit.users.dto.SignupRequest;
import esprit.users.dto.UserProfileUpdateRequest;
import esprit.users.entity.Role;
import esprit.users.entity.Status;
import esprit.users.entity.User;

import java.util.List;

public interface UserService {

    User createUser(User user);

    /** Recherche dynamique : search (nom, prénom, email), role, status (null/empty = tous). */
    List<User> searchUsers(String search, String role, String status);

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

    /** Seul un ADMIN peut bloquer/débloquer un utilisateur. */
    User setUserStatus(Long userId, Long adminId, Status status);
}

