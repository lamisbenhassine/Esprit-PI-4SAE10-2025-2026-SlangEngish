package tn.esprit.gestioncours.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.gestioncours.Entities.Progression;
import tn.esprit.gestioncours.Services.IProgressionService;

@RestController
@RequestMapping("/progression")
@RequiredArgsConstructor
public class ProgressionController {

    private final IProgressionService progressionService;

    @PostMapping("/chapter")
    public Progression updateChapterProgress(@RequestParam Long userId,
                                             @RequestParam Long chapterId,
                                             @RequestParam Integer lastPage,
                                             @RequestParam Double progressPercentage) {
        return progressionService.saveOrUpdateProgress(userId, chapterId, lastPage, progressPercentage);
    }

    @GetMapping("/chapter")
    public Progression getChapterProgress(@RequestParam Long userId,
                                          @RequestParam Long chapterId) {
        return progressionService.getProgressForChapter(userId, chapterId);
    }

    @GetMapping("/course")
    public Double getCourseProgress(@RequestParam Long userId,
                                    @RequestParam Long courseId) {
        return progressionService.getCourseProgress(userId, courseId);
    }
}

