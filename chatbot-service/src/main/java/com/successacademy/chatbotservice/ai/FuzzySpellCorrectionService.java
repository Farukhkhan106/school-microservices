package com.successacademy.chatbotservice.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class FuzzySpellCorrectionService {

    // Canonical ERP domain dictionary with common misspellings & synonyms (English & Hindi/Hinglish)
    private static final Map<String, List<String>> ERP_KEYWORD_SYNONYMS = new LinkedHashMap<>();

    static {
        // Attendance keywords
        ERP_KEYWORD_SYNONYMS.put("attendance", List.of(
            "attendance", "attendence", "attendece", "attandance", "atendance", "atendence", "attend",
            "absent", "absnt", "present", "prsnt", "presenti", "presentee", "hazri", "haziri", "rollcall", "roll call"
        ));

        // Student keywords
        ERP_KEYWORD_SYNONYMS.put("student", List.of(
            "student", "stuent", "studnt", "studnet", "sutdent", "stundet", "students", "stu",
            "baccha", "bachha", "bachhe", "vidyarthi", "shishya", "chhatra", "pupil", "learner"
        ));

        // Teacher / Faculty keywords
        ERP_KEYWORD_SYNONYMS.put("teacher", List.of(
            "teacher", "teachr", "techr", "techar", "faclty", "faculity", "faculty", "prof", "profeser",
            "professor", "sir", "madam", "shikshak", "adhypak", "adhyapak", "guruji", "instructor", "tutor"
        ));

        // Fees keywords
        ERP_KEYWORD_SYNONYMS.put("fees", List.of(
            "fees", "fee", "fe", "fess", "feedetail", "dues", "balance", "balanc", "bal", "pending",
            "paisa", "rupee", "rupees", "hisab", "shulk", "receipt", "ledger", "financial"
        ));

        // Details / Information keywords
        ERP_KEYWORD_SYNONYMS.put("details", List.of(
            "details", "detail", "detals", "detailes", "dtails", "information", "infomation", "infomtn",
            "info", "infor", "jankari", "maloomat", "batao", "dikhao", "profile", "record", "rcord"
        ));

        // Timetable / Schedule keywords
        ERP_KEYWORD_SYNONYMS.put("timetable", List.of(
            "timetable", "timetbl", "timtable", "time table", "schedule", "shedul", "shedule", "period",
            "periods", "routine", "classes today", "class routine"
        ));

        // Class keywords
        ERP_KEYWORD_SYNONYMS.put("class", List.of(
            "class", "clas", "clss", "kaksha", "grade", "standard", "std"
        ));

        // Section keywords
        ERP_KEYWORD_SYNONYMS.put("section", List.of(
            "section", "secton", "sec", "vibhag", "group"
        ));

        // Notice keywords
        ERP_KEYWORD_SYNONYMS.put("notice", List.of(
            "notice", "notis", "notce", "notices", "announcement", "circular", "khabar", "suchna", "suchana"
        ));

        // Event keywords
        ERP_KEYWORD_SYNONYMS.put("event", List.of(
            "event", "evnt", "events", "evnts", "program", "func", "function", "samaroh", "karyakram"
        ));

        // Inquiries / Contact keywords
        ERP_KEYWORD_SYNONYMS.put("inquiry", List.of(
            "inquiry", "inqury", "inquiries", "enquiry", "enqury", "contact", "contct", "shikayat", "complaint"
        ));

        // Analytics / Statistics keywords
        ERP_KEYWORD_SYNONYMS.put("analytics", List.of(
            "analytics", "anlytics", "statistics", "statistcs", "stastic", "stats", "summary", "overview", "telemetry"
        ));

        // Chatbot Greetings
        ERP_KEYWORD_SYNONYMS.put("greeting", List.of(
            "hi", "hello", "hey", "hola", "namaste", "namaskar", "kaise ho", "kese ho", "kya haal hai",
            "kya hal hai", "kya chal raha hai", "good morning", "good evening", "good afternoon", "shubh prabhat"
        ));
    }

    /**
     * Pre-processes query string:
     * 1. Collapses extreme letter repetitions (e.g. "likeee" -> "like", "pleeease" -> "please", "chatboat" -> "chatbot")
     * 2. Replaces common misspellings with canonical keywords for intent recognition
     */
    public String normalizeAndCorrect(String input) {
        if (input == null || input.isBlank()) return "";

        String normalized = input.toLowerCase().trim();

        // 1. Collapse duplicate characters occurring 3+ times in a row (e.g. "likeee" -> "like", "soooo" -> "so")
        normalized = normalized.replaceAll("(.)\\1{2,}", "$1");

        // 2. Specific domain replacements
        normalized = normalized.replaceAll("\\bchatboat\\b", "chatbot")
                               .replaceAll("\\bmistacke\\b", "mistake")
                               .replaceAll("\\bplz\\b|\\bpls\\b", "please");

        // 3. Token-by-token spell correction using Levenshtein distance
        String[] tokens = normalized.split("\\s+");
        StringBuilder corrected = new StringBuilder();

        for (String token : tokens) {
            String bestMatch = findBestKeywordMatch(token);
            if (bestMatch != null) {
                corrected.append(bestMatch).append(" ");
            } else {
                corrected.append(token).append(" ");
            }
        }

        return corrected.toString().trim();
    }

    /**
     * Checks if a word is close to an ERP canonical keyword (edit distance <= 2 or similarity >= 0.70).
     */
    private String findBestKeywordMatch(String word) {
        if (word == null || word.length() < 3) return null;

        // Skip numbers and common short prepositions
        if (word.matches("\\d+") || Set.of("the", "and", "for", "from", "with", "that", "this", "what", "how", "who", "all", "are", "you", "ke", "ki", "ka", "ko", "se", "hai", "hain").contains(word)) {
            return null;
        }

        for (Map.Entry<String, List<String>> entry : ERP_KEYWORD_SYNONYMS.entrySet()) {
            String canonical = entry.getKey();
            List<String> variants = entry.getValue();

            // Direct variant match
            if (variants.contains(word)) {
                return canonical;
            }

            // Fuzzy variant match (Levenshtein distance <= 2)
            for (String v : variants) {
                int dist = calculateLevenshteinDistance(word, v);
                int maxLen = Math.max(word.length(), v.length());
                double similarity = 1.0 - ((double) dist / maxLen);

                if (dist <= 2 && similarity >= 0.70 && Math.abs(word.length() - v.length()) <= 2) {
                    return canonical;
                }
            }
        }

        return null;
    }

    /**
     * Computes similarity score between 0.0 (completely different) and 1.0 (identical)
     */
    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        String a = s1.toLowerCase().trim();
        String b = s2.toLowerCase().trim();
        if (a.equals(b)) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;

        int dist = calculateLevenshteinDistance(a, b);
        int maxLen = Math.max(a.length(), b.length());
        return 1.0 - ((double) dist / maxLen);
    }

    /**
     * Classical Levenshtein Distance Algorithm (O(M*N))
     */
    public int calculateLevenshteinDistance(String a, String b) {
        if (a == null || b == null) return Integer.MAX_VALUE;
        int lenA = a.length();
        int lenB = b.length();

        int[][] dp = new int[lenA + 1][lenB + 1];

        for (int i = 0; i <= lenA; i++) dp[i][0] = i;
        for (int j = 0; j <= lenB; j++) dp[0][j] = j;

        for (int i = 1; i <= lenA; i++) {
            for (int j = 1; j <= lenB; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[lenA][lenB];
    }

    /**
     * Checks if input contains any greeting intent (English, Hindi, Hinglish)
     */
    public boolean isGreeting(String text) {
        if (text == null || text.isBlank()) return false;
        String s = text.toLowerCase().trim();
        List<String> greetings = List.of(
            "hi", "hello", "hey", "namaste", "namaskar", "kaise ho", "kese ho", "kya haal", "kya hal",
            "good morning", "good evening", "good afternoon", "shubh prabhat", "who are you", "help",
            "kya kar sakte ho", "madad"
        );
        for (String g : greetings) {
            if (s.equals(g) || s.startsWith(g + " ") || s.endsWith(" " + g)) {
                return true;
            }
        }
        return false;
    }
}
