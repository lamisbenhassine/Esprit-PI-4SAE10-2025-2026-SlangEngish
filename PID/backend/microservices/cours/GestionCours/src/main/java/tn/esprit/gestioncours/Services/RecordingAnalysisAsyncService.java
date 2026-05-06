package tn.esprit.gestioncours.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordingAnalysisAsyncService {

    private final RecordingAnalysisService recordingAnalysisService;

    @Transactional
    public boolean triggerAnalysisAsync(Long recordingId) {
        boolean prepared = recordingAnalysisService.prepareForAnalysis(recordingId);
        if (!prepared) {
            return false;
        }
        analyzeRecordingAsync(recordingId);
        return true;
    }

    @Async
    public void analyzeRecordingAsync(Long recordingId) {
        try {
            recordingAnalysisService.analyzeRecording(recordingId);
        } catch (Exception e) {
            log.error("Unexpected async analysis error for recording {}", recordingId, e);
            recordingAnalysisService.markFailed(recordingId);
        }
    }
}
