pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    skipDefaultCheckout(true)
    timeout(time: 60, unit: 'MINUTES')
  }

  parameters {
    // Backend tests may require MySQL; keep configurable.
    booleanParam(name: 'RUN_TESTS', defaultValue: false, description: 'Run backend tests (needs DB for some services)')
    booleanParam(name: 'SKIP_EVALUATION_TESTS', defaultValue: true, description: 'Skip evaluation tests if DB is not available')
    string(name: 'SONAR_PROJECT_KEY_PREFIX', defaultValue: 'slangenglish-backend', description: 'Prefix for Sonar project keys (one project per microservice)')
  }

  environment {
    // Must match: Manage Jenkins → System → SonarQube servers (Name)
    SONARQUBE_SERVER = 'Sonarqube'
  }

  stages {
    stage('Checkout') {
      steps {
        // Shallow checkout + higher timeout (avoids slow fetch issues)
        checkout([
          $class: 'GitSCM',
          branches: scm.branches,
          userRemoteConfigs: scm.userRemoteConfigs,
          extensions: [[
            $class: 'CloneOption',
            shallow: true,
            depth: 1,
            noTags: false,
            honorRefspec: true,
            timeout: 30
          ]]
        ])
      }
    }

    stage('Backend - clean/compile/test') {
      steps {
        script {
          def services = [
            [name: 'eureka',     dir: 'backend/eureka'],
            [name: 'gateway',    dir: 'backend/gateway'],
            [name: 'users',      dir: 'backend/microservices/users'],
            [name: 'notebook',   dir: 'backend/microservices/notebook'],
            [name: 'evaluation', dir: 'backend/microservices/evaluation'],
          ]

          for (s in services) {
            dir(s.dir) {
              if (isUnix()) {
                sh """
                  set -e
                  MVN='./mvnw'
                  if [ -f ./mvnw ]; then chmod +x ./mvnw || true; else MVN='mvn'; fi

                  GOAL='clean package -DskipTests'
                  if [ '${params.RUN_TESTS}' = 'true' ]; then GOAL='clean test'; fi
                  if [ '${s.name}' = 'evaluation' ] && [ '${params.SKIP_EVALUATION_TESTS}' = 'true' ]; then GOAL='clean package -DskipTests'; fi

                  echo "=== ${s.name}: \$MVN \$GOAL ==="
                  \$MVN -B -U \$GOAL
                """
              } else {
                bat """
                  set MVN=.\\mvnw.cmd
                  if not exist .\\mvnw.cmd set MVN=mvn

                  set GOAL=clean package -DskipTests
                  if "${params.RUN_TESTS}"=="true" set GOAL=clean test
                  if "${s.name}"=="evaluation" if "${params.SKIP_EVALUATION_TESTS}"=="true" set GOAL=clean package -DskipTests

                  echo === ${s.name}: %MVN% %GOAL% ===
                  %MVN% -B -U %GOAL%
                """
              }
            }
          }
        }
      }
    }

    stage('SonarQube') {
      steps {
        withSonarQubeEnv(env.SONARQUBE_SERVER) {
          script {
            def services = [
              [name: 'eureka',     dir: 'backend/eureka'],
              [name: 'gateway',    dir: 'backend/gateway'],
              [name: 'users',      dir: 'backend/microservices/users'],
              [name: 'notebook',   dir: 'backend/microservices/notebook'],
              [name: 'evaluation', dir: 'backend/microservices/evaluation'],
            ]

            for (s in services) {
              dir(s.dir) {
                def key = "${params.SONAR_PROJECT_KEY_PREFIX}-${s.name}"
                if (isUnix()) {
                  sh """
                    set -e
                    MVN='./mvnw'
                    if [ -f ./mvnw ]; then chmod +x ./mvnw || true; else MVN='mvn'; fi
                    echo "=== Sonar ${s.name}: projectKey=${key} ==="
                    \$MVN -B sonar:sonar -Dsonar.projectKey='${key}'
                  """
                } else {
                  bat """
                    set MVN=.\\mvnw.cmd
                    if not exist .\\mvnw.cmd set MVN=mvn
                    echo === Sonar ${s.name}: projectKey=${key} ===
                    %MVN% -B sonar:sonar -Dsonar.projectKey="${key}"
                  """
                }
              }
            }
          }
        }
      }
    }
  }
}

