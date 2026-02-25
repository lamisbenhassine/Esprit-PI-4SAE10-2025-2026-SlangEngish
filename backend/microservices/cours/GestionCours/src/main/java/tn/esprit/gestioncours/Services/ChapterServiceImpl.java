package tn.esprit.gestioncours.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.gestioncours.Entities.Chapter;
import tn.esprit.gestioncours.Entities.Course;
import tn.esprit.gestioncours.Repositories.ChapterRepository;
import tn.esprit.gestioncours.Repositories.CourseRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChapterServiceImpl implements IChapterService {

    private final ChapterRepository chapterRepository;
    private final CourseRepository courseRepository;

    @Override
    public Chapter addChapter(Chapter chapter, Long courseId) {

        Course course = courseRepository.findById(courseId).orElse(null);

        chapter.setCourse(course); // IMPORTANT

        return chapterRepository.save(chapter);
    }

    @Override
    public Chapter updateChapter(Chapter chapter) {
        return chapterRepository.save(chapter);
    }

    @Override
    public void deleteChapter(Long id) {
        chapterRepository.deleteById(id);
    }

    @Override
    public Chapter retrieveChapter(Long id) {
        return chapterRepository.findById(id).orElse(null);
    }

    @Override
    public List<Chapter> retrieveAllChapters() {
        return chapterRepository.findAll();
    }

    @Override
    public List<Chapter> retrieveChaptersByCourse(Long courseId) {
        return chapterRepository.findByCourse_IdCourse(courseId);
    }
}