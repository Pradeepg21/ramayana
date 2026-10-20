package com.satya.ramayana.game

import com.satya.ramayana.engine.Game
import com.satya.ramayana.stages.BaliStage
import com.satya.ramayana.stages.BaseStage
import com.satya.ramayana.stages.ForestStage
import com.satya.ramayana.stages.GurukulStage
import com.satya.ramayana.stages.HanumanStage
import com.satya.ramayana.stages.RavanStage
import com.satya.ramayana.stages.SetuStage
import com.satya.ramayana.stages.SitaSearchStage
import com.satya.ramayana.stages.SwayamvarStage
import com.satya.ramayana.stages.VanarSenaStage
import com.satya.ramayana.stages.YagnaStage

class StageInfo(
    val num: Int,
    val title: String,
    val place: String,
    val story: List<String>,
    val mission: String,
    val timeLimit: Float,
    /** Stages 4 and 10 count progress in percent, so their goal does not grow with the level. */
    val percentGoal: Boolean = false,
) {
    /** Knowledge and lesson for one of the 3 levels. */
    fun level(l: Int) = Stages.levels[num - 1][(l - 1).coerceIn(0, Stages.LEVELS - 1)]

    /** The lesson learnt just before this: the previous stage on the same level, or the final stage of the previous level. */
    fun previousLesson(l: Int): String? = when {
        num > 1 -> Stages.info(num - 1).level(l).lesson
        l > 1 -> Stages.info(Stages.COUNT).level(l - 1).lesson
        else -> null
    }
}

class LevelInfo(val knowledge: String, val lesson: String)

object Stages {
    const val COUNT = 10
    const val HANUMAN_STAGE = 6
    const val LEVELS = 3

    val all = listOf(
        StageInfo(
            1, "Bal Ram at the Gurukul", "Ayodhya",
            listOf(
                "Long ago, in the golden city of Ayodhya, lived the kind King Dasharath. He had four sons – Ram, Lakshman, Bharat and Shatrughan. Ram, the eldest, was gentle, truthful and loved by everyone.",
                "The princes went to the Gurukul of Guru Vashishtha to learn. Every morning young Ram practised archery with great patience. Guruji smiled – \"A good archer keeps a calm mind and a steady hand.\"",
            ),
            "Hit {n} moving targets with your arrows. Tap where you want to shoot!",
            60f
        ),
        StageInfo(
            2, "Protect the Yagna", "Forest Ashram",
            listOf(
                "One day, Rishi Vishwamitra came to Ayodhya. \"O King, demons keep disturbing our holy Yagna. Please send Ram and Lakshman to protect it.\"",
                "The brave brothers walked with the Rishi to his forest ashram. As the sacred fire was lit, the demons Tadaka, Maricha and Subahu came flying in to spoil the prayer!",
            ),
            "Stop {n} demons before they reach the Yagna fire. Watch out for big Tadaka!",
            60f
        ),
        StageInfo(
            3, "Sita Swayamvar", "Mithila",
            listOf(
                "In the kingdom of Mithila, King Janak announced that his daughter Sita would marry the hero who could lift and string the mighty bow of Lord Shiva.",
                "Great kings tried, but nobody could even move the bow! Then Ram bowed to his Guru, walked up calmly and lifted it as if it were a flower…",
            ),
            "Tap when the glowing needle is inside the golden zone. {n} perfect taps to string Shiva's bow!",
            45f
        ),
        StageInfo(
            4, "Vanvas – Journey to the Forest", "Dandaka Forest",
            listOf(
                "Years later, Queen Kaikeyi asked King Dasharath for two wishes: Bharat should be king, and Ram must live in the forest for 14 years.",
                "To keep his father's promise, Ram smiled and left the palace. Sita and Lakshman lovingly went with him. They walked through thick forests towards Panchavati.",
            ),
            "Tap to jump over rocks and thorny bushes. Collect fruits for extra time. Reach Panchavati!",
            50f, percentGoal = true
        ),
        StageInfo(
            5, "The Search for Sita", "Panchavati",
            listOf(
                "In Panchavati, the clever Ravan, king of Lanka, tricked Ram with a golden deer and carried Sita away in his flying chariot, the Pushpak Viman.",
                "Brave Sita dropped her jewels from the sky as a trail. The noble bird Jatayu told Ram: \"Ravan went south!\" Kind Shabari fed them sweet berries.",
            ),
            "Drag Ram left and right to catch {n} of Sita's jewels. Berries give extra time – avoid thorns!",
            50f
        ),
        StageInfo(
            6, "Meeting Hanuman", "Rishyamukh Mountain",
            listOf(
                "Near the Rishyamukh mountain, a wise vanar came to meet Ram. It was Hanuman, the son of Vayu, the wind god!",
                "When Hanuman saw Ram, his heart filled with love. He chanted \"Ram… Ram… Ram…\" and promised to serve him forever. His devotion gives him amazing strength!",
            ),
            "Tap only the lotuses that carry Ram's name (राम) to fill Hanuman's devotion. Earn HANUMAN SHAKTI!",
            45f
        ),
        StageInfo(
            7, "Help Sugriv – The Duel with Bali", "Kishkindha",
            listOf(
                "Hanuman took Ram to Sugriv, the vanar prince. Sugriv's powerful brother Bali had wrongly taken his kingdom and his family.",
                "Ram promised to help. Sugriv challenged Bali to a duel. To tell the brothers apart, Lakshman put a flower garland on Sugriv. Ram waited behind a tree to help his friend.",
            ),
            "Tap on Bali (the one with the golden crown) to help Sugriv. Don't hit Sugriv – he wears the flower garland!",
            50f
        ),
        StageInfo(
            8, "Build the Vanar Sena", "Kishkindha",
            listOf(
                "Sugriv became king and kept his promise. He called vanars and bears from every forest and mountain to help Ram.",
                "Wise Jambavan, the old king of bears, came too! But beware – Ravan's sneaky spies tried to hide among the army.",
            ),
            "Tap leaping vanars and bears to recruit {n} warriors. Jambavan counts as 3! Don't tap the purple spies.",
            50f
        ),
        StageInfo(
            9, "Ram Setu – Bridge over the Ocean", "Rameswaram",
            listOf(
                "Hanuman leapt across the ocean, found Sita in Lanka's Ashok Vatika and gave her Ram's ring. Now the whole army had to cross the sea!",
                "Nal and Neel said: \"Stones with Ram's name will float!\" The vanars dropped stones, and with Ram's name written on them, they floated to make a bridge.",
            ),
            "Tap each falling stone to write राम on it before it touches the water. Build a bridge of {n} stones!",
            55f
        ),
        StageInfo(
            10, "The Final Battle – Ravan", "Lanka",
            listOf(
                "The great battle began. Ravan, proud of his ten heads, fought with all his might. Every time a head was struck down, it grew back!",
                "Ravan's brother Vibhishan had joined Ram. He whispered: \"Prabhu, Ravan's strength is the Amrit hidden in his navel. Aim there!\"",
            ),
            "Shoot Ravan's heads to break his pride. When his navel glows, hit it with the golden arrow! Tap fireballs to stop them.",
            80f, percentGoal = true
        ),
    )

    fun info(n: Int) = all[(n - 1).coerceIn(0, COUNT - 1)]

    fun create(game: Game, n: Int, level: Int = 1): BaseStage = when (n) {
        1 -> GurukulStage(game, level)
        2 -> YagnaStage(game, level)
        3 -> SwayamvarStage(game, level)
        4 -> ForestStage(game, level)
        5 -> SitaSearchStage(game, level)
        6 -> HanumanStage(game, level)
        7 -> BaliStage(game, level)
        8 -> VanarSenaStage(game, level)
        9 -> SetuStage(game, level)
        else -> RavanStage(game, level)
    }

    /** Did-you-know facts and lessons, three levels per stage. */
    val levels = listOf(
        listOf(
            LevelInfo(
                "Ram was born on Ram Navami, the ninth day of the month of Chaitra. Ayodhya stood on the banks of the holy river Saryu, and King Dasharath belonged to the Raghu dynasty – so Ram is also called Raghav.",
                "Practice and discipline make us strong. Always respect your teachers."
            ),
            LevelInfo(
                "At the Gurukul, princes and ordinary students lived together. They woke before sunrise, served their Guru, and learnt the Vedas, archery, horse riding and good manners. Ram and Lakshman were always together, like best friends.",
                "Brothers and friends who stand together are never alone."
            ),
            LevelInfo(
                "Guru Vashishtha taught that a true warrior uses his weapons only to protect others, never to show off. Young Ram promised to always speak the truth – and he never broke that promise in his whole life.",
                "Speak the truth, and use your strength only to protect others."
            ),
        ),
        listOf(
            LevelInfo(
                "A Yagna is a prayer around a sacred fire, done for the good of everyone. Rishi Vishwamitra was once a mighty king who became a great sage through years of tapasya – deep meditation.",
                "We must protect good work and help the people who do it."
            ),
            LevelInfo(
                "On the way to the ashram, Vishwamitra taught Ram and Lakshman two special mantras, Bala and Atibala, so that they would never feel hungry, tired or sleepy on their mission.",
                "Learning from wise people gives us strength that never runs out."
            ),
            LevelInfo(
                "After the Yagna was safe, Ram touched a stone and it became Devi Ahalya, freed from a long curse. Ram treated everyone – rich or poor – with respect and kindness.",
                "Respect everyone. A kind touch can change someone's life."
            ),
        ),
        listOf(
            LevelInfo(
                "Lord Shiva's bow was called Pinaka. It was so heavy that it was kept in an iron box on a cart pulled by hundreds of strong men!",
                "True strength comes with humility and a calm mind."
            ),
            LevelInfo(
                "Sita was found as a baby by King Janak in a furrow while he was ploughing a field. That is why she is called Bhumija – the daughter of Mother Earth. Janak was a wise king who loved learning.",
                "Wisdom and goodness make a person truly royal."
            ),
            LevelInfo(
                "When the bow broke, Sage Parashuram arrived, very angry. Ram answered him calmly and respectfully, and Parashuram blessed him. On the same day, Lakshman, Bharat and Shatrughan were married too!",
                "Answer anger with calm and respectful words."
            ),
        ),
        listOf(
            LevelInfo(
                "Vanvas means living in the forest. Ram, Sita and Lakshman gave up their royal clothes, wore simple bark garments and lived like rishis for fourteen years.",
                "Keeping a promise is very important. Ram obeyed his father happily."
            ),
            LevelInfo(
                "The boatman Kevat lovingly washed Ram's feet and rowed them across the river Ganga. Nishadraj Guha, the tribal king, welcomed them too. Ram hugged them as dear friends.",
                "Love and friendship do not depend on riches or position."
            ),
            LevelInfo(
                "Bharat did not want the throne. He came to the forest and begged Ram to return. Finally he carried Ram's paduka – his wooden sandals – back to Ayodhya, placed them on the throne and ruled only as Ram's servant.",
                "True love for family means being selfless, like Bharat."
            ),
        ),
        listOf(
            LevelInfo(
                "Ravan sent the demon Maricha disguised as a golden deer. Lakshman drew a protective line – the Lakshman Rekha – around the hut, but Ravan came disguised as a sage asking for alms.",
                "Never give up hope. Be careful of tricks and strangers."
            ),
            LevelInfo(
                "The brave old eagle Jatayu fought Ravan to save Sita, even though he knew he could not win. Ram held Jatayu lovingly and honoured him like his own father.",
                "Courage means doing the right thing, even when it is hard."
            ),
            LevelInfo(
                "Shabari waited many years for Ram. She tasted every berry first so that only the sweetest ones reached him. Ram ate them happily – he valued her love more than any rules.",
                "God loves a pure heart more than anything else."
            ),
        ),
        listOf(
            LevelInfo(
                "Hanuman first met Ram disguised as a scholar, to find out whether he was a friend or a foe. Ram was amazed by how politely and wisely Hanuman spoke.",
                "Devotion and true friendship give us great strength."
            ),
            LevelInfo(
                "As a baby, Hanuman thought the rising sun was a ripe mango and leapt into the sky to eat it! The gods then blessed him with great strength, speed and wisdom.",
                "Use your powers with wisdom and humility."
            ),
            LevelInfo(
                "Hanuman is called Sankat Mochan – the one who removes troubles. His heart is so full of love that, in the stories, he opened his chest to show Ram and Sita living in his heart.",
                "Serving others with love makes us strong and fearless."
            ),
        ),
        listOf(
            LevelInfo(
                "Bali had a boon: anyone who fought him face to face lost half of their strength to him. That is why Ram helped Sugriv from behind the trees.",
                "Stand up for what is right and help a friend in need."
            ),
            LevelInfo(
                "Bali and Sugriv were brothers who once loved each other. A misunderstanding at the mouth of a cave turned them into enemies – they never sat down and talked it through.",
                "Talk and clear misunderstandings before they grow into fights."
            ),
            LevelInfo(
                "At the end, Bali asked Ram to look after his son Angad. Angad grew into a brave warrior in Ram's army, and Sugriv cared for him like his own son.",
                "Forgive others, and look after those who need help."
            ),
        ),
        listOf(
            LevelInfo(
                "King Sugriv sent search parties in all four directions to look for Sita. The bravest team – Hanuman, Angad and Jambavan – went south, towards the ocean.",
                "Together we are strong. Teamwork can do the impossible."
            ),
            LevelInfo(
                "At the seashore, wise Jambavan reminded Hanuman of his forgotten powers. Hanuman grew as big as a mountain and leapt a hundred yojanas across the ocean to Lanka!",
                "Good friends remind us of our strengths."
            ),
            LevelInfo(
                "Vibhishan, Ravan's own brother, told Ravan to return Sita. Ravan insulted him, so Vibhishan came to Ram for shelter. Ram welcomed him at once – he never turns away anyone who asks for help.",
                "Always give shelter to those who come to you for help."
            ),
        ),
        listOf(
            LevelInfo(
                "First, Ram prayed to Samudra Dev, the god of the ocean, for three days, asking for a path. Samudra Dev appeared and said that Nal and Neel, the vanar engineers, could build a bridge.",
                "Faith and hard work together can cross any ocean."
            ),
            LevelInfo(
                "A tiny squirrel helped too – it rolled in the sand and shook it off between the stones. Ram gently stroked its back, and people say the three stripes on squirrels are his finger marks!",
                "No help is too small. Every effort counts."
            ),
            LevelInfo(
                "Before crossing, Ram prayed to Lord Shiva and set up a Shivling at Rameswaram, which is still a holy place today. The vanar army finished the great bridge in just five days!",
                "Begin every big task with prayer and good planning."
            ),
        ),
        listOf(
            LevelInfo(
                "Before the war, Ram sent Angad to Ravan's court with a message of peace. Angad planted his foot on the floor, and not one of Ravan's mighty warriors could move it!",
                "Always try for peace first. Fighting is the very last choice."
            ),
            LevelInfo(
                "When Lakshman was struck down by a powerful weapon, Hanuman flew to the Himalayas for the Sanjeevani herb. He could not tell which plant it was – so he carried the whole mountain back!",
                "When a friend is in trouble, do everything you can."
            ),
            LevelInfo(
                "Ram won the battle on Dashami, so we celebrate Dussehra – Vijayadashami. When he returned to Ayodhya on the dark night of Amavasya, people lit rows of diyas everywhere. That is Diwali!",
                "Good always wins over evil. Pride (ahankar) leads to a fall."
            ),
        ),
    )
}
