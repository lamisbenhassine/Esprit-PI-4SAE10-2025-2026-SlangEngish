package com.school.schoolservice.matching.service;

import com.school.schoolservice.matching.dto.MatchingResultDto;
import com.school.schoolservice.matching.dto.VisitorProfileDto;
import java.util.List;

public interface MatchingService {
    List<MatchingResultDto> getMatchingOffersForVisitor(VisitorProfileDto visitor);
    MatchingResultDto getMatchScore(Long offerId, VisitorProfileDto visitor);
}