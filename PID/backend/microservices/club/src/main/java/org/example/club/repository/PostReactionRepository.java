package org.example.club.repository;

import org.example.club.entity.PostReaction;
import org.example.club.entity.PostReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {

    List<PostReaction> findByPost_Id(Long postId);

    Optional<PostReaction> findByPost_IdAndIdEtudiant(Long postId, Long idEtudiant);

    void deleteByPost_IdAndIdEtudiant(Long postId, Long idEtudiant);

    long countByPost_IdAndReactionType(Long postId, PostReactionType reactionType);

    List<PostReaction> findByPost_IdIn(Collection<Long> postIds);
}
