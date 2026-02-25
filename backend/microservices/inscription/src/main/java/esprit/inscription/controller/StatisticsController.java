package esprit.inscription.controller;

import esprit.inscription.dto.DashboardStatsDTO;
import esprit.inscription.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inscription/stats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    public DashboardStatsDTO getStats() {
        return statisticsService.getDashboardStats();
    }
}
