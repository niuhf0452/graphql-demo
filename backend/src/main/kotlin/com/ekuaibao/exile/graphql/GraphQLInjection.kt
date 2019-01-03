package com.ekuaibao.exile.graphql

import com.ekuaibao.exile.boot.Autowire
import com.ekuaibao.exile.boot.Injector
import com.ekuaibao.exile.boot.PrebindInjection
import com.ekuaibao.exile.boot.getServiceList
import graphql.GraphQL
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema

@Autowire
class GraphQLInjection(
        private val injector: Injector
) : PrebindInjection() {
    override fun config() {
        bind(GraphQL::class).toProvider {
            val c = ContextImpl()
            val extensions = injector.getServiceList(GraphQLSchemaExtension::class)
            extensions.forEach { r ->
                r.get().config(c)
            }
            GraphQL.newGraphQL(c.build()).build()
        }.asSingleton()
    }

    private class ContextImpl : GraphQLSchemaExtension.Context {
        private val queryBuilder = GraphQLObjectType.newObject()
                .name("Root")

        private val mutationBuilder = GraphQLObjectType.newObject()
                .name("Mutation")

        override fun addQuery(query: GraphQLFieldDefinition) {
            queryBuilder.field(query)
        }

        override fun addMutation(mutation: GraphQLFieldDefinition) {
            mutationBuilder.field(mutation)
        }

        fun build(): GraphQLSchema {
            return GraphQLSchema.newSchema()
                    .query(queryBuilder)
                    .mutation(mutationBuilder)
                    .build()
        }
    }
}