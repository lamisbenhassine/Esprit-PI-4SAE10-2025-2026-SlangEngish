package tn.esprit.gestioncours.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.gestioncours.DTO.RecordingRequestDto;
import tn.esprit.gestioncours.DTO.RecordingResponseDto;
import tn.esprit.gestioncours.Entities.NotificationType;
import tn.esprit.gestioncours.Entities.Recording;
import tn.esprit.gestioncours.Entities.RecordingAnalysisStatus;
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
    private final RecordingAnalysisAsyncService recordingAnalysisAsyncService;

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
        if (recording.getAnalysisStatus() == null) {
            recording.setAnalysisStatus(RecordingAnalysisStatus.PENDING);
        }

        Recording saved = recordingRepository.save(recording);
        triggerAutoAnalysis(saved);

        String message = "Nouvel enregistrement disponible : " + saved.getTitle();
        notificationService.createNotificationForUser(1L, message, NotificationType.RECORDING);

        return mapToResponseDto(saved);
    }

    @Override
    public RecordingResponseDto updateRecording(Long id, RecordingRequestDto request) {
        return recordingRepository.findById(id)
                .map(existing -> {
                    RecordingStatus previousStatus = existing.getStatus();
                    String previousRecordingLink = existing.getRecordingLink();
                    applyRequestToEntity(request, existing);
                    if (existing.getStatus() == RecordingStatus.AVAILABLE
                            && previousStatus != RecordingStatus.AVAILABLE) {
                        existing.setAnalysisStatus(RecordingAnalysisStatus.PENDING);
                    }
                    if (existing.getRecordingLink() != null
                            && !existing.getRecordingLink().equals(previousRecordingLink)) {
                        existing.setAnalysisStatus(RecordingAnalysisStatus.PENDING);
                    }
                    Recording updated = recordingRepository.save(existing);
                    triggerAutoAnalysis(updated);
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
                    existing.setAnalysisStatus(RecordingAnalysisStatus.PENDING);
                    Recording updated = recordingRepository.save(existing);
                    triggerAutoAnalysis(updated);
                    return mapToResponseDto(updated);
                })
                .orElse(null);
    }

    private void triggerAutoAnalysis(Recording recording) {
        if (recording == null || recording.getId() == null) {
            return;
        }
        if (recording.getStatus() != RecordingStatus.AVAILABLE) {
            return;
        }
        if (recording.getRecordingLink() == null || recording.getRecordingLink().isBlank()) {
            return;
        }
        if (recording.getAnalysisStatus() == RecordingAnalysisStatus.PROCESSING
                || recording.getAnalysisStatus() == RecordingAnalysisStatus.COMPLETED) {
            return;
        }
        recordingAnalysisAsyncService.triggerAnalysisAsync(recording.getId());
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
                recording.getStatus(),
                recording.getAnalysisStatus()
        );
    }
}

