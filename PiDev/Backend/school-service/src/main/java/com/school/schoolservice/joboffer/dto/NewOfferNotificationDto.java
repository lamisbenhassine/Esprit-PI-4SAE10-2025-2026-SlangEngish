package com.school.schoolservice.joboffer.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewOfferNotificationDto {

  private Long id;
  private String title;
  private String company;
  private String location;
  private String contractType;
  private LocalDateTime date;
  private String message;
}
