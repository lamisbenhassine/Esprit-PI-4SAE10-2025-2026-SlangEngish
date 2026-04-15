package org.example.club.service;

import org.example.club.dto.ClubMessageDto;

import java.util.List;

public interface ClubMessageService {

    List<ClubMessageDto> getMessagesByClub(Long clubId, Long idEtudiant, String scope, String departement);

    ClubMessageDto envoyerMessage(Long clubId, Long idEtudiant, String contenu, String scope, String departement);
}
