pipeline {
  agent any

  options {
    timestamps()
    skipDefaultCheckout(true)
  }

  parameters {
    booleanParam(name: 'RUN_CD', defaultValue: false, description: 'If true, deploy to Kubernetes after CI succeeds.')
    string(name: 'K8S_NAMESPACE', defaultValue: 'slangenglish', description: 'Kubernetes namespace for deployment.')
    booleanParam(name: 'PUSH_IMAGES', defaultValue: true, description: 'If true, build and push Docker images (required for CD).')
    string(name: 'IMAGE_REGISTRY', defaultValue: 'ghcr.io', description: 'Docker registry host (e.g. ghcr.io or docker.io).')
    string(name: 'IMAGE_NAMESPACE', defaultValue: 'lamisbenhassine', description: 'Registry namespace/user/org.')
    string(name: 'REGISTRY_CREDENTIALS_ID', defaultValue: 'registry-creds', description: 'Jenkins credentials ID (Username/Password or token) for docker login.')
  }

  environment {
    // Jenkins "Configure System" SonarQube server name
    SONARQUBE_SERVER = 'sonarqube'
    SONAR_PROJECT_KEY = 'slangenglish'
    SONAR_PROJECT_NAME = 'SlangEnglish'

    // Jenkins container uses this kubeconfig (we created it earlier)
    KUBECONFIG = '/var/jenkins_home/.kube/config.jenkins'
  }

  stages {
    stage('Checkout') {
      steps {
        deleteDir()
        timeout(time: 10, unit: 'MINUTES') {
          checkout([
            $class: 'GitSCM',
            branches: scm.branches,
            userRemoteConfigs: scm.userRemoteConfigs,
            extensions: [[
              $class: 'CloneOption',
              shallow: true,
              depth: 1,
              noTags: true,
              timeout: 10
            ]]
          ])
        }
      }
    }

    stage('CI - Backend') {
      parallel {
        stage('eureka - test & package') {
          steps {
            script {
              docker.image('maven:3.9.9-eclipse-temurin-17').inside('-u root:root') {
                sh 'cd backend/eureka && mvn -B test'
                sh 'cd backend/eureka && mvn -B -DskipTests package'
              }
            }
          }
        }

        stage('gateway - test & package') {
          steps {
            script {
              docker.image('maven:3.9.9-eclipse-temurin-17').inside('-u root:root') {
                sh 'cd backend/gateway && mvn -B test'
                sh 'cd backend/gateway && mvn -B -DskipTests package'
              }
            }
          }
        }

        stage('evaluation - test & package') {
          steps {
            script {
              docker.image('maven:3.9.9-eclipse-temurin-17').inside('-u root:root') {
                sh 'cd backend/microservices/evaluation && mvn -B test'
                sh 'cd backend/microservices/evaluation && mvn -B -DskipTests package'
              }
            }
          }
        }

        stage('users - test & package') {
          steps {
            script {
              docker.image('maven:3.9.9-eclipse-temurin-17').inside('-u root:root') {
                sh 'cd backend/microservices/users && mvn -B test'
                sh 'cd backend/microservices/users && mvn -B -DskipTests package'
              }
            }
          }
        }

        stage('notebook - test & package') {
          steps {
            script {
              docker.image('maven:3.9.9-eclipse-temurin-17').inside('-u root:root') {
                sh 'cd backend/microservices/notebook && mvn -B test'
                sh 'cd backend/microservices/notebook && mvn -B -DskipTests package'
              }
            }
          }
        }
      }
    }

    stage('CI - Frontend build') {
      steps {
        script {
          docker.image('node:20-bookworm').inside('-u root:root') {
            sh 'cd frontend && npm ci'
            sh 'cd frontend && npm run build'
          }
        }
      }
    }

    stage('CI - SonarQube scan') {
      steps {
        withSonarQubeEnv("${SONARQUBE_SERVER}") {
          script {
            docker.image('sonarsource/sonar-scanner-cli:latest').inside('-u root:root') {
              sh """
                sonar-scanner \\
                  -Dsonar.projectKey=${SONAR_PROJECT_KEY} \\
                  -Dsonar.projectName=${SONAR_PROJECT_NAME} \\
                  -Dsonar.sources=. \\
                  -Dsonar.exclusions=**/node_modules/**,**/dist/**,**/target/**,**/.angular/**,**/coverage/** \\
                  -Dsonar.java.binaries=backend/eureka/target/classes,backend/gateway/target/classes,backend/microservices/evaluation/target/classes,backend/microservices/users/target/classes,backend/microservices/notebook/target/classes
              """
            }
          }
        }
      }
    }

    stage('CI - SonarQube quality gate') {
      steps {
        timeout(time: 10, unit: 'MINUTES') {
          waitForQualityGate abortPipeline: true
        }
      }
    }

    stage('CI - Build & Push images') {
      when {
        expression { return params.PUSH_IMAGES || params.RUN_CD }
      }
      environment {
        IMAGE_TAG = "${env.GIT_COMMIT}"
      }
      stages {
        stage('Compute image tags') {
          steps {
            script {
              def registry = params.IMAGE_REGISTRY
              def ns = params.IMAGE_NAMESPACE

              env.IMG_EUREKA = "${registry}/${ns}/slangenglish-eureka:${env.IMAGE_TAG}"
              env.IMG_GATEWAY = "${registry}/${ns}/slangenglish-gateway:${env.IMAGE_TAG}"
              env.IMG_EVALUATION = "${registry}/${ns}/slangenglish-evaluation:${env.IMAGE_TAG}"
              env.IMG_USERS = "${registry}/${ns}/slangenglish-users:${env.IMAGE_TAG}"
              env.IMG_NOTEBOOK = "${registry}/${ns}/slangenglish-notebook:${env.IMAGE_TAG}"
            }
          }
        }

        stage('Docker login') {
          steps {
            withCredentials([usernamePassword(credentialsId: "${params.REGISTRY_CREDENTIALS_ID}", usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
              sh 'echo "$REG_PASS" | docker login -u "$REG_USER" --password-stdin "${IMAGE_REGISTRY}"'
            }
          }
        }

        stage('Build & push (parallel)') {
          parallel {
            stage('eureka') {
              steps {
                sh 'docker build -t "$IMG_EUREKA" backend/eureka'
                sh 'docker push "$IMG_EUREKA"'
              }
            }
            stage('gateway') {
              steps {
                sh 'docker build -t "$IMG_GATEWAY" backend/gateway'
                sh 'docker push "$IMG_GATEWAY"'
              }
            }
            stage('evaluation') {
              steps {
                sh 'docker build -t "$IMG_EVALUATION" backend/microservices/evaluation'
                sh 'docker push "$IMG_EVALUATION"'
              }
            }
            stage('users') {
              steps {
                sh 'docker build -t "$IMG_USERS" backend/microservices/users'
                sh 'docker push "$IMG_USERS"'
              }
            }
            stage('notebook') {
              steps {
                sh 'docker build -t "$IMG_NOTEBOOK" backend/microservices/notebook'
                sh 'docker push "$IMG_NOTEBOOK"'
              }
            }
          }
        }
      }
      post {
        always {
          sh 'docker logout "${IMAGE_REGISTRY}" || true'
        }
      }
    }

    stage('CD - Deploy to Kubernetes') {
      when {
        expression { return params.RUN_CD }
      }
      steps {
        sh 'kubectl version --client'
        sh "kubectl get ns ${params.K8S_NAMESPACE} || kubectl create ns ${params.K8S_NAMESPACE}"
        sh '''
          set -e
          mkdir -p k8s-rendered
          for f in k8s/*.yaml; do
            out="k8s-rendered/$(basename "$f")"
            sed \
              -e "s|__EUREKA_IMAGE__|${IMG_EUREKA}|g" \
              -e "s|__GATEWAY_IMAGE__|${IMG_GATEWAY}|g" \
              -e "s|__EVALUATION_IMAGE__|${IMG_EVALUATION}|g" \
              -e "s|__USERS_IMAGE__|${IMG_USERS}|g" \
              -e "s|__NOTEBOOK_IMAGE__|${IMG_NOTEBOOK}|g" \
              "$f" > "$out"
          done
        '''

        sh "kubectl -n ${params.K8S_NAMESPACE} apply -f k8s-rendered/"
        sh "kubectl -n ${params.K8S_NAMESPACE} rollout status deploy/eureka --timeout=180s"
        sh "kubectl -n ${params.K8S_NAMESPACE} rollout status deploy/gateway --timeout=180s"
        sh "kubectl -n ${params.K8S_NAMESPACE} rollout status deploy/evaluation --timeout=180s"
        sh "kubectl -n ${params.K8S_NAMESPACE} rollout status deploy/users --timeout=180s"
        sh "kubectl -n ${params.K8S_NAMESPACE} rollout status deploy/notebook --timeout=180s"
      }
    }
  }
}

