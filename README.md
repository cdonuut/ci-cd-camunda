<h1 align="center">Camunda CI/CD Workshop</h1>

<h2>Overview</h2>
<p>This project demonstrates a complete GitHub Actions-based CI/CD workflow integrated with Camunda 8 SaaS.</p>

<h2>What We Built</h2>
<ul>
  <li><b>Branching Strategy:</b> <code>feature/</code> → <code>dev</code> → <code>stage</code> → <code>main</code></li>
  <li><b>CI Checks:</b> Linting and process tests on pull requests</li>
  <li><b>Auto-deploy:</b> Deployment to Camunda clusters from <code>dev</code> and <code>stage</code> branches</li>
  <li><b>BPMN-controlled flow:</b> Process triggers deployments and webhooks using GitHub and Camunda</li>
</ul>

<h2>Workflow & Action Reference</h2>

<h3>CI Checks</h3>
<p><strong>Trigger:</strong> Pull Request to <code>dev</code>, <code>stage</code>, or <code>main</code><br>
<strong>Purpose:</strong> Runs Maven tests and BPMN linting to validate model quality before merge.</p>

<h3>Notify Camunda – Dev PR Merge Event</h3>
<p><strong>Trigger:</strong> Pull Request merged into <code>dev</code><br>
<strong>Purpose:</strong> Sends the <code>devMergeRequestPassed</code> message to Camunda Dev Cluster to start the CI/CD BPMN process.</p>

<h3>Trigger Camunda CI/CD Process</h3>
<p><strong>Trigger:</strong> Successful completion of <code>Notify Camunda – Dev PR Merge Event</code><br>
<strong>Purpose:</strong> Starts the BPMN pipeline via Camunda Webhook (using a Connector) to automate deployment to Stage.</p>

<h3>Auto Merge Dev to Stage</h3>
<p><strong>Trigger:</strong> BPMN process REST call (optional fallback: push to <code>dev</code>)<br>
<strong>Purpose:</strong> Automatically merges <code>dev</code> into <code>stage</code>, preparing for deployment to Stage.</p>

<h3>Camunda CI/CD Deployment (Dev)</h3>
<p><strong>Trigger:</strong> Push to <code>dev</code><br>
<strong>Purpose:</strong> Deploys BPMN, DMN, and Forms to Camunda Dev Cluster.</p>

<h3>Camunda CI/CD Deployment (Stage)</h3>
<p><strong>Trigger:</strong> Successful completion of <code>Auto Merge Dev to Stage</code> (or push to <code>stage</code>)<br>
<strong>Purpose:</strong> Deploys the newly merged artefacts to Camunda Stage Cluster.</p>

<h3>Notify Camunda – Stage Deployment Started</h3>
<p><strong>Trigger:</strong> Push to <code>stage</code><br>
<strong>Purpose:</strong> Sends <code>stageDeploymentStarted</code> message to Camunda Dev Cluster as input for deployment tracking or downstream actions.</p>

<h3>Feature Branch Cleanup</h3>
<p><strong>Trigger:</strong> Pull Request closed and merged into <code>dev</code><br>
<strong>Purpose:</strong> Deletes merged branches matching prefixes like <code>feature/</code>, <code>bugfix/</code>, <code>release/</code>, etc. to keep the repo tidy.</p>

<h3>get-bearer-token (Custom Action)</h3>
<p><strong>Type:</strong> Composite GitHub Action<br>
<strong>Purpose:</strong> Obtains Zeebe/Camunda bearer token via OAuth2 using client credentials.</p>

<h3>send-camunda-message (Custom Action)</h3>
<p><strong>Type:</strong> Composite GitHub Action<br>
<strong>Purpose:</strong> Sends a message to Zeebe/Camunda using a specified token, message name, correlation key, and dynamic variables.</p>

<h2>Workflow Execution Details</h2>

<h3>1. Pull Request Created (feature → dev)</h3>
<ul>
  <li>Runs <code>CI Checks</code> workflow</li>
  <li>Executes:
    <ul>
      <li>Maven unit tests</li>
      <li>BPMN linting with <code>bpmnlint</code></li>
    </ul>
  </li>
</ul>

<h3>2. PR Merged into <code>dev</code></h3>
<ul>
  <li>Sends a Camunda message: <code>devMergeRequestPassed</code></li>
  <li>Triggers the BPMN process in Camunda 8 SaaS via REST</li>
</ul>

<h3>3. CI/CD BPMN Execution</h3>
<ul>
  <li>BPMN process receives the merge message</li>
  <li>Automatically merges <code>dev</code> → <code>stage</code> via REST call or GitHub CLI</li>
</ul>

<h3>4. Push to <code>stage</code> Detected</h3>
<ul>
  <li>Sends <code>stageDeploymentStarted</code> message to Camunda</li>
  <li>Triggers GitHub Actions workflow:
    <ul>
      <li>Checks out <code>stage</code> code</li>
      <li>Fetches secrets & token</li>
      <li>Deploys models to Camunda Stage cluster</li>
    </ul>
  </li>
</ul>

<h3>5. Manual Merge to <code>main</code></h3>
<ul>
  <li>Currently not automated — reserved for production promotion</li>
</ul>

<h3>Messaging Strategy</h3>
<ul>
  <li>Camunda messages used:
    <ul>
      <li><code>devMergeRequestPassed</code></li>
      <li><code>stageDeploymentStarted</code></li>
    </ul>
  </li>
  <li>All messages use the Dev cluster as central controller</li>
</ul>

<h2>Folders</h2>
<ul>
  <li><code>.github/workflows/</code> – CI/CD pipeline logic</li>
  <li><code>actions/</code> – Custom GitHub actions (bearer token, Camunda messages)</li>
  <li><code>src/main/resources/models/</code> – BPMN, DMN, and Form artifacts</li>
</ul>

<h2>Appendix – Legacy <code>_promote</code> Trigger Strategy</h2>

<p>
  Before introducing a fully message-driven, BPMN-controlled deployment pipeline, the CI/CD process was manually triggered using a special <code>_promote</code> marker in the Git commit message.
</p>

<h3>Concept</h3>
<p>
  A GitHub Actions workflow listened for pushes to the <code>dev</code> branch and would only proceed if the commit message contained <code>_promote</code>.
  This gave developers manual control to initiate CI/CD promotion logic without merging a PR, but it relied on commit message conventions and was prone to being overlooked or misused.
</p>

<h3>Example Workflow (Deprecated)</h3>

<pre><code>name: Trigger CI/CD Pipeline from _promote commit

on:
  push:
    branches:
      - dev

jobs:
  trigger-cicd:
    if: contains(github.event.head_commit.message, '_promote')
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Trigger Camunda SaaS CI/CD Process
        run: |
          curl -X POST "https://bru-2.connectors.camunda.io/&lt;cluster-id&gt;/inbound/&lt;webhook-id&gt;" \
            -H "Content-Type: application/json" \
            -d '{
                  "folderId": "your-folder-id",
                  "env": "dev",
                  "stageEnvironment": "stage",
                  "productionEnvironment": "main",
                  "initiator": "GitHub Actions"
              }'
</code></pre>

<h3>Replaced By</h3>
<p>
  This mechanism was replaced with a <strong>pull request + merge-based model</strong>, where:
  <ul>
    <li>The BPMN pipeline is triggered on a <code>dev</code> PR merge,</li>
    <li>Controlled via GitHub workflows and Camunda messages,</li>
    <li>Each step is auditable and BPMN-governed.</li>
  </ul>
</p>

<h2>Results</h2>
<p>This repo showcases a reproducible GitOps-style automation pipeline for Camunda model deployments with governance and visibility.</p>    