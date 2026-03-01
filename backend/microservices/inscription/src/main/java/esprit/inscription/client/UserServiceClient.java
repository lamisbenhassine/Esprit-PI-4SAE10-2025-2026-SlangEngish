package esprit.inscription.client;

import esprit.inscription.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "user")
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}")
    User getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/api/users")
    List<User> getAllUsers();

    @GetMapping("/api/users/segment/{segmentName}")
    List<User> getUsersBySegment(@PathVariable("segmentName") String segmentName);

    @GetMapping("/api/users/level/{englishLevel}")
    List<User> getUsersByEnglishLevel(@PathVariable("englishLevel") String englishLevel);

    @GetMapping("/api/users/subscription/{status}")
    List<User> getUsersBySubscriptionStatus(@PathVariable("status") String status);

    @GetMapping("/api/users/inactive/{days}")
    List<User> getInactiveUsers(@PathVariable("days") Integer days);

    @GetMapping("/api/users/revenue/{minRevenue}")
    List<User> getUsersWithMinRevenue(@PathVariable("minRevenue") Double minRevenue);

    @GetMapping("/api/users/progress/{minCompletion}")
    List<User> getUsersWithMinProgress(@PathVariable("minCompletion") Integer minCompletion);
}
