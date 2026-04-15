package com.school.schoolservice.simulation.service.impl;

import com.school.schoolservice.application.entity.Application;
import com.school.schoolservice.application.enums.ApplicationStatus;
import com.school.schoolservice.application.repository.ApplicationRepository;
import com.school.schoolservice.common.service.EmailService;
import com.school.schoolservice.joboffer.entity.JobOffer;
import com.school.schoolservice.joboffer.repository.JobOfferRepository;
import com.school.schoolservice.simulation.dto.*;
import com.school.schoolservice.simulation.dto.QuizResultDto.QuestionResult;
import com.school.schoolservice.simulation.service.QuizService;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizServiceImpl implements QuizService {

    private final JobOfferRepository jobOfferRepository;
    private final ApplicationRepository applicationRepository;
    private final EmailService emailService;

    private static final int QUALIFICATION_THRESHOLD = 60;

    // ─── Banque de Questions ──────────────────────────────────────────────────

    private static final Map<String, List<QuizQuestionDto>>
            QUESTION_BANK = new HashMap<>();

    static {
        List<QuizQuestionDto> languageQuestions = Arrays.asList(

                QuizQuestionDto.builder()
                        .question("What is the correct greeting "
                                + "in a formal business context?")
                        .choices(Arrays.asList(
                                "A) Hey, what's up?",
                                "B) Good morning, how may I assist you?",
                                "C) Yo, how are you doing?",
                                "D) Hi dude, nice to meet ya!"))
                        .correctAnswer("B")
                        .category("business_english")
                        .explanation("In formal business contexts, always use "
                                + "polite and professional greetings.")
                        .build(),

                QuizQuestionDto.builder()
                        .question("How do you politely say NO "
                                + "to a customer's request?")
                        .choices(Arrays.asList(
                                "A) No, we can't do that.",
                                "B) I'm afraid that's not possible, however "
                                        + "I can offer you an alternative.",
                                "C) That's impossible, sorry.",
                                "D) We don't do that here."))
                        .correctAnswer("B")
                        .category("customer_service")
                        .explanation("Always offer an alternative when declining "
                                + "a customer request professionally.")
                        .build(),

                QuizQuestionDto.builder()
                        .question("What does 'Can you elaborate on that?' mean?")
                        .choices(Arrays.asList(
                                "A) Can you repeat that?",
                                "B) Can you provide more details?",
                                "C) Can you speak louder?",
                                "D) Can you write it down?"))
                        .correctAnswer("B")
                        .category("communication")
                        .explanation("'Elaborate' means to explain in more "
                                + "detail or give more information.")
                        .build(),

                QuizQuestionDto.builder()
                        .question("Which sentence is grammatically correct?")
                        .choices(Arrays.asList(
                                "A) I has been working here for 3 years.",
                                "B) I have been working here for 3 years.",
                                "C) I am work here since 3 years.",
                                "D) I working here for 3 years."))
                        .correctAnswer("B")
                        .category("grammar")
                        .explanation("Present perfect continuous is used for "
                                + "actions that started in the past and continue now.")
                        .build(),

                QuizQuestionDto.builder()
                        .question("How do you introduce yourself professionally "
                                + "in English?")
                        .choices(Arrays.asList(
                                "A) My name is John and I like pizza.",
                                "B) Good morning, I'm John Smith, "
                                        + "I work as a sales manager with 5 years experience.",
                                "C) Hi, I'm John, I'm here.",
                                "D) Hello, call me John."))
                        .correctAnswer("B")
                        .category("professional_english")
                        .explanation("A professional introduction includes "
                                + "your name, title and relevant experience.")
                        .build(),

                QuizQuestionDto.builder()
                        .question("What does 'ASAP' mean in business English?")
                        .choices(Arrays.asList(
                                "A) As Soon As Possible",
                                "B) As Safe As Possible",
                                "C) As Simple As Possible",
                                "D) As Slow As Possible"))
                        .correctAnswer("A")
                        .category("business_english")
                        .explanation("ASAP stands for 'As Soon As Possible', "
                                + "commonly used in business communication.")
                        .build(),

                QuizQuestionDto.builder()
                        .question("How do you ask for clarification politely?")
                        .choices(Arrays.asList(
                                "A) What? I don't understand.",
                                "B) Could you please clarify what you mean by that?",
                                "C) That makes no sense.",
                                "D) Repeat that please."))
                        .correctAnswer("B")
                        .category("communication")
                        .explanation("Using 'Could you please' makes requests "
                                + "more polite and professional.")
                        .build(),

                QuizQuestionDto.builder()
                        .question("What is the correct way to end "
                                + "a professional email?")
                        .choices(Arrays.asList(
                                "A) Bye bye, John",
                                "B) Best regards, John Smith",
                                "C) See you, John",
                                "D) Later, J."))
                        .correctAnswer("B")
                        .category("business_writing")
                        .explanation("'Best regards' or 'Sincerely' are standard "
                                + "professional email closings.")
                        .build(),

                QuizQuestionDto.builder()
                        .question("A tourist asks: 'Where is the nearest bank?' "
                                + "What is the best response?")
                        .choices(Arrays.asList(
                                "A) I don't know.",
                                "B) Go straight ahead, turn left at the traffic "
                                        + "lights, it's on your right.",
                                "C) Bank is far.",
                                "D) Use Google Maps."))
                        .correctAnswer("B")
                        .category("tourism")
                        .explanation("A good guide gives clear, detailed "
                                + "directions in a helpful and friendly manner.")
                        .build(),

                QuizQuestionDto.builder()
                        .question("What does 'fluent in English' mean?")
                        .choices(Arrays.asList(
                                "A) Knowing a few English words",
                                "B) Speaking English naturally and accurately "
                                        + "without difficulty",
                                "C) Having passed an English exam",
                                "D) Understanding English movies"))
                        .correctAnswer("B")
                        .category("language")
                        .explanation("Fluency means speaking naturally, accurately "
                                + "and without difficulty in communication.")
                        .build(),

                QuizQuestionDto.builder()
                        .question("How do you handle an angry customer "
                                + "in English?")
                        .choices(Arrays.asList(
                                "A) Get angry back at them",
                                "B) Stay calm, listen actively and apologize sincerely",
                                "C) Ignore them until they calm down",
                                "D) Transfer them immediately without explanation"))
                        .correctAnswer("B")
                        .category("customer_service")
                        .explanation("Staying calm and showing empathy is key "
                                + "to resolving customer complaints professionally.")
                        .build(),

                QuizQuestionDto.builder()
                        .question("What is 'small talk' in a professional context?")
                        .choices(Arrays.asList(
                                "A) Talking in a quiet voice",
                                "B) Light casual conversation to build rapport "
                                        + "before business discussions",
                                "C) Discussing confidential business matters",
                                "D) Talking about small problems"))
                        .correctAnswer("B")
                        .category("professional_english")
                        .explanation("Small talk helps build relationships "
                                + "and create a comfortable atmosphere.")
                        .build(),

                QuizQuestionDto.builder()
                        .question("Which is correct: 'I am interesting in this "
                                + "job' OR 'I am interested in this job'?")
                        .choices(Arrays.asList(
                                "A) I am interesting in this job",
                                "B) I am interested in this job",
                                "C) Both are correct",
                                "D) Neither is correct"))
                        .correctAnswer("B")
                        .category("grammar")
                        .explanation("'Interested' describes your feeling. "
                                + "'Interesting' describes the job itself.")
                        .build(),

                QuizQuestionDto.builder()
                        .question("What does 'Could you please hold the line?' "
                                + "mean?")
                        .choices(Arrays.asList(
                                "A) Please hang up the phone",
                                "B) Please wait on the phone while I assist you",
                                "C) Please call back later",
                                "D) Please speak louder"))
                        .correctAnswer("B")
                        .category("telephone_english")
                        .explanation("'Hold the line' means wait on the phone — "
                                + "commonly used in customer service calls.")
                        .build(),

                QuizQuestionDto.builder()
                        .question("How do you describe a historical monument "
                                + "to tourists?")
                        .choices(Arrays.asList(
                                "A) It's old and big.",
                                "B) This magnificent structure was built in the "
                                        + "9th century and represents the rich cultural "
                                        + "heritage of our region.",
                                "C) I don't know much about it.",
                                "D) It's famous, you should take a photo."))
                        .correctAnswer("B")
                        .category("tourism")
                        .explanation("A professional tour guide uses descriptive "
                                + "and informative language to engage tourists.")
                        .build()
        );

        // ✅ Applique pour tous les domaines
        QUESTION_BANK.put("java",      languageQuestions);
        QUESTION_BANK.put("angular",   languageQuestions);
        QUESTION_BANK.put("python",    languageQuestions);
        QUESTION_BANK.put("marketing", languageQuestions);
        QUESTION_BANK.put("finance",   languageQuestions);
        QUESTION_BANK.put("hr",        languageQuestions);
        QUESTION_BANK.put("general",   languageQuestions);
        QUESTION_BANK.put("tourism",   languageQuestions);
        QUESTION_BANK.put("teaching",  languageQuestions);
    }

    // ─── Generate Quiz ─────────────────────────────────────────────────────────

    @Override
    public List<QuizQuestionDto> generateQuiz(Long jobOfferId) {
        System.out.println("🎯 Génération quiz pour offre: " + jobOfferId);

        JobOffer jobOffer = jobOfferRepository.findById(jobOfferId)
                .orElseThrow(() -> new RuntimeException(
                        "JobOffer not found: " + jobOfferId));

        String description = jobOffer.getDescription() != null
                ? jobOffer.getDescription().toLowerCase() : "";

        String domain = detectDomain(description);
        System.out.println("🔍 Domaine détecté: " + domain);

        List<QuizQuestionDto> domainQuestions = new ArrayList<>(
                QUESTION_BANK.getOrDefault(domain,
                        QUESTION_BANK.get("general")));

        if (domainQuestions.size() < 5) {
            List<QuizQuestionDto> general = new ArrayList<>(
                    QUESTION_BANK.get("general"));
            Collections.shuffle(general);
            domainQuestions.addAll(general);
        }

        Collections.shuffle(domainQuestions);
        List<QuizQuestionDto> selected = domainQuestions.stream()
                .limit(5).collect(Collectors.toList());

        // ✅ Numérotation + Shuffle des choix
        for (int i = 0; i < selected.size(); i++) {
            selected.set(i, shuffleChoices(selected.get(i)));
            selected.get(i).setNumber(i + 1);
        }

        System.out.println("✅ " + selected.size()
                + " questions générées pour: " + domain);
        return selected;
    }

    // ✅ Mélange les choix aléatoirement
    private QuizQuestionDto shuffleChoices(QuizQuestionDto question) {
        List<String> choices = new ArrayList<>(question.getChoices());

        String correctText = choices.stream()
                .filter(c -> c.startsWith(
                        question.getCorrectAnswer() + ")"))
                .findFirst()
                .orElse(choices.get(0));

        Collections.shuffle(choices);

        List<String> letters = Arrays.asList("A", "B", "C", "D");
        List<String> newChoices = new ArrayList<>();
        String newCorrectAnswer = "A";

        for (int i = 0; i < choices.size()
                && i < letters.size(); i++) {
            String text = choices.get(i).length() > 3
                    ? choices.get(i).substring(3)
                    : choices.get(i);
            String newChoice = letters.get(i) + ") " + text;
            newChoices.add(newChoice);

            if (choices.get(i).equals(correctText)) {
                newCorrectAnswer = letters.get(i);
            }
        }

        return QuizQuestionDto.builder()
                .number(question.getNumber())
                .question(question.getQuestion())
                .choices(newChoices)
                .correctAnswer(newCorrectAnswer)
                .category(question.getCategory())
                .explanation(question.getExplanation())
                .build();
    }

    // ─── Evaluate Quiz ─────────────────────────────────────────────────────────

    @Override
    public QuizResultDto evaluateQuiz(QuizRequestDto request) {
        System.out.println("📊 Évaluation quiz pour offre: "
                + request.getJobOfferId());

        JobOffer jobOffer = jobOfferRepository
                .findById(request.getJobOfferId())
                .orElseThrow(() -> new RuntimeException(
                        "JobOffer not found"));

        // ✅ ÉTAPE 1 : Score QCM
        List<QuestionResult> questionResults = new ArrayList<>();
        int correctCount = 0;

        if (request.getAnswers() != null) {
            for (QuizRequestDto.QuizAnswer qa : request.getAnswers()) {
                boolean isCorrect = qa.getSelectedAnswer() != null
                        && qa.getSelectedAnswer()
                        .equals(qa.getCorrectAnswer());
                if (isCorrect) correctCount++;

                questionResults.add(QuestionResult.builder()
                        .number(qa.getQuestionNumber())
                        .question(qa.getQuestion())
                        .selectedAnswer(qa.getSelectedAnswer())
                        .correctAnswer(qa.getCorrectAnswer())
                        .correct(isCorrect)
                        .build());
            }
        }

        int totalQuestions = request.getAnswers() != null
                ? request.getAnswers().size() : 5;
        int quizScore = totalQuestions > 0
                ? (int) Math.round(
                (double) correctCount / totalQuestions * 100)
                : 0;

        System.out.println("📝 Quiz: " + correctCount
                + "/" + totalQuestions + " → " + quizScore + "%");

        // ✅ ÉTAPE 2 : Score CV
        int cvScore = 10;
        List<String> cvMatchedSkills = new ArrayList<>();
        List<String> cvMissingSkills = new ArrayList<>();

        if (request.getCvUrl() != null
                && !request.getCvUrl().isEmpty()) {
            String cvText = extractTextFromPdf(request.getCvUrl());
            String cvLower = cvText.toLowerCase();
            String descLower = jobOffer.getDescription() != null
                    ? jobOffer.getDescription().toLowerCase() : "";

            // ✅ Compétences linguistiques et professionnelles
            List<String> allSkills = Arrays.asList(
                    "english", "french", "arabic", "spanish",
                    "german", "italian", "bilingual", "multilingual",
                    "fluent", "native", "proficient", "advanced",
                    "intermediate", "toefl", "ielts", "cambridge",
                    "communication", "public speaking", "presentation",
                    "writing", "listening", "interpersonal",
                    "tourism", "hospitality", "hotel", "travel",
                    "guide", "tour guide", "reception", "customer service",
                    "teaching", "training", "tutoring", "education",
                    "translation", "interpretation", "proofreading",
                    "sales", "negotiation", "call center",
                    "teamwork", "leadership", "adaptability",
                    "problem solving", "time management",
                    "creativity", "organization", "motivation",
                    "excel", "powerpoint", "word", "microsoft office",
                    "agile", "scrum", "management", "hr",
                    "finance", "accounting", "audit", "marketing"
            );

            List<String> requiredSkills = allSkills.stream()
                    .filter(descLower::contains)
                    .collect(Collectors.toList());

            for (String skill : requiredSkills) {
                if (cvLower.contains(skill)) {
                    cvMatchedSkills.add(skill);
                } else {
                    cvMissingSkills.add(skill);
                }
            }

            double skillsScore = requiredSkills.isEmpty() ? 0.5
                    : (double) cvMatchedSkills.size()
                    / requiredSkills.size();
            String location = detectLocationFromText(cvLower);
            double locationScore = calculateLocationScore(
                    location, jobOffer.getLocation());
            int expYears = detectExperienceYears(cvLower);
            double expScore = expYears >= 3 ? 1.0
                    : expYears >= 1 ? 0.7 : 0.3;

            cvScore = (int) Math.round(
                    (skillsScore * 0.50
                            + locationScore * 0.30
                            + expScore * 0.20) * 100);

            System.out.println("📄 CV Score: " + cvScore + "%");
        }

        // ✅ ÉTAPE 3 : Score Global
        // ✅ CV = 20% | Quiz = 80%
        int globalScore = (int) Math.round(
                cvScore * 0.20 + quizScore * 0.80);

        String globalLevel;
        if (globalScore >= 75)      globalLevel = "Excellent";
        else if (globalScore >= 60) globalLevel = "Good";
        else if (globalScore >= 40) globalLevel = "Average";
        else                        globalLevel = "Low";

        System.out.println("🎯 Global: " + globalScore
                + "% (CV 20% + Quiz 80%) (" + globalLevel + ")");

        // ✅ ÉTAPE 4 : Qualification
        boolean cvPassed   = cvScore >= 10;
        boolean quizPassed = quizScore >= 50;
        boolean qualified  = cvPassed && quizPassed
                && globalScore >= QUALIFICATION_THRESHOLD;

        LocalDateTime scheduledInterview = null;
        String message;

        if (qualified) {
            scheduledInterview = findNextAvailableSlot();
            message = "🎉 Congratulations! CV: " + cvScore
                    + "% | Quiz: " + quizScore
                    + "% | Global: " + globalScore
                    + "%. Interview scheduled on "
                    + scheduledInterview.toLocalDate()
                    + " at " + scheduledInterview.getHour() + ":00!";
            autoSchedule(request, jobOffer,
                    scheduledInterview, globalScore);

        } else if (!cvPassed) {
            message = "❌ CV score too low (" + cvScore
                    + "%). Please improve your CV and try again.";
        } else if (!quizPassed) {
            message = "❌ Quiz score too low (" + quizScore
                    + "%). Review the concepts and try again!";
        } else {
            message = "😔 Global score insufficient (" + globalScore
                    + "%). You need at least 60% to qualify.";
        }

        List<String> tips = generateTips(cvScore, quizScore,
                correctCount, totalQuestions, cvMissingSkills);

        return QuizResultDto.builder()
                .cvScore(cvScore)
                .quizScore(quizScore)
                .globalScore(globalScore)
                .globalLevel(globalLevel)
                .correctAnswers(correctCount)
                .totalQuestions(totalQuestions)
                .questionResults(questionResults)
                .cvMatchedSkills(cvMatchedSkills)
                .cvMissingSkills(cvMissingSkills)
                .qualified(qualified)
                .message(message)
                .scheduledInterview(scheduledInterview)
                .tips(tips)
                .build();
    }

    // ─── analyzeCvOnly ─────────────────────────────────────────────────────────

    @Override
    public int analyzeCvOnly(String cvUrl, Long jobOfferId) {
        JobOffer jobOffer = jobOfferRepository.findById(jobOfferId)
                .orElseThrow(() -> new RuntimeException(
                        "JobOffer not found"));

        String cvText = extractTextFromPdf(cvUrl);
        String cvLower = cvText.toLowerCase();
        String descLower = jobOffer.getDescription() != null
                ? jobOffer.getDescription().toLowerCase() : "";

        List<String> allSkills = Arrays.asList(
                "english", "french", "arabic", "spanish",
                "german", "italian", "bilingual", "multilingual",
                "fluent", "native", "proficient", "advanced",
                "communication", "public speaking", "presentation",
                "tourism", "hospitality", "hotel", "travel",
                "guide", "customer service", "reception",
                "teaching", "training", "tutoring", "education",
                "translation", "interpretation",
                "sales", "negotiation", "call center",
                "teamwork", "leadership", "adaptability",
                "management", "hr", "finance", "marketing"
        );

        List<String> requiredSkills = allSkills.stream()
                .filter(descLower::contains)
                .collect(Collectors.toList());

        long matched = requiredSkills.stream()
                .filter(cvLower::contains).count();

        double skillsScore = requiredSkills.isEmpty() ? 0.5
                : (double) matched / requiredSkills.size();

        String location = detectLocationFromText(cvLower);
        double locationScore = calculateLocationScore(
                location, jobOffer.getLocation());

        int expYears = detectExperienceYears(cvLower);
        double expScore = expYears >= 3 ? 1.0
                : expYears >= 1 ? 0.7 : 0.3;

        int cvScore = (int) Math.round(
                (skillsScore * 0.50
                        + locationScore * 0.30
                        + expScore * 0.20) * 100);

        System.out.println("📄 CV analysé: " + cvScore + "%");
        return cvScore;
    }

    // ─── Auto Schedule ─────────────────────────────────────────────────────────

    private void autoSchedule(QuizRequestDto request,
                              JobOffer jobOffer, LocalDateTime date, int score) {
        try {
            List<Application> existing = applicationRepository
                    .findByJobOfferId(request.getJobOfferId());

            Application app = existing.stream()
                    .filter(a -> request.getCvUrl() != null
                            && request.getCvUrl().equals(a.getCvUrl()))
                    .findFirst().orElse(null);

            if (app != null) {
                app.setStatus(ApplicationStatus.INTERVIEW);
                app.setInterviewDate(date);
                applicationRepository.save(app);
                System.out.println("✅ Entretien planifié: "
                        + app.getApplicantName() + " → " + date);

                try {
                    emailService.sendInterviewInvitation(
                            app.getApplicantEmail(),
                            app.getApplicantName(),
                            jobOffer.getTitle(),
                            jobOffer.getCompany(),
                            date);
                } catch (Exception e) {
                    log.warn("Email non envoyé: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Auto schedule error: {}", e.getMessage());
        }
    }

    // ─── Find Next Slot ─────────────────────────────────────────────────────────

    private LocalDateTime findNextAvailableSlot() {
        LocalDateTime candidate = LocalDateTime.now()
                .plusDays(1).withHour(9)
                .withMinute(0).withSecond(0).withNano(0);

        List<LocalDateTime> booked = applicationRepository
                .findByStatus(ApplicationStatus.INTERVIEW)
                .stream()
                .map(Application::getInterviewDate)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        for (int day = 0; day < 30; day++) {
            for (int hour = 9; hour <= 15; hour++) {
                LocalDateTime slot = candidate.withHour(hour);
                boolean taken = booked.stream().anyMatch(b ->
                        Math.abs(java.time.Duration
                                .between(b, slot).toMinutes()) < 60);
                if (!taken) return slot;
            }
            candidate = candidate.plusDays(1);
        }

        return LocalDateTime.now().plusDays(1)
                .withHour(9).withMinute(0)
                .withSecond(0).withNano(0);
    }

    // ─── Generate Tips ──────────────────────────────────────────────────────────

    private List<String> generateTips(int cvScore, int quizScore,
                                      int correct, int total, List<String> missingSkills) {
        List<String> tips = new ArrayList<>();

        if (cvScore < 10) {
            tips.add("📄 CV score too low — add more relevant details");
            if (!missingSkills.isEmpty()) {
                tips.add("🎯 Missing skills to add: "
                        + missingSkills.stream().limit(3)
                        .collect(Collectors.joining(", ")));
            }
            tips.add("✏️ Mention your language skills "
                    + "and years of experience");
        }

        if (quizScore < 50) {
            tips.add("📚 Quiz score too low — "
                    + "practice your English communication skills");
            tips.add("💡 " + (total - correct)
                    + " incorrect answer(s) out of " + total);
            tips.add("🔁 You can retake the test after reviewing");
        }

        if (cvScore >= 10 && quizScore >= 50) {
            tips.add("✅ CV and Quiz both passed!");
            tips.add("🚀 Prepare yourself for the interview");
            tips.add("🔍 Research the company before your interview");
            tips.add("💬 Practice your English speaking skills");
        }

        return tips;
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private String detectDomain(String description) {
        if (description.contains("tourism")
                || description.contains("guide")
                || description.contains("hotel")
                || description.contains("travel")) return "tourism";
        if (description.contains("teach")
                || description.contains("english")
                || description.contains("professor")
                || description.contains("training")) return "teaching";
        if (description.contains("translation")
                || description.contains("interpreter")) return "general";
        if (description.contains("call center")
                || description.contains("customer service")) return "general";
        return "general";
    }

    private String detectLocationFromText(String text) {
        return Arrays.asList("tunis", "sfax", "sousse",
                        "monastir", "bizerte", "nabeul", "ariana",
                        "marsa", "paris", "lyon", "remote")
                .stream().filter(text::contains)
                .findFirst().orElse(null);
    }

    private double calculateLocationScore(
            String detected, String preferred) {
        if (detected == null || preferred == null) return 0.5;
        return detected.equalsIgnoreCase(preferred.trim())
                ? 1.0 : 0.3;
    }

    private int detectExperienceYears(String text) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(\\d+)\\s*(?:ans?|years?|an)");
        java.util.regex.Matcher m = p.matcher(text);
        int max = 0;
        while (m.find()) {
            try {
                int y = Integer.parseInt(m.group(1));
                if (y > max && y < 50) max = y;
            } catch (NumberFormatException ignored) {}
        }
        return max;
    }

    private String extractTextFromPdf(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return "";
        try {
            byte[] bytes;
            if (fileUrl.startsWith("http")) {
                String fileName = fileUrl
                        .substring(fileUrl.lastIndexOf("/") + 1)
                        .trim().replaceAll("[\\r\\n%0A]", "");
                File f = new File("uploads/" + fileName);
                bytes = f.exists()
                        ? Files.readAllBytes(f.toPath())
                        : new URL(fileUrl.trim())
                        .openStream().readAllBytes();
            } else {
                File f = new File(fileUrl.trim());
                if (!f.exists()) {
                    f = new File("uploads/"
                            + fileUrl.replaceAll(".*[/\\\\]", "").trim());
                }
                if (!f.exists()) return "";
                bytes = Files.readAllBytes(f.toPath());
            }
            try (PDDocument doc = Loader.loadPDF(bytes)) {
                return new PDFTextStripper().getText(doc);
            }
        } catch (Exception e) {
            log.error("PDF error: {}", e.getMessage());
            return "";
        }
    }
}