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
		jdk 'openjdk-jdk17-latest'
	}
	stages {
		stage('Build') {
			steps {
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh """
					mvn -U -e -f pom.xml -Dmaven.compiler.failOnWarning=true -DskipTests=false -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						clean verify --batch-mode -Pbuild-individual-bundles -Pbree-libs -Papi-check -Dcompare-version-with-baselines.skip=false
					"""
				}
			}
			post {
				always {
					archiveArtifacts artifacts: '*.log,*/target/work/data/.metadata/*.log,*/tests/target/work/data/.metadata/*.log,apiAnalyzer-workspace/.metadata/*.log', allowEmptyArchive: true
					publishIssues issues:[scanForIssues(tool: java()), scanForIssues(tool: mavenConsole())]
					junit '**/target/surefire-reports/*.xml'
				}
			}
		}
	}
}
