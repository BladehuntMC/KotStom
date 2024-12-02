package net.bladehunt.kotstom.dsl.kommand

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.bladehunt.kotstom.command.Kommand
import net.bladehunt.kotstom.command.KommandConditionContext
import net.bladehunt.kotstom.command.KommandExecutorContext
import net.bladehunt.kotstom.command.KommandSyntax
import net.bladehunt.kotstom.coroutines.MinestomDispatcher
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.CommandExecutor
import net.minestom.server.command.builder.CommandSyntax
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.condition.CommandCondition
import net.minestom.server.entity.Player
import java.util.function.Function
import kotlin.coroutines.CoroutineContext

/**
 * `CommandSyntax` builder DSL
 *
 * @property conditions The list of command conditions
 * @property executor The command executor
 * @property defaultValues The map of default values for command arguments
 * @property arguments The list of command arguments
 * @author oglassdev
 */
data class SyntaxBuilder(
    val conditions: MutableList<CommandCondition> = arrayListOf(),
    var executor: CommandExecutor? = null,
    val defaultValues: MutableMap<String, Function<CommandSender, Any>> = hashMapOf(),
    val arguments: MutableList<Argument<*>> = arrayListOf()
) {
    /**
     * Adds a condition to allow only players to execute the command
     *
     * @return The player-only command condition
     * @author oglassdev
     */
    @KommandDSL
    fun onlyPlayers(): CommandCondition =
        CommandCondition { commandSender, _ -> commandSender is Player }.also(conditions::add)

    /**
     * Adds a condition
     *
     * @param block The condition logic
     * @return The custom command condition
     * @author oglassdev
     */
    @KommandDSL
    inline fun condition(
        crossinline block: @KommandDSL KommandConditionContext.() -> Boolean
    ): CommandCondition =
        CommandCondition { commandSender, input ->
            KommandConditionContext(commandSender, input).block()
        }
            .also { conditions += it }

    /**
     * Sets the executor for the command
     *
     * @param block The executor logic
     * @return The command executor
     * @author oglassdev
     */
    @KommandDSL
    inline fun executor(
        crossinline block: @KommandDSL KommandExecutorContext.() -> Unit
    ): CommandExecutor =
        CommandExecutor { commandSender, commandContext ->
            KommandExecutorContext(commandSender, commandContext).block()
        }
            .also { executor = it }

    /**
     * Sets the async executor for the command
     *
     * @param block The executor logic
     * @return The command executor
     * @author oglassdev
     */
    @KommandDSL
    inline fun executorAsync(
        context: CoroutineContext = MinestomDispatcher,
        crossinline block: @KommandDSL suspend KommandExecutorContext.() -> Unit
    ) =
        CommandExecutor { sender, ctx ->
            CoroutineScope(context).launch { KommandExecutorContext(sender, ctx).block() }
        }
            .also { executor = it }

    /**
     * Sets the blocking executor for the command
     *
     * @param block The executor logic
     * @return The command executor
     * @author oglassdev
     */
    @KommandDSL
    inline fun executorBlocking(
        crossinline block: @KommandDSL suspend KommandExecutorContext.() -> Unit
    ) =
        CommandExecutor { sender, ctx ->
            runBlocking { KommandExecutorContext(sender, ctx).block() }
        }
            .also { executor = it }

    /**
     * Sets a default value for a command argument
     *
     * @param key The argument key
     * @param block The function to provide the default value
     * @return The function providing the default value
     * @author oglassdev
     */
    @KommandDSL
    inline fun <T> default(
        argument: Argument<T>,
        crossinline block: @KommandDSL (CommandSender) -> T
    ): Function<CommandSender, T> =
        Function<CommandSender, T> { sender -> block(sender) }.also {
            defaultValues[argument.id] = it as Function<CommandSender, Any>
        }

    /**
     * Sets a default value for a command argument
     *
     * @param key The argument key
     * @param block The function to provide the default value
     * @return The function providing the default value
     * @author oglassdev
     */
    @KommandDSL
    inline fun default(
        key: String,
        crossinline block: @KommandDSL (CommandSender) -> Any
    ): Function<CommandSender, Any> =
        Function<CommandSender, Any> { sender -> block(sender) }.also { defaultValues[key] = it }

    /**
     * Builds the command syntax from the provided conditions, executor, and arguments
     *
     * @return The constructed command syntax
     * @throws IllegalArgumentException if the executor is null.
     * @author oglassdev
     */
    fun build(): CommandSyntax {
        if (executor == null) throw IllegalArgumentException("Executor cannot be null!")
        return KommandSyntax(
            commandCondition =
                when (conditions.size) {
                    1 -> conditions.first()
                    /* Automagically returns true if nothing exists */
                    else ->
                        CommandCondition { sender, label ->
                            conditions.all { it.canUse(sender, label) }
                        }
                },
            commandExecutor = executor!!,
            defaultValues = defaultValues,
            args = arguments.toTypedArray()
        )
    }
}

/**
 * Extension function for `KommandBuilder` to define a command syntax
 *
 * @param arguments The command arguments
 * @param condition The condition to execute the command
 * @param defaultValues The map of default values for command arguments
 * @param block The executor logic
 * @author oglassdev
 */
@KommandDSL
inline fun Kommand.syntax(
    vararg arguments: Argument<*>,
    crossinline condition: @KommandDSL (sender: CommandSender, label: String?) -> Boolean =
        { _, _ ->
            true
        },
    defaultValues: Map<String, Function<CommandSender, Any>> = emptyMap(),
    crossinline block: @KommandDSL KommandExecutorContext.() -> Unit,
) =
    KommandSyntax(
        commandCondition = { sender, label -> condition(sender, label) },
        commandExecutor = { sender, context ->
            KommandExecutorContext(sender, context).block()
        },
        defaultValues = defaultValues,
        args = arguments
    )
        .let { syntaxes.add(it) }

/**
 * Extension function for `KommandBuilder` to build a command syntax using `SyntaxBuilder`
 *
 * @param arguments The command arguments
 * @param block The `SyntaxBuilder` logic
 * @author oglassdev
 */
@KommandDSL
inline fun Kommand.buildSyntax(
    vararg arguments: Argument<*>,
    block: @KommandDSL SyntaxBuilder.() -> Unit
) =
    SyntaxBuilder(arguments = arguments.toMutableList()).let {
        it.block()
        syntaxes.add(it.build())
    }

/**
 * Extension function for `KommandBuilder` to build a command syntax using `SyntaxBuilder`
 *
 * @param block The `SyntaxBuilder` logic.
 * @author oglassdev
 */
@KommandDSL
inline fun Kommand.buildSyntax(block: @KommandDSL SyntaxBuilder.() -> Unit) =
    SyntaxBuilder().let {
        it.block()
        syntaxes.add(it.build())
    }
