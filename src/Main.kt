val red = "\u001b[31m"
val reset = "\u001b[39m"

fun Int.quadRoot(): Int {
    for (i: Int in (1..this)) {
        if (i * i * i * i == this)
            return i
    }
    return 0
}

class Sudoku(
    private val grid: List<List<Int>>,
    private val oldGrid: List<List<Int>>? = null,
    private val size: Int = 3
) {
    constructor(string: String) : this(
        string.trimIndent().split("\n")
            .map { line -> line.map { it.toString().takeIf { it != "." }?.toInt(16) ?: -1 } },
        size = string.trimIndent().replace("\n", "").length.quadRoot()
    )

    private fun getLineValues(lineNumber: Int) = grid[lineNumber].filter { it >= 0 }

    private fun getColumnValues(columnNumber: Int) =
        grid.indices.map { lineNumber -> grid[lineNumber][columnNumber] }.filter { it >= 0 }

    private fun getBlockNumber(lineNumber: Int, columnNumber: Int) =
        lineNumber.div(size) * size + columnNumber.div(size)

    public fun getBlockValues(blockNumber: Int): List<Int> {
        val numbers = mutableListOf<Int>()
        for (i: Int in (0..<size)) {
            for (j: Int in (0..<size)) {
                numbers += grid[blockNumber.div(size) * size + i][blockNumber.mod(size) * size + j]
            }
        }
        return numbers.filter { it >= 0}
    }

    private fun getPossibleValues(lineNumber: Int, columnNumber: Int) = (0..<size * size).shuffled()
        .filterNot { it in getLineValues(lineNumber) }
        .filterNot { it in getColumnValues(columnNumber) }
        .filterNot { it in getBlockValues(getBlockNumber(lineNumber, columnNumber)) }

    val getAllPossibleSteps =
        (0..<size * size).map { i ->
            (0..<size * size).mapNotNull { j ->
                if (grid[i][j] == -1) (i to j) to getPossibleValues(i, j) else null
            }
        }.flatten().sortedBy { it.second.size }

    val isBlocked by lazy { getAllPossibleSteps.any { it.second.isEmpty() } || (getAllPossibleSteps.isEmpty() && !isSolved) }
    val isSolved by lazy { grid.all { line -> line.all { it >= 0 } } }

    fun doStep(lineNumber: Int, columnNumber: Int, value: Int): Sudoku {
        val newGrid = grid.map { it.toMutableList() }.toMutableList()
        newGrid[lineNumber][columnNumber] = value
        return Sudoku(newGrid, grid, size)
    }

    fun doAllTrivialSteps(): Sudoku? {
        val possibleSteps = getAllPossibleSteps.filter { it.second.size <= 1 }
        if (possibleSteps.any { it.second.isEmpty() }) {
            throw SudokuException("Error in possibleSteps: $possibleSteps")
        }
        if (possibleSteps.isNotEmpty()) {
            val newGrid = grid.map { it.toMutableList() }.toMutableList()
            for ((coordinates, step) in possibleSteps) {
                newGrid[coordinates.first][coordinates.second] = step.first()
            }
            return Sudoku(newGrid, grid, size)
        }
        return null
    }

    class SudokuException(message: String) : Exception(message)

    override fun toString(): String = "$topRow\n" +
            grid.chunked(size)
                .mapIndexed { lineGroupIndex, lineGroup ->
                    lineGroup.mapIndexed { lineIndex, line ->
                        val lineStr = line.chunked(size)
                            .mapIndexed { colGroupIndex, colGroup ->
                                colGroup.mapIndexed { colIndex, value ->
                                    val stringValue = if (value >= 0) Integer.toHexString(value) else '.'
                                    if (oldGrid != null && oldGrid[lineGroupIndex * size + lineIndex][colGroupIndex * size + colIndex] != value) {
                                        red + stringValue + reset
                                    } else stringValue
                                }.joinToString("")
                            }.joinToString(" │ ")
                        return@mapIndexed "│ $lineStr │"
                    }
                        .joinToString("\n")
                }
                .joinToString("\n$midRow\n") +
            "\n$botRow"

    private val row = "─".repeat(size + 2)
    private val topRow = "╭" + "$row┬".repeat(size - 1) + "$row╮"
    private val midRow = "├" + "$row┼".repeat(size - 1) + "$row┤"
    private val botRow = "╰" + "$row┴".repeat(size - 1) + "$row╯"
}

data class SudokuHelper(val sudoku: Sudoku, var currentTry: Int, var currentTryIndex: Int) {

}

fun main() {
//    val firstSudoku = Sudoku(
//        """
//            306508400
//            520000000
//            087000031
//            003010080
//            900863005
//            050090600
//            130000250
//            000000074
//            005206300
//            """
//    )
    val firstSudoku = Sudoku(
        """ 
            ..D.C95..32F.8..
            .....7.4C.8.....
            68C52..DA..E4073
            .3.F..1..7..A.C.
            36.E.C....7.D.F0
            .25....9F....76.
            9....F6AB2C....4
            .F4.E.D..8.0.C9.
            .DA.9.7..B.C.FE.
            7....4B59AE....D
            .C3....E8....54.
            59.2.8....D.0.1A
            .4.3..E..0..5.A.
            15FC7..2E..8963B
            .....A.F3.B.....
            ..0.319..C67.4..
            """.lowercase()
    )
    println(firstSudoku)
    println(firstSudoku.getBlockValues(5).filter { it >= 0}.map {Integer.toHexString(it)})
    val sudokuList: MutableList<SudokuHelper> = mutableListOf(SudokuHelper(firstSudoku, 0, 0))
    while (true) {
        val sudokuHelper = sudokuList.last()
        if (sudokuHelper.sudoku.isBlocked) {
//            println(" ".repeat(sudokuList.size) + "Backtracking: Blocked !")
            sudokuList.removeLast()
            continue
        }
        if (sudokuHelper.sudoku.isSolved) {
            break
        }
        val possibleSteps = sudokuHelper.sudoku.getAllPossibleSteps
        if (sudokuHelper.currentTry >= possibleSteps.size) {
//            println(" ".repeat(sudokuList.size) + "Backtracking: no more tries !")
            sudokuList.removeLast()
            continue
        }
        if (sudokuHelper.currentTryIndex >= possibleSteps[sudokuHelper.currentTry].second.size) {
            sudokuHelper.currentTry++
            sudokuHelper.currentTryIndex = 0
            continue
        }
        val firstFork = possibleSteps.indexOfFirst { it.second.size >= 2 }
        if (firstFork == -1 || firstFork >= 1 && firstFork >= sudokuHelper.currentTry) {
//            println(" ".repeat(sudokuList.size) + "Jumping $firstFork trivial choices !")
            sudokuHelper.currentTry = possibleSteps.size
            // If we come back to this step, it means that these obvious steps were wrong.
            sudokuHelper.currentTryIndex = 0
            val nextStep = sudokuHelper.sudoku.doAllTrivialSteps()!!
            sudokuList.add(SudokuHelper(nextStep, 0, 0))
            continue
        }
        println(" ".repeat(sudokuList.size) + "Forking ${sudokuHelper.currentTry + 1}/${possibleSteps.size} " +
                "(${sudokuHelper.currentTryIndex + 1}/${possibleSteps[sudokuHelper.currentTry].second.size}) " +
                "(${possibleSteps.subList(sudokuHelper.currentTry, possibleSteps.size).sumOf { it.second.size }} " +
                "possibles steps)"
        )
        val (lineNumber, colNumber) = possibleSteps[sudokuHelper.currentTry].first
        val tryValue = possibleSteps[sudokuHelper.currentTry].second[sudokuHelper.currentTryIndex]
        val nextStep = sudokuHelper.sudoku.doStep(lineNumber, colNumber, tryValue)
        sudokuHelper.currentTryIndex++;
        if (!nextStep.isBlocked)
            sudokuList.add(SudokuHelper(nextStep, 0, 0))
    }
    for (s in sudokuList) {
        println(s.sudoku)
        println()
    }
}
