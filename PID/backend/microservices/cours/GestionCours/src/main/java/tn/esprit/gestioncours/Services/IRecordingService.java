package tn.esprit.gestioncours.Services;

import tn.esprit.gestioncours.DTO.RecordingRequestDto;
import tn.esprit.gestioncours.DTO.RecordingResponseDto;

import java.util.List;

public interface IRecordingService {

    RecordingResponseDto createRecording(RecordingRequestDto request);

    RecordingResponseDto updateRecording(Long id, RecordingRequestDto request);

    boolean deleteRecording(Long id);

    List<RecordingResponseDto> getAllRecordings();

    List<RecordingResponseDto> getAvailableRecordings();

    RecordingResponseDto attachFile(Long id, String recordingLink);
}

