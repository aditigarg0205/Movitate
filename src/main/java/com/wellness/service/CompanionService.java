package com.wellness.service;

import com.wellness.model.MoodEntry;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Offline, rule-based companion (no internet, no AI model, nothing leaves the computer).
 * It listens for topics, answers kindly, and suggests small coping steps.
 * Any sign of self-harm switches it to a fixed safety message with helplines.
 */
public class CompanionService {
    public record Reply(String text, boolean crisis) {}

    private static final String[] CRISIS = {
            "suicide", "suicidal", "kill myself", "end my life", "want to die", "wanna die",
            "hurt myself", "harm myself", "self harm", "self-harm", "no reason to live",
            "better off dead", "don't want to live", "dont want to live", "end it all", "cut myself",
            "marna chahta", "marna chahti", "mar jana chahta", "jeena nahi", "khud ko khatam"};

    private static final String[] NEGATED_POSITIVE = {
            "not happy", "not okay", "not ok", "not fine", "not good", "not great", "not well",
            "don't feel good", "dont feel good", "not feeling good", "not feeling okay", "not feeling well"};

    private final MoodService mood;
    private int turn;

    public CompanionService(MoodService mood) {
        this.mood = mood;
    }

    public String greeting(String name) {
        return "Hi " + name + ". I'm MindEase's built-in companion. I'm a simple program, not a person or a "
                + "therapist, but I'm happy to listen and share a few ideas. How are you feeling?";
    }

    public Reply reply(String userId, String message) {
        String m = message == null ? "" : message.toLowerCase(Locale.ROOT).trim();
        if (m.isEmpty()) return new Reply("I'm here whenever you're ready to share.", false);

        if (containsAny(m, CRISIS)) return new Reply(crisisMessage(), true);

        int t = turn++;
        Topic topic = detect(m);
        String text;
        if (topic == Topic.UNKNOWN) {
            text = unknown(userId, t);
        } else {
            text = pick(topic.openers, t) + " " + pick(topic.tips, t) + " " + pick(topic.questions, t);
        }
        return new Reply(text, false);
    }

    // ---------------- topics ----------------

    private enum Topic {
        SADNESS(new String[]{"sad", "down", "depressed", "hopeless", "empty", "cry", "crying", "worthless",
                "unhappy", "low", "numb", "udaas", "dukhi"},
                new String[]{"I'm sorry you're feeling this way. It makes sense to feel low sometimes, and you don't have to hide it here.",
                        "That sounds really heavy. Thank you for putting it into words.",
                        "Feeling down can be exhausting. You're not alone in this."},
                new String[]{"One small thing that sometimes helps: step outside for a few minutes of daylight, or wash your face with cool water.",
                        "If you can, do one tiny thing for yourself, like a glass of water or a short walk.",
                        "Writing a few lines in your journal can take some weight off. Reaching out to someone you trust can help too."},
                new String[]{"What do you think is weighing on you the most?",
                        "Did something happen today, or has it been building up?",
                        "Is there someone you feel comfortable talking to about this?"}),
        ANXIETY(new String[]{"anxious", "anxiety", "panic", "nervous", "worried", "worry", "scared", "afraid",
                "overthinking", "overthink", "restless", "dar lag", "ghabra"},
                new String[]{"That sounds uncomfortable. Anxiety can feel intense, but it does pass.",
                        "Thanks for telling me. Worry can make everything feel urgent.",
                        "I hear you. Your body may be on high alert right now."},
                new String[]{"Try slow breathing: in for 4 seconds, out for 6. The Breathe screen can guide you.",
                        "Try grounding: name 5 things you can see, 4 you can touch, 3 you can hear.",
                        "Put the worry on paper, then circle only the parts you can act on today."},
                new String[]{"What is your mind circling around right now?",
                        "Is this about something specific coming up?",
                        "How does it feel in your body at the moment?"}),
        STRESS(new String[]{"stress", "stressed", "pressure", "overwhelmed", "exam", "exams", "deadline", "workload",
                "burnout", "burned out", "too much", "tension"},
                new String[]{"That's a lot to carry. Feeling overwhelmed is a sign you're stretched, not that you're failing.",
                        "Pressure like that is draining. It's good that you're pausing to check in.",
                        "It sounds like your plate is very full."},
                new String[]{"Pick just the next smallest step and ignore the rest for 20 minutes.",
                        "Write everything down, then mark one thing as 'today' and let the rest wait.",
                        "A short break (stretch, water, fresh air) often makes the next hour more productive."},
                new String[]{"What is the one thing that feels most urgent?",
                        "Is there anything on your list that someone could help with or that could wait?",
                        "When did you last take a proper break?"}),
        SLEEP(new String[]{"sleep", "insomnia", "can't sleep", "cant sleep", "tired", "exhausted", "awake", "nightmare", "neend"},
                new String[]{"Poor sleep makes everything feel harder. I'm sorry you're dealing with that.",
                        "Being this tired is tough, and it affects mood a lot.",
                        "Rest matters so much, and it's frustrating when it won't come."},
                new String[]{"Try dimming lights and putting your phone away 30 minutes before bed, and keep a steady wake-up time.",
                        "If your mind is racing, jot the thoughts on paper so they don't have to stay in your head.",
                        "Slow breathing with a longer out-breath can help your body wind down."},
                new String[]{"What does your usual bedtime routine look like?",
                        "Is it hard to fall asleep, or do you wake up during the night?",
                        "Do racing thoughts keep you up?"}),
        LONELY(new String[]{"lonely", "alone", "no friends", "isolated", "nobody", "no one cares", "akela", "akelapan"},
                new String[]{"Feeling lonely hurts, and it's more common than people say.",
                        "Thank you for sharing that. Wanting connection is completely human.",
                        "I'm sorry you feel on your own right now."},
                new String[]{"A small step can help: send a simple message to someone you haven't spoken to in a while.",
                        "Being around people without pressure to talk, like a park or a library, can ease loneliness.",
                        "Joining a group around something you enjoy can make connection easier."},
                new String[]{"Is there someone you used to feel close to?",
                        "When do you feel the loneliest, at certain times of the day?",
                        "What's one activity you'd enjoy doing with others?"}),
        ANGER(new String[]{"angry", "anger", "furious", "frustrated", "irritated", "annoyed", "mad", "gussa", "hate"},
                new String[]{"It sounds like something really got to you. Anger is a valid feeling.",
                        "Frustration builds up fast. I'm glad you're letting it out here.",
                        "That sounds infuriating."},
                new String[]{"Give it a few minutes before acting: breathe slowly, or walk it off.",
                        "Writing out what you'd like to say, without sending it, can release a lot of tension.",
                        "Cold water on your hands or a few stretches can help your body come down from anger."},
                new String[]{"What happened?",
                        "What do you need right now: space, someone to listen, or a plan?",
                        "Is this about a person, or about a situation?"}),
        POSITIVE(new String[]{"happy", "great", "good", "better", "proud", "excited", "grateful", "relaxed", "calm",
                "wonderful", "achieved", "accomplished", "did well", "khush"},
                new String[]{"That's lovely to hear. I'm glad.",
                        "Wonderful. These moments are worth noticing.",
                        "That's great news. Well done."},
                new String[]{"Take a moment to notice what helped, so you can repeat it on harder days.",
                        "Why not note it in your journal? Good days are great to look back on.",
                        "Sharing good news with someone can double the joy."},
                new String[]{"What made today go well?",
                        "What are you most proud of?",
                        "Is there anyone you'd like to share this with?"}),
        THANKS(new String[]{"thank", "thanks", "shukriya", "dhanyavad"},
                new String[]{"You're welcome. I'm glad it helped.", "Anytime. Looking after yourself matters.",
                        "Happy to be here for you."},
                new String[]{"Remember you can check in on the Mood screen anytime.",
                        "A short breathing session can be a nice way to close the day.",
                        "Be kind to yourself today."},
                new String[]{"Is there anything else on your mind?", "How are you feeling now?",
                        "Anything else you'd like to talk through?"}),
        GREETING(new String[]{"hello", "hi", "hey", "namaste", "good morning", "good evening", "good afternoon"},
                new String[]{"Hello. It's good to see you here.", "Hi there. I'm glad you stopped by.",
                        "Hey. Welcome back."},
                new String[]{"Take a breath. There's no rush.", "This is a safe space to say what's on your mind.",
                        "You can tell me about your day, or how you're feeling."},
                new String[]{"How are you feeling today?", "What's on your mind?",
                        "How has your day been so far?"}),
        UNKNOWN(new String[0], new String[0], new String[0], new String[0]);

        final String[] keywords, openers, tips, questions;

        Topic(String[] keywords, String[] openers, String[] tips, String[] questions) {
            this.keywords = keywords;
            this.openers = openers;
            this.tips = tips;
            this.questions = questions;
        }
    }

    private static Topic detect(String m) {
        if (containsAny(m, NEGATED_POSITIVE)) return Topic.SADNESS;
        for (Topic t : List.of(Topic.SADNESS, Topic.ANXIETY, Topic.STRESS, Topic.SLEEP, Topic.LONELY,
                Topic.ANGER, Topic.POSITIVE, Topic.THANKS, Topic.GREETING))
            if (containsWord(m, t.keywords)) return t;
        return Topic.UNKNOWN;
    }

    private String unknown(String userId, int t) {
        Optional<MoodEntry> last = mood.latest(userId);
        String[] general = {
                "I'm listening. Could you tell me a bit more about how that feels?",
                "Thank you for sharing. What do you feel most strongly right now?",
                "I want to understand. What's been on your mind the most today?"};
        String base = pick(general, t);
        if (last.isPresent() && last.get().mood() <= 2)
            return base + " I noticed your last check-in was on the lower side, so go gently with yourself today.";
        return base;
    }

    private static String crisisMessage() {
        return "I'm really glad you told me, and I'm concerned about your safety. You deserve support from a "
                + "real person right now.\n\n"
                + "• Call 112 (emergency) or Tele-MANAS 14416 (free, 24x7).\n"
                + "• Tell someone near you, such as a family member, friend or neighbour, so you aren't alone.\n"
                + "• If you can, stay away from anything you could use to hurt yourself.\n\n"
                + "I'm only a simple app and can't give the help you deserve, but please reach out to one of these now.";
    }

    private static String pick(String[] options, int t) {
        return options[t % options.length];
    }

    /** Whole-word match, so "mad" does not match "made" and "low" does not match "follow". */
    private static boolean containsWord(String text, String[] words) {
        for (String w : words) {
            int from = 0, i;
            while ((i = text.indexOf(w, from)) >= 0) {
                boolean startOk = i == 0 || !Character.isLetter(text.charAt(i - 1));
                int end = i + w.length();
                boolean endOk = end >= text.length() || !Character.isLetter(text.charAt(end));
                if (startOk && endOk) return true;
                from = i + 1;
            }
        }
        return false;
    }

    private static boolean containsAny(String text, String[] needles) {
        for (String n : needles) if (text.contains(n)) return true;
        return false;
    }
}
