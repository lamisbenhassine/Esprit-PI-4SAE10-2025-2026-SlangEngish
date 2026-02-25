package tn.esprit.gestioncours.Services;

import tn.esprit.gestioncours.DTO.StreamRequestDto;
import tn.esprit.gestioncours.DTO.StreamResponseDto;

import java.util.List;

public interface IStreamService {

    StreamResponseDto createStream(StreamRequestDto request);

    StreamResponseDto updateStream(Long id, StreamRequestDto request);

    boolean deleteStream(Long id);

    List<StreamResponseDto> getAllStreams();

    List<StreamResponseDto> getLiveStreams();
}
