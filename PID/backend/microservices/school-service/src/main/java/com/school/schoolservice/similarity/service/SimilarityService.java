package com.school.schoolservice.similarity.service;

import com.school.schoolservice.similarity.dto.SimilarityResultDto;
import java.util.List;

public interface SimilarityService {
    List<SimilarityResultDto> findSimilarOffers(Long offerId, int topN);
}