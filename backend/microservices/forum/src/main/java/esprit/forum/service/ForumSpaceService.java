package esprit.forum.service;

import esprit.forum.entity.ForumSpace;
import esprit.forum.entity.ForumSpace.ForumSpaceType;
import esprit.forum.repository.ForumSpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ForumSpaceService {

    private final ForumSpaceRepository forumSpaceRepository;

    public List<ForumSpace> getSpacesByType(ForumSpaceType type) {
        return forumSpaceRepository.findByType(type);
    }

    public Optional<ForumSpace> getSpaceById(Long id) {
        return forumSpaceRepository.findById(id);
    }

    public Optional<ForumSpace> findByTypeAndKey(ForumSpaceType type, String key) {
        return forumSpaceRepository.findByTypeAndKey(type, key);
    }

    @Transactional
    public ForumSpace createIfMissing(ForumSpaceType type, String key, String title, boolean isPublic) {
        return forumSpaceRepository.findByTypeAndKey(type, key)
                .orElseGet(() -> forumSpaceRepository.save(new ForumSpace(
                        null,
                        type,
                        key,
                        title,
                        isPublic,
                        null,
                        null
                )));
    }
}

