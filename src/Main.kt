import java.util.*

// 盤のサイズ
const val BAN_SIZE: Int = 8
// 探索する深さ
const val SEARCH_DEPTH: Int = 4
// 石を裏返す向き
val DIRECTIONS: Array<Pair<Int, Int>> =
    arrayOf(Pair(-1, -1), Pair(-1, 0), Pair(-1, 1), Pair(0, -1), Pair(0, 1), Pair(1, -1), Pair(1, 0), Pair(1, 1))
// 角の場所
val KADOS: Array<Pair<Int, Int>> =
    arrayOf(Pair(0, 0), Pair(0, BAN_SIZE - 1), Pair(BAN_SIZE - 1, 0), Pair(BAN_SIZE - 1, BAN_SIZE - 1))

var calcScoreTime: Long = 0L
var scoredPositionTime: Long = 0L
var copiedBanmenTime: Long = 0L

fun main() {
    println("main ${Thread.currentThread().id}")
    // 初期値の盤を生成する
    var ban = createNewBan()
    show(ban)

    do {
        // マスを取得する
        val position = when (ban.teban) {
            Stone.BLACK -> getInputPosition(ban)    // 黒：プレイヤー
            Stone.WHITE -> getScoredPosition(ban)   // 白：マシン
        }
        ban = position?.let {
            ban.putStone(it)
        } ?: run {
            println("pass...")
            ban.pass()
        }
        // 盤を表示する
        show(ban)

        println("scoredPositionTime = ${scoredPositionTime}")
        println("calcScoreTime = ${calcScoreTime}")
        println("copiedBanmenTime = ${copiedBanmenTime}")
        calcScoreTime = 0L
    } while (!ban.isGameEnd())
    println("game end...")
}

fun getScoredPosition(ban: Ban): Pair<Int, Int>? {
    val start = Date().time
    val result = ban.getPositions()
        .map {
            val nextBan = ban.putStone(it)
            val score = getMinScore(nextBan, 0, null)
            Pair(it, score)
        }
        .maxBy { it.second }
        ?.let { it.first }
    scoredPositionTime = (Date().time - start)
    return result
}

// 自分の手番
fun getMaxScore(currentBan: Ban, depth: Int, b: Int?): Int {

    val positions = currentBan.getPositions()

    // 所定の深さなら
    if (depth >= SEARCH_DEPTH) {
        // 評価値を返却する
        return currentBan.calcScore(positions);
    }

    var maxScore = 0
    for (position in currentBan.getPositions()) {
        val score = getMinScore(currentBan.putStone(position), depth, maxScore)
        if (maxScore < score) {
            maxScore = score
        }
        // βカット
        if (b != null && maxScore > b) {
            return maxScore
        }
    }
    return maxScore
}

// 相手の手番
fun getMinScore(currentBan: Ban, depth: Int, a: Int?): Int {
    var minScore = 100
    for (position in currentBan.getPositions()) {
        val score = getMaxScore(currentBan.putStone(position), depth + 1, minScore)
        if (minScore > score) {
            minScore = score
        }
        // αカット
        if (a != null && minScore < a) {
            return minScore
        }
    }
    return minScore
}

// 初期値で盤を生成する
fun createNewBan(): Ban {
    var banmen: Array<Array<Stone?>> = Array(BAN_SIZE, { arrayOfNulls<Stone?>(BAN_SIZE) })
    banmen[BAN_SIZE / 2][BAN_SIZE / 2] = Stone.WHITE
    banmen[BAN_SIZE / 2 - 1][BAN_SIZE / 2 - 1] = Stone.WHITE
    banmen[BAN_SIZE / 2 - 1][BAN_SIZE / 2] = Stone.BLACK
    banmen[BAN_SIZE / 2][BAN_SIZE / 2 - 1] = Stone.BLACK
    return Ban(banmen, Stone.BLACK)
}

// 入力する：デバッグ用
fun getInputPosition(ban: Ban): Pair<Int, Int>? {
    val positions = ban.getPositions()
    if (positions.isEmpty()) {
        return null
    }
    while (true) {
        // 入力を受け付ける
        val input = readLine()
        try {
            // 入力値を数値に変換する
            val inputNumber = input?.toInt()
            // 入力値のマスが存在すれば
            positions.withIndex().find { (it.index + 1) == inputNumber }?.let {
                // 選択マスとして返却する
                return it.value
            }
        } catch (e: java.lang.NumberFormatException) {
            ;
        }
    }
}

// 表示する：デバッグ用
fun show(ban: Ban) {
    println("-------------")
    when (ban.teban) {
        Stone.BLACK -> println("next ●")
        Stone.WHITE -> println("next ○")
    }
    val positions = ban.getPositions()

    ban.banmen.withIndex().forEach { array ->
        array.value.withIndex().forEach { masu ->
            masu.value.also {
                when (it) {
                    Stone.BLACK -> print("|●")
                    Stone.WHITE -> print("|○")
                }
            } ?: run {
                positions.withIndex().find { it.value == Pair(array.index, masu.index) }?.let {
                    print("|${it.index + 1}")
                } ?: run {
                    print("| ")
                }
            }
        }
        println("|")
    }
}

// 石を表現する
enum class Stone {
    BLACK {
        override fun reverse() = WHITE
    },
    WHITE {
        override fun reverse() = BLACK
    };

    // 裏返す
    abstract fun reverse(): Stone
}

// 盤を表現する
class Ban(var banmen: Array<Array<Stone?>>, var teban: Stone) {
    // 石を置けるマスを取得する
    fun getPositions(): List<Pair<Int, Int>> {
//        // 18096
//        return banmen.mapIndexed { y, array ->
//            array.mapIndexedNotNull { x, stone ->
//                when (stone) {
//                    null -> Pair(y, x)
//                    else -> null
//                }
//            }
//        }.flatten().filter { canPutStone(it) }

        // 16460
        var results: List<Pair<Int, Int>> = mutableListOf()
        for (y in 0 until BAN_SIZE) {
            for (x in 0 until BAN_SIZE) {
                val position: Pair<Int, Int> = Pair(y, x)
                if (getStone(position) == null && canPutStone(position)) {
                    results += position
                }
            }
        }
        return results
    }

    // 1行毎の反転数を取得する
    fun getReverseCountInLine(position: Pair<Int, Int>, direction: Pair<Int, Int>, reverseCount: Int): Int {
        val newPosition = Pair(position.first + direction.first, position.second + direction.second)
        return when (getStone(newPosition)) {
            teban -> reverseCount
            teban.reverse() -> getReverseCountInLine(newPosition, direction, reverseCount + 1)
            else -> 0
        }
    }

    // 反転数を取得する
    private fun canPutStone(position: Pair<Int, Int>): Boolean =
        DIRECTIONS.any { getReverseCountInLine(position, it, 0) > 0 }

    // 位置を指定して石を取得する
    private fun getStone(position: Pair<Int, Int>): Stone? = when {
        position.first < 0 -> null
        position.first >= BAN_SIZE -> null
        position.second < 0 -> null
        position.second >= BAN_SIZE -> null
        else -> banmen[position.first][position.second]
    }

    // 指定位置に石を置く
    fun putStone(position: Pair<Int, Int>): Ban {

        val start = Date().time
        // 盤面を複製する
        val copiedBanmen = Array(BAN_SIZE, { y -> Array(BAN_SIZE, { x -> banmen[y][x] }) })
        copiedBanmenTime += (Date().time - start)

        // 1行裏返す
        fun reverseLine(position: Pair<Int, Int>, direction: Pair<Int, Int>): Boolean {
            val newPosition = Pair(position.first + direction.first, position.second + direction.second)
            return when (getStone(newPosition)) {
                teban -> true
                teban.reverse() -> {
                    val reverse = reverseLine(newPosition, direction)
                    if (reverse) {
                        copiedBanmen[newPosition.first][newPosition.second] = teban
                    }
                    reverse
                }
                else -> false
            }
        }
        // 1行ずつ裏返す
        DIRECTIONS.map { direction -> reverseLine(position, direction) }
        // 指定されたマスに石を置く
        copiedBanmen[position.first][position.second] = teban
        // 新しい盤を返却する
        return Ban(copiedBanmen, teban.reverse())
    }

    // 手番を渡す
    fun pass(): Ban {
        // 盤面を複製する
        val copiedBanmen = Array(BAN_SIZE, { y -> Array(BAN_SIZE, { x -> banmen[y][x] }) })
        return Ban(copiedBanmen, this.teban.reverse())
    }

    // ゲームが終わっているか
    fun isGameEnd(): Boolean = getPositions().isEmpty() && pass().getPositions().isEmpty()

    // 石だけを並べる
    private fun getLinedBanmen(): List<Stone> = this.banmen.flatMap { array -> array.asList() }.filterNotNull()

    // 石の数を数える
    private fun getCount(stone: Stone): Int = getLinedBanmen().filter { masu -> masu == stone }.count()

    // 盤面を点数化する
    fun calcScore(positions: List<Pair<Int, Int>>): Int {

        val start = Date().time

        // ゲームが終了していれば
        if (positions.isEmpty() && pass().getPositions().isEmpty()) {
            // 石の数を取得して
            val stoneCount = getCount(teban)
            val reversedStoneCount = getCount(teban.reverse())
            return when {
                stoneCount > reversedStoneCount -> 1000    // 勝っていれば 1000点
                stoneCount < reversedStoneCount -> -1000   // 負けていれば-1000点
                else -> 0    // 引き分けていれば 0点
            }
        }

        // 置ける場所×1点
        val positionScore: Int = positions.size
        // カドの石数×10点
        val kadoScore = KADOS.map { getStone(it) }.filterNotNull().filter { it == teban }.size * 10

        calcScoreTime += (Date().time - start)

        // これが評価値
        return positionScore + kadoScore
    }
}
