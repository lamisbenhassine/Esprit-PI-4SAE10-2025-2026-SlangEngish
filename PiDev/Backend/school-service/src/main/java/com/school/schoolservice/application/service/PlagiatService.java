package com.school.schoolservice.application.service;

import com.school.schoolservice.application.dto.PlagiatResultDto;
import java.util.List;

public interface PlagiatService {
    List<PlagiatResultDto> detectPlagiat(Long jobOfferId);
}