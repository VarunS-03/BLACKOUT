const scenarioList = document.querySelector("#scenario-list");
const output = document.querySelector("#output");
const connectionState = document.querySelector("#connection-state");
const runStatus = document.querySelector("#run-status");

async function loadScenarios() {
  try {
    const response = await fetch("/api/scenarios");
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || "Could not load scenarios.");
    scenarioList.replaceChildren(...data.scenarios.map(createScenarioButton));
    connectionState.textContent = "Java backend ready";
  } catch (error) {
    connectionState.textContent = "Backend unavailable";
    scenarioList.replaceChildren(message("The simulator could not be reached. Start the Java dashboard and reload this page."));
    setStatus("ERROR", "error");
  }
}

function createScenarioButton(scenario) {
  const button = document.createElement("button");
  button.className = "scenario-button";
  button.type = "button";
  button.textContent = scenario.label;
  button.addEventListener("click", () => runScenario(scenario.id));
  return button;
}

async function runScenario(name) {
  const buttons = scenarioList.querySelectorAll("button");
  buttons.forEach(button => { button.disabled = true; });
  output.replaceChildren(message("Running the selected Java scenario…"));
  setStatus("RUNNING", "running");
  try {
    const response = await fetch("/api/run", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ scenario: name })
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || "The scenario did not complete.");
    renderResults(data);
    setStatus(data.passed ? "VERIFIED" : "NEEDS REVIEW", data.passed ? "success" : "warning");
  } catch (error) {
    output.replaceChildren(message(error.message));
    setStatus("ERROR", "error");
  } finally {
    buttons.forEach(button => { button.disabled = false; });
  }
}

function renderResults(data) {
  const fragment = document.createDocumentFragment();
  const summary = document.createElement("div");
  summary.className = "result-summary";
  summary.append(
    chip(`${data.summary.passed} of ${data.summary.total} scenarios passed`),
    chip(`Request: ${data.scenario}`)
  );
  fragment.append(summary);
  data.results.forEach(result => fragment.append(renderScenarioResult(result)));
  output.replaceChildren(fragment);
}

function renderScenarioResult(result) {
  const section = document.createElement("section");
  section.className = "scenario-result";
  const heading = document.createElement("div");
  heading.className = "result-heading";
  const title = document.createElement("h4");
  title.textContent = result.name;
  const passed = document.createElement("span");
  passed.className = result.passed ? "pass" : "fail";
  passed.textContent = result.passed ? "PASS" : "NEEDS REVIEW";
  heading.append(title, passed);

  const grid = document.createElement("dl");
  grid.className = "result-grid";
  if (Object.keys(result.serverOnline).length > 0) {
    const states = document.createElement("div");
    states.className = "result-field";
    const label = document.createElement("dt");
    label.textContent = "Server state";
    const list = document.createElement("dd");
    list.className = "state-list";
    Object.entries(result.serverOnline).forEach(([server, online]) => {
      const item = document.createElement("span");
      item.className = `state-item ${online ? "online" : "offline"}`;
      item.textContent = `${server} · ${online ? "ONLINE" : "OFFLINE"}`;
      list.append(item);
    });
    states.append(label, list);
    grid.append(states);
  }
  if (result.impactedServers.length) grid.append(field("Reachable impact context", result.impactedServers.join(", ")));
  if (result.recoveryOrder.length) grid.append(field("Recovery order", result.recoveryOrder.join(" → ")));
  grid.append(field("Pending recovery tasks", String(result.pendingTasks)));
  grid.append(field("Audit records", String(result.auditRecordCount)));
  section.append(heading, grid);
  return section;
}

function field(labelText, valueText) {
  const wrapper = document.createElement("div");
  wrapper.className = "result-field";
  const label = document.createElement("dt");
  label.textContent = labelText;
  const value = document.createElement("dd");
  value.textContent = valueText;
  wrapper.append(label, value);
  return wrapper;
}

function chip(text) {
  const element = document.createElement("span");
  element.className = "summary-chip";
  element.textContent = text;
  return element;
}

function message(text) {
  const element = document.createElement("p");
  element.className = "empty-state";
  element.textContent = text;
  return element;
}

function setStatus(text, state) {
  runStatus.textContent = text;
  runStatus.className = `status-pill ${state}`;
}

loadScenarios();
