pipeline {
    agent any

    environment {
        //BRANCH = "${env.GIT_BRANCH.split("/")[1]}"
        BRANCH = getBranchName()
        DOCKER_REGISTRY = getDockerRegistry(BRANCH)
        DOCKER_TLS_VERIFY = "1"
        DOCKER_HOST = "tcp://192.168.99.104:2376"
        DOCKER_CERT_PATH = "/Users/aloksingh/.docker/machine/machines/default"
        DOCKER_MACHINE_NAME = "default"
        ENV_NAME = getEnvName(BRANCH)
        //Use Pipeline Utility Steps plugin to read information from pom.xml into env variables - pipeline-utility-steps plugin
        ARTIFACT = readMavenPom().getArtifactId()
        VERSION = readMavenPom().getVersion()
        DO_NOT_SKIP_BUILD = doNotSkipBuild(BRANCH)
    }

    stages {
        stage ('Compile, Test and Package') {
            when {
                expression {return DO_NOT_SKIP_BUILD == 'true' }
            }
            steps {
                withMaven(maven : 'maven-3-6-3') {
                    sh './mvnw clean jxr:jxr verify package surefire-report:report-only'
                }
            }
        }

        stage ('Deploy Artifact') {
            when {
                expression {return DO_NOT_SKIP_BUILD == 'true' }
            }
            steps {
                withMaven(maven : 'maven-3-6-3') {
                    echo "Skipping for now!"
                    //sh 'mvn deploy -DskipTests'
                }
            }
        }

        stage ('Build Docker Image') {
            when {
                expression {return DO_NOT_SKIP_BUILD == 'true' }
            }
            steps {
                echo "Building ${ARTIFACT} - ${VERSION} - ${ENV_NAME}"
                script {
                    if (BRANCH == 'master') {
                        sh "docker build -t ${DOCKER_REGISTRY}/${ARTIFACT}:latest -t ${DOCKER_REGISTRY}/${ARTIFACT}:${VERSION} --build-arg JAR_FILE=target/${ARTIFACT}-${VERSION}.jar ."
                    } else if (BRANCH == 'dev') {
                        sh "docker build -t ${DOCKER_REGISTRY}/${ARTIFACT}-dev:latest -t ${DOCKER_REGISTRY}/${ARTIFACT}-dev:${VERSION} --build-arg JAR_FILE=target/${ARTIFACT}-${VERSION}.jar ."
                    } else {
                        echo "Don't know how to create image for ${env.GIT_BRANCH} branch"
                    }
                }
            }
        }

        stage ('Push Docker Image') {
            when {
                expression {return DO_NOT_SKIP_BUILD == 'true' }
            }
            steps {
                script {
                    // the login profile has to be created in adavnce for each environemnt using: aws configure --profile jenkins-dev
                    sh 'aws ecr get-login-password --profile jenkins-${ENV_NAME} | docker login --username AWS --password-stdin ${DOCKER_REGISTRY}'
                    if (BRANCH == 'master') {
                        sh "docker push ${DOCKER_REGISTRY}/${ARTIFACT}:${VERSION}"
                        sh "docker push ${DOCKER_REGISTRY}/${ARTIFACT}:latest"
                    } else if (BRANCH == 'dev') {
                        sh "docker push ${DOCKER_REGISTRY}/${ARTIFACT}-dev:${VERSION}"
                        sh "docker push ${DOCKER_REGISTRY}/${ARTIFACT}-dev:latest"
                    } else {
                        echo "Don't know which image to push ${env.BRANCH_NAME} branch"
                    }
                }
            }
        }
    }
}

def getBranchName() {
    if (env.BRANCH_NAME.startsWith('PR')) {
        return "${env.CHANGE_BRANCH}"
    } else {
        return "${env.BRANCH_NAME}"
    }
}

def getEnvName(branchName) {
    if( branchName == "master") {
        return "prod";
    } else if (branchName == "dev") {
        return "dev";
    } else {
        return "future";
    }
}

def doNotSkipBuild(branchName) {
    echo "Branch Name: ${branchName}"
    if( branchName == "master") {
        echo "Master"
        return 'true';
    } else if (branchName == "dev") {
        echo "Dev"
        return 'true';
    } else {
        echo "Other"
        return 'false';
    }
}

def getDockerRegistry(branchName) {
    if( branchName == "master") {
        return "040180071884.dkr.ecr.ap-south-1.amazonaws.com";
    } else if (branchName == "dev") {
        return "alokkusingh";
    } else {
        return "unknown";
    }
}

