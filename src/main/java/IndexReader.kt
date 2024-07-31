import java.io.File
import java.io.RandomAccessFile

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: IndexReader [--doc DOCNAME | --term TERM | --term TERM --doc DOCNAME]")
        return
    }
    when {
        args.size == 2 && args[0] == "--doc" -> printDocInfo(args[1])
        args.size == 2 && args[0] == "--term" -> printTermInfo(args[1])
        args.size == 4 && args[0] == "--term" && args[2] == "--doc" -> printInvertedList(args[1], args[3])
        else -> println("Invalid arguments")
    }
}

fun printDocInfo(docName: String) {
    val docIdMap = readDocIds("docids.txt")
    val docId = docIdMap[docName] ?: run {
        println("Document $docName not found.")
        return
    }
    val docIndex = readDocIndex("doc_index.txt").filter { it.first == docId }
    val distinctTerms = docIndex.size
    val totalTerms = docIndex.sumOf { it.third.size }
    println("Listing for document: $docName")
    println("DOCID: $docId")
    println("Distinct terms: $distinctTerms")
    println("Total terms: $totalTerms")
}

fun printTermInfo(term: String) {
    val stemmedTerm = stem(term)
    val termIdMap = readTermIds("termids.txt")
    val termId = termIdMap[stemmedTerm] ?: run {
        println("Term $term not found.")
        return
    }
    val termInfo = readTermInfo("term_info.txt")[termId] ?: run {
        println("Term info for $term not found.")
        return
    }
    println("Listing for term: $term")
    println("TERMID: $termId")
    println("Number of documents containing term: ${termInfo[2]}")
    println("Term frequency in corpus: ${termInfo[1]}")
    println("Inverted list offset: ${termInfo[0]}")
}

fun printInvertedList(term: String, docName: String) {
    val stemmedTerm = stem(term)
    val termIdMap = readTermIds("termids.txt")
    val termId = termIdMap[stemmedTerm] ?: run {
        println("Term $term not found.")
        return
    }
    val docIdMap = readDocIds("docids.txt")
    val docId = docIdMap[docName] ?: run {
        println("Document $docName not found.")
        return
    }
    val termInfo = readTermInfo("term_info.txt")[termId] ?: run {
        println("Term info for $term not found.")
        return
    }
    val termIndex = readTermIndex("term_index.txt", termInfo[0], termId)
    val postings = termIndex.filter { it.first == docId }
    val termFreq = postings.sumOf { it.second.size }
    val positions = postings.flatMap { it.second }
    println("Inverted list for term: $term")
    println("In document: $docName")
    println("TERMID: $termId")
    println("DOCID: $docId")
    println("Term frequency in document: $termFreq")
    println("Positions: ${positions.joinToString(", ")}")
}

fun readDocIds(filePath: String): Map<String, Int> {
    return File(filePath).readLines().associate {
        val parts = it.split("\t")
        parts[1] to parts[0].toInt()
    }
}

fun readTermIds(filePath: String): Map<String, Int> {
    return File(filePath).readLines().associate {
        val parts = it.split("\t")
        parts[1] to parts[0].toInt()
    }
}

fun readTermInfo(filePath: String): Map<Int, List<Int>> {
    return File(filePath).readLines().associate {
        val parts = it.split("\t")
        val termId = parts[0].toInt()
        val info = parts.subList(1, parts.size).map { it.toInt() }
        termId to info
    }
}

// todo
fun readTermIndex(filePath: String, offset: Int, termId: Int): List<Pair<Int, List<Int>>> {
    RandomAccessFile(filePath, "r").use { termIndexFile ->
        termIndexFile.seek(offset.toLong())
        val line = termIndexFile.readLine() ?: return emptyList()
        val parts = line.split("\t")
        if (parts[0].toInt() != termId) {
            return emptyList() // Ensure we have the correct termId
        }
        var prevDocId = 0
        val result = mutableListOf<Pair<Int, List<Int>>>()

        var currentDocId = 0
        var currentPos = 0
        var currentPositions = mutableListOf<Int>()

        for (i in 1 until parts.size) {
            val (docDeltaStr, posDeltaStr) = parts[i].split(":")
            val docDelta = docDeltaStr.toInt()
            val posDelta = posDeltaStr.toInt()

            if (docDelta != 0) {
                // New document, so save the previous document's positions if any
                if (currentPositions.isNotEmpty()) {
                    result.add(currentDocId to currentPositions)
                    currentPositions = mutableListOf()
                }
                currentDocId = prevDocId + docDelta
                prevDocId = currentDocId
                currentPos = 0 // Reset position for the new document
            }

            currentPos += posDelta
            currentPositions.add(currentPos)
        }

        // Add the last document's positions
        if (currentPositions.isNotEmpty()) {
            result.add(currentDocId to currentPositions)
        }

        return result
    }
}
