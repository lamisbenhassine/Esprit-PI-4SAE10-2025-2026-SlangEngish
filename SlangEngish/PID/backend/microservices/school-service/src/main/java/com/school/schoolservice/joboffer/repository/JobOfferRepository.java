package com.school.schoolservice.joboffer.repository;

import com.school.schoolservice.joboffer.entity.JobOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {
    List<JobOffer> findByActiveTrue(); // ✅ ajoute cette ligne
    // ✅ Offres avec salaire non null
    @Query("SELECT j FROM JobOffer j WHERE j.salary IS NOT NULL")
    List<JobOffer> findAllWithSalary();

}

