package esprit.users.service;

import esprit.users.dto.SigninRequest;
import esprit.users.dto.SignupRequest;
import esprit.users.entity.User;

import java.util.List;

public interface UserService {

    User createUser(User user);

    User signup(SignupRequest request);

    User signin(SigninRequest request);

    User updateUser(Long id, User user);

    void deleteUser(Long id);

    User getUserById(Long id);

    List<User> getAllUsers();
}

