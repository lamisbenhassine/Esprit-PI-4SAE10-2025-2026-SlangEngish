package tn.esprit.gestioncours.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.gestioncours.Entities.Chapter;
import tn.esprit.gestioncours.Entities.Progression;
import tn.esprit.gestioncours.Repositories.ChapterRepository;
import tn.esprit.gestioncours.Repositories.ProgressionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProgressionServiceImpl implements IProgressionService {

    private final ProgressionRepository progressionRepository;
    private final ChapterRepository chapterRepository;

    @Override
    public Progression saveOrUpdateProgress(Long userId,
                                            Long chapterId,
                                            Integer lastPage,
                                            Double progressPercentage) {

        Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
        if (chapter == null) {
            return null;
        }

        Progression progression = progressionRepository
                .findByUserIdAndChapter_IdChapter(userId, chapterId)
                .orElseGet(Progression::new);

        progression.setUserId(userId);
        progression.setChapter(chapter);
        progression.setLastPage(lastPage);
        progression.setProgressPercentage(progressPercentage);

        return progressionRepository.save(progression);
    }

    @Override
    public Progression getProgressForChapter(Long userId, Long chapterId) {
        return progressionRepository
                .findByUserIdAndChapter_IdChapter(userId, chapterId)
                .orElse(null);
    }

    @Override
    public Double getCourseProgress(Long userId, Long courseId) {
        List<Chapter> chapters = chapterRepository.findByCourse_IdCourse(courseId);
        if (chapters.isEmpty()) {
            return 0.0;
        }

        List<Progression> progresses =
                progressionRepository.findByUserIdAndChapter_Course_IdCourse(userId, courseId);

        // Map chapterId -> progressPercentage pour accès rapide
        Map<Long, Double> progressByChapter = new HashMap<>();
        for (Progression p : progresses) {
            if (p.getChapter() != null && p.getProgressPercentage() != null) {
                progressByChapter.put(p.getChapter().getIdChapter(), p.getProgressPercentage());
            }
        }

        double sum = 0.0;
        for (Chapter ch : chapters) {
            sum += progressByChapter.getOrDefault(ch.getIdChapter(), 0.0);
        }

        return sum / chapters.size();
    }
}

