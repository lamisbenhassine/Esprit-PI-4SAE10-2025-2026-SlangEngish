package org.example.club.service;


import org.example.club.entity.PostClub;

import java.util.List;
import java.util.Optional;

public interface PostClubService {
    
    // Créer un nouveau post
    PostClub createPost(PostClub post);
    
    // Récupérer tous les posts
    List<PostClub> getAllPosts();
    
    // Récupérer un post par son ID
    Optional<PostClub> getPostById(Long id);
    
    // Mettre à jour un post
    PostClub updatePost(Long id, PostClub post);
    
    // Supprimer un post
    void deletePost(Long id);
    
    // Récupérer les posts d'un club
    List<PostClub> getPostsByClub(Long clubId);
    
    // Récupérer les posts d'un auteur
    List<PostClub> getPostsByAuteur(Long idAuteur);
    
    // Récupérer les posts d'un club, triés par date décroissante
    List<PostClub> getPostsByClubOrderByDateDesc(Long clubId);
}

