package tn.esprit.gestioncours.Repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.gestioncours.Entities.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
}