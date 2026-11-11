package com.petpal.app.domain.logic

import com.petpal.app.domain.logic.PersonalityQuiz.Trait.*
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Species.*
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Personality test that picks the device's one partner pet.
 * Every answer scores personality traits; the pet whose trait profile best matches yours wins.
 */
object PersonalityQuiz {
    enum class Trait(val label: String) {
        ENERGY("Energy"), SOCIAL("Social"), CURIOSITY("Curiosity"),
        CALM("Calm"), COURAGE("Courage"), HEART("Heart"),
    }

    data class Answer(val text: String, val traits: Map<Trait, Int>)
    data class Question(val text: String, val answers: List<Answer>)

    /** Your traits (0..1, relative to the max you could score in each) and how well the pet fits you. */
    data class Result(val species: Species, val traits: Map<Trait, Float>, val matchPercent: Int)

    private fun a(text: String, vararg t: Pair<Trait, Int>) = Answer(text, t.toMap())
    private fun q(text: String, vararg answers: Answer) = Question(text, answers.toList()).also { require(answers.size == 4) }

    val questions = listOf(
        q("It's a free Saturday. What do you do?",
            a("Explore a trail outdoors", ENERGY to 2, COURAGE to 1),
            a("Curl up with a good book", CURIOSITY to 2, CALM to 1),
            a("Hang out with friends", SOCIAL to 2, HEART to 1),
            a("Build or fix something", CURIOSITY to 1, COURAGE to 1, CALM to 1)),
        q("A friend is upset. You...",
            a("Listen quietly", HEART to 2, CALM to 1),
            a("Make them laugh", SOCIAL to 2, ENERGY to 1),
            a("Help fix the problem", CURIOSITY to 2, COURAGE to 1),
            a("Bring them snacks", HEART to 2, SOCIAL to 1)),
        q("Pick a place to spend the day",
            a("A quiet forest", CALM to 2, CURIOSITY to 1),
            a("The beach or a pool", ENERGY to 2, SOCIAL to 1),
            a("Up in the mountains", COURAGE to 2, ENERGY to 1),
            a("A cozy room at home", CALM to 2, HEART to 1)),
        q("How do you face a hard challenge?",
            a("Head-on", COURAGE to 3),
            a("With a clever plan", CURIOSITY to 2, CALM to 1),
            a("Slow and steady", CALM to 2, HEART to 1),
            a("Team up with friends", SOCIAL to 2, HEART to 1)),
        q("Your friends would describe you as...",
            a("Loyal", HEART to 3),
            a("Curious", CURIOSITY to 3),
            a("Calm", CALM to 3),
            a("Bold", COURAGE to 2, ENERGY to 1)),
        q("Favorite time of day?",
            a("Sunrise", ENERGY to 2, CALM to 1),
            a("Midday", SOCIAL to 2, ENERGY to 1),
            a("Sunset", CALM to 2, HEART to 1),
            a("Midnight", CURIOSITY to 2, COURAGE to 1)),
        q("In a group project, you are the...",
            a("Leader", COURAGE to 2, SOCIAL to 1),
            a("Idea person", CURIOSITY to 2, SOCIAL to 1),
            a("Helper", HEART to 2, SOCIAL to 1),
            a("Quiet solo worker", CALM to 2, CURIOSITY to 1)),
        q("What recharges you the most?",
            a("A long nap", CALM to 3),
            a("Moving and exercising", ENERGY to 3),
            a("A chat with friends", SOCIAL to 2, HEART to 1),
            a("Learning something new", CURIOSITY to 3)),
        q("Someone cuts in line in front of you. You...",
            a("Call it out right away", COURAGE to 3),
            a("Let it go, no big deal", CALM to 2, HEART to 1),
            a("Crack a joke about it", SOCIAL to 2, ENERGY to 1),
            a("Spot a faster line instead", CURIOSITY to 2, ENERGY to 1)),
        q("Pick a superpower",
            a("Super speed", ENERGY to 2, COURAGE to 1),
            a("Reading minds", CURIOSITY to 2, SOCIAL to 1),
            a("Healing others", HEART to 3),
            a("Invisibility", CALM to 2, CURIOSITY to 1)),
        q("Your pace in life is...",
            a("Fast, always on the go", ENERGY to 3),
            a("Steady and unhurried", CALM to 3),
            a("Wherever my friends are going", SOCIAL to 2, HEART to 1),
            a("One wild adventure at a time", COURAGE to 2, CURIOSITY to 1)),
        q("You find a lost wallet. You...",
            a("Track down the owner yourself", HEART to 2, COURAGE to 1),
            a("Hand it to someone in charge", CALM to 2, HEART to 1),
            a("Ask everyone nearby", SOCIAL to 2, ENERGY to 1),
            a("Look for clues inside", CURIOSITY to 2, COURAGE to 1)),
        q("Which snack are you?",
            a("Extra spicy chips", COURAGE to 2, ENERGY to 1),
            a("Warm cookies to share", HEART to 2, SOCIAL to 1),
            a("Trail mix for the road", ENERGY to 2, CURIOSITY to 1),
            a("Tea and toast", CALM to 2, CURIOSITY to 1)),
        q("At a party, you're...",
            a("In the middle of the dance floor", ENERGY to 2, SOCIAL to 1),
            a("Chatting with everyone", SOCIAL to 3),
            a("Deep in talk with one close friend", HEART to 2, CALM to 1),
            a("Checking out the host's bookshelf", CURIOSITY to 2, CALM to 1)),
        q("Something goes wrong at the last minute. You...",
            a("Jump in and take charge", COURAGE to 2, ENERGY to 1),
            a("Stay cool and work through it", CALM to 3),
            a("Rally everyone to help", SOCIAL to 2, COURAGE to 1),
            a("Make sure nobody is stressed", HEART to 2, CALM to 1)),
        q("Pick a weekend trip",
            a("A theme park", ENERGY to 2, SOCIAL to 1),
            a("A museum or old library", CURIOSITY to 3),
            a("Camping under the stars", COURAGE to 1, CALM to 1, CURIOSITY to 1),
            a("Visiting family", HEART to 2, SOCIAL to 1)),
        q("You get a surprise day off. First move?",
            a("Text everyone to make plans", SOCIAL to 3),
            a("Sleep in, no alarms", CALM to 3),
            a("Go somewhere you've never been", COURAGE to 2, CURIOSITY to 1),
            a("Go for a run", ENERGY to 3)),
        q("A new kid joins your class. You...",
            a("Wave them over to your table", HEART to 2, SOCIAL to 1),
            a("Ask them a hundred questions", CURIOSITY to 2, SOCIAL to 1),
            a("Show them around the school", ENERGY to 1, HEART to 1, COURAGE to 1),
            a("Give them space to settle in", CALM to 2, HEART to 1)),
        q("Pick a pet chore",
            a("Long walks every day", ENERGY to 3),
            a("Teaching it new tricks", CURIOSITY to 2, CALM to 1),
            a("Cuddles and brushing", HEART to 3),
            a("Taking it to the park to meet others", SOCIAL to 3)),
        q("There's a spider in the room. You...",
            a("Catch it and put it outside", COURAGE to 2, HEART to 1),
            a("Look up what kind it is", CURIOSITY to 3),
            a("Leave it be, it's fine", CALM to 3),
            a("Yell for backup", SOCIAL to 2, ENERGY to 1)),
        q("Which game do you pick?",
            a("A fast sport", ENERGY to 2, COURAGE to 1),
            a("A puzzle or strategy game", CURIOSITY to 2, CALM to 1),
            a("A party game with lots of people", SOCIAL to 3),
            a("A co-op game to help each other", HEART to 2, SOCIAL to 1)),
        q("You win a big prize. You...",
            a("Share it with family", HEART to 3),
            a("Throw a party", SOCIAL to 2, ENERGY to 1),
            a("Save it and plan carefully", CALM to 3),
            a("Spend it on a crazy adventure", COURAGE to 3)),
        q("How do you learn best?",
            a("Trying it myself, right away", ENERGY to 2, COURAGE to 1),
            a("Reading everything about it", CURIOSITY to 3),
            a("Studying with a group", SOCIAL to 3),
            a("Slowly, with lots of practice", CALM to 3)),
        q("Pick a weather",
            a("A wild thunderstorm", COURAGE to 3),
            a("A bright hot sunny day", ENERGY to 3),
            a("A soft rainy afternoon", CALM to 2, HEART to 1),
            a("A strange foggy morning", CURIOSITY to 3)),
        q("Your room is usually...",
            a("Full of half-done projects", CURIOSITY to 2, ENERGY to 1),
            a("Neat and peaceful", CALM to 3),
            a("Covered in photos of friends", SOCIAL to 2, HEART to 1),
            a("Hardly used, I'm always out", ENERGY to 2, COURAGE to 1)),
        q("Someone dares you to try a weird food. You...",
            a("Eat it right away", COURAGE to 3),
            a("Ask what's in it first", CURIOSITY to 3),
            a("Only if my friends try it too", SOCIAL to 3),
            a("Politely say no thanks", CALM to 3)),
        q("A stray animal follows you home. You...",
            a("Take care of it right away", HEART to 3),
            a("Find out who it belongs to", CURIOSITY to 2, HEART to 1),
            a("Post about it to get help", SOCIAL to 3),
            a("Stay calm and give it water", CALM to 2, HEART to 1)),
        q("Pick a job for a day",
            a("Firefighter", COURAGE to 3),
            a("Scientist", CURIOSITY to 3),
            a("Nurse", HEART to 3),
            a("Tour guide", SOCIAL to 2, ENERGY to 1)),
        q("Which motto fits you?",
            a("Go big or go home", COURAGE to 2, ENERGY to 1),
            a("Slow and steady wins", CALM to 3),
            a("The more the merrier", SOCIAL to 3),
            a("Never stop asking why", CURIOSITY to 3)),
        q("Your phone dies for a whole day. You...",
            a("Love the peace and quiet", CALM to 3),
            a("Go outside and play", ENERGY to 3),
            a("Miss my friends already", SOCIAL to 2, HEART to 1),
            a("Take it apart to see what broke", CURIOSITY to 2, COURAGE to 1)),
    )

    /** Each pet's personality, 0..3 per trait: ENERGY, SOCIAL, CURIOSITY, CALM, COURAGE, HEART. */
    val profiles: Map<Species, IntArray> = mapOf(
        SPROUT to intArrayOf(1, 1, 1, 3, 0, 3),
        KITTY to intArrayOf(1, 0, 3, 2, 1, 0),
        DRAKE to intArrayOf(2, 1, 0, 0, 3, 1),
        BUNNY to intArrayOf(3, 1, 1, 0, 2, 1),
        PUP to intArrayOf(2, 3, 0, 1, 1, 3),
        OWL to intArrayOf(0, 0, 3, 3, 0, 1),
        TURTLE to intArrayOf(0, 1, 0, 3, 1, 2),
        FOX to intArrayOf(2, 2, 3, 0, 1, 0),
        FROG to intArrayOf(2, 3, 1, 2, 0, 1),
        BEAR to intArrayOf(1, 1, 0, 1, 3, 3),
        PENGUIN to intArrayOf(1, 2, 0, 3, 2, 1),
        HEDGEHOG to intArrayOf(0, 0, 1, 2, 2, 2),
        BAT to intArrayOf(1, 0, 3, 1, 1, 2),
        DEER to intArrayOf(3, 1, 0, 2, 0, 2),
        OTTER to intArrayOf(3, 3, 1, 1, 0, 1),
        PANDA to intArrayOf(0, 2, 0, 3, 0, 2),
        LIZARD to intArrayOf(0, 0, 1, 3, 2, 0),
        RACCOON to intArrayOf(2, 1, 3, 0, 2, 0),
        HAMSTER to intArrayOf(3, 2, 1, 0, 1, 2),
        PARROT to intArrayOf(2, 3, 2, 0, 1, 1),
        AXOLOTL to intArrayOf(0, 2, 2, 2, 1, 3),
        MOLE to intArrayOf(2, 0, 2, 2, 1, 1),
        KOALA to intArrayOf(0, 1, 0, 3, 0, 2),
        SHARK to intArrayOf(3, 0, 0, 1, 3, 0),
        CHAMELEON to intArrayOf(1, 1, 3, 2, 0, 1),
        BEAVER to intArrayOf(2, 2, 1, 1, 1, 3),
        MONKEY to intArrayOf(3, 2, 2, 0, 2, 0),
        KANGAROO to intArrayOf(3, 1, 0, 1, 3, 2),
        EAGLE to intArrayOf(1, 0, 2, 1, 3, 0),
        SEAL to intArrayOf(1, 3, 0, 2, 0, 2),
        ELEPHANT to intArrayOf(1, 3, 2, 1, 2, 3),
        TIGER to intArrayOf(2, 0, 1, 0, 3, 1),
        TILAPIA to intArrayOf(0, 3, 1, 3, 1, 0),
    )

    /** Raw trait points for the chosen answer index per question. */
    fun scores(choices: List<Int>): IntArray {
        val s = IntArray(Trait.entries.size)
        choices.forEachIndexed { q, a -> questions[q].answers[a].traits.forEach { (t, p) -> s[t.ordinal] += p } }
        return s
    }

    /** Most anyone can score in each trait, used to show traits as 0..1 bars. */
    private val maxScores = IntArray(Trait.entries.size) { t ->
        questions.sumOf { q -> q.answers.maxOf { it.traits[Trait.entries[t]] ?: 0 } }
    }

    /** Average and spread of each trait for random answers, so the pet matches what stands out about you. */
    private val avg = FloatArray(Trait.entries.size) { t ->
        questions.sumOf { q -> q.answers.sumOf { it.traits[Trait.entries[t]] ?: 0 } }.toFloat() / 4f
    }
    private val spread = FloatArray(Trait.entries.size) { t ->
        val tr = Trait.entries[t]
        sqrt(questions.sumOf { q ->
            val m = q.answers.sumOf { it.traits[tr] ?: 0 } / 4.0
            q.answers.sumOf { a -> ((a.traits[tr] ?: 0) - m).let { it * it } } / 4.0
        }).toFloat()
    }

    /** Per-pet nudges tuned by simulation so every pet comes up about equally often (~3% each). */
    private val bias: Map<Species, Float> = mapOf(
        SPROUT to 0.035f, KITTY to -0.074f, DRAKE to -0.022f, BUNNY to -0.001f,
        PUP to -0.003f, OWL to -0.057f, TURTLE to 0.077f, FOX to -0.053f,
        FROG to 0.056f, BEAR to -0.021f, PENGUIN to 0.092f, HEDGEHOG to 0.031f,
        BAT to -0.095f, DEER to 0.026f, OTTER to 0.015f, PANDA to 0.108f,
        LIZARD to -0.013f, RACCOON to -0.066f, HAMSTER to 0.030f, PARROT to -0.061f,
        AXOLOTL to 0.002f, MOLE to -0.014f, KOALA to 0.066f, SHARK to -0.002f,
        CHAMELEON to -0.048f, BEAVER to -0.007f, MONKEY to -0.003f, KANGAROO to 0.049f,
        EAGLE to -0.079f, SEAL to 0.058f, ELEPHANT to -0.066f, TIGER to -0.014f,
        TILAPIA to 0.056f,
    )

    /** [choices] = chosen answer index per question. Ties are broken at random. */
    fun evaluate(choices: List<Int>, rng: Random = Random.Default): Result {
        val raw = scores(choices)
        // Each trait vs. the average answerer, then cosine vs. each pet's shape.
        val you = centered(FloatArray(raw.size) { (raw[it] - avg[it]) / spread[it].coerceAtLeast(1f) })
        val fit = profiles.mapValues { (_, p) -> cosine(you, centered(FloatArray(p.size) { p[it].toFloat() })) }
        val score = fit.mapValues { (s, f) -> f + (bias[s] ?: 0f) }
        val best = score.values.max()
        val species = score.filterValues { it >= best - 1e-6f }.keys.random(rng)
        return Result(
            species,
            Trait.entries.associateWith { raw[it.ordinal].toFloat() / maxScores[it.ordinal].coerceAtLeast(1) },
            ((fit.getValue(species) + 1f) / 2f * 100).toInt().coerceIn(0, 100),
        )
    }

    fun result(choices: List<Int>, rng: Random = Random.Default): Species = evaluate(choices, rng).species

    private fun centered(v: FloatArray): FloatArray {
        val mean = v.average().toFloat()
        return FloatArray(v.size) { v[it] - mean }
    }

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        var dot = 0f; var na = 0f; var nb = 0f
        for (i in a.indices) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
        return if (na == 0f || nb == 0f) 0f else dot / sqrt(na * nb)
    }
}
