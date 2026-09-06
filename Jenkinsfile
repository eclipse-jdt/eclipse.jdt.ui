pipeline {
	options {
		timeout(time: 60, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
		timestamps()
	}
	agent {
		label "ubuntu-latest"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'temurin-jdk21-latest'
	}
	stages {
		stage('Build') {
			steps {
				xvnc(useXauthority: true) {
					script {
						// TEMPORARY: remove this PR-specific instrumentation before merging.
						if (env.CHANGE_ID == '3154') {
							sh '''#!/usr/bin/env bash
set -uo pipefail
rm -rf pr3154-diagnostics
mkdir -p pr3154-diagnostics
rm -f pr3154-diagnostics/stop
watcher=''
cleanup() {
    touch pr3154-diagnostics/stop
    if [[ -n "$watcher" ]]; then wait "$watcher" || true; fi
}
trap cleanup EXIT
if command -v python3 >/dev/null; then
    python3 .github/qa/pr3154_jenkins_diagnostics.py watch \
        --workspace "$WORKSPACE" --output "$WORKSPACE/pr3154-diagnostics" \
        --stop-file "$WORKSPACE/pr3154-diagnostics/stop" --jcmd "$JAVA_HOME/bin/jcmd" \
        > pr3154-diagnostics/observer.log 2>&1 &
    watcher=$!
else
    echo 'python3 unavailable: thread diagnostics will be missing' > pr3154-diagnostics/observer.log
fi
mvn -U -e -DskipTests=false -Dmaven.repo.local=$WORKSPACE/.m2/repository \
    clean verify --batch-mode --fail-at-end \
    -Pbree-libs -Papi-check -Pjavadoc -Pbuild-individual-bundles \
    -Dmaven.test.failure.ignore=true \
    -Dcompare-version-with-baselines.skip=false 2>&1 | tee pr3154-diagnostics/maven.log
codes=("${PIPESTATUS[@]}")
result=${codes[0]}
if [[ "$result" == 0 && "${codes[1]}" != 0 ]]; then result=${codes[1]}; fi
printf '%s\n' "$result" > pr3154-diagnostics/maven-exit.txt
cleanup
python3 .github/qa/pr3154_jenkins_diagnostics.py summarize \
    --workspace "$WORKSPACE" --output "$WORKSPACE/pr3154-diagnostics" \
    || echo 'Diagnostic summary unavailable; Maven status is preserved.'
exit "$result"
'''
						} else {
					sh """
					mvn -U -e -DskipTests=false -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						clean verify --batch-mode --fail-at-end \
						-Pbree-libs -Papi-check -Pjavadoc -Pbuild-individual-bundles \
						-Dmaven.test.failure.ignore=true \
						-Dcompare-version-with-baselines.skip=false
					"""
						}
					}

				}
			}
			post {
				always {
					script {
						if (env.CHANGE_ID == '3154') {
							sh returnStatus: true, script: 'python3 .github/qa/pr3154_jenkins_diagnostics.py summarize --workspace "$WORKSPACE" --output "$WORKSPACE/pr3154-diagnostics"'
							archiveArtifacts artifacts: 'pr3154-diagnostics/**,**/target/surefire-reports/*.xml', allowEmptyArchive: true
							if (fileExists('pr3154-diagnostics/summary.md')) {
								try {
									publishChecks name: 'PR-3154 runtime diagnostics', status: 'COMPLETED', conclusion: 'NEUTRAL',
										title: 'Diagnostic evidence only, not a test result',
										summary: 'Read-only snapshots from this Jenkins test runtime. Existing build and test checks remain authoritative.',
										text: readFile('pr3154-diagnostics/summary.md')
								} catch (Exception failure) {
									echo "Diagnostic check publication failed: ${failure.message}"
								}
							}
						}
					}
					archiveArtifacts artifacts: '*.log,*/target/work/data/.metadata/*.log,*/tests/target/work/data/.metadata/*.log,apiAnalyzer-workspace/.metadata/*.log', allowEmptyArchive: true
					// The following lines use the newest build on master that did not fail a reference
					// To not fail master build on failed test maven needs to be started with "-Dmaven.test.failure.ignore=true" it will then only marked unstable.
					// To not fail the build also "unstable: true" is used to only mark the build unstable instead of failing when qualityGates are missed
					// To accept unstable builds (test errors or new warnings introduced by third party changes) as reference using "ignoreQualityGate:true"
					// To only show warnings related to the PR on a PR using "publishAllIssues:false"
					discoverGitReferenceBuild referenceJob: 'eclipse.jdt.ui-github/master'
					junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
					recordIssues publishAllIssues: false, ignoreQualityGate: true, enabledForFailure: true, tools: [
							eclipse(name: 'Compiler', pattern: '**/target/compilelogs/*.xml'),
							issues(name: 'API Tools', id: 'apitools', pattern: '**/target/apianalysis/*.xml'),
						], qualityGates: [[threshold: 1, type: 'DELTA', unstable: true]]
					recordIssues tools: [javaDoc(), mavenConsole()]
				}
			}
		}
	}
}
