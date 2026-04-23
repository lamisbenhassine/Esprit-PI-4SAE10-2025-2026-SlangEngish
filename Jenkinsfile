pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    // Disable Jenkins' implicit "Declarative: Checkout SCM" (it uses small default timeouts).
    skipDefaultCheckout(true)
    // Global safety timeout for the whole job
    timeout(time: 60, unit: 'MINUTES')
  }

  parameters {
    string(name: 'DOCKER_REGISTRY', defaultValue: 'docker.io', description: 'Registry hostname (ex: docker.io, ghcr.io)')
    booleanParam(name: 'PUSH_IMAGES', defaultValue: true, description: 'Push Docker images to registry')
    booleanParam(name: 'BUILD_ONLY_CHANGED', defaultValue: true, description: 'Build only microservices changed in this commit/PR')
    booleanParam(name: 'PARALLEL_BUILDS', defaultValue: true, description: 'Build services in parallel (faster)')
  }

  environment {
    // Jenkins credentials id that contains username+password for docker login
    DOCKER_CREDS_ID = "docker-registry-creds"
  }

  stages {
    stage('Checkout') {
      steps {
        // Faster + less fragile checkout (useful when Jenkins runs in Docker)
        // Shallow clone (depth=1) and higher timeout.
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

    stage('Build matrix (Maven + Docker)') {
      steps {
        script {
          // ---- Services to build ----
          // Each service dir must contain: pom.xml, Dockerfile (we added them), mvnw/mvnw.cmd
          def services = [
            [ name: 'eureka',    dir: 'backend/eureka',                 image: 'slangenglish/eureka' ],
            [ name: 'gateway',   dir: 'backend/gateway',                image: 'slangenglish/gateway' ],
            [ name: 'users',     dir: 'backend/microservices/users',    image: 'slangenglish/users' ],
            [ name: 'notebook',  dir: 'backend/microservices/notebook', image: 'slangenglish/notebook' ],
            [ name: 'evaluation',dir: 'backend/microservices/evaluation', image: 'slangenglish/evaluation' ],
          ]

          // ---- Detect changed files in this build ----
          def changedPaths = []
          try {
            for (cs in currentBuild.changeSets) {
              for (entry in cs.items) {
                for (f in entry.affectedFiles) {
                  changedPaths << (f.path ?: '')
                }
              }
            }
          } catch (ignored) {
            // If changeSets not available, fall back to building all.
          }

          def shouldBuildService = { String serviceDir ->
            if (!params.BUILD_ONLY_CHANGED) return true
            if (changedPaths == null || changedPaths.isEmpty()) return true // e.g., manual build
            return changedPaths.any { p -> p == serviceDir || p.startsWith(serviceDir + "/") }
          }

          def selected = services.findAll { s -> shouldBuildService(s.dir as String) }
          if (selected.isEmpty()) {
            // If nothing matched, build all (safer default).
            selected = services
          }

          // ---- Tag ----
          def shortCommit = (env.GIT_COMMIT ?: "dev").take(7)
          env.IMAGE_TAG = "${env.BUILD_NUMBER}-${shortCommit}"

          echo "Changed paths: ${changedPaths}"
          echo "Selected services: ${selected.collect { it.name }.join(', ')}"
          echo "Image tag: ${env.IMAGE_TAG}"

          def runCmd = { String cmd ->
            if (isUnix()) {
              sh cmd
            } else {
              bat cmd
            }
          }

          def buildOne = { svc ->
            def svcDir = svc.dir as String
            def imageName = svc.image as String
            def fullImage = "${params.DOCKER_REGISTRY}/${imageName}"

            dir(svcDir) {
              // Maven wrapper: mvnw on Unix, mvnw.cmd on Windows
              if (isUnix()) {
                // Some repos don't have mvnw (ex: notebook), and in Linux mvnw may lose exec bit.
                // So: chmod if present; otherwise fall back to system mvn.
                runCmd "if [ -f ./mvnw ]; then chmod +x ./mvnw || true; ./mvnw -B -U test package; else mvn -B -U test package; fi"
              } else {
                runCmd ".\\mvnw.cmd -B -U test package"
              }

              runCmd "docker build -t ${fullImage}:${env.IMAGE_TAG} ."
              runCmd "docker tag ${fullImage}:${env.IMAGE_TAG} ${fullImage}:latest"
            }

            return [ image: fullImage, tag: env.IMAGE_TAG ]
          }

          def results = []
          if (params.PARALLEL_BUILDS && selected.size() > 1) {
            def branches = [:]
            for (svc in selected) {
              def s = svc
              branches["${s.name}"] = {
                def r = buildOne(s)
                // keep simple log per branch; pushing is later
                echo "Built ${r.image}:${r.tag}"
              }
            }
            parallel branches
          } else {
            for (svc in selected) {
              def r = buildOne(svc)
              results << r
            }
          }
        }
      }
    }

    stage('Docker Push') {
      when { expression { return params.PUSH_IMAGES } }
      steps {
        script {
          def services = [
            [ dir: 'backend/eureka',                    image: 'slangenglish/eureka' ],
            [ dir: 'backend/gateway',                   image: 'slangenglish/gateway' ],
            [ dir: 'backend/microservices/users',       image: 'slangenglish/users' ],
            [ dir: 'backend/microservices/notebook',    image: 'slangenglish/notebook' ],
            [ dir: 'backend/microservices/evaluation',  image: 'slangenglish/evaluation' ],
          ]

          // Use same selection rule as build stage (based on changeSets)
          def changedPaths = []
          try {
            for (cs in currentBuild.changeSets) {
              for (entry in cs.items) {
                for (f in entry.affectedFiles) {
                  changedPaths << (f.path ?: '')
                }
              }
            }
          } catch (ignored) {}

          def shouldPush = { String serviceDir ->
            if (!params.BUILD_ONLY_CHANGED) return true
            if (changedPaths == null || changedPaths.isEmpty()) return true
            return changedPaths.any { p -> p == serviceDir || p.startsWith(serviceDir + "/") }
          }

          def selected = services.findAll { s -> shouldPush(s.dir as String) }
          if (selected.isEmpty()) selected = services

          def runCmd = { String cmd ->
            if (isUnix()) {
              sh cmd
            } else {
              bat cmd
            }
          }

          withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDS_ID, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
            if (isUnix()) {
              sh "echo $DOCKER_PASS | docker login ${params.DOCKER_REGISTRY} -u $DOCKER_USER --password-stdin"
            } else {
              // Windows: password-stdin supported by Docker Desktop; use bat with echo pipe
              bat "echo %DOCKER_PASS% | docker login ${params.DOCKER_REGISTRY} -u %DOCKER_USER% --password-stdin"
            }
          }

          for (svc in selected) {
            def fullImage = "${params.DOCKER_REGISTRY}/${svc.image}"
            runCmd "docker push ${fullImage}:${env.IMAGE_TAG}"
            runCmd "docker push ${fullImage}:latest"
          }
        }
      }
    }
  }

  post {
    always {
      echo "Done. Tag: ${env.IMAGE_TAG}"
    }
  }
}

