package com.ekuaibao.exile.todomvc

import com.ekuaibao.exile.boot.Autowire
import com.ekuaibao.exile.graphql.GraphQLSchemaExtension
import com.ekuaibao.exile.modeling.internal.RandomIdGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import graphql.Scalars
import graphql.relay.Relay
import graphql.relay.SimpleListConnection
import graphql.schema.*
import java.util.concurrent.CopyOnWriteArrayList

interface Node {
    val id: String
}

interface Mutation {
    val clientMutationId: String?
}

data class Todo(
        override val id: String,
        val text: String,
        val complete: Boolean
) : Node

object TodoList {
    private val idGenerator = RandomIdGenerator()

    val todos = CopyOnWriteArrayList<Todo>()

    init {
        todos.add(Todo(idGenerator.blockingNextId(), "Do Something", false))
        todos.add(Todo(idGenerator.blockingNextId(), "Another todo", false))
    }

    fun getTodo(id: String): Todo? {
        return todos.find { it.id == id }
    }

    fun addTodo(text: String): String {
        val id = idGenerator.blockingNextId()
        todos.add(0, Todo(id, text, false))
        return id
    }

    fun removeTodo(todoId: String) {
        todos.removeIf { it.id == todoId }
    }

    fun completeTodo(todoId: String, complete: Boolean) {
        todos.replaceAll { t ->
            if (t.id == todoId && t.complete != complete) {
                t.copy(complete = complete)
            } else {
                t
            }
        }
    }
}

data class AddTodoRequest(
        override val clientMutationId: String?,
        val text: String
) : Mutation

data class RemoveTodoRequest(
        override val clientMutationId: String?,
        val todoId: String
) : Mutation

data class CompleteTodoRequest(
        override val clientMutationId: String?,
        val todoId: String,
        val complete: Boolean
) : Mutation

data class UpdateTodoResponse(
        override val clientMutationId: String?,
        val todoId: String
) : Mutation

@Autowire
class TodoSchemaExtension(
        private val mapper: ObjectMapper
) : GraphQLSchemaExtension {
    override fun config(context: GraphQLSchemaExtension.Context) {
        buildTodos(context)
    }

    private fun buildTodos(context: GraphQLSchemaExtension.Context) {
        val relay = Relay()
        val typeResolverProxy = TypeResolverProxy()
        val nodeType = relay.nodeInterface(typeResolverProxy)
        val todoType = GraphQLObjectType.newObject()
                .name("Todo")
                .withInterface(nodeType)
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(GraphQLNonNull.nonNull(Scalars.GraphQLID))
                        .dataFetcher { e ->
                            val todo = e.getSource<Todo>()
                            relay.toGlobalId("Todo", todo.id)
                        }
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("text")
                        .type(GraphQLNonNull.nonNull(Scalars.GraphQLString))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("complete")
                        .type(GraphQLNonNull.nonNull(Scalars.GraphQLBoolean))
                        .build())
                .build()
        val edgeType = relay.edgeType("Todo", todoType, nodeType, emptyList())
        val todoConnectionType = relay.connectionType("Todo", edgeType, emptyList())
        typeResolverProxy.setTypeResolver { e ->
            when (e.getObject<Any>()) {
                is Todo -> todoType
                else -> null
            }
        }
        val todoConnection = SimpleListConnection(TodoList.todos)
        context.addQuery(GraphQLFieldDefinition.newFieldDefinition()
                .name("todos")
                .type(todoConnectionType)
                .argument(relay.connectionFieldArguments)
                .dataFetcher(todoConnection)
                .build())
        val outputFields = listOf(
                GraphQLFieldDefinition.newFieldDefinition()
                        .name("todo")
                        .type(todoType)
                        .dataFetcher { e ->
                            val resp = e.getSource<UpdateTodoResponse>()
                            TodoList.getTodo(resp.todoId)
                        }
                        .build()
        )
        context.addMutation(relay.mutationWithClientMutationId(
                "AddTodo", "addTodo",
                listOf(
                        GraphQLInputObjectField.newInputObjectField()
                                .name("text")
                                .type(GraphQLNonNull.nonNull(Scalars.GraphQLString))
                                .build()
                ),
                outputFields
        ) { e ->
            val node = e.getArgument<Any>("input")
            val request = mapper.convertValue(node, AddTodoRequest::class.java)
            val todoId = TodoList.addTodo(request.text)
            UpdateTodoResponse(request.clientMutationId, todoId)
        })
        context.addMutation(relay.mutationWithClientMutationId(
                "RemoveTodo", "removeTodo",
                listOf(
                        GraphQLInputObjectField.newInputObjectField()
                                .name("todoId")
                                .type(GraphQLNonNull.nonNull(Scalars.GraphQLString))
                                .build()
                ),
                outputFields
        ) { e ->
            val node = e.getArgument<Any>("input")
            val request = mapper.convertValue(node, RemoveTodoRequest::class.java)
            val todoId = relay.fromGlobalId(request.todoId).id
            TodoList.removeTodo(todoId)
            UpdateTodoResponse(request.clientMutationId, todoId)
        })
        context.addMutation(relay.mutationWithClientMutationId(
                "CompleteTodo", "completeTodo",
                listOf(
                        GraphQLInputObjectField.newInputObjectField()
                                .name("todoId")
                                .type(GraphQLNonNull.nonNull(Scalars.GraphQLString))
                                .build(),
                        GraphQLInputObjectField.newInputObjectField()
                                .name("complete")
                                .type(GraphQLNonNull.nonNull(Scalars.GraphQLBoolean))
                                .build()
                ),
                outputFields
        ) { e ->
            val node = e.getArgument<Any>("input")
            val request = mapper.convertValue(node, CompleteTodoRequest::class.java)
            val todoId = relay.fromGlobalId(request.todoId).id
            TodoList.completeTodo(todoId, request.complete)
            UpdateTodoResponse(request.clientMutationId, todoId)
        })
    }
}