package com.ekuaibao.exile.graphql

import graphql.schema.GraphQLFieldDefinition

interface GraphQLSchemaExtension {
    fun config(context: Context)

    interface Context {
        fun addQuery(query: GraphQLFieldDefinition)

        fun addMutation(mutation: GraphQLFieldDefinition)
    }
}