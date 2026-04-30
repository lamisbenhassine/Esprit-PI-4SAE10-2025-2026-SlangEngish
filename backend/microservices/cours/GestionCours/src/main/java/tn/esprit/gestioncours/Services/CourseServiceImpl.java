package tn.esprit.gestioncours.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.gestioncours.Entities.Course;
import tn.esprit.gestioncours.Entities.NotificationType;
import tn.esprit.gestioncours.Repositories.ChapterLearningContentRepository;
import tn.esprit.gestioncours.Repositories.ChapterNoteRepository;
import tn.esprit.gestioncours.Repositories.ChapterPresentationRepository;
import tn.esprit.gestioncours.Repositories.CourseRepository;
import tn.esprit.gestioncours.Repositories.ProgressionRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements ICourseService {

    private final CourseRepository courseRepository;
    private final ProgressionRepository progressionRepository;
    private final ChapterLearningContentRepository chapterLearningContentRepository;
    private final ChapterNoteRepository chapterNoteRepository;
    private final ChapterPresentationRepository chapterPresentationRepository;
    private final INotificationService notificationService;

    @Override
    public Course addCourse(Course course) {
        Course saved = courseRepository.save(course);

        // Ici, on notifie un utilisateur générique (ex: 1L). Plus tard, on pourra
        // cibler des utilisateurs précis (étudiants inscrits, etc.).
        String message = "Nouveau cours disponible : " + saved.getName();
        notificationService.createNotificationForUser(1L, message, NotificationType.COURSE);

        return saved;
    }

    @Override
    public Course updateCourse(Course course) {
        return courseRepository.save(course);
    }

    @Override
    @Transactional
    public void deleteCourse(Long id) {
        // Rows that reference Chapter must be removed before Course/Chapters (FK constraints).
        chapterLearningContentRepository.deleteByChapter_Course_IdCourse(id);
        chapterNoteRepository.deleteByChapter_Course_IdCourse(id);
        chapterPresentationRepository.deleteByChapter_Course_IdCourse(id);
        progressionRepository.deleteByChapter_Course_IdCourse(id);
        courseRepository.deleteById(id);
    }

    @Override
    public Course retrieveCourse(Long id) {
        return courseRepository.findById(id).orElse(null);
    }

    @Override
    public List<Course> retrieveAllCourses() {
        return courseRepository.findAll();
    }
}