import * as React from 'react';
import './App.css';

import Payment from "./components/Payment";

class App extends React.Component {
  public render() {
    return (
      <div className="App">
        <Payment />
      </div>
    );
  }
}

export default App;
