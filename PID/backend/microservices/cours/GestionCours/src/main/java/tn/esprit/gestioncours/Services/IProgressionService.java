package tn.esprit.gestioncours.Services;

import tn.esprit.gestioncours.Entities.Progression;

public interface IProgressionService {

    Progression saveOrUpdateProgress(Long userId,
                                     Long chapterId,
                                     Integer lastPage,
                                     Double progressPercentage);

    Progression getProgressForChapter(Long userId, Long chapterId);

    Double getCourseProgress(Long userId, Long courseId);
}

