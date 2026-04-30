package tn.esprit.gestionclasses.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.gestionclasses.Entities.ProfessorSessionAvailability;

import java.util.List;

public interface ProfessorSessionAvailabilityRepository extends JpaRepository<ProfessorSessionAvailability, Long> {

    List<ProfessorSessionAvailability> findByProfessorUserId(Long professorUserId);

    /**
     * Suppression explicite (évite les 500 Hibernate / contraintes si le delete dérivé
     * n’est pas flush avant les INSERT du même lot).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ProfessorSessionAvailability e WHERE e.professorUserId = :professorUserId")
    int deleteByProfessorUserId(@Param("professorUserId") Long professorUserId);
}
