import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

private val CLASS_PATH = System.getProperty("java.class.path")
private val JAVA_PATH = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("java").toString()
private val JVM_OPTIONS = listOf("-Xmx5m", "-XX:+PrintGCDetails", "-Xloggc:/Users/victoria/gc.log")

var value: AtomicInteger = AtomicInteger(0)
var random = Random()

fun main(args: Array<String>) {
    val successful = runProcess(Process::class.java.name, JVM_OPTIONS, args)

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
                        val newVal = random.nextInt(1000)
                        value.set(newVal)
                    }
                }
            }
            threads.forEach { it.join() }
            print(System.currentTimeMillis() - t0)
        }
    }
}