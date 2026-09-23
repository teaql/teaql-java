const assert = require('node:assert/strict');
const test = require('node:test');
const select = require('./select-odc-snapshot.cjs');

function fixture(runs, artifactsByRun = {}) {
  const outputs = new Map();
  const visited = [];
  return {
    outputs,
    visited,
    input: {
      context: { repo: { owner: 'teaql', repo: 'teaql-java' } },
      core: {
        info() {},
        setOutput(name, value) { outputs.set(name, value); },
      },
      github: {
        rest: {
          actions: {
            async listWorkflowRunsForWorkflow(args) {
              assert.equal(args.workflow_id, 'dependency-check-data.yml');
              assert.equal(args.branch, 'main');
              assert.equal(args.status, 'success');
              return { data: { workflow_runs: runs } };
            },
            async listWorkflowRunArtifacts({ run_id }) {
              visited.push(run_id);
              return { data: { artifacts: artifactsByRun[run_id] ?? [] } };
            },
          },
        },
      },
    },
  };
}

const trustedRun = {
  id: 42,
  conclusion: 'success',
  head_branch: 'main',
  event: 'schedule',
  head_repository: { full_name: 'teaql/teaql-java' },
};
const goodArtifact = {
  name: 'teaql-odc-v13.0.0-Linux',
  expired: false,
  size_in_bytes: 2_000_000,
};

test('selects a successful updater artifact from main', async () => {
  const state = fixture([trustedRun], { 42: [goodArtifact] });
  assert.equal(await select(state.input), 42);
  assert.equal(state.outputs.get('run_id'), '42');
});

test('rejects PR and fork provenance even if an artifact exists', async () => {
  const state = fixture([
    { ...trustedRun, id: 1, event: 'pull_request' },
    { ...trustedRun, id: 2, head_repository: { full_name: 'other/teaql-java' } },
    { ...trustedRun, id: 3, head_branch: 'feature' },
  ], { 1: [goodArtifact], 2: [goodArtifact], 3: [goodArtifact] });
  await assert.rejects(select(state.input), /No successful main-branch/);
  assert.deepEqual(state.visited, []);
});

test('fails closed for missing, expired or undersized artifacts', async () => {
  const state = fixture([trustedRun], {
    42: [
      { ...goodArtifact, expired: true },
      { ...goodArtifact, size_in_bytes: 100 },
    ],
  });
  await assert.rejects(select(state.input), /No successful main-branch/);
  assert.deepEqual(state.visited, [42]);
  assert.equal(state.outputs.size, 0);
});
