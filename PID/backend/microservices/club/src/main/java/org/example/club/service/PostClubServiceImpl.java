package org.example.club.service;

import org.example.club.entity.Club;
import org.example.club.entity.PostClub;
import org.example.club.repository.ClubRepository;
import org.example.club.repository.PostClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PostClubServiceImpl implements PostClubService {

    @Autowired
    private PostClubRepository postRepository;
    
    @Autowired
    private ClubRepository clubRepository;

    private static final int CONTENU_MAX_LENGTH = 16_000;

    @Override
    public PostClub createPost(PostClub post) {
        if (post.getContenu() == null || post.getContenu().trim().isEmpty()) {
            throw new IllegalArgumentException("Le contenu du post est obligatoire");
        }
        String contenu = post.getContenu().trim();
        if (contenu.length() > CONTENU_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Contenu trop long (max " + CONTENU_MAX_LENGTH + " caractères). Réduisez le texte ou les médias.");
        }
        post.setContenu(contenu);
        if (post.getIdAuteur() == null) {
            throw new IllegalArgumentException("L'ID de l'auteur est obligatoire");
        }
        if (post.getClub() == null || post.getClub().getId() == null) {
            throw new IllegalArgumentException("Le club est obligatoire");
        }
        
        // Vérifier si le club existe
        Club club = clubRepository.findById(post.getClub().getId())
                .orElseThrow(() -> new RuntimeException("Club non trouvé avec l'ID: " + post.getClub().getId()));
        
        // Définir la date du post si elle n'est pas définie
        if (post.getDatePost() == null) {
            post.setDatePost(LocalDate.now());
        }
        
        post.setClub(club);
        return postRepository.save(post);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostClub> getAllPosts() {
        return postRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PostClub> getPostById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID du post ne peut pas être null");
        }
        return postRepository.findById(id);
    }

    @Override
    public PostClub updatePost(Long id, PostClub post) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID du post ne peut pas être null");
        }
        
        PostClub existingPost = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post non trouvé avec l'ID: " + id));
        
        if (post.getContenu() != null && !post.getContenu().trim().isEmpty()) {
            String c = post.getContenu().trim();
            if (c.length() > CONTENU_MAX_LENGTH) {
                throw new IllegalArgumentException(
                        "Contenu trop long (max " + CONTENU_MAX_LENGTH + " caractères).");
            }
            existingPost.setContenu(c);
        }
        
        if (post.getDatePost() != null) {
            existingPost.setDatePost(post.getDatePost());
        }
        
        if (post.getIdAuteur() != null) {
            existingPost.setIdAuteur(post.getIdAuteur());
        }
        
        if (post.getClub() != null && post.getClub().getId() != null) {
            Club club = clubRepository.findById(post.getClub().getId())
                    .orElseThrow(() -> new RuntimeException("Club non trouvé avec l'ID: " + post.getClub().getId()));
            existingPost.setClub(club);
        }
        
        return postRepository.save(existingPost);
    }

    @Override
    public void deletePost(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID du post ne peut pas être null");
        }
        
        if (!postRepository.existsById(id)) {
            throw new RuntimeException("Post non trouvé avec l'ID: " + id);
        }
        
        postRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostClub> getPostsByClub(Long clubId) {
        return postRepository.findByClubId(clubId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostClub> getPostsByAuteur(Long idAuteur) {
        return postRepository.findByIdAuteur(idAuteur);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostClub> getPostsByClubOrderByDateDesc(Long clubId) {
        return postRepository.findByClubIdOrderByDatePostDesc(clubId);
    }
}

