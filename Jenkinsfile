// Jenkins pipeline for the Password Generator API.
//
//   Checkout -> Version -> Test -> Build -> Docker image -> (archive artifacts)
//
// Trigger: SCM polling every minute. Jenkins runs on a laptop with no public address, so
// GitHub cannot deliver a webhook to it; instead Jenkins asks GitHub for the latest commit
// and builds only when it differs from the last one it built.
//
// CI enhancements:
//   * Automated tests - the Test stage runs JUnit + the JaCoCo 80% coverage gate BEFORE
//     anything is packaged; a failing test or low coverage stops the build.
//   * Build versioning - the JAR is named b<build number>-<commit SHA>, e.g.
//     password-generator-api-b7-3f2a9c1.jar, so every artifact traces back to its commit.
//   * GitHub Actions - .github/workflows/ci.yml runs the same build on every push and PR.

pipeline {
    agent any

    triggers {
        pollSCM('* * * * *')
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        MAVEN_OPTS = '-Djava.awt.headless=true'
        IMAGE = 'password-generator-api'
    }

    stages {

        stage('Version') {
            steps {
                script {
                    // GIT_COMMIT is set by the automatic checkout of the Jenkinsfile's repository.
                    env.SHORT_SHA = env.GIT_COMMIT.take(7)
                    env.VERSION_TAG = "b${env.BUILD_NUMBER}-${env.SHORT_SHA}"
                    currentBuild.displayName = "#${env.BUILD_NUMBER} ${env.SHORT_SHA}"
                }
                echo "Building commit ${env.GIT_COMMIT} as ${env.VERSION_TAG}"
                sh 'java -version && mvn -v'
            }
        }

        stage('Test') {
            steps {
                // test phase = compile + JUnit + JaCoCo report + coverage check
                sh 'mvn -B clean test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build') {
            steps {
                // Tests already passed above, so packaging skips them.
                sh 'mvn -B package -DskipTests -Dbuild.tag="$VERSION_TAG"'
                sh 'ls -l target/*.jar'
            }
        }

        stage('Docker image') {
            steps {
                // Uses the laptop's Docker daemon through the mounted socket, so the image
                // is immediately available to `kind load docker-image`.
                sh '''
                    docker build \
                        --build-arg BUILD_TAG="$VERSION_TAG" \
                        --build-arg VCS_REF="$GIT_COMMIT" \
                        -t "$IMAGE:$VERSION_TAG" -t "$IMAGE:latest" .
                    docker run --rm --entrypoint java "$IMAGE:$VERSION_TAG" -version
                    docker image ls "$IMAGE"
                '''
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
            echo "Artifact: target/password-generator-api-${env.VERSION_TAG}.jar  Image: ${env.IMAGE}:${env.VERSION_TAG}"
        }
        failure {
            echo 'Build failed - read the stage that turned red in the console output above.'
        }
    }
}
