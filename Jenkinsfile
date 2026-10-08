pipeline {

    agent any

    stages {

        stage('Test') {
            steps {
                sh 'mvn -B clean test'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B package -DskipTests'
            }
        }
    }

    post {

        always {
            junit 'target/surefire-reports/*.xml'
        }

        success {
            archiveArtifacts(
                artifacts: 'target/*.jar',
                fingerprint: true
            )
        }
    }
}
