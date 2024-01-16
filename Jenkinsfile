pipeline {
	options {
		timeout(time: 40, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
		timestamps()
	}
	agent {
		label "centos-latest"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'temurin-jdk17-latest'
	}
	stages {
		stage('Build') {
			steps {
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh """
					mvn -U -e -Dmaven.compiler.failOnWarning=false -DskipTests=false -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						clean verify --batch-mode -Pbuild-individual-bundles -Pbree-libs -Papi-check -Dcompare-version-with-baselines.skip=false
					"""
				}
			}
			post {
				always {
					archiveArtifacts artifacts: '*.log,*/target/work/data/.metadata/*.log,*/tests/target/work/data/.metadata/*.log,apiAnalyzer-workspace/.metadata/*.log', allowEmptyArchive: true
					junit '**/target/surefire-reports/*.xml'
					discoverGitReferenceBuild referenceJob: 'eclipse.jdt.ui-github/master'
					recordIssues publishAllIssues:false, tools:  [eclipse()], qualityGates: [[threshold: 1, type: 'DELTA_NORMAL', unstable: true]], minimumSeverity: 'NORMAL'
					recordIssues publishAllIssues:false, tools:  [javaDoc()], qualityGates: [[threshold: 1, type: 'DELTA', unstable: true]]
					recordIssues publishAllIssues:false, tools: [mavenConsole()], qualityGates: [[threshold: 1, type: 'DELTA_ERROR', unstable: true]]
				}
			}
		}
	}
}
