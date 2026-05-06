package com.school.schoolservice.application.scheduler;

import com.school.schoolservice.application.entity.Application;
import com.school.schoolservice.application.enums.ApplicationStatus;
import com.school.schoolservice.application.repository.ApplicationRepository;
import com.school.schoolservice.common.service.EmailService;
import com.school.schoolservice.joboffer.entity.JobOffer;
import com.school.schoolservice.joboffer.repository.JobOfferRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewReminderScheduler {

    private final ApplicationRepository applicationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final EmailService emailService;

    // ✅ Change pour tester — vérifie dans 1 minute
    @Scheduled(fixedRate = 60000) // toutes les minutes
    public void sendInterviewReminders() {
        System.out.println("🔔 Checking interviews for reminders...");

        LocalDateTime now = LocalDateTime.now();

        // ✅ Pour tester — cherche les entretiens dans les 2 prochaines minutes
        LocalDateTime in1min = now.plusMinutes(1);
        LocalDateTime in2min = now.plusMinutes(2);

        List<Application> upcoming = applicationRepository
                .findByStatusAndInterviewDateBetween(
                        ApplicationStatus.INTERVIEW, in1min, in2min);

        System.out.println("📅 Found " + upcoming.size() + " interviews to remind");
        // ... reste du code


    // ✅ Vérifie toutes les heures
    /*@Scheduled(fixedRate = 3600000)
    public void sendInterviewReminders() {
        System.out.println("🔔 Checking interviews for reminders...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in24h = now.plusHours(24);
        LocalDateTime in25h = now.plusHours(25);

        // ✅ Trouve les entretiens dans les prochaines 24-25h
        List<Application> upcoming = applicationRepository
                .findByStatusAndInterviewDateBetween(
                        ApplicationStatus.INTERVIEW, in24h, in25h);

        System.out.println("📅 Found " + upcoming.size() + " interviews to remind");*/

        for (Application app : upcoming) {
            try {
                JobOffer jobOffer = jobOfferRepository
                        .findById(app.getJobOfferId())
                        .orElse(null);

                if (jobOffer != null && app.getApplicantEmail() != null) {
                    emailService.sendInterviewReminder(
                            app.getApplicantEmail(),
                            app.getApplicantName(),
                            jobOffer.getTitle(),
                            jobOffer.getCompany(),
                            app.getInterviewDate()
                    );
                    System.out.println("✅ Reminder sent to: " + app.getApplicantEmail());
                }
            } catch (Exception e) {
                System.out.println("❌ Reminder failed for app " + app.getId());
                log.error("Reminder error: {}", e.getMessage());
            }
        }
    }
}