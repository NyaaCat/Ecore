pipeline {
    agent any
    stages {
            stage('Build') {
                tools {
                    jdk "jdk21"
                    maven "apache-maven-3.9.9"
                }
                steps {
                    sh 'mvn deploy --batch-mode -Pdeploy-local'
                }
            }
        }

    post {
           always {
               archiveArtifacts artifacts: 'target/ecore-*.jar', fingerprint: true
               cleanWs()
           }
    }
}