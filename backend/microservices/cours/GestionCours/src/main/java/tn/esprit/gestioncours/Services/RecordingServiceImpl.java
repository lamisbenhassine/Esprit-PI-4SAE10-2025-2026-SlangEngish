package tn.esprit.gestioncours.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.gestioncours.DTO.RecordingRequestDto;
import tn.esprit.gestioncours.DTO.RecordingResponseDto;
import tn.esprit.gestioncours.Entities.NotificationType;
import tn.esprit.gestioncours.Entities.Recording;
import tn.esprit.gestioncours.Entities.RecordingStatus;
import tn.esprit.gestioncours.Repositories.RecordingRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecordingServiceImpl implements IRecordingService {

    private final RecordingRepository recordingRepository;
    private final INotificationService notificationService;

    @Override
    public RecordingResponseDto createRecording(RecordingRequestDto request) {
        Recording recording = new Recording();
        applyRequestToEntity(request, recording);

        if (recording.getRecordedAt() == null) {
            recording.setRecordedAt(LocalDateTime.now());
        }
        if (recording.getStatus() == null) {
            recording.setStatus(RecordingStatus.AVAILABLE);
        }

        Recording saved = recordingRepository.save(recording);

        String message = "Nouvel enregistrement disponible : " + saved.getTitle();
        notificationService.createNotificationForUser(1L, message, NotificationType.RECORDING);

        return mapToResponseDto(saved);
    }

    @Override
    public RecordingResponseDto updateRecording(Long id, RecordingRequestDto request) {
        return recordingRepository.findById(id)
                .map(existing -> {
                    applyRequestToEntity(request, existing);
                    Recording updated = recordingRepository.save(existing);
                    return mapToResponseDto(updated);
                })
                .orElse(null);
    }

    @Override
    public boolean deleteRecording(Long id) {
        if (!recordingRepository.existsById(id)) {
            return false;
        }
        recordingRepository.deleteById(id);
        return true;
    }

    @Override
    public List<RecordingResponseDto> getAllRecordings() {
        return recordingRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<RecordingResponseDto> getAvailableRecordings() {
        return recordingRepository.findByStatus(RecordingStatus.AVAILABLE)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public RecordingResponseDto attachFile(Long id, String recordingLink) {
        return recordingRepository.findById(id)
                .map(existing -> {
                    existing.setRecordingLink(recordingLink);
                    if (existing.getRecordedAt() == null) {
                        existing.setRecordedAt(LocalDateTime.now());
                    }
                    if (existing.getStatus() == null) {
                        existing.setStatus(RecordingStatus.AVAILABLE);
                    }
                    Recording updated = recordingRepository.save(existing);
                    return mapToResponseDto(updated);
                })
                .orElse(null);
    }

    private void applyRequestToEntity(RecordingRequestDto request, Recording recording) {
        recording.setTitle(request.getTitle());
        recording.setStreamLink(request.getStreamLink());
        recording.setRecordingLink(request.getRecordingLink());
        recording.setRecordedAt(request.getRecordedAt());
        recording.setStatus(request.getStatus());
    }

    private RecordingResponseDto mapToResponseDto(Recording recording) {
        return new RecordingResponseDto(
                recording.getId(),
                recording.getTitle(),
                recording.getStreamLink(),
                recording.getRecordingLink(),
                recording.getRecordedAt(),
                recording.getStatus()
        );
    }
}

