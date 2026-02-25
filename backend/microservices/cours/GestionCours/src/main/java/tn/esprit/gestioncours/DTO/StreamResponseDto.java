package tn.esprit.gestioncours.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.gestioncours.Entities.StreamStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StreamResponseDto {

    private Long id;

    private String title;

    private String meetingLink;

    private LocalDateTime startTime;

    private StreamStatus status;
}
