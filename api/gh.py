import requests

url = 'https://api.github.com/repos/SonarSource/%s'
checks_run_url = url + '/commits/LATEST/check-runs'
headers = {
    'Accept': 'application/vnd.github.v3+json'
}
params = {
    'branch': ''
}


def get_actions_run(repo, branch):
    target_url = checks_run_url % repo
    params['branch'] = branch
    response = requests.post(target_url, headers=headers, params=params)
    if response.status_code == 200:
        workflow_runs = response.json()
        print(workflow_runs)
        for run in workflow_runs['workflow_runs']:
            print(f"Run ID: {run['id']}, Status: {run['status']}, Conclusion: {run['conclusion']}")
    else:
        print(f"Failed to retrieve workflow runs: {response.status_code}")


get_actions_run('sonar-java', 'tt/isEmpty-quickfix')
