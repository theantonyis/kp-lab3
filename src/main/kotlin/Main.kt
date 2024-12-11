import org.jub.kotlin.hometask4.Application
import org.jub.kotlin.hometask4.tasks

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: Provide the results file path.")
        return
    }

    val app = Application.create("results.txt", tasks)
    app.run()
    app.waitToFinish()
}
