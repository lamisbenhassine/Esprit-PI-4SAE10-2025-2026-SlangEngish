package esprit.forum.client;

import esprit.forum.client.dto.UserProfileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user")
public interface UserClient {

    @GetMapping("/api/user/{id}")
    UserProfileDto getUser(@PathVariable("id") Long id);
}
