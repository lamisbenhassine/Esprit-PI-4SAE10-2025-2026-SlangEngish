package com.school.schoolservice.matching.service;

import com.school.schoolservice.matching.dto.MatchingResultDto;
import java.util.List;

public interface MatchingService {
    List<MatchingResultDto> getMatchingOffers(Long studentId);
    MatchingResultDto getMatchScore(Long studentId, Long offerId);
}