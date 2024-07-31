import java.io.File
import java.io.RandomAccessFile

fun main() {
    val docIndex = readDocIndex("doc_index.txt")
    val termIndex = buildTermIndex(docIndex)
    writeTermIndex(termIndex, "term_index.txt")
    writeTermInfo(termIndex, "term_info.txt", "term_index.txt")
}

fun readDocIndex(filePath: String): List<Triple<Int, Int, List<Int>>> {
    println("readDocIndex")

    return File(filePath).readLines().map { line ->
        val parts = line.split("\t")
        val docId = parts[0].toInt()
        val termId = parts[1].toInt()
        val positions = parts.subList(2, parts.size).map { it.toInt() }
        Triple(docId, termId, positions)
    }
}

fun buildTermIndex(docIndex: List<Triple<Int, Int, List<Int>>>): Map<Int, List<Pair<Int, List<Int>>>> {
    println("buildTermIndex")

    val termIndex = mutableMapOf<Int, MutableList<Pair<Int, List<Int>>>>()
    docIndex.forEach { (docId, termId, positions) ->
        termIndex.getOrPut(termId) { mutableListOf() }
            .add(docId to positions)
    }
    termIndex.forEach { (_, postings) ->
        postings.sortBy { it.first }
        postings.replaceAll { (docId, positions) ->
            docId to positions.sorted()
        }
    }
    return termIndex
}

fun writeTermIndex(termIndex: Map<Int, List<Pair<Int, List<Int>>>>, filePath: String) {
    println("writeTermIndex")

    File(filePath).printWriter().use { out ->
        termIndex.forEach { (termId, postings) ->
            val postingsStr = buildString {
                var prevDocId = 0

                postings.forEach { (docId, positions) ->
                var prevPos = 0
                    positions.forEach { pos ->
                        val docDelta = docId - prevDocId
                        val posDelta = pos - prevPos
                        prevPos = pos
                        prevDocId = docId
                        append("$docDelta:$posDelta\t")
                    }
                }
            }.trim()
            out.println("$termId\t$postingsStr")
        }
    }
}

fun writeTermInfo(termIndex: Map<Int, List<Pair<Int, List<Int>>>>, termInfoFilePath: String, termIndexFilePath: String) {
    println("writeTermInfo")

    RandomAccessFile(termIndexFilePath, "r").use { termIndexFile ->
        File(termInfoFilePath).printWriter().use { out ->
            termIndex.forEach { (termId, postings) ->
                val offset = termIndexFile.filePointer
                val totalOccurrences = postings.sumOf { it.second.size }
                val docCount = postings.size
                out.println("$termId\t$offset\t$totalOccurrences\t$docCount")
                termIndexFile.readLine()
            }
        }
    }
}