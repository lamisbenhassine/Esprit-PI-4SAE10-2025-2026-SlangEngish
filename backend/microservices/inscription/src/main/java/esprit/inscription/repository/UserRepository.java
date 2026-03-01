package esprit.inscription.repository;

import esprit.inscription.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByEnglishLevel(String englishLevel);

    @Query("SELECT u FROM User u WHERE u.englishLevel = :level AND u.isActive = true")
    List<User> findActiveUsersByLevel(@Param("level") String level);

    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :cutoffDate")
    List<User> findInactiveUsersSince(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT u FROM User u WHERE u.totalRevenue >= :minRevenue")
    List<User> findHighValueUsers(@Param("minRevenue") BigDecimal minRevenue);

    @Query("SELECT u FROM User u WHERE u.trialEndsAt BETWEEN :now AND :futureDate")
    List<User> findUsersWithExpiringTrials(@Param("now") LocalDateTime now);

    @Query("SELECT u FROM User u WHERE u.createdAt >= :since")
    List<User> findNewUsersSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(u) FROM User u WHERE u.englishLevel = :level")
    Long countUsersByLevel(@Param("level") String level);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    Long countActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginAt >= :since")
    Long countRecentlyActiveUsers(@Param("since") LocalDateTime since);

    boolean existsByEmail(String email);
}
