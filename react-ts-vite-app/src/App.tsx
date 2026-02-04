import { useState } from "react";
import reactLogo from "./assets/react.svg";
import viteLogo from "/vite.svg";
import "./App.css";

const DPM_BUTTON_LABEL = "dpm button";
const DPM_ALERT_MESSAGE = "Hello from dpm button!";

type GreetingProps = { name: string };

function Greeting({ name }: GreetingProps) {
  return <h1>Hello, my friend {name}</h1>;
}

type DPMButtonProps = {
  message: string;
  onClick: (message: string) => void;
};

function DPMButton({ message, onClick }: DPMButtonProps) {
  return (
    <button type="button" onClick={() => onClick(message)}>
      {DPM_BUTTON_LABEL}
    </button>
  );
}

function App() {
  const [count, setCount] = useState(0);
  const handleDpmClick = (message: string) => {
    alert(message);
  };

  return (
    <>
      <div>
        <Greeting name="world" />
        <DPMButton message={DPM_ALERT_MESSAGE} onClick={handleDpmClick} />
        <a href="https://vite.dev" target="_blank">
          <img src={viteLogo} className="logo" alt="Vite logo" />
        </a>
        <a href="https://react.dev" target="_blank">
          <img src={reactLogo} className="logo react" alt="React logo" />
        </a>
      </div>
      <h1>Vite + React</h1>
      <div className="card">
        <button onClick={() => setCount((count) => count + 1)}>
          count is {count}
        </button>
        <button onClick={() => setCount((count) => count + 1)}>
          count is {count}
        </button>
        <p>
          Edit <code>src/App.tsx</code> and save to test HMR
        </p>
      </div>
      <p className="read-the-docs">
        Click on the Vite and React logos to learn more
      </p>
    </>
  );
}

export default App;
