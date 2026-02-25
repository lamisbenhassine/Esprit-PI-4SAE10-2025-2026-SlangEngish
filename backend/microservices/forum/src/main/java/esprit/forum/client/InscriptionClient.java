package esprit.forum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "inscription")
public interface InscriptionClient {

    @GetMapping("/api/inscription/payment/verify/{userId}")
    Boolean isUserPaid(@PathVariable("userId") Long userId);
}
