package org.example.club.controller;

import org.example.club.entity.PostClub;
import org.example.club.service.PostClubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts-club")
@CrossOrigin(origins = "*")
public class PostClubController {

    @Autowired
    private PostClubService postService;

    // CREATE - Créer un nouveau post
    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody PostClub post) {
        try {
            PostClub createdPost = postService.createPost(post);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la création du post: " + e.getMessage());
        }
    }

    // READ - Récupérer tous les posts
    @GetMapping
    public ResponseEntity<List<PostClub>> getAllPosts() {
        List<PostClub> posts = postService.getAllPosts();
        return ResponseEntity.ok(posts);
    }

    // READ - Récupérer un post par son ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPostById(@PathVariable Long id) {
        Optional<PostClub> post = postService.getPostById(id);

        if (post.isPresent()) {
            return ResponseEntity.ok(post.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Post non trouvé avec l'ID: " + id);
        }
    }

    // UPDATE - Mettre à jour un post
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable Long id, @RequestBody PostClub post) {
        try {
            PostClub updatedPost = postService.updatePost(id, post);
            return ResponseEntity.ok(updatedPost);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la mise à jour du post: " + e.getMessage());
        }
    }

    // DELETE - Supprimer un post
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        try {
            postService.deletePost(id);
            return ResponseEntity.ok("Post supprimé avec succès");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression du post: " + e.getMessage());
        }
    }

    // Récupérer les posts d'un club
    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<PostClub>> getPostsByClub(@PathVariable Long clubId) {
        List<PostClub> posts = postService.getPostsByClub(clubId);
        return ResponseEntity.ok(posts);
    }

    // Récupérer les posts d'un auteur
    @GetMapping("/auteur/{idAuteur}")
    public ResponseEntity<List<PostClub>> getPostsByAuteur(@PathVariable Long idAuteur) {
        List<PostClub> posts = postService.getPostsByAuteur(idAuteur);
        return ResponseEntity.ok(posts);
    }

    // Récupérer les posts d'un club, triés par date décroissante
    @GetMapping("/club/{clubId}/recent")
    public ResponseEntity<List<PostClub>> getPostsByClubOrderByDateDesc(@PathVariable Long clubId) {
        List<PostClub> posts = postService.getPostsByClubOrderByDateDesc(clubId);
        return ResponseEntity.ok(posts);
    }
}

