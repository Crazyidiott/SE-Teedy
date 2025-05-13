pipeline {
    agent any

     environment {
        DEPLOYMENT_NAME = "hello-node"
        CONTAINER_NAME = "hello-node-8dffb55fb-wr57c"
        IMAGE_NAME = "liee2023/teedy2025_manual"
    }
    stages {
        stage('Start Minikube') {
            steps {
                sh '''
                    if ! minikube status | grep -q "Running"; then
                        echo "Starting Minikube..."
                        minikube start
                    else
                        echo "Minikube already running."
                    fi
                '''
            }
        }
        stage('Set Image') {
            steps {
                sh '''
                    echo "Setting image for deployment..."
                    kubectl set image deployment/${DEPLOYMENT_NAME} ${CONTAINER_NAME}=${IMAGE_NAME}
                '''
            }
        }
        stage('Verify') {
            steps {
                sh 'kubectl rollout status deployment/${DEPLOYMENT_NAME}'
                sh 'kubectl get pods'
            }
        }
    }

    // docker implementation
    // environment { 
    //     // define environment variable 
    //     // Jenkins credentials configuration 
    //     DOCKER_HUB_CREDENTIALS = credentials('dockerhub_credentials') // Docker Hub credentials ID store in Jenkins 
    //     // Docker Hub Repository's name 
    //     DOCKER_IMAGE = 'liee2023/teedy2025_manual' // your Docker Hub user name and Repository's name 
    //     DOCKER_TAG = "${env.BUILD_NUMBER}" // use build number as tag 
    // }

    // stages { 
    //     stage('Build') { 
    //         steps { 
    //             checkout scmGit( 
    //                 branches: [[name: '*/b-12112411']],  
    //                 extensions: [],  
    //                 // userRemoteConfigs: [[url: 'https://github.com/Crazyidiott/SE-Teedy.git']] 
    //             // your github Repository 
    //             ) 
    //             sh 'mvn -B -DskipTests clean package' 
    //         } 
    //     } 
    //     // Building Docker images 
    //     stage('Building image') { 
    //         steps { 
    //             script { 
    //                 // assume Dockerfile locate at root  
    //                 docker.build("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}") 
    //             } 
    //         } 
    //     } 
    //     // Uploading Docker images into Docker Hub 
    //     stage('Upload image') { 
    //         steps { 
    //             script { 
    //                 // sign in Docker Hub 
    //                 docker.withRegistry('https://registry.hub.docker.com', 'dockerhub_credentials') { 
    //                     // push image 
    //                     docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push() 
    //                     // ：optional: label latest 
    //                     docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push('latest') 
    //                 } 
    //             } 
    //         }
    //     } 
    //     // Running Docker container 
    //     stage('Run containers') { 
    //         steps { 
    //             script { 
    //                 // // stop then remove containers if exists 
    //                 // sh 'docker stop teedy-container-8081 || true' 
    //                 // sh 'docker rm teedy-container-8081 || true' 
    //                 // // run Container 
    //                 // docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").run( 
    //                 // '--name teedy-container-8081 -d -p 8081:8080' 
    //                 // ) 
    //                 // // Optional: list all teedy-containers 
    //                 // sh 'docker ps --filter "name=teedy-container"' 

    //                 // stop then remove containers if exists 
    //                 sh 'docker stop teedy-container-8082 || true' 
    //                 sh 'docker rm teedy-container-8082 || true' 
    //                 sh 'docker stop teedy-container-8083 || true' 
    //                 sh 'docker rm teedy-container-8083 || true' 
    //                 sh 'docker stop teedy-container-8084 || true' 
    //                 sh 'docker rm teedy-container-8084 || true' 
    //                 // run Container 
    //                 docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").run( 
    //                 '--name teedy-container-8082 -d -p 8082:8080' 
    //                 ) 

    //                 docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").run( 
    //                 '--name teedy-container-8083 -d -p 8083:8080' 
    //                 )

    //                 docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").run( 
    //                 '--name teedy-container-8084 -d -p 8084:8080' 
    //                 ) 
    //                 // Optional: list all teedy-containers 
    //                 sh 'docker ps --filter "name=teedy-container"' 
    //             } 
    //         } 
    //     } 
    // } 
    // stages {
    //     stage('Clean') {
    //         steps {
    //             sh 'mvn clean'
    //         }
    //     }
    //     stage('Compile') {
    //         steps {
    //             sh 'mvn compile'
    //         }
    //     }
    //     stage('Test') {
    //         steps {
    //             sh 'mvn test -Dmaven.test.failure.ignore=true'
    //         }
    //     }
    //     stage('PMD') {
    //         steps {
    //             sh 'mvn pmd:pmd'
    //         }
    //     }
    //     stage('JaCoCo') {
    //         steps {
    //             sh 'mvn jacoco:report'
    //         }
    //     }
    //     stage('Javadoc') {
    //         steps {
    //             sh 'mvn javadoc:javadoc'
    //         }
    //     }
    //     stage('Site') {
    //         steps {
    //             sh 'mvn site'
    //         }
    //     }
    //     stage('Package') {
    //         steps {
    //             sh 'mvn package -DskipTests'
    //         }
    //     }
    // }
    // post {
    //     always {
    //         archiveArtifacts artifacts: '**/target/site/**/*.*', fingerprint: true
    //         archiveArtifacts artifacts: '**/target/**/*.jar', fingerprint: true
    //         archiveArtifacts artifacts: '**/target/**/*.war', fingerprint: true
    //         junit '**/target/surefire-reports/*.xml'
    //     }
    // }
}