package esprit.inscription.controller;

import esprit.inscription.dto.MonthlyRevenueRecognitionDTO;
import esprit.inscription.service.RevenueRecognitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/inscription/revenue")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RevenueRecognitionController {

    private final RevenueRecognitionService revenueRecognitionService;

    @GetMapping("/recognition-monthly")
    public List<MonthlyRevenueRecognitionDTO> getMonthlyRevenue(
            @RequestParam(name = "year", required = false) Integer year) {
        int effectiveYear = (year == null || year == 0) ? Year.now().getValue() : year;
        return revenueRecognitionService.getMonthlyRevenue(effectiveYear);
    }
}

