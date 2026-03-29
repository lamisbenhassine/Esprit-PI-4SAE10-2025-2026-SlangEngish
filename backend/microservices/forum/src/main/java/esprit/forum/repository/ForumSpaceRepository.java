package esprit.forum.repository;

import esprit.forum.entity.ForumSpace;
import esprit.forum.entity.ForumSpace.ForumSpaceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumSpaceRepository extends JpaRepository<ForumSpace, Long> {
    Optional<ForumSpace> findByTypeAndKey(ForumSpaceType type, String key);

    List<ForumSpace> findByType(ForumSpaceType type);
}

