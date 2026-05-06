package tn.esprit.gestioncours.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.gestioncours.Entities.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
}

