import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import java.util.regex.Pattern

private val CLASS_PATH = System.getProperty("java.class.path")
private val JAVA_PATH = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("java").toString()
private val JVM_OPTIONS = listOf(/*"-Xmx4097k", */"-XX:+PrintGCDetails", "-Xloggc:/Users/victoria/gc.log")
private val PATTERN = ".*GC\\(([^()]*)\\)"

private val MAX = 52428800 // 50mb

var value: AtomicReference<Int> = AtomicReference(0)
var random = Random()
val pattern = Pattern.compile(PATTERN)

fun main(args: Array<String>) {
    // Warmup
    repeat(1000) {
        run(args, MAX)
    }

//    repeat(1) {
//        run(listOf("printTime").toTypedArray(), MAX)
//    }

    var l = 0
    var r = MAX

    while (r - l > 1) {
        val mid = (l + r) / 2

        var error = false
        try {
            run(args, mid)
        } catch (e : Exception) {
            error = true
        }

        if (getGcNum() > 10 || error) {
            l = mid
        } else {
            r = mid
        }
    }

    println(l)
}

fun run(args: Array<String>, memorySize: Int) {
    val successful = runProcess(Process::class.java.name, JVM_OPTIONS + "-Xmx$memorySize", args)

    if (!successful) {
        println("The benchmark failed with error, see the output.")
        return
    }
}

fun runProcess(classNameToRun : String, jvmOptions : List<String>, args : Array<String>) : Boolean {
    val runJavaProcessCommand = ArrayList<String>()
    runJavaProcessCommand.run {
        this.add(JAVA_PATH)
        this.addAll(jvmOptions)
        this.add("-cp")
        this.add(CLASS_PATH)
        this.add(classNameToRun)
        this.addAll(args)
    }
    val processBuilder = ProcessBuilder(runJavaProcessCommand)
    val process = processBuilder.inheritIO().redirectError(ProcessBuilder.Redirect.INHERIT).start()
    return process.waitFor() == 0
}

private class Process {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val t0 = System.currentTimeMillis()
            val threads = (0 until 10).map { threadId ->
                thread {
                    repeat(1000000) {
                        val newVal = random.nextInt(10000000)
                        value.set(newVal)
                    }
                }
            }
            threads.forEach { it.join() }
            if (args.isNotEmpty() && args[0] == "printTime") {
                println(System.currentTimeMillis() - t0)
            }
        }
    }
}

fun getGcNum():Int {
    val text = File("/Users/victoria/gc.log").readText(Charsets.UTF_8)
    val m = pattern.matcher(text)
    var last = 0
    while (m.find()) {
        last = m.group(1).toInt()
    }

    return last
}