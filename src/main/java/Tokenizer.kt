import org.jsoup.Jsoup
import org.tartarus.snowball.ext.englishStemmer
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Usage: Tokenizer <directory>")
        return
    }
    val directory = args[0]
    val stopwords = loadStopWords("stopwords.txt")
    val docIdMap = mutableMapOf<String, Int>()
    val termIdMap = mutableMapOf<String, Int>()
    val docIndex = mutableListOf<Triple<Int, Int, MutableList<Int>>>()

    val files = File(directory).listFiles()
    var docIdCounter = 1
    var termIdCounter = 1

    files.forEachIndexed { fileIndex, file ->
        println("Processing file ${fileIndex + 1}/${files.size}")

        val docId = docIdCounter++
        docIdMap[file.name] = docId

        val text = readHtmlFile(file) ?: return@forEachIndexed
        val tokens = tokenize(text, stopwords)

        val termPositions = mutableMapOf<Int, MutableList<Int>>()
        tokens.forEachIndexed { index, token ->
            val termId = termIdMap.getOrPut(token) { termIdCounter++ }
            termPositions.getOrPut(termId) { mutableListOf() }.add(index + 1)
        }
        termPositions.forEach { (termId, positions) ->
            docIndex.add(Triple(docId, termId, positions))
        }
    }

    writeDocIds(docIdMap)
    writeTermIds(termIdMap)
    writeDocIndex(docIndex)
}

fun readHtmlFile(file: File): String? {
    val fileContent = file.readText()
    val htmlStartIndex = fileContent.indexOf("<head>")
    return if (htmlStartIndex != -1) {
        val htmlContent = fileContent.substring(htmlStartIndex)

        // Parse the HTML content
        val document = Jsoup.parse(htmlContent)

        // Extract text content, ignoring all HTML tags
        document.body().text()
    } else {
        null
    }
}

fun loadStopWords(filePath: String): Set<String> {
    return Files.readAllLines(Paths.get(filePath)).toSet()
}

fun tokenize(text: String, stopwords: Set<String>): List<String> {
    // Improved regex to handle words and contractions correctly
    val regex = "\\b[\\w']+(?:\\.\\w+)*\\b".toRegex()

    return regex.findAll(text)
        .map { it.value.lowercase().trim('.', ',', ';', ':', '?', '!') } // Trim trailing punctuation
        .filter { it !in stopwords && it.length > 1 } // Ignore stopwords and single letters
        .map { stem(it) }
        .toList()
}


fun stem(word: String): String {
    val stemmer = englishStemmer()
    stemmer.current = word
    stemmer.stem()
    return stemmer.current
}

fun writeDocIds(docIdMap: Map<String, Int>) {
    println("Writing doc id map")

    File("docids.txt").printWriter().use { out ->
        docIdMap.forEach { (filename, docId) ->
            out.println("$docId\t$filename")
        }
    }
}

fun writeTermIds(termIdMap: Map<String, Int>) {
    println("Writing term id map")

    File("termids.txt").printWriter().use { out ->
        termIdMap.forEach { (term, termId) ->
            out.println("$termId\t$term")
        }
    }
}

fun writeDocIndex(docIndex: List<Triple<Int, Int, List<Int>>>) {
    println("Writing doc index map")

    File("doc_index.txt").printWriter().use { out ->
        docIndex.forEach { (docId, termId, positions) ->
            out.println("$docId\t$termId\t${positions.joinToString("\t")}")
        }
    }
}