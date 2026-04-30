package tn.esprit.gestioncours.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.gestioncours.Entities.Course;
import tn.esprit.gestioncours.Entities.NotificationType;
import tn.esprit.gestioncours.Repositories.ChapterLearningContentRepository;
import tn.esprit.gestioncours.Repositories.ChapterNoteRepository;
import tn.esprit.gestioncours.Repositories.ChapterPresentationRepository;
import tn.esprit.gestioncours.Repositories.CourseRepository;
import tn.esprit.gestioncours.Repositories.ProgressionRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private ProgressionRepository progressionRepository;
    @Mock
    private ChapterLearningContentRepository chapterLearningContentRepository;
    @Mock
    private ChapterNoteRepository chapterNoteRepository;
    @Mock
    private ChapterPresentationRepository chapterPresentationRepository;
    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private CourseServiceImpl courseService;

    @Test
    void addCourse_shouldSaveAndNotify() {
        Course input = new Course();
        input.setName("Spring");
        Course saved = new Course();
        saved.setIdCourse(5L);
        saved.setName("Spring");

        when(courseRepository.save(input)).thenReturn(saved);

        Course result = courseService.addCourse(input);

        assertEquals(saved, result);
        verify(courseRepository).save(input);
        verify(notificationService).createNotificationForUser(
                1L,
                "Nouveau cours disponible : Spring",
                NotificationType.COURSE
        );
    }

    @Test
    void deleteCourse_shouldDeleteChildrenThenCourse() {
        Long courseId = 11L;

        courseService.deleteCourse(courseId);

        verify(chapterLearningContentRepository).deleteByChapter_Course_IdCourse(courseId);
        verify(chapterNoteRepository).deleteByChapter_Course_IdCourse(courseId);
        verify(chapterPresentationRepository).deleteByChapter_Course_IdCourse(courseId);
        verify(progressionRepository).deleteByChapter_Course_IdCourse(courseId);
        verify(courseRepository).deleteById(courseId);
    }

    @Test
    void retrieveCourse_shouldReturnNullWhenNotFound() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        Course result = courseService.retrieveCourse(99L);

        assertNull(result);
    }
}
