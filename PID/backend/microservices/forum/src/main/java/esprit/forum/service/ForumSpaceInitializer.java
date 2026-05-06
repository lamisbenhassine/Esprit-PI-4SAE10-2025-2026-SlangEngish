package esprit.forum.service;

import esprit.forum.entity.ForumSpace;
import esprit.forum.entity.ForumTopic;
import esprit.forum.repository.ForumTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ForumSpaceInitializer implements ApplicationRunner {

    private final ForumSpaceService forumSpaceService;
    private final ForumTopicRepository forumTopicRepository;

    @Value("${forum.course.keys:}")
    private String courseKeysConfig;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        forumSpaceService.createIfMissing(ForumSpace.ForumSpaceType.GENERAL, "GENERAL", "Forum Général", true);

        List<String> levels = List.of("A1", "A2", "B1", "B2", "C1", "C2");
        for (String level : levels) {
            forumSpaceService.createIfMissing(ForumSpace.ForumSpaceType.LEVEL, level, "Niveau " + level, false);
        }

        for (String courseKey : parseCourseKeys(courseKeysConfig)) {
            forumSpaceService.createIfMissing(ForumSpace.ForumSpaceType.COURSE, courseKey, "Cours " + courseKey, false);
        }

        migrateExistingTopicsToSpaces();
    }

    private List<String> parseCourseKeys(String cfg) {
        if (cfg == null || cfg.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(cfg.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private void migrateExistingTopicsToSpaces() {
        List<ForumTopic> topics = forumTopicRepository.findBySpaceIsNull();
        if (topics.isEmpty()) {
            return;
        }

        for (ForumTopic topic : topics) {
            String category = topic.getCategory();
            if (category == null || category.trim().isEmpty()) {
                continue;
            }
            String normalized = category.trim().toUpperCase(Locale.ROOT);
            if ("GENERAL".equals(normalized)) {
                ForumSpace general = forumSpaceService.findByTypeAndKey(ForumSpace.ForumSpaceType.GENERAL, "GENERAL")
                        .orElseThrow(() -> new RuntimeException("GENERAL forum space not initialized"));
                topic.setSpace(general);
            } else {
                ForumSpace level = forumSpaceService.findByTypeAndKey(ForumSpace.ForumSpaceType.LEVEL, normalized)
                        .orElse(null);
                if (level != null) {
                    topic.setSpace(level);
                }
            }
        }

        forumTopicRepository.saveAll(topics);
    }
}

