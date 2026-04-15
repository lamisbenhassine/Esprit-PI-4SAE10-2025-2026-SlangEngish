package org.example.club.repository;

import org.example.club.entity.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    List<PostComment> findByPost_IdOrderByDateCreationAsc(Long postId);

    long countByPost_Id(Long postId);

    @Query("select c from PostComment c where c.post.id in :ids order by c.post.id asc, c.dateCreation asc")
    List<PostComment> findAllForPosts(@Param("ids") Collection<Long> ids);
}
