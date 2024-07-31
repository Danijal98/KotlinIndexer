import java.io.RandomAccessFile

fun main() {
    val file = "term_index.txt"
//    printFileContent(file)
    var offset: Long = 0
    var line = ""
    do {
        printLineWithOffset(
            filePath = file,
            offset = offset,
            newOffset = {
                offset = it
            },
            line = {
                line = it
            }
        )
        println("Line $line")
    } while (line != "")
}

fun printLineWithOffset(filePath: String, offset: Long, newOffset: (Long) -> Unit, line: (String) -> Unit) {
    RandomAccessFile(filePath, "r").use { raf ->
        raf.seek(offset)
        line(raf.readLine() ?: "")
        newOffset(raf.filePointer)
    }
}