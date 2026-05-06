package esprit.inscription.controller;

import esprit.inscription.dto.ProrataCalculateRequest;
import esprit.inscription.dto.ProrataCalculateResponse;
import esprit.inscription.service.ProrataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inscription/prorata")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProrataController {

    private final ProrataService prorataService;

    /**
     * Métier avancé 2: calculate upgrade/downgrade prorata.
     * Returns credit for remaining time, new plan prorata cost, amount to pay or refund.
     */
    @PostMapping("/calculate")
    public ResponseEntity<ProrataCalculateResponse> calculate(@RequestBody ProrataCalculateRequest request) {
        try {
            return ResponseEntity.ok(prorataService.calculateChange(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
