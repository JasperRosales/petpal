package com.petpal.app.domain.logic

import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Species.*
import kotlin.random.Random

/** Personality test that picks the device's one partner pet. */
object PersonalityQuiz {
    data class Answer(val text: String, val points: List<Species>)
    data class Question(val text: String, val answers: List<Answer>)

    val questions = listOf(
        Question("It's a free Saturday. What do you do?", listOf(
            Answer("Explore a trail outdoors", listOf(BUNNY, FOX, PUP)),
            Answer("Curl up with a good book", listOf(OWL, TURTLE, KITTY)),
            Answer("Hang out with friends", listOf(PUP, BEAR, FROG)),
            Answer("Work on a personal project", listOf(DRAKE, SPROUT)),
        )),
        Question("A friend is upset. You...", listOf(
            Answer("Listen quietly", listOf(TURTLE, KITTY)),
            Answer("Make them laugh", listOf(FROG, FOX, BUNNY)),
            Answer("Help fix the problem", listOf(DRAKE, OWL)),
            Answer("Bring them snacks", listOf(BEAR, SPROUT, PUP)),
        )),
        Question("Pick a place to spend the day", listOf(
            Answer("A quiet forest", listOf(SPROUT, FOX, OWL)),
            Answer("By a pond or the beach", listOf(FROG, TURTLE)),
            Answer("Up in the mountains", listOf(DRAKE, BEAR)),
            Answer("A cozy room at home", listOf(KITTY, BUNNY, PUP)),
        )),
        Question("How do you face a hard challenge?", listOf(
            Answer("Head-on", listOf(DRAKE, BEAR, PUP)),
            Answer("With a clever plan", listOf(FOX, OWL, KITTY)),
            Answer("Slow and steady", listOf(TURTLE, SPROUT)),
            Answer("Go with the flow", listOf(FROG, BUNNY)),
        )),
        Question("Your friends would describe you as...", listOf(
            Answer("Loyal", listOf(PUP, BEAR)),
            Answer("Curious", listOf(FOX, KITTY, FROG)),
            Answer("Calm", listOf(TURTLE, SPROUT, OWL)),
            Answer("Bold", listOf(DRAKE, BUNNY)),
        )),
        Question("Favorite time of day?", listOf(
            Answer("Sunrise", listOf(SPROUT, BUNNY, PUP)),
            Answer("Midday", listOf(DRAKE, FROG)),
            Answer("Sunset", listOf(BEAR, TURTLE, FOX)),
            Answer("Midnight", listOf(OWL, KITTY)),
        )),
        Question("In a group project, you are the...", listOf(
            Answer("Leader", listOf(DRAKE, BEAR)),
            Answer("Idea person", listOf(FOX, OWL, FROG)),
            Answer("Helper", listOf(PUP, SPROUT, TURTLE)),
            Answer("Quiet solo worker", listOf(KITTY, BUNNY)),
        )),
        Question("What recharges you the most?", listOf(
            Answer("A long nap", listOf(KITTY, BEAR, TURTLE)),
            Answer("Moving and exercising", listOf(BUNNY, PUP, DRAKE)),
            Answer("Time in nature", listOf(SPROUT, FROG)),
            Answer("Learning something new", listOf(OWL, FOX)),
        )),
    )

    /** [choices] = chosen answer index per question. Ties are broken at random. */
    fun result(choices: List<Int>, rng: Random = Random.Default): Species {
        val score = IntArray(Species.entries.size)
        choices.forEachIndexed { q, a -> questions[q].answers[a].points.forEach { score[it.ordinal]++ } }
        val best = score.max()
        return Species.entries.filter { score[it.ordinal] == best }.random(rng)
    }
}
