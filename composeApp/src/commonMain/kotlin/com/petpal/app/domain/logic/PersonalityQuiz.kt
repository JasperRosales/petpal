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
            Answer("Explore a trail outdoors", listOf(BUNNY, FOX, PUP, DEER)),
            Answer("Curl up with a good book", listOf(OWL, TURTLE, KITTY, HEDGEHOG)),
            Answer("Hang out with friends", listOf(PUP, BEAR, FROG, OTTER)),
            Answer("Work on a personal project", listOf(DRAKE, SPROUT, RACCOON)),
        )),
        Question("A friend is upset. You...", listOf(
            Answer("Listen quietly", listOf(TURTLE, KITTY, HEDGEHOG, DEER)),
            Answer("Make them laugh", listOf(FROG, FOX, BUNNY, OTTER)),
            Answer("Help fix the problem", listOf(DRAKE, OWL, PENGUIN)),
            Answer("Bring them snacks", listOf(BEAR, SPROUT, PUP, PANDA)),
        )),
        Question("Pick a place to spend the day", listOf(
            Answer("A quiet forest", listOf(SPROUT, FOX, OWL, DEER, HEDGEHOG)),
            Answer("By a pond or the beach", listOf(FROG, TURTLE, PENGUIN)),
            Answer("Up in the mountains", listOf(DRAKE, BEAR, BAT)),
            Answer("A cozy room at home", listOf(KITTY, BUNNY, PUP, PANDA)),
        )),
        Question("How do you face a hard challenge?", listOf(
            Answer("Head-on", listOf(DRAKE, BEAR, PUP, LIZARD)),
            Answer("With a clever plan", listOf(FOX, OWL, KITTY, RACCOON)),
            Answer("Slow and steady", listOf(TURTLE, SPROUT, PENGUIN)),
            Answer("Go with the flow", listOf(FROG, BUNNY, PANDA)),
        )),
        Question("Your friends would describe you as...", listOf(
            Answer("Loyal", listOf(PUP, BEAR, PENGUIN)),
            Answer("Curious", listOf(FOX, KITTY, FROG, BAT)),
            Answer("Calm", listOf(TURTLE, SPROUT, OWL, DEER)),
            Answer("Bold", listOf(DRAKE, BUNNY, LIZARD)),
        )),
        Question("Favorite time of day?", listOf(
            Answer("Sunrise", listOf(SPROUT, BUNNY, PUP)),
            Answer("Midday", listOf(DRAKE, FROG, LIZARD)),
            Answer("Sunset", listOf(BEAR, TURTLE, FOX, LIZARD)),
            Answer("Midnight", listOf(OWL, KITTY, BAT, HEDGEHOG, RACCOON)),
        )),
        Question("In a group project, you are the...", listOf(
            Answer("Leader", listOf(DRAKE, BEAR)),
            Answer("Idea person", listOf(FOX, OWL, FROG, RACCOON)),
            Answer("Helper", listOf(PUP, SPROUT, TURTLE, PENGUIN)),
            Answer("Quiet solo worker", listOf(KITTY, BUNNY, BAT, HEDGEHOG)),
        )),
        Question("What recharges you the most?", listOf(
            Answer("A long nap", listOf(KITTY, BEAR, TURTLE, BAT, PANDA)),
            Answer("Moving and exercising", listOf(BUNNY, PUP, DRAKE, OTTER)),
            Answer("Time in nature", listOf(SPROUT, FROG, DEER)),
            Answer("Learning something new", listOf(OWL, FOX, RACCOON)),
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
