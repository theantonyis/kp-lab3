package org.jub.kotlin.hometask4

import java.io.File
import java.io.IOException
import java.util.concurrent.*
import kotlin.system.exitProcess
import kotlin.io.*

interface Application : Runnable {
    /**
     * Wait for your application to stop.
     * You might need this, you might not, that's fine.
     */
    fun waitToFinish() {}

    companion object {
        /**
         * Creates a new Application, which writes results to the file at the given path.
         * You have to think of what type the `tasks` should be yourself.
         *
         * @param resultsFile path to a file in which results should be stored
         * @param tasks List of available tasks.
         */
        fun create(resultsFile: String, tasks: List<Callable<out Any>>): Application {
            return object : Application {
                val resultsFile = File(resultsFile)
                private val executor = Executors.newFixedThreadPool(6)
                private val taskQueue = LinkedBlockingQueue<Pair<String, Future<out Any>>>()
                private var acceptingTasks = true

                init {
                    if (!this.resultsFile.exists()) {
                        this.resultsFile.createNewFile()
                    }
                }

                override fun run() {
                    println("Application starting in 1.5 seconds...")
                    Thread.sleep(1500)
                    println("Type 'help' for guidelines.")

                    while (true) {
                        print(":> ")
                        val input = readlnOrNull() ?: run {
                            finishGracefully()
                            exitProcess(0)
                        }

                        when {
                            input.startsWith("task ") -> handleTaskCommand(input)
                            input == "get" -> handleGetCommand()
                            input == "finish grace" -> {
                                acceptingTasks = false
                                finishGracefully()
                                exitProcess(0)
                            }
                            input == "finish force" -> {
                                executor.shutdownNow()
                                println("Application terminated forcefully.")
                                exitProcess(0)
                            }
                            input == "clean" -> handleCleanCommand()
                            input == "help" -> showHelp()
                            else -> println("Unknown command. Type 'help' for guidelines.")
                        }
                    }
                }

                private fun handleTaskCommand(input: String) {
                    if (!acceptingTasks) {
                        println("Cannot accept new tasks. Finishing mode is active.")
                        return
                    }

                    val parts = input.split(" ")
                    if (parts.size != 3) {
                        println("Invalid command format. Use: task NAME X")
                        return
                    }

                    val name = parts[1]
                    val taskIndex = parts[2].toIntOrNull()

                    if (" " in name) {
                        println("NAME cannot contain whitespaces.")
                        return
                    }

                    if (taskIndex == null || taskIndex !in tasks.indices) {
                        println("Invalid task index. Provide a number between 0 and ${tasks.size - 1}.")
                        return
                    }

                    val task = tasks[taskIndex]
                    val future = executor.submit(task)
                    taskQueue.add(name to future)
                }

                private fun handleCleanCommand() {
                    try {
                        resultsFile.writeText("")
                        println("Results file cleaned.")
                    } catch (e: IOException) {
                        println("Error cleaning results file: ${e.message}")
                    }
                }

                private fun handleGetCommand() {
                    val lastResult = taskQueue.peek()
                    if (lastResult == null || lastResult.second.isDone.not()) {
                        println("No completed tasks available.")
                    } else {
                        try {
                            println("${lastResult.second.get()} [${lastResult.first}]")
                        } catch (e: Exception) {
                            println("Error fetching last result: ${e.message}")
                        }
                    }
                }

                private fun showHelp() {
                    println(
                        """
                        Available commands:
                        task NAME X: Execute task X, name it NAME, and write the result to the results file
                        get: Output the last result and its name to the console
                        finish grace: Finish all pending tasks and exit
                        finish force: Exit immediately without finishing tasks
                        clean: Clear the results file
                        help: Show this help message
                        """.trimIndent()
                    )
                }

                private fun finishGracefully() {
                    println("Finishing gracefully...")
                    executor.shutdown()
                    try {
                        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                            println("Timeout waiting for tasks to finish. Forcing shutdown.")
                            executor.shutdownNow()
                        }
                    } catch (e: InterruptedException) {
                        println("Error during shutdown: ${e.message}")
                        executor.shutdownNow()
                    }

                    while (taskQueue.isNotEmpty()) {
                        val (name, future) = taskQueue.poll()
                        if (future.isDone) {
                            try {
                                val result = future.get()
                                resultsFile.appendText("$name: $result\n")
                            } catch (e: Exception) {
                                println("Error writing result: ${e.message}")
                            }
                        }
                    }
                }

                override fun waitToFinish() {
                    finishGracefully()
                }
            }
        }
    }
}
