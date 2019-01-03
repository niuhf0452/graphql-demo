import React, { Component } from 'react'
import './App.css'

let todoItemId = 0

class App extends Component {
  constructor (props) {
    super(props)
    this.state = {todos: []}
  }

  onKeyPress (e) {
    const text = e.target.value
    if (e.key === 'Enter' && text !== '') {
      e.target.value = ''
      this.setState((state) => {
        const todos = state.todos.slice()
        todos.push({
          id: todoItemId++,
          text: text,
          complete: false,
        })
        return {todos}
      })
    }
  }

  onCheck (e, i) {
    const checked = e.target.checked
    this.setState((state) => {
      const todos = state.todos.slice()
      const index = todos.findIndex(it => it.id === i.id)
      if (index >= 0) {
        todos[index] = {...i, complete: checked}
      }
      return {todos}
    })
  }

  render () {
    return (
      <div className="App">
        <input className="input" onKeyPress={e => this.onKeyPress(e)}/>
        <ul>
          {this.state.todos.map(i =>
            <li key={i.id} className={i.complete ? 'complete' : ''}>
              <input type="checkbox" onChange={e => this.onCheck(e, i)}/>
              <span>{i.text}</span>
            </li>,
          )}
        </ul>
      </div>
    )
  }
}

export default App
