package com.school.schoolservice.chatbot.service.impl;

import com.school.schoolservice.chatbot.dto.ChatMessageDto;
import com.school.schoolservice.chatbot.dto.ChatMessageDto.JobOfferSuggestion;
import com.school.schoolservice.chatbot.service.ChatbotService;
import com.school.schoolservice.joboffer.entity.JobOffer;
import com.school.schoolservice.joboffer.repository.JobOfferRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    private final JobOfferRepository jobOfferRepository;

    // ─── Keywords ─────────────────────────────────────────────────────────────

    private static final List<String> FR_GREETINGS = Arrays.asList(
            "bonjour", "salut", "bonsoir", "hello", "hi", "hey", "coucou"
    );
    private static final List<String> FR_SEARCH = Arrays.asList(
            "cherche", "recherche", "trouver", "offre", "emploi",
            "travail", "poste", "job", "opportunité", "stage",
            "alternance", "disponible", "ouvert"
    );
    private static final List<String> FR_APPLY = Arrays.asList(
            "postuler", "candidature", "candidater", "appliquer",
            "envoyer", "soumettre", "cv", "lettre"
    );
    private static final List<String> FR_INFO = Arrays.asList(
            "comment", "aide", "help", "info", "information",
            "expliquer", "qu'est", "quoi", "c'est", "fonctionne"
    );
    private static final List<String> FR_SALARY = Arrays.asList(
            "salaire", "rémunération", "paye", "combien", "tnd",
            "money", "salary", "pay", "budget"
    );
    private static final List<String> FR_DEADLINE = Arrays.asList(
            "deadline", "expiration", "date limite", "expire",
            "dernier délai", "clôture", "quand"
    );
    private static final List<String> FR_GOODBYE = Arrays.asList(
            "bye", "au revoir", "goodbye", "merci", "thanks",
            "à bientôt", "bonne journée", "ciao"
    );

    private static final List<String> EN_SEARCH = Arrays.asList(
            "looking", "find", "search", "job", "offer", "position",
            "work", "career", "opportunity", "internship", "vacancy",
            "hiring", "available", "open"
    );
    private static final List<String> EN_APPLY = Arrays.asList(
            "apply", "application", "submit", "send", "cv", "resume",
            "cover letter", "how to apply"
    );

    // ─── Contract types ───────────────────────────────────────────────────────

    private static final List<String> CONTRACT_CDI = Arrays.asList(
            "cdi", "permanent", "full-time", "temps plein"
    );
    private static final List<String> CONTRACT_CDD = Arrays.asList(
            "cdd", "contract", "fixed-term", "contrat temporaire"
    );
    private static final List<String> CONTRACT_STAGE = Arrays.asList(
            "stage", "internship", "intern", "stagiaire"
    );
    private static final List<String> CONTRACT_ALTERNANCE = Arrays.asList(
            "alternance", "apprentissage", "apprenticeship"
    );
    private static final List<String> CONTRACT_FREELANCE = Arrays.asList(
            "freelance", "consultant", "indépendant", "independent"
    );

    // ─── Cities ───────────────────────────────────────────────────────────────

    private static final List<String> CITIES = Arrays.asList(
            "tunis", "sfax", "sousse", "monastir", "bizerte", "nabeul",
            "gabes", "ariana", "marsa", "carthage", "manouba", "gafsa",
            "kairouan", "hammamet", "djerba",
            "paris", "lyon", "marseille", "bordeaux", "lille",
            "remote", "télétravail", "hybrid", "hybride"
    );

    // ─── Domains ──────────────────────────────────────────────────────────────

    private static final List<String> DOMAINS = Arrays.asList(
            "marketing", "finance", "accounting", "comptabilité",
            "rh", "hr", "human resources", "ressources humaines",
            "design", "it", "informatique", "developer", "développeur",
            "sales", "vente", "commercial", "management", "law", "droit",
            "audit", "communication", "engineering", "ingénieur",
            "tourisme", "tourism", "hotellerie", "hotel", "enseignement",
            "teaching", "teacher", "professeur", "traduction", "translation",
            "guide", "reception", "receptionist", "customer service",
            "service client", "call center", "anglais", "english"
    );

    // ─── Process Message ──────────────────────────────────────────────────────

    @Override
    public ChatMessageDto processMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return buildResponse("en", "UNKNOWN",
                    "Please type a message!",
                    "Veuillez taper un message !", null);
        }

        String msgLower = message.toLowerCase().trim();
        String language = detectLanguage(msgLower);
        String intent = detectIntent(msgLower);

        System.out.println("🤖 Message: " + message);
        System.out.println("🌍 Language: " + language);
        System.out.println("🎯 Intent: " + intent);

        switch (intent) {
            case "GREETING":  return handleGreeting(language);
            case "SEARCH":    return handleSearch(msgLower, language);
            case "APPLY":     return handleApply(language);
            case "INFO":      return handleInfo(language);
            case "SALARY":    return handleSalary(msgLower, language);
            case "DEADLINE":  return handleDeadline(language);
            case "GOODBYE":   return handleGoodbye(language);
            case "COUNT":     return handleCount(msgLower, language);
            case "HELP":      return handleHelp(language);
            default:          return handleUnknown(language);
        }
    }

    // ─── Detection ────────────────────────────────────────────────────────────

    private String detectLanguage(String msg) {
        long frScore = FR_SEARCH.stream().filter(msg::contains).count()
                + FR_APPLY.stream().filter(msg::contains).count()
                + FR_GREETINGS.stream().filter(msg::contains).count()
                + FR_INFO.stream().filter(msg::contains).count();
        long enScore = EN_SEARCH.stream().filter(msg::contains).count()
                + EN_APPLY.stream().filter(msg::contains).count();
        return frScore >= enScore ? "fr" : "en";
    }

    private String detectIntent(String msg) {
        boolean isGreeting  = FR_GREETINGS.stream().anyMatch(msg::contains);
        boolean isSearch    = FR_SEARCH.stream().anyMatch(msg::contains)
                || EN_SEARCH.stream().anyMatch(msg::contains);
        boolean isApply     = FR_APPLY.stream().anyMatch(msg::contains)
                || EN_APPLY.stream().anyMatch(msg::contains);
        boolean isInfo      = FR_INFO.stream().anyMatch(msg::contains);
        boolean isSalary    = FR_SALARY.stream().anyMatch(msg::contains);
        boolean isDeadline  = FR_DEADLINE.stream().anyMatch(msg::contains);
        boolean isGoodbye   = FR_GOODBYE.stream().anyMatch(msg::contains);
        boolean isCount     = msg.contains("combien") || msg.contains("how many")
                || msg.contains("nombre");
        boolean isHelp      = msg.contains("help") || msg.contains("aide")
                || msg.contains("commands") || msg.contains("commandes");

        if (isGoodbye)                    return "GOODBYE";
        if (isHelp && !isSearch)          return "HELP";
        if (isCount)                      return "COUNT";
        if (isSalary)                     return "SALARY";
        if (isDeadline)                   return "DEADLINE";
        if (isGreeting && !isSearch)      return "GREETING";
        if (isApply)                      return "APPLY";
        if (isSearch)                     return "SEARCH";
        if (isInfo)                       return "INFO";
        return "UNKNOWN";
    }

    private String detectCity(String msg) {
        return CITIES.stream()
                .filter(msg::contains)
                .findFirst().orElse(null);
    }

    private String detectContract(String msg) {
        if (CONTRACT_CDI.stream().anyMatch(msg::contains))
            return "CDI";
        if (CONTRACT_CDD.stream().anyMatch(msg::contains))
            return "CDD";
        if (CONTRACT_STAGE.stream().anyMatch(msg::contains))
            return "STAGE";
        if (CONTRACT_ALTERNANCE.stream().anyMatch(msg::contains))
            return "ALTERNANCE";
        if (CONTRACT_FREELANCE.stream().anyMatch(msg::contains))
            return "FREELANCE";
        return null;
    }

    private List<String> detectKeywords(String msg) {
        List<String> keywords = new ArrayList<>();
        DOMAINS.stream().filter(msg::contains).forEach(keywords::add);
        return keywords;
    }

    private Integer detectMaxSalary(String msg) {
        // ✅ Cherche patterns comme "moins de 3000", "less than 2000"
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(\\d+)");
        java.util.regex.Matcher m = p.matcher(msg);
        List<Integer> numbers = new ArrayList<>();
        while (m.find()) {
            try {
                int n = Integer.parseInt(m.group(1));
                if (n > 500 && n < 100000) numbers.add(n);
            } catch (NumberFormatException ignored) {}
        }
        return numbers.isEmpty() ? null : numbers.get(0);
    }

    // ─── Handlers ─────────────────────────────────────────────────────────────

    private ChatMessageDto handleGreeting(String lang) {
        String response = lang.equals("fr")
                ? "👋 Bonjour ! Je suis votre assistant de recrutement **Slang English**.\n\n"
                + "Je peux vous aider à :\n"
                + "🔍 Trouver des offres d'emploi\n"
                + "💰 Filtrer par salaire\n"
                + "📍 Chercher par ville\n"
                + "📝 Postuler à une offre\n"
                + "📊 Voir les statistiques\n\n"
                + "Tapez **'aide'** pour voir toutes mes commandes !\n"
                + "Que recherchez-vous ?"
                : "👋 Hello! I'm your **Slang English** recruitment assistant.\n\n"
                + "I can help you:\n"
                + "🔍 Find job offers\n"
                + "💰 Filter by salary\n"
                + "📍 Search by city\n"
                + "📝 Apply for a position\n"
                + "📊 View statistics\n\n"
                + "Type **'help'** to see all my commands!\n"
                + "What are you looking for?";

        return buildResponse(lang, "GREETING", response, response, null);
    }

    private ChatMessageDto handleSearch(String msg, String lang) {
        String city      = detectCity(msg);
        String contract  = detectContract(msg);
        List<String> keywords = detectKeywords(msg);
        Integer maxSalary = detectMaxSalary(msg);

        List<JobOffer> offers = jobOfferRepository.findByActiveTrue();

        // ✅ Filtre ville
        if (city != null) {
            final String fc = city;
            offers = offers.stream()
                    .filter(o -> o.getLocation() != null
                            && o.getLocation().toLowerCase().contains(fc))
                    .collect(Collectors.toList());
        }

        // ✅ Filtre contrat
        if (contract != null) {
            final String fct = contract;
            offers = offers.stream()
                    .filter(o -> o.getContractType() != null
                            && o.getContractType().name().equals(fct))
                    .collect(Collectors.toList());
        }

        // ✅ Filtre salaire
        if (maxSalary != null) {
            final int ms = maxSalary;
            offers = offers.stream()
                    .filter(o -> o.getSalary() == null
                            || parseSalary(o.getSalary()) <= ms)
                    .collect(Collectors.toList());
        }

        // ✅ Filtre mots-clés
        if (!keywords.isEmpty()) {
            offers = offers.stream()
                    .filter(o -> {
                        String text = ((o.getTitle() != null
                                ? o.getTitle() : "") + " "
                                + (o.getDescription() != null
                                ? o.getDescription() : ""))
                                .toLowerCase();
                        return keywords.stream().anyMatch(text::contains);
                    })
                    .collect(Collectors.toList());
        }

        List<JobOfferSuggestion> suggestions = offers.stream()
                .limit(5)
                .map(o -> JobOfferSuggestion.builder()
                        .id(o.getId())
                        .title(o.getTitle())
                        .company(o.getCompany())
                        .location(o.getLocation())
                        .contractType(o.getContractType() != null
                                ? o.getContractType().name() : "-")
                        .salary(o.getSalary())
                        .matchPercent(calculateMatchPercent(
                                o, city, contract, keywords))
                        .build())
                .sorted((a, b) -> Integer.compare(
                        b.getMatchPercent(), a.getMatchPercent()))
                .collect(Collectors.toList());

        String filters = buildFilterSummary(
                city, contract, maxSalary, lang);

        String response;
        if (suggestions.isEmpty()) {
            response = lang.equals("fr")
                    ? "😕 Aucune offre trouvée" + filters + ".\n\n"
                    + "💡 Essayez :\n"
                    + "• Sans filtres de ville\n"
                    + "• Un autre type de contrat\n"
                    + "• Des mots-clés différents"
                    : "😕 No offers found" + filters + ".\n\n"
                    + "💡 Try:\n"
                    + "• Without city filters\n"
                    + "• A different contract type\n"
                    + "• Different keywords";
        } else {
            response = lang.equals("fr")
                    ? "🎯 " + suggestions.size()
                    + " offre(s) trouvée(s)" + filters + " !\n"
                    + "👆 Cliquez sur une offre pour postuler !"
                    : "🎯 " + suggestions.size()
                    + " offer(s) found" + filters + "!\n"
                    + "👆 Click on an offer to apply!";
        }

        return buildResponse(lang, "SEARCH", response, response, suggestions);
    }

    private ChatMessageDto handleSalary(String msg, String lang) {
        Integer maxSalary = detectMaxSalary(msg);
        List<JobOffer> offers = jobOfferRepository.findByActiveTrue();

        if (maxSalary != null) {
            // ✅ Filtre par salaire max
            final int ms = maxSalary;
            offers = offers.stream()
                    .filter(o -> o.getSalary() == null
                            || parseSalary(o.getSalary()) <= ms)
                    .collect(Collectors.toList());

            List<JobOfferSuggestion> suggestions = offers.stream()
                    .limit(5)
                    .map(o -> JobOfferSuggestion.builder()
                            .id(o.getId())
                            .title(o.getTitle())
                            .company(o.getCompany())
                            .location(o.getLocation())
                            .contractType(o.getContractType() != null
                                    ? o.getContractType().name() : "-")
                            .salary(o.getSalary())
                            .matchPercent(70)
                            .build())
                    .collect(Collectors.toList());

            String response = lang.equals("fr")
                    ? "💰 " + suggestions.size()
                    + " offre(s) avec salaire ≤ " + maxSalary + " TND !"
                    : "💰 " + suggestions.size()
                    + " offer(s) with salary ≤ " + maxSalary + " TND!";

            return buildResponse(lang, "SALARY",
                    response, response, suggestions);
        }

        // ✅ Donne les stats salaires
        double avgSalary = offers.stream()
                .filter(o -> o.getSalary() != null && !o.getSalary().isEmpty())
                .mapToDouble(o -> parseSalary(o.getSalary()))
                .average().orElse(0);

        String response = lang.equals("fr")
                ? "💰 Informations sur les salaires :\n\n"
                + "📊 Salaire moyen des offres actives : "
                + Math.round(avgSalary) + " TND\n"
                + "🔍 Pour filtrer : 'offres moins de 3000 TND'\n"
                + "📈 Pour les mieux payées : 'meilleures offres salaire'"
                : "💰 Salary information:\n\n"
                + "📊 Average salary of active offers: "
                + Math.round(avgSalary) + " TND\n"
                + "🔍 To filter: 'offers less than 3000 TND'\n"
                + "📈 For best paid: 'best salary offers'";

        return buildResponse(lang, "SALARY", response, response, null);
    }

    private ChatMessageDto handleDeadline(String lang) {
        List<JobOffer> offers = jobOfferRepository.findByActiveTrue();

        // ✅ Offres avec expirationDate proche (7 jours)
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime in7days = now.plusDays(7);

        List<JobOfferSuggestion> urgentOffers = offers.stream()
                .filter(o -> o.getExpirationDate() != null)
                .filter(o -> o.getExpirationDate().isAfter(now)
                        && o.getExpirationDate().isBefore(in7days))
                .limit(5)
                .map(o -> JobOfferSuggestion.builder()
                        .id(o.getId())
                        .title(o.getTitle())
                        .company(o.getCompany())
                        .location(o.getLocation())
                        .contractType(o.getContractType() != null
                                ? o.getContractType().name() : "-")
                        .salary(o.getSalary())
                        .matchPercent(90)
                        .build())
                .collect(Collectors.toList());

        String response = lang.equals("fr")
                ? urgentOffers.isEmpty()
                ? "✅ Aucune offre n'expire dans les 7 prochains jours !"
                : "⚠️ " + urgentOffers.size()
                + " offre(s) expirent dans les 7 jours !\n"
                + "Postulez vite ! 🚀"
                : urgentOffers.isEmpty()
                ? "✅ No offers expire in the next 7 days!"
                : "⚠️ " + urgentOffers.size()
                + " offer(s) expire within 7 days!\n"
                + "Apply quickly! 🚀";

        return buildResponse(lang, "DEADLINE",
                response, response,
                urgentOffers.isEmpty() ? null : urgentOffers);
    }

    private ChatMessageDto handleCount(String msg, String lang) {
        List<JobOffer> allOffers = jobOfferRepository.findByActiveTrue();
        String city = detectCity(msg);
        String contract = detectContract(msg);

        long total = allOffers.size();
        long cdi = allOffers.stream()
                .filter(o -> o.getContractType() != null
                        && o.getContractType().name().equals("CDI")).count();
        long stage = allOffers.stream()
                .filter(o -> o.getContractType() != null
                        && o.getContractType().name().equals("STAGE")).count();
        long cdd = allOffers.stream()
                .filter(o -> o.getContractType() != null
                        && o.getContractType().name().equals("CDD")).count();

        String response = lang.equals("fr")
                ? "📊 Statistiques des offres actives :\n\n"
                + "📋 Total : **" + total + "** offres\n"
                + "💼 CDI : " + cdi + " offres\n"
                + "📄 CDD : " + cdd + " offres\n"
                + "🎓 Stage : " + stage + " offres\n\n"
                + "🔍 Cherchez par ville ou domaine pour affiner !"
                : "📊 Active offers statistics:\n\n"
                + "📋 Total: **" + total + "** offers\n"
                + "💼 CDI: " + cdi + " offers\n"
                + "📄 CDD: " + cdd + " offers\n"
                + "🎓 Internship: " + stage + " offers\n\n"
                + "🔍 Search by city or domain to refine!";

        return buildResponse(lang, "COUNT", response, response, null);
    }

    private ChatMessageDto handleApply(String lang) {
        String response = lang.equals("fr")
                ? "📝 **Comment postuler ?**\n\n"
                + "1️⃣ Tapez 'cherche [votre métier]'\n"
                + "2️⃣ Cliquez sur l'offre qui vous intéresse\n"
                + "3️⃣ Remplissez votre nom et email\n"
                + "4️⃣ Uploadez votre CV (PDF)\n"
                + "5️⃣ Uploadez votre lettre de motivation\n"
                + "6️⃣ Cliquez sur 'Submit Application'\n\n"
                + "💡 **Conseil :** Passez le test de compatibilité\n"
                + "pour augmenter vos chances !\n\n"
                + "🔍 Commencez par chercher une offre !"
                : "📝 **How to apply?**\n\n"
                + "1️⃣ Type 'find [your job]'\n"
                + "2️⃣ Click on the offer you like\n"
                + "3️⃣ Fill in your name and email\n"
                + "4️⃣ Upload your CV (PDF)\n"
                + "5️⃣ Upload your cover letter\n"
                + "6️⃣ Click on 'Submit Application'\n\n"
                + "💡 **Tip:** Take the compatibility test\n"
                + "to boost your chances!\n\n"
                + "🔍 Start by searching for an offer!";

        return buildResponse(lang, "APPLY", response, response, null);
    }

    private ChatMessageDto handleInfo(String lang) {
        String response = lang.equals("fr")
                ? "ℹ️ **Bienvenue sur Slang English !**\n\n"
                + "📌 Notre plateforme de recrutement spécialisée\n"
                + "dans les métiers nécessitant l'anglais :\n\n"
                + "🗺️ Guide touristique\n"
                + "👨‍🏫 Professeur d'anglais\n"
                + "📞 Agent call center\n"
                + "🏨 Réceptionniste\n"
                + "🌐 Traducteur / Interprète\n"
                + "✈️ Agent de voyage\n\n"
                + "🔍 Tapez 'aide' pour voir mes commandes !"
                : "ℹ️ **Welcome to Slang English!**\n\n"
                + "📌 Our recruitment platform specialized\n"
                + "in English-speaking jobs:\n\n"
                + "🗺️ Tour guide\n"
                + "👨‍🏫 English teacher\n"
                + "📞 Call center agent\n"
                + "🏨 Receptionist\n"
                + "🌐 Translator / Interpreter\n"
                + "✈️ Travel agent\n\n"
                + "🔍 Type 'help' to see my commands!";

        return buildResponse(lang, "INFO", response, response, null);
    }

    private ChatMessageDto handleHelp(String lang) {
        String response = lang.equals("fr")
                ? "🤖 **Mes commandes disponibles :**\n\n"
                + "🔍 **Recherche :**\n"
                + "• 'cherche emploi à Tunis'\n"
                + "• 'offres CDI marketing'\n"
                + "• 'stage guide touristique'\n\n"
                + "💰 **Salaire :**\n"
                + "• 'offres moins de 3000 TND'\n"
                + "• 'infos salaires'\n\n"
                + "📅 **Délais :**\n"
                + "• 'offres qui expirent bientôt'\n\n"
                + "📊 **Statistiques :**\n"
                + "• 'combien d offres disponibles'\n\n"
                + "📝 **Candidature :**\n"
                + "• 'comment postuler'\n\n"
                + "ℹ️ **Infos :**\n"
                + "• 'à propos de la plateforme'"
                : "🤖 **Available commands:**\n\n"
                + "🔍 **Search:**\n"
                + "• 'find job in Tunis'\n"
                + "• 'CDI marketing offers'\n"
                + "• 'internship tour guide'\n\n"
                + "💰 **Salary:**\n"
                + "• 'offers less than 3000 TND'\n"
                + "• 'salary information'\n\n"
                + "📅 **Deadlines:**\n"
                + "• 'offers expiring soon'\n\n"
                + "📊 **Statistics:**\n"
                + "• 'how many offers available'\n\n"
                + "📝 **Application:**\n"
                + "• 'how to apply'\n\n"
                + "ℹ️ **Info:**\n"
                + "• 'about the platform'";

        return buildResponse(lang, "HELP", response, response, null);
    }

    private ChatMessageDto handleGoodbye(String lang) {
        String response = lang.equals("fr")
                ? "👋 Au revoir ! Bonne chance dans votre recherche d'emploi !\n\n"
                + "💡 N'oubliez pas de passer le test de compatibilité\n"
                + "pour augmenter vos chances ! 🚀"
                : "👋 Goodbye! Good luck with your job search!\n\n"
                + "💡 Don't forget to take the compatibility test\n"
                + "to boost your chances! 🚀";

        return buildResponse(lang, "GOODBYE", response, response, null);
    }

    private ChatMessageDto handleUnknown(String lang) {
        String response = lang.equals("fr")
                ? "🤔 Je ne comprends pas votre demande.\n\n"
                + "Essayez par exemple :\n"
                + "• '**cherche emploi à Tunis**'\n"
                + "• '**offres CDI moins de 3000 TND**'\n"
                + "• '**combien d offres disponibles**'\n"
                + "• '**offres qui expirent bientôt**'\n\n"
                + "Tapez **'aide'** pour voir toutes les commandes !"
                : "🤔 I don't understand your request.\n\n"
                + "Try for example:\n"
                + "• '**find job in Tunis**'\n"
                + "• '**CDI offers less than 3000 TND**'\n"
                + "• '**how many offers available**'\n"
                + "• '**offers expiring soon**'\n\n"
                + "Type **'help'** to see all commands!";

        return buildResponse(lang, "UNKNOWN", response, response, null);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String buildFilterSummary(String city, String contract,
                                      Integer maxSalary, String lang) {
        StringBuilder sb = new StringBuilder();
        if (city != null || contract != null || maxSalary != null) {
            sb.append(lang.equals("fr") ? " pour " : " for ");
            if (city != null) sb.append("📍 ")
                    .append(city).append(" ");
            if (contract != null) sb.append("📋 ")
                    .append(contract).append(" ");
            if (maxSalary != null) sb.append("💰 ≤")
                    .append(maxSalary).append(" TND ");
        }
        return sb.toString().trim();
    }

    private double parseSalary(String salary) {
        if (salary == null || salary.isEmpty()) return 0;
        try {
            return Double.parseDouble(
                    salary.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private ChatMessageDto buildResponse(String lang, String intent,
                                         String enResponse, String frResponse,
                                         List<JobOfferSuggestion> suggestions) {
        return ChatMessageDto.builder()
                .botResponse(lang.equals("fr") ? frResponse : enResponse)
                .intent(intent)
                .language(lang)
                .suggestions(suggestions)
                .build();
    }

    private int calculateMatchPercent(JobOffer offer, String city,
                                      String contract, List<String> keywords) {
        int score = 30;
        if (city != null && offer.getLocation() != null
                && offer.getLocation().toLowerCase().contains(city))
            score += 30;
        if (contract != null && offer.getContractType() != null
                && offer.getContractType().name().equals(contract))
            score += 25;
        if (!keywords.isEmpty()) {
            String text = ((offer.getTitle() != null
                    ? offer.getTitle() : "") + " "
                    + (offer.getDescription() != null
                    ? offer.getDescription() : "")).toLowerCase();
            long matches = keywords.stream()
                    .filter(text::contains).count();
            score += (int)(matches * 15.0 / keywords.size());
        }
        return Math.min(score, 100);
    }
}