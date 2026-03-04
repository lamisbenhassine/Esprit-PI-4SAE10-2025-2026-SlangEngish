package tn.esprit.gestioncours.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.gestioncours.Entities.RecordingStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecordingRequestDto {

    private String title;

    private String streamLink;

    private String recordingLink;

    private LocalDateTime recordedAt;

    private RecordingStatus status;
}

