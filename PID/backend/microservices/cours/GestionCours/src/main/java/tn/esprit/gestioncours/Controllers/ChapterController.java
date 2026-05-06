package tn.esprit.gestioncours.Controllers;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.gestioncours.Entities.Chapter;
import tn.esprit.gestioncours.Services.IChapterService;

import java.util.List;

@RestController
@RequestMapping("/chapter")
public class ChapterController {

    private final IChapterService chapterService;

    public ChapterController(@Qualifier("chapterServiceImpl") IChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @PostMapping("/add/{courseId}")
    public Chapter addChapter(@RequestBody Chapter chapter, @PathVariable Long courseId) {
        // On autorise largement la taille côté backend (≈ 35–40MB de base64)
        if (chapter.getPdfSupport() != null && chapter.getPdfSupport().length() > 50_000_000) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "PDF support too large");
        }
        return chapterService.addChapter(chapter, courseId);
    }

    @PutMapping("/update/{id}")
    public Chapter updateChapter(@PathVariable Long id,
                                 @RequestBody Chapter chapter) {

        Chapter existing = chapterService.retrieveChapter(id);

        existing.setName(chapter.getName());
        existing.setDescription(chapter.getDescription());
        existing.setPdfSupport(chapter.getPdfSupport());
        existing.setVideoSupport(chapter.getVideoSupport());

        return chapterService.updateChapter(existing);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteChapter(@PathVariable Long id) {
        chapterService.deleteChapter(id);
    }

    @GetMapping("/get/{id}")
    public Chapter getChapter(@PathVariable Long id) {
        return chapterService.retrieveChapter(id);
    }

    @GetMapping("/all")
    public List<Chapter> getAllChapters() {
        return chapterService.retrieveAllChapters();
    }

    @GetMapping("/by-course/{courseId}")
    public List<Chapter> getChaptersByCourse(@PathVariable Long courseId) {
        return chapterService.retrieveChaptersByCourse(courseId);
    }
}