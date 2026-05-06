package org.example.club.repository;

import org.example.club.entity.ClubMessage;
import org.example.club.entity.MessageScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClubMessageRepository extends JpaRepository<ClubMessage, Long> {

    List<ClubMessage> findByClubIdOrderByDateCreationAsc(Long clubId);
    List<ClubMessage> findByClubIdAndScopeOrderByDateCreationAsc(Long clubId, MessageScope scope);
    List<ClubMessage> findByClubIdAndScopeAndDepartementOrderByDateCreationAsc(Long clubId, MessageScope scope, String departement);
}
