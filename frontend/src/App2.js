import React from 'react'
import ApolloClient from 'apollo-boost'
import Query from 'react-apollo/Query'
import Mutation from 'react-apollo/Mutation'
import ApolloProvider from 'react-apollo/ApolloProvider'
import gql from 'graphql-tag'
import './App.css'

const client = new ApolloClient({
  uri: '/api/graphql',
})

const AddTodo = gql`
mutation addTodo($clientMutationId: String!, $text: String!) {
  addTodo(input: {
    clientMutationId: $clientMutationId
    text: $text
  }) {
    clientMutationId
  }
}`

const CompleteTodo = gql`
mutation completeTodo($clientMutationId: String!, $todoId: String!, $complete: Boolean!) {
  completeTodo(input: {
    clientMutationId: $clientMutationId
    todoId: $todoId
    complete: $complete
  }) {
    clientMutationId
    todo {
      id
      complete
    }
  }
}`

const QueryTodo = gql`
{
  todos {
    edges {
      node {
        id
        text
        complete
      }
      cursor
    }
  }
}`

let clientMutationId = 0

function TodoList () {
  function onKeyPress (e, addTodo) {
    const text = e.target.value
    if (e.key === 'Enter' && text !== '') {
      e.target.value = ''
      addTodo({
        variables: {
          clientMutationId: clientMutationId++,
          text: text,
        },
        refetchQueries: [{query: QueryTodo}],
      })
    }
  }

  function onCheck (e, id, completeTodo) {
    const checked = e.target.checked
    completeTodo({
      variables: {
        clientMutationId: clientMutationId++,
        todoId: id,
        complete: checked,
      },
    })
  }

  return (
    <div className="App">
      <Mutation mutation={AddTodo}>
        {(addTodo, {loading, error}) => {
          if (loading) {
            return <p>loading...</p>
          }
          if (error) {
            return <p>{'error: ' + error.message}</p>
          }
          return <input className="input"
                        onKeyPress={e => onKeyPress(e, addTodo)}/>
        }}
      </Mutation>
      <Query query={QueryTodo}>
        {({loading, error, data}) => {
          if (loading) {
            return <p>loading...</p>
          }
          if (error) {
            return <p>{'error: ' + error.message}</p>
          }
          return (
            <ul>
              {data.todos.edges.map(({node}) =>
                <li key={node.id} className={node.complete ? 'complete' : ''}>
                  <Mutation mutation={CompleteTodo}>
                    {(completeTodo, {loading, error}) => {
                      if (loading) {
                        return <p>loading...</p>
                      }
                      if (error) {
                        return <p>{'error: ' + error.message}</p>
                      }
                      return <input type="checkbox"
                                    checked={node.complete}
                                    onChange={e => onCheck(e, node.id,
                                      completeTodo)}/>
                    }}
                  </Mutation>
                  <span>{node.text}</span>
                </li>,
              )}
            </ul>
          )
        }}
      </Query>
    </div>
  )
}

function App () {
  return (
    <ApolloProvider client={client}>
      <TodoList/>
    </ApolloProvider>
  )
}

export default App
