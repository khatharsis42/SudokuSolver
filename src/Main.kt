val red = "\u001b[31m"
val reset = "\u001b[39m"

class Sudoku(private val grid: List<List<Int>>, private val oldGrid: List<List<Int>>? = null) {
    constructor(string: String) : this(
        string.trimIndent().split("\n")
            .map { line -> line.map { it.digitToInt() } }
    )

    private fun getLineValues(lineNumber: Int) = grid[lineNumber].filterNot { it == 0 }

    private fun getColumnValues(columnNumber: Int) =
        grid.indices.map { lineNumber -> grid[lineNumber][columnNumber] }.filterNot { it == 0 }

    private fun getBlockNumber(lineNumber: Int, columnNumber: Int) = lineNumber.div(3) * 3 + columnNumber.div(3)

    private fun getBlockValues(blockNumber: Int): List<Int> {
        val numbers = mutableListOf<Int>()
        for (i: Int in (0..<3)) {
            for (j: Int in (0..<3)) {
                numbers += grid[blockNumber.div(3) * 3 + i][blockNumber.mod(3) * 3 + j]
            }
        }
        return numbers.filterNot { it == 0 }
    }

    private fun getPossibleValues(lineNumber: Int, columnNumber: Int) = (1..9).shuffled()
        .filterNot { it in getLineValues(lineNumber) }
        .filterNot { it in getColumnValues(columnNumber) }
        .filterNot { it in getBlockValues(getBlockNumber(lineNumber, columnNumber)) }

    val getAllPossibleSteps =
        (0..8).map { i ->
            (0..8).mapNotNull { j ->
                if (grid[i][j] == 0) (i to j) to getPossibleValues(i, j) else null
            }
        }.flatten().sortedBy { it.second.size }

    val isInvalid by lazy { getAllPossibleSteps.any { it.second.isEmpty() } }
    val isSolved by lazy { grid.all { line -> line.none { it == 0 } } }

    fun doStep(lineNumber: Int, columnNumber: Int, value: Int): Sudoku {
        val newGrid = grid.map { it.toMutableList() }.toMutableList()
        newGrid[lineNumber][columnNumber] = value
        return Sudoku(newGrid, grid)
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
            return Sudoku(newGrid, grid)
        }
        return null
    }

    class SudokuException(message: String) : Exception(message)

    override fun toString(): String = "$topRow\n" +
            grid.chunked(3)
                .mapIndexed { lineGroupIndex, lineGroup ->
                    lineGroup.mapIndexed { lineIndex, line ->
                        val lineStr = line.chunked(3)
                            .mapIndexed { colGroupIndex, colGroup ->
                                colGroup.mapIndexed { colIndex, value ->
                                    if (oldGrid != null && oldGrid[lineGroupIndex * 3 + lineIndex][colGroupIndex * 3 + colIndex] != value) {
                                        red + value + reset
                                    } else value
                                }.joinToString("").replace("0", ".")
                            }.joinToString(" │ ")
                        return@mapIndexed "│ $lineStr │"
                    }
                        .joinToString("\n")
                }
                .joinToString("\n$midRow\n") +
            "\n$botRow"

    companion object {
        val topRow = "╭─────┬─────┬─────╮"
        val midRow = "├─────┼─────┼─────┤"
        val botRow = "╰─────┴─────┴─────╯"
    }
}

data class SudokuHelper(val sudoku: Sudoku, var currentTry: Int, var currentTryIndex: Int) {

}

fun main() {
    val firstSudoku = Sudoku(
        """
            306508400
            520000000
            087000031
            003010080
            900863005
            050090600
            130000250
            000000074
            005206300
            """
    )
    val sudokuList: MutableList<SudokuHelper> = mutableListOf(SudokuHelper(firstSudoku, 0, 0))
    while (true) {
        val sudokuHelper = sudokuList.last()
        if (sudokuHelper.sudoku.isInvalid) {
            println(" ".repeat(sudokuList.size) + "Backtracking: invalid !")
            sudokuList.removeLast()
            continue
        }
        if (sudokuHelper.sudoku.isSolved) {
            break
        }
        val possibleSteps = sudokuHelper.sudoku.getAllPossibleSteps
        if (sudokuHelper.currentTry >= possibleSteps.size) {
            println(" ".repeat(sudokuList.size) + "Backtracking: no more tries !")
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
            println(" ".repeat(sudokuList.size) + "Jumping $firstFork trivial choices !")
            sudokuHelper.currentTry = possibleSteps.size
            // If we come back to this step, it means that these obvious steps were wrong.
            sudokuHelper.currentTryIndex = 0
            val nextStep = sudokuHelper.sudoku.doAllTrivialSteps()!!
            sudokuList.add(SudokuHelper(nextStep, 0, 0))
            continue
        }
        println(" ".repeat(sudokuList.size) + "Forking ${sudokuHelper.currentTry + 1}/${possibleSteps.size} " +
                "(${sudokuHelper.currentTryIndex + 1}/${possibleSteps[sudokuHelper.currentTry].second.size}) " +
                "(${
                    possibleSteps.subList(sudokuHelper.currentTry, possibleSteps.size).sumOf { it.second.size }
                } possibles steps)"
        )
        val (lineNumber, colNumber) = possibleSteps[sudokuHelper.currentTry].first
        val tryValue = possibleSteps[sudokuHelper.currentTry].second[sudokuHelper.currentTryIndex]
        val nextStep = sudokuHelper.sudoku.doStep(lineNumber, colNumber, tryValue)
        sudokuHelper.currentTryIndex++;
        sudokuList.add(SudokuHelper(nextStep, 0, 0))
    }
    for (s in sudokuList) {
        println(s.sudoku)
        println()
    }
}
