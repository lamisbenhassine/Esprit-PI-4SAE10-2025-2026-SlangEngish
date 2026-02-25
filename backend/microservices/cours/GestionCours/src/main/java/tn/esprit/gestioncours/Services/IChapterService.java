package tn.esprit.gestioncours.Services;

import tn.esprit.gestioncours.Entities.Chapter;

import java.util.List;

public interface IChapterService {

    Chapter addChapter(Chapter chapter, Long courseId);

    Chapter updateChapter(Chapter chapter);

    void deleteChapter(Long id);

    Chapter retrieveChapter(Long id);

    List<Chapter> retrieveAllChapters();

    List<Chapter> retrieveChaptersByCourse(Long courseId);
}