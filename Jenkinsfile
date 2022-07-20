pipeline {
	options {
		timeout(time: 40, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		timestamps()
	}
	agent {
		label "centos-latest"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'openjdk-jdk17-latest'
	}
	stages {
		stage('Build') {
			steps {
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh """
					mvn -f pom.xml -Dmaven.compiler.failOnWarning=true -DskipTests=false -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						clean verify --batch-mode -Pbuild-individual-bundles -Pbree-libs -Papi-check
					"""
				}
			}
			post {
				always {
					archiveArtifacts artifacts: '*.log,*/target/work/data/.metadata/*.log,*/tests/target/work/data/.metadata/*.log,apiAnalyzer-workspace/.metadata/*.log', allowEmptyArchive: true
					recordIssues aggregatingResults: true, enabledForFailure: true, qualityGates: [[threshold: 1, type: 'DELTA', unstable: false]], tools: [acuCobol()]
					publishIssues issues:[scanForIssues(tool: java()), scanForIssues(tool: mavenConsole())]
					junit '**/target/surefire-reports/*.xml'
				}
			}
		}
	}
}
