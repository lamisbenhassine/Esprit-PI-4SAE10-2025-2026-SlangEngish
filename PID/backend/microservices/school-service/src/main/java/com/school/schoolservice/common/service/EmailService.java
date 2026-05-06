package com.school.schoolservice.common.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendApplicationConfirmation(
            String toEmail,
            String applicantName,
            String offerTitle,
            String company) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("✅ Candidature reçue — " + offerTitle);
            helper.setText(buildEmailHtml(applicantName, offerTitle, company), true);

            mailSender.send(message);
            log.info("✅ Email envoyé à {}", toEmail);

        } catch (MessagingException e) {
            log.error("❌ Erreur envoi email : {}", e.getMessage());
        }
    }

    private String buildEmailHtml(String name, String offerTitle, String company) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8"/>
          <style>
            body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; padding: 0; }
            .container { max-width: 600px; margin: 40px auto; background: white;
                         border-radius: 16px; overflow: hidden;
                         box-shadow: 0 4px 20px rgba(0,0,0,0.08); }
            .header { background: linear-gradient(135deg, #1877f2, #0d5cbf);
                      padding: 40px 32px; text-align: center; }
            .header h1 { color: white; margin: 0; font-size: 24px; font-weight: 700; }
            .header p { color: rgba(255,255,255,0.85); margin: 8px 0 0 0; font-size: 15px; }
            .body { padding: 32px; }
            .greeting { font-size: 18px; font-weight: 600; color: #0f172a; margin-bottom: 16px; }
            .message { font-size: 15px; color: #475569; line-height: 1.7; margin-bottom: 24px; }
            .offer-box { background: #f0f7ff; border: 1px solid #c7d9fd;
                         border-radius: 12px; padding: 20px 24px; margin-bottom: 24px; }
            .offer-box .label { font-size: 11px; font-weight: 700; color: #1877f2;
                                text-transform: uppercase; letter-spacing: 0.5px; }
            .offer-box .value { font-size: 18px; font-weight: 700; color: #0f172a; margin-top: 4px; }
            .offer-box .company { font-size: 14px; color: #475569; margin-top: 4px; }
            .status-badge { display: inline-flex; align-items: center; gap: 6px;
                            background: #dcfce7; color: #166534; padding: 8px 16px;
                            border-radius: 20px; font-size: 13px; font-weight: 700; }
            .footer { background: #f8fafc; padding: 20px 32px; text-align: center;
                      font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <h1>🎓 School Platform</h1>
              <p>Confirmation de candidature</p>
            </div>
            <div class="body">
              <p class="greeting">Bonjour %s,</p>
              <p class="message">
                Votre candidature a bien été reçue et est en cours d'examen.
                Nous vous contacterons dès que possible.
              </p>
              <div class="offer-box">
                <div class="label">Offre concernée</div>
                <div class="value">%s</div>
                <div class="company">🏢 %s</div>
              </div>
              <div class="status-badge">
                ✅ Statut : En attente de réponse
              </div>
              <p class="message" style="margin-top: 24px;">
                Bonne chance pour votre candidature !
              </p>
            </div>
            <div class="footer">
              School Platform • Cet email a été envoyé automatiquement, merci de ne pas y répondre.
            </div>
          </div>
        </body>
        </html>
        """.formatted(name, offerTitle, company);
    }


    // ✅ Email invitation entretien
    @Async
    public void sendInterviewInvitation(
            String toEmail,
            String applicantName,
            String offerTitle,
            String company,
            LocalDateTime interviewDate) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("📅 Interview Scheduled — " + offerTitle);
            helper.setText(buildInterviewHtml(applicantName, offerTitle, company, interviewDate), true);

            mailSender.send(message);
            System.out.println("✅ Interview email sent to: " + toEmail);
            log.info("✅ Interview email sent to {}", toEmail);

        } catch (MessagingException e) {
            System.out.println("❌ Failed to send interview email: " + e.getMessage());
            log.error("❌ Interview email error: {}", e.getMessage());
        }
    }

    // ✅ Email rappel 24h avant
    @Async
    public void sendInterviewReminder(
            String toEmail,
            String applicantName,
            String offerTitle,
            String company,
            LocalDateTime interviewDate) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("⏰ Reminder — Interview Tomorrow: " + offerTitle);
            helper.setText(buildReminderHtml(applicantName, offerTitle, company, interviewDate), true);

            mailSender.send(message);
            System.out.println("✅ Reminder email sent to: " + toEmail);
            log.info("✅ Reminder email sent to {}", toEmail);

        } catch (MessagingException e) {
            System.out.println("❌ Failed to send reminder: " + e.getMessage());
            log.error("❌ Reminder email error: {}", e.getMessage());
        }
    }

    private String buildInterviewHtml(String name, String offerTitle,
                                      String company, LocalDateTime interviewDate) {
        String dateStr = interviewDate.format(
                java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy 'at' HH:mm"));
        return """
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8"/>
      <style>
        body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; padding: 0; }
        .container { max-width: 600px; margin: 40px auto; background: white;
                     border-radius: 16px; overflow: hidden;
                     box-shadow: 0 4px 20px rgba(0,0,0,0.08); }
        .header { background: linear-gradient(135deg, #7c3aed, #5b21b6);
                  padding: 40px 32px; text-align: center; }
        .header h1 { color: white; margin: 0; font-size: 24px; font-weight: 700; }
        .header p { color: rgba(255,255,255,0.85); margin: 8px 0 0 0; }
        .body { padding: 32px; }
        .greeting { font-size: 18px; font-weight: 600; color: #0f172a; margin-bottom: 16px; }
        .message { font-size: 15px; color: #475569; line-height: 1.7; margin-bottom: 24px; }
        .date-box { background: linear-gradient(135deg, #f3e8ff, #ede9fe);
                    border: 1px solid #c4b5fd; border-radius: 12px;
                    padding: 24px; text-align: center; margin-bottom: 24px; }
        .date-box .date-label { font-size: 12px; font-weight: 700; color: #7c3aed;
                                text-transform: uppercase; letter-spacing: 1px; }
        .date-box .date-value { font-size: 28px; font-weight: 800; color: #4c1d95;
                                margin-top: 8px; }
        .offer-box { background: #f0f7ff; border: 1px solid #c7d9fd;
                     border-radius: 12px; padding: 20px 24px; margin-bottom: 24px; }
        .offer-box .label { font-size: 11px; font-weight: 700; color: #1877f2;
                            text-transform: uppercase; letter-spacing: 0.5px; }
        .offer-box .value { font-size: 18px; font-weight: 700; color: #0f172a; margin-top: 4px; }
        .offer-box .company { font-size: 14px; color: #475569; margin-top: 4px; }
        .tips { background: #f0fdf4; border: 1px solid #bbf7d0;
                border-radius: 12px; padding: 20px 24px; }
        .tips h3 { color: #166534; margin: 0 0 12px 0; font-size: 15px; }
        .tips ul { margin: 0; padding-left: 20px; color: #15803d; }
        .tips li { margin-bottom: 6px; font-size: 14px; }
        .footer { background: #f8fafc; padding: 20px 32px; text-align: center;
                  font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
      </style>
    </head>
    <body>
      <div class="container">
        <div class="header">
          <h1>🎓 School Platform</h1>
          <p>Interview Invitation</p>
        </div>
        <div class="body">
          <p class="greeting">Hello %s,</p>
          <p class="message">
            Congratulations! We are pleased to invite you for an interview.
            Please find the details below.
          </p>
          <div class="date-box">
            <div class="date-label">📅 Interview Date</div>
            <div class="date-value">%s</div>
          </div>
          <div class="offer-box">
            <div class="label">Position</div>
            <div class="value">%s</div>
            <div class="company">🏢 %s</div>
          </div>
          <div class="tips">
            <h3>💡 Tips for your interview</h3>
            <ul>
              <li>Research the company beforehand</li>
              <li>Prepare examples of your past experience</li>
              <li>Arrive 10 minutes early</li>
              <li>Bring a copy of your CV</li>
            </ul>
          </div>
        </div>
        <div class="footer">
          School Platform • This email was sent automatically, please do not reply.
        </div>
      </div>
    </body>
    </html>
    """.formatted(name, dateStr, offerTitle, company);
    }

    private String buildReminderHtml(String name, String offerTitle,
                                     String company, LocalDateTime interviewDate) {
        String dateStr = interviewDate.format(
                java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy 'at' HH:mm"));
        return """
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8"/>
      <style>
        body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; padding: 0; }
        .container { max-width: 600px; margin: 40px auto; background: white;
                     border-radius: 16px; overflow: hidden;
                     box-shadow: 0 4px 20px rgba(0,0,0,0.08); }
        .header { background: linear-gradient(135deg, #f59e0b, #d97706);
                  padding: 40px 32px; text-align: center; }
        .header h1 { color: white; margin: 0; font-size: 24px; font-weight: 700; }
        .header p { color: rgba(255,255,255,0.85); margin: 8px 0 0 0; }
        .body { padding: 32px; }
        .greeting { font-size: 18px; font-weight: 600; color: #0f172a; margin-bottom: 16px; }
        .message { font-size: 15px; color: #475569; line-height: 1.7; margin-bottom: 24px; }
        .reminder-box { background: #fffbeb; border: 2px solid #fbbf24;
                        border-radius: 12px; padding: 24px; text-align: center;
                        margin-bottom: 24px; }
        .reminder-box .icon { font-size: 48px; margin-bottom: 8px; }
        .reminder-box .text { font-size: 20px; font-weight: 800; color: #92400e; }
        .reminder-box .date { font-size: 16px; color: #b45309; margin-top: 8px; }
        .offer-box { background: #f0f7ff; border: 1px solid #c7d9fd;
                     border-radius: 12px; padding: 20px 24px; }
        .offer-box .label { font-size: 11px; font-weight: 700; color: #1877f2;
                            text-transform: uppercase; }
        .offer-box .value { font-size: 18px; font-weight: 700; color: #0f172a; margin-top: 4px; }
        .offer-box .company { font-size: 14px; color: #475569; margin-top: 4px; }
        .footer { background: #f8fafc; padding: 20px 32px; text-align: center;
                  font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
      </style>
    </head>
    <body>
      <div class="container">
        <div class="header">
          <h1>⏰ Interview Reminder</h1>
          <p>Your interview is tomorrow!</p>
        </div>
        <div class="body">
          <p class="greeting">Hello %s,</p>
          <p class="message">
            This is a friendly reminder that your interview is scheduled for tomorrow.
            Good luck! 🍀
          </p>
          <div class="reminder-box">
            <div class="icon">📅</div>
            <div class="text">Tomorrow!</div>
            <div class="date">%s</div>
          </div>
          <div class="offer-box">
            <div class="label">Position</div>
            <div class="value">%s</div>
            <div class="company">🏢 %s</div>
          </div>
        </div>
        <div class="footer">
          School Platform • Automated reminder, please do not reply.
        </div>
      </div>
    </body>
    </html>
    """.formatted(name, dateStr, offerTitle, company);
    }
}