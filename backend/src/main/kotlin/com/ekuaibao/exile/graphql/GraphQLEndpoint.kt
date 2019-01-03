package com.ekuaibao.exile.graphql

import com.ekuaibao.exile.web.*
import graphql.ExecutionInput
import graphql.GraphQL
import org.slf4j.LoggerFactory

@WebEndpoint("/graphql")
class GraphQLEndpoint(
        private val graphql: GraphQL
) {
    private val log = LoggerFactory.getLogger(GraphQLEndpoint::class.java)

    @WebMethod(method = "POST", path = "/", consumes = MediaType.C_JSON, produces = MediaType.C_JSON)
    fun execute(@WebParam(From.ENTITY) req: GraphqlRequest): Any {
        log.info("Execute query:\n{}", req.query)
        val result = graphql.execute(ExecutionInput.newExecutionInput()
                .query(req.query)
                .operationName(req.operationName)
                .variables(req.variables ?: emptyMap())
                .build())
        return result.toSpecification()
    }

    data class GraphqlRequest(val query: String, val operationName: String?, val variables: Map<String, Any?>?)
}