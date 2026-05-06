package tn.esprit.gestioncours.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.gestioncours.DTO.StreamRequestDto;
import tn.esprit.gestioncours.DTO.StreamResponseDto;
import tn.esprit.gestioncours.Entities.NotificationType;
import tn.esprit.gestioncours.Entities.Stream;
import tn.esprit.gestioncours.Entities.StreamStatus;
import tn.esprit.gestioncours.Repositories.StreamRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StreamServiceImpl implements IStreamService {

    private static final String JITSI_BASE_URL = "https://meet.jit.si/GestionCours_Stream_";

    private final StreamRepository streamRepository;
    private final INotificationService notificationService;

    @Override
    public StreamResponseDto createStream(StreamRequestDto request) {
        Stream stream = new Stream();
        applyRequestToEntity(request, stream);

        // Première sauvegarde pour générer l'id
        stream = streamRepository.save(stream);

        // Génération du lien Jitsi basé sur l'id
        String meetingLink = JITSI_BASE_URL + stream.getId();
        stream.setMeetingLink(meetingLink);

        // Deuxième sauvegarde avec le lien Jitsi
        stream = streamRepository.save(stream);

        // Notification
        String message = "Nouveau live planifié : " + stream.getTitle();
        notificationService.createNotificationForUser(1L, message, NotificationType.STREAM);

        return mapToResponseDto(stream);
    }

    @Override
    public StreamResponseDto updateStream(Long id, StreamRequestDto request) {
        return streamRepository.findById(id)
                .map(existing -> {
                    applyRequestToEntity(request, existing);
                    Stream updated = streamRepository.save(existing);
                    return mapToResponseDto(updated);
                })
                .orElse(null);
    }

    @Override
    public boolean deleteStream(Long id) {
        if (!streamRepository.existsById(id)) {
            return false;
        }
        streamRepository.deleteById(id);
        return true;
    }

    @Override
    public List<StreamResponseDto> getAllStreams() {
        return streamRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<StreamResponseDto> getLiveStreams() {
        return streamRepository.findByStatus(StreamStatus.LIVE)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private void applyRequestToEntity(StreamRequestDto request, Stream stream) {
        stream.setTitle(request.getTitle());
        stream.setStartTime(request.getStartTime());

        StreamStatus status = request.getStatus() != null
                ? request.getStatus()
                : StreamStatus.PLANNED;
        stream.setStatus(status);
    }

    private StreamResponseDto mapToResponseDto(Stream stream) {
        return new StreamResponseDto(
                stream.getId(),
                stream.getTitle(),
                stream.getMeetingLink(),
                stream.getStartTime(),
                stream.getStatus()
        );
    }
}
