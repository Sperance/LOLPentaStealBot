package ru.descend.bot.lolapi.champions

import com.google.gson.annotations.SerializedName
import ru.descend.bot.postgre.TableParticipant
import ru.descend.bot.savedObj.MMRCalculate
import ru.descend.bot.to2Digits
import ru.descend.bot.toModMax

data class ChampionsDTO(
    val type: String,
    val format: String,
    val version: String,
    val data: Data,
)

data class Data(
    @SerializedName("Aatrox") val aatrox: Aatrox,
    @SerializedName("Ahri") val ahri: Ahri,
    @SerializedName("Akali") val akali: Akali,
    @SerializedName("Akshan") val akshan: Akshan,
    @SerializedName("Alistar") val alistar: Alistar,
    @SerializedName("Amumu") val amumu: Amumu,
    @SerializedName("Anivia") val anivia: Anivia,
    @SerializedName("Annie") val annie: Annie,
    @SerializedName("Aphelios") val aphelios: Aphelios,
    @SerializedName("Ashe") val ashe: Ashe,
    @SerializedName("AurelionSol") val aurelionSol: AurelionSol,
    @SerializedName("Azir") val azir: Azir,
    @SerializedName("Bard") val bard: Bard,
    @SerializedName("Belveth") val belveth: Belveth,
    @SerializedName("Blitzcrank") val blitzcrank: Blitzcrank,
    @SerializedName("Brand") val brand: Brand,
    @SerializedName("Braum") val braum: Braum,
    @SerializedName("Briar") val briar: Briar,
    @SerializedName("Caitlyn") val caitlyn: Caitlyn,
    @SerializedName("Camille") val camille: Camille,
    @SerializedName("Cassiopeia") val cassiopeia: Cassiopeia,
    @SerializedName("Chogath") val chogath: Chogath,
    @SerializedName("Corki") val corki: Corki,
    @SerializedName("Darius") val darius: Darius,
    @SerializedName("Diana") val diana: Diana,
    @SerializedName("Draven") val draven: Draven,
    @SerializedName("DrMundo") val drMundo: DrMundo,
    @SerializedName("Ekko") val ekko: Ekko,
    @SerializedName("Elise") val elise: Elise,
    @SerializedName("Evelynn") val evelynn: Evelynn,
    @SerializedName("Ezreal") val ezreal: Ezreal,
    @SerializedName("Fiddlesticks") val fiddlesticks: Fiddlesticks,
    @SerializedName("Fiora") val fiora: Fiora,
    @SerializedName("Fizz") val fizz: Fizz,
    @SerializedName("Galio") val galio: Galio,
    @SerializedName("Gangplank") val gangplank: Gangplank,
    @SerializedName("Garen") val garen: Garen,
    @SerializedName("Gnar") val gnar: Gnar,
    @SerializedName("Gragas") val gragas: Gragas,
    @SerializedName("Graves") val graves: Graves,
    @SerializedName("Gwen") val gwen: Gwen,
    @SerializedName("Hecarim") val hecarim: Hecarim,
    @SerializedName("Heimerdinger") val heimerdinger: Heimerdinger,
    @SerializedName("Hwei") val hwei: Hwei,
    @SerializedName("Illaoi") val illaoi: Illaoi,
    @SerializedName("Irelia") val irelia: Irelia,
    @SerializedName("Ivern") val ivern: Ivern,
    @SerializedName("Janna") val janna: Janna,
    @SerializedName("JarvanIV") val jarvanIv: JarvanIv,
    @SerializedName("Jax") val jax: Jax,
    @SerializedName("Jayce") val jayce: Jayce,
    @SerializedName("Jhin") val jhin: Jhin,
    @SerializedName("Jinx") val jinx: Jinx,
    @SerializedName("Kaisa") val kaisa: Kaisa,
    @SerializedName("Kalista") val kalista: Kalista,
    @SerializedName("Karma") val karma: Karma,
    @SerializedName("Karthus") val karthus: Karthus,
    @SerializedName("Kassadin") val kassadin: Kassadin,
    @SerializedName("Katarina") val katarina: Katarina,
    @SerializedName("Kayle") val kayle: Kayle,
    @SerializedName("Kayn") val kayn: Kayn,
    @SerializedName("Kennen") val kennen: Kennen,
    @SerializedName("Khazix") val khazix: Khazix,
    @SerializedName("Kindred") val kindred: Kindred,
    @SerializedName("Kled") val kled: Kled,
    @SerializedName("KogMaw") val kogMaw: KogMaw,
    @SerializedName("KSante") val ksante: Ksante,
    @SerializedName("Leblanc") val leblanc: Leblanc,
    @SerializedName("LeeSin") val leeSin: LeeSin,
    @SerializedName("Leona") val leona: Leona,
    @SerializedName("Lillia") val lillia: Lillia,
    @SerializedName("Lissandra") val lissandra: Lissandra,
    @SerializedName("Lucian") val lucian: Lucian,
    @SerializedName("Lulu") val lulu: Lulu,
    @SerializedName("Lux") val lux: Lux,
    @SerializedName("Malphite") val malphite: Malphite,
    @SerializedName("Malzahar") val malzahar: Malzahar,
    @SerializedName("Maokai") val maokai: Maokai,
    @SerializedName("MasterYi") val masterYi: MasterYi,
    @SerializedName("Milio") val milio: Milio,
    @SerializedName("MissFortune") val missFortune: MissFortune,
    @SerializedName("MonkeyKing") val monkeyKing: MonkeyKing,
    @SerializedName("Mordekaiser") val mordekaiser: Mordekaiser,
    @SerializedName("Morgana") val morgana: Morgana,
    @SerializedName("Naafiri") val naafiri: Naafiri,
    @SerializedName("Nami") val nami: Nami,
    @SerializedName("Nasus") val nasus: Nasus,
    @SerializedName("Nautilus") val nautilus: Nautilus,
    @SerializedName("Neeko") val neeko: Neeko,
    @SerializedName("Nidalee") val nidalee: Nidalee,
    @SerializedName("Nilah") val nilah: Nilah,
    @SerializedName("Nocturne") val nocturne: Nocturne,
    @SerializedName("Nunu") val nunu: Nunu,
    @SerializedName("Olaf") val olaf: Olaf,
    @SerializedName("Orianna") val orianna: Orianna,
    @SerializedName("Ornn") val ornn: Ornn,
    @SerializedName("Pantheon") val pantheon: Pantheon,
    @SerializedName("Poppy") val poppy: Poppy,
    @SerializedName("Pyke") val pyke: Pyke,
    @SerializedName("Qiyana") val qiyana: Qiyana,
    @SerializedName("Quinn") val quinn: Quinn,
    @SerializedName("Rakan") val rakan: Rakan,
    @SerializedName("Rammus") val rammus: Rammus,
    @SerializedName("RekSai") val rekSai: RekSai,
    @SerializedName("Rell") val rell: Rell,
    @SerializedName("Renata") val renata: Renata,
    @SerializedName("Renekton") val renekton: Renekton,
    @SerializedName("Rengar") val rengar: Rengar,
    @SerializedName("Riven") val riven: Riven,
    @SerializedName("Rumble") val rumble: Rumble,
    @SerializedName("Ryze") val ryze: Ryze,
    @SerializedName("Samira") val samira: Samira,
    @SerializedName("Sejuani") val sejuani: Sejuani,
    @SerializedName("Senna") val senna: Senna,
    @SerializedName("Seraphine") val seraphine: Seraphine,
    @SerializedName("Sett") val sett: Sett,
    @SerializedName("Shaco") val shaco: Shaco,
    @SerializedName("Shen") val shen: Shen,
    @SerializedName("Shyvana") val shyvana: Shyvana,
    @SerializedName("Singed") val singed: Singed,
    @SerializedName("Sion") val sion: Sion,
    @SerializedName("Sivir") val sivir: Sivir,
    @SerializedName("Skarner") val skarner: Skarner,
    @SerializedName("Smolder") val smolder: Smolder,
    @SerializedName("Sona") val sona: Sona,
    @SerializedName("Soraka") val soraka: Soraka,
    @SerializedName("Swain") val swain: Swain,
    @SerializedName("Sylas") val sylas: Sylas,
    @SerializedName("Syndra") val syndra: Syndra,
    @SerializedName("TahmKench") val tahmKench: TahmKench,
    @SerializedName("Taliyah") val taliyah: Taliyah,
    @SerializedName("Talon") val talon: Talon,
    @SerializedName("Taric") val taric: Taric,
    @SerializedName("Teemo") val teemo: Teemo,
    @SerializedName("Thresh") val thresh: Thresh,
    @SerializedName("Tristana") val tristana: Tristana,
    @SerializedName("Trundle") val trundle: Trundle,
    @SerializedName("Tryndamere") val tryndamere: Tryndamere,
    @SerializedName("TwistedFate") val twistedFate: TwistedFate,
    @SerializedName("Twitch") val twitch: Twitch,
    @SerializedName("Udyr") val udyr: Udyr,
    @SerializedName("Urgot") val urgot: Urgot,
    @SerializedName("Varus") val varus: Varus,
    @SerializedName("Vayne") val vayne: Vayne,
    @SerializedName("Veigar") val veigar: Veigar,
    @SerializedName("Velkoz") val velkoz: Velkoz,
    @SerializedName("Vex") val vex: Vex,
    @SerializedName("Vi") val vi: Vi,
    @SerializedName("Viego") val viego: Viego,
    @SerializedName("Viktor") val viktor: Viktor,
    @SerializedName("Vladimir") val vladimir: Vladimir,
    @SerializedName("Volibear") val volibear: Volibear,
    @SerializedName("Warwick") val warwick: Warwick,
    @SerializedName("Xayah") val xayah: Xayah,
    @SerializedName("Xerath") val xerath: Xerath,
    @SerializedName("XinZhao") val xinZhao: XinZhao,
    @SerializedName("Yasuo") val yasuo: Yasuo,
    @SerializedName("Yone") val yone: Yone,
    @SerializedName("Yorick") val yorick: Yorick,
    @SerializedName("Yuumi") val yuumi: Yuumi,
    @SerializedName("Zac") val zac: Zac,
    @SerializedName("Zed") val zed: Zed,
    @SerializedName("Zeri") val zeri: Zeri,
    @SerializedName("Ziggs") val ziggs: Ziggs,
    @SerializedName("Zilean") val zilean: Zilean,
    @SerializedName("Zoe") val zoe: Zoe,
    @SerializedName("Zyra") val zyra: Zyra,
)

interface InterfaceChampionBase {
    val key: String
    val name: String
    val title: String
    val blurb: String
    val tags: List<String>
}

data class Aatrox(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {

    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setDamageTakenOnTeamPercentage(15.0)
        setMinionsKills(40.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(12.0)
        setEnemyChampionImmobilizations(30.0)
    }
}

data class Ahri(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(50.0)
//        setSkillsCast()
//        setTotalDamageShieldedOnTeammates()
        setTotalHealsOnTeammates(800.0)
        setTimeCCingOthers(12.0)
//        setEnemyChampionImmobilizations(20.0, 3.0)
//        setDamageTakenOnTeamPercentage(15.0)
    }
}

data class Akali(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(40.0)
//        setSkillsCast()
//        setTotalDamageShieldedOnTeammates()
        setTotalHealsOnTeammates(500.0)
        setTimeCCingOthers(5.0)
//        setEnemyChampionImmobilizations(20.0, 3.0)
//        setDamageTakenOnTeamPercentage(15.0)
    }
}

data class Akshan(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(40.0)
//        setSkillsCast()
//        setTotalDamageShieldedOnTeammates()
        setTotalHealsOnTeammates(500.0)
        setTimeCCingOthers(5.0)
//        setEnemyChampionImmobilizations(20.0, 3.0)
//        setDamageTakenOnTeamPercentage(15.0)
    }
}

data class Alistar(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(20.0)
        setSkillsCast(50.0)
//        setTotalDamageShieldedOnTeammates()
        setTotalHealsOnTeammates(2500.0)
        setTimeCCingOthers(25.0)
        setEnemyChampionImmobilizations(45.0)
        setDamageTakenOnTeamPercentage(10.0)
    }
}

data class Amumu(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(20.0)
//        setSkillsCast(60.0, 4.0)
//        setTotalDamageShieldedOnTeammates()
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(15.0)
        setEnemyChampionImmobilizations(40.0)
        setDamageTakenOnTeamPercentage(9.0)
    }
}

data class Anivia(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(30.0)
//        setSkillsCast(60.0, 4.0)
//        setTotalDamageShieldedOnTeammates()
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(25.0)
//        setEnemyChampionImmobilizations(40.0, 3.0)
//        setDamageTakenOnTeamPercentage(9.0)
    }
}

data class Annie(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(25.0)
        setSkillsCast(100.0)
        setTotalDamageShieldedOnTeammates(1000.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(20.0)
//        setEnemyChampionImmobilizations(40.0, 3.0)
//        setDamageTakenOnTeamPercentage(9.0)
    }
}

data class Aphelios(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(40.0)
//        setSkillsCast(100.0, 2.0)
//        setTotalDamageShieldedOnTeammates(1000.0, 3.0)
//        setTotalHealsOnTeammates(500.0, 2.0)
        setTimeCCingOthers(15.0)
//        setEnemyChampionImmobilizations(40.0, 3.0)
//        setDamageTakenOnTeamPercentage(9.0)
    }
}

data class Ashe(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(40.0)
//        setSkillsCast(100.0, 2.0)
//        setTotalDamageShieldedOnTeammates(1000.0, 3.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(25.0)
//        setEnemyChampionImmobilizations(40.0, 3.0)
//        setDamageTakenOnTeamPercentage(9.0)
    }
}

data class AurelionSol(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(40.0)
//        setSkillsCast(100.0, 2.0)
//        setTotalDamageShieldedOnTeammates(1000.0, 3.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(15.0)
//        setSkillshotsDodged(30.0, 3.0)
//        setEnemyChampionImmobilizations(40.0, 3.0)
//        setDamageTakenOnTeamPercentage(9.0)
    }
}

data class Azir(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(45.0)
//        setSkillsCast(100.0, 2.0)
//        setTotalDamageShieldedOnTeammates(1000.0, 3.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(10.0)
//        setEnemyChampionImmobilizations(40.0, 3.0)
//        setDamageTakenOnTeamPercentage(9.0)
    }
}

data class Bard(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(20.0)
        setSkillsCast(60.0)
        setTotalDamageShieldedOnTeammates(700.0)
        setTotalHealsOnTeammates(800.0)
        setTimeCCingOthers(18.0)
//        setEnemyChampionImmobilizations(40.0, 3.0)
    }
}

data class Belveth(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(30.0)
//        setSkillsCast(70.0, 2.0)
//        setTotalDamageShieldedOnTeammates(800.0, 4.0)
//        setTotalHealsOnTeammates(1000.0, 3.0)
        setTimeCCingOthers(10.0)
        setEnemyChampionImmobilizations(15.0)
        setDamageTakenOnTeamPercentage(8.0)
    }
}

data class Blitzcrank(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(10.0)
        setSkillsCast(60.0)
//        setTotalDamageShieldedOnTeammates(800.0, 4.0)
        setTotalHealsOnTeammates(500.0)
        setTimeCCingOthers(30.0)
        setEnemyChampionImmobilizations(50.0)
        setDamageTakenOnTeamPercentage(8.0)
    }
}

data class Brand(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(20.0)
        setSkillsCast(65.0)
//        setTotalDamageShieldedOnTeammates(800.0, 4.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(12.0)
//        setEnemyChampionImmobilizations(50.0)
//        setDamageTakenOnTeamPercentage(8.0)
    }
}

data class Braum(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(10.0)
        setSkillsCast(80.0)
        setTotalDamageShieldedOnTeammates(800.0)
        setTotalHealsOnTeammates(700.0)
        setTimeCCingOthers(20.0)
//        setEnemyChampionImmobilizations(50.0)
        setDamageTakenOnTeamPercentage(8.0)
    }
}

data class Briar(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(25.0)
//        setSkillsCast(80.0)
//        setTotalDamageShieldedOnTeammates(800.0)
//        setTotalHealsOnTeammates(700.0)
        setTimeCCingOthers(12.0)
//        setEnemyChampionImmobilizations(50.0)
        setDamageTakenOnTeamPercentage(7.0)
    }
}

data class Caitlyn(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(35.0)
//        setSkillsCast(80.0)
//        setTotalDamageShieldedOnTeammates(800.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(10.0)
//        setEnemyChampionImmobilizations(50.0)
//        setDamageTakenOnTeamPercentage(7.0)
    }
}

data class Camille(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(20.0)
//        setSkillsCast(80.0)
//        setTotalDamageShieldedOnTeammates(800.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(10.0)
//        setEnemyChampionImmobilizations(50.0)
//        setDamageTakenOnTeamPercentage(7.0)
    }
}

data class Cassiopeia(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(25.0)
        setSkillsCast(150.0)
//        setTotalDamageShieldedOnTeammates(800.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(28.0)
//        setEnemyChampionImmobilizations(50.0)
//        setDamageTakenOnTeamPercentage(7.0)
    }
}

data class Chogath(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(18.0)
        setSkillsCast(100.0)
//        setTotalDamageShieldedOnTeammates(800.0)
        setTotalHealsOnTeammates(500.0)
        setTimeCCingOthers(80.0)
//        setEnemyChampionImmobilizations(50.0)
//        setDamageTakenOnTeamPercentage(7.0)
    }
}

data class Corki(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(35.0)
        setSkillsCast(120.0)
//        setTotalDamageShieldedOnTeammates(800.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(12.0)
//        setEnemyChampionImmobilizations(50.0)
//        setDamageTakenOnTeamPercentage(7.0)
    }
}

data class Darius(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(25.0)
        setSkillsCast(80.0)
//        setTotalDamageShieldedOnTeammates(800.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(24.0)
//        setEnemyChampionImmobilizations(50.0)
        setDamageTakenOnTeamPercentage(6.0)
    }
}

data class Diana(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(24.0)
//        setSkillsCast(80.0)
//        setTotalDamageShieldedOnTeammates(800.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(4.0)
//        setEnemyChampionImmobilizations(50.0)
//        setDamageTakenOnTeamPercentage(6.0)
    }
}

data class Draven(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(55.0)
//        setSkillsCast(80.0)
//        setTotalDamageShieldedOnTeammates(800.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(8.0)
//        setEnemyChampionImmobilizations(50.0)
//        setDamageTakenOnTeamPercentage(6.0)
    }
}

data class DrMundo(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(22.0)
//        setSkillsCast(80.0)
//        setTotalDamageShieldedOnTeammates(800.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(8.0)
//        setEnemyChampionImmobilizations(50.0)
        setDamageTakenOnTeamPercentage(8.0)
    }
}

data class Ekko(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(24.0)
//        setSkillsCast(80.0)
//        setTotalDamageShieldedOnTeammates(800.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(20.0)
//        setEnemyChampionImmobilizations(50.0)
//        setDamageTakenOnTeamPercentage(8.0)
    }
}

data class Elise(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(18.0)
        setSkillsCast(140.0)
//        setTotalDamageShieldedOnTeammates(800.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(6.0)
//        setEnemyChampionImmobilizations(50.0)
//        setDamageTakenOnTeamPercentage(8.0)
    }
}

data class Evelynn(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(14.0)
        setSkillsCast(100.0)
//        setTotalDamageShieldedOnTeammates(800.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(6.0)
//        setEnemyChampionImmobilizations(50.0)
//        setDamageTakenOnTeamPercentage(8.0)
    }
}

data class Ezreal(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(60.0)
        setSkillsCast(260.0)
//        setTotalDamageShieldedOnTeammates(800.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(6.0)
//        setEnemyChampionImmobilizations(50.0)
//        setDamageTakenOnTeamPercentage(8.0)
    }
}

data class Fiddlesticks(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase {
    fun getMMR(participant: TableParticipant) = MMRCalculate(participant).apply {
        setMinionsKills(60.0)
        setSkillsCast(260.0)
//        setTotalDamageShieldedOnTeammates(800.0)
        setTotalHealsOnTeammates(400.0)
        setTimeCCingOthers(6.0)
//        setEnemyChampionImmobilizations(50.0)
//        setDamageTakenOnTeamPercentage(8.0)
    }
}

data class Fiora(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Fizz(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Galio(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Gangplank(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Garen(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Gnar(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Gragas(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Graves(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Gwen(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Hecarim(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Heimerdinger(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Hwei(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Illaoi(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Irelia(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Ivern(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Janna(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class JarvanIv(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Jax(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Jayce(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Jhin(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Jinx(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Kaisa(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Kalista(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Karma(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Karthus(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Kassadin(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Katarina(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Kayle(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Kayn(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Kennen(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Khazix(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Kindred(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Kled(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class KogMaw(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Ksante(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Leblanc(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class LeeSin(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Leona(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Lillia(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Lissandra(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Lucian(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Lulu(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Lux(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Malphite(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Malzahar(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Maokai(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class MasterYi(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Milio(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class MissFortune(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class MonkeyKing(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Mordekaiser(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Morgana(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Naafiri(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Nami(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Nasus(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Nautilus(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Neeko(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Nidalee(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Nilah(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Nocturne(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Nunu(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Olaf(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Orianna(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Ornn(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Pantheon(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Poppy(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Pyke(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Qiyana(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Quinn(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Rakan(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Rammus(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class RekSai(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Rell(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Renata(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Renekton(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Rengar(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Riven(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Rumble(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Ryze(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Samira(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Sejuani(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Senna(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Seraphine(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Sett(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Shaco(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Shen(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Shyvana(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Singed(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Sion(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Sivir(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Skarner(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Smolder(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Sona(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Soraka(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Swain(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Sylas(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Syndra(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class TahmKench(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Taliyah(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Talon(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Taric(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Teemo(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Thresh(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Tristana(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Trundle(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Tryndamere(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class TwistedFate(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Twitch(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Udyr(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Urgot(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Varus(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Vayne(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Veigar(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Velkoz(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Vex(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Vi(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Viego(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Viktor(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Vladimir(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Volibear(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Warwick(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Xayah(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Xerath(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class XinZhao(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Yasuo(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Yone(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Yorick(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Yuumi(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Zac(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Zed(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Zeri(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Ziggs(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Zilean(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Zoe(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase

data class Zyra(
    override val key: String,
    override val name: String,
    override val title: String,
    override val blurb: String,
    override val tags: List<String>
) : InterfaceChampionBase