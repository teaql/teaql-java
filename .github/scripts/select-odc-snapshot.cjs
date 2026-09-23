// Only successful main-branch updater runs can supply vulnerability data to CI.
// A PR-scoped cache is not acceptable provenance for a security gate.
module.exports = async function selectOdcSnapshot({ github, context, core }) {
  const { owner, repo } = context.repo;
  const fullName = `${owner}/${repo}`;
  const artifactName = 'teaql-odc-v13.0.0-Linux';
  const { data } = await github.rest.actions.listWorkflowRuns({
    owner,
    repo,
    workflow_id: 'dependency-check-data.yml',
    branch: 'main',
    status: 'success',
    per_page: 30,
  });

  for (const run of data.workflow_runs) {
    if (
      run.conclusion !== 'success' ||
      run.head_branch !== 'main' ||
      !['schedule', 'workflow_dispatch'].includes(run.event) ||
      run.head_repository?.full_name !== fullName
    ) {
      continue;
    }
    const response = await github.rest.actions.listWorkflowRunArtifacts({
      owner,
      repo,
      run_id: run.id,
      per_page: 100,
    });
    const artifact = response.data.artifacts.find(
      (entry) => entry.name === artifactName && !entry.expired && entry.size_in_bytes > 1_000_000,
    );
    if (artifact) {
      core.info(`Using trusted NVD snapshot from updater run ${run.id}`);
      core.setOutput('run_id', String(run.id));
      return run.id;
    }
  }
  throw new Error('No successful main-branch Dependency Check updater run has a usable snapshot');
};
