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

    /** [choices] = chosen answer index per question. Ties are broken at random. */
    fun evaluate(choices: List<Int>, rng: Random = Random.Default): Result {
        val raw = scores(choices)
        // Compare shapes, not sizes: each trait relative to its max, centered, then cosine similarity.
        val you = centered(FloatArray(raw.size) { raw[it].toFloat() / maxScores[it].coerceAtLeast(1) })
        val fit = profiles.mapValues { (_, p) -> cosine(you, centered(FloatArray(p.size) { p[it] / 3f })) }
        val best = fit.values.max()
        val species = fit.filterValues { it >= best - 1e-6f }.keys.random(rng)
        return Result(
            species,
            Trait.entries.associateWith { raw[it.ordinal].toFloat() / maxScores[it.ordinal].coerceAtLeast(1) },
            ((best + 1f) / 2f * 100).toInt().coerceIn(0, 100),
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
