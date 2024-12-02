package net.bladehunt.kotstom.example.command

import kotlinx.coroutines.delay
import net.bladehunt.kotstom.dsl.kommand.buildSyntax
import net.bladehunt.kotstom.dsl.kommand.defaultExecutor
import net.bladehunt.kotstom.dsl.kommand.kommand

val SuspendingCommand = kommand("suspending") {

    defaultExecutor { sender.sendMessage("You aren't a player.") }

    buildSyntax {
        onlyPlayers()

        executorAsync {
            repeat(11) { n ->
                player.sendMessage("This is the $n-th time")
                delay(1000)
            }
        }
    }
}
