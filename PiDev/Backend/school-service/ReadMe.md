
# Install Maven
sudo apt-get install -y maven

# Install Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker

# Install Trivy
sudo apt-get install -y wget apt-transport-https gnupg
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
echo "deb https://aquasecurity.github.io/trivy-repo/deb generic main" | sudo tee /etc/apt/sources.list.d/trivy.list
sudo apt-get update && sudo apt-get install -y trivy

# Install Jenkins
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee /usr/share/keyrings/jenkins-keyring.asc >
/dev/null
echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/" | sudo tee
/etc/apt/sources.list.d/jenkins.list > /dev/null
sudo apt-get update && sudo apt-get install -y jenkins
sudo usermod -aG docker jenkins
sudo systemctl enable jenkins && sudo systemctl start jenkins

# Verify
java -version && mvn -version && docker --version && trivy --version

Step 1.3 — Get Jenkins initial password
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
Open http://localhost:8080 in your Windows browser, paste the password, install suggested plugins.

  ---
PHASE 2 — SonarQube Setup (Docker)

Step 2.1 — Run SonarQube in Docker
docker run -d --name sonarqube \
-p 9000:9000 \
-e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
sonarqube:community
Open http://localhost:9000 → login: admin / admin → change password → Create Project → name: pidev-school-service → generate a   
token, save it.

  ---
PHASE 3 — Jenkins Configuration

Step 3.1 — Install Jenkins Plugins

Go to Manage Jenkins → Plugins → Available:
- Docker Pipeline
- SonarQube Scanner
- Pipeline

Step 3.2 — Add Credentials (Manage Jenkins → Credentials → Global → Add Credentials)

┌───────────────────────┬───────────────────┬─────────────────────────────────────────┐
│          ID           │       Type        │                 Values                  │
├───────────────────────┼───────────────────┼─────────────────────────────────────────┤
│ dockerhub-credentials │ Username/Password │ Your DockerHub username + password      │
├───────────────────────┼───────────────────┼─────────────────────────────────────────┤
│ github-credentials    │ Username/Password │ GitHub username + personal access token │
├───────────────────────┼───────────────────┼─────────────────────────────────────────┤
│ sonar-token           │ Secret text       │ The SonarQube token from Step 2.1       │
└───────────────────────┴───────────────────┴─────────────────────────────────────────┘

Step 3.3 — Configure SonarQube (Manage Jenkins → System → SonarQube Servers)
- Name: SonarQube
- URL: http://localhost:9000
- Authentication token: sonar-token

Step 3.4 — Configure SonarQube Scanner (Manage Jenkins → Tools → SonarQube Scanner)
- Name: SonarScanner
- Install automatically: checked

  ---
PHASE 4 — Prepare the Deployment Directory in WSL

# Create deploy dir
mkdir -p /home/pidev/deployment/monitoring

# Copy docker-compose files (access Windows files via /mnt/c/)
cp /mnt/c/PIWEB/PiDev/Backend/docker-compose.yml /home/pidev/deployment/
cp /mnt/c/PIWEB/PiDev/Backend/monitoring/docker-compose.monitoring.yml /home/pidev/deployment/monitoring/
cp /mnt/c/PIWEB/PiDev/Backend/monitoring/prometheus.yml /home/pidev/deployment/monitoring/

# Create .env file
cat > /home/pidev/deployment/.env <<EOF
DOCKER_USERNAME=your-dockerhub-username
MYSQL_ROOT_PASSWORD=root
MAIL_USERNAME=tayechihamza7@gmail.com
MAIL_PASSWORD=sstv ycyj aupv wzmg
EOF

  ---
PHASE 5 — Edit Jenkinsfile (Set Your DockerHub Username)

Open Backend/Jenkinsfile and Backend/Jenkinsfile-CD, replace the line:
DOCKER_USERNAME = 'your-dockerhub-username'
with your actual DockerHub username.

  ---
PHASE 6 — Create CI Pipeline in Jenkins

1. New Item → name: PiDev-CI → Pipeline
2. Under Pipeline:
   - Definition: Pipeline script from SCM
   - SCM: Git
   - Repository URL: https://github.com/lamisbenhassine/Esprit-PI-4SAE10-2025-2026-SlangEngish.git
   - Branch: */gestion-des-offres
   - Credentials: github-credentials
   - Script Path: Backend/Jenkinsfile
3. Save → Build Now

  ---
PHASE 7 — Create CD Pipeline in Jenkins

1. New Item → name: PiDev-CD → Pipeline
2. Same SCM config but Script Path: Backend/Jenkinsfile-CD
3. Optionally add Build Triggers → Build after other projects are built → PiDev-CI

  ---
PHASE 8 — Access Services After Deployment

┌──────────────────┬─────────────────────────────────────┐
│     Service      │                 URL                 │
├──────────────────┼─────────────────────────────────────┤
│ Eureka Dashboard │ http://localhost:8761               │
├──────────────────┼─────────────────────────────────────┤
│ API Gateway      │ http://localhost:8085               │
├──────────────────┼─────────────────────────────────────┤
│ School Service   │ http://localhost:8081               │
├──────────────────┼─────────────────────────────────────┤
│ SonarQube        │ http://localhost:9000               │
├──────────────────┼─────────────────────────────────────┤
│ Prometheus       │ http://localhost:9090               │
├──────────────────┼─────────────────────────────────────┤
│ Grafana          │ http://localhost:3000 (admin/admin) │
└──────────────────┴─────────────────────────────────────┘

Grafana — Add Prometheus datasource:
1. Settings → Data Sources → Add → Prometheus
2. URL: http://prometheus:9090
3. Import dashboard ID 4701 (Spring Boot stats from grafana.com)

  ---
Files Created in This Session

Backend/
├── Jenkinsfile              ← CI pipeline
├── Jenkinsfile-CD           ← CD pipeline
├── docker-compose.yml       ← app deployment
├── .env.example             ← env var template
├── eureka-server/Dockerfile
├── api-gateway/Dockerfile
├── school-service/Dockerfile
└── monitoring/
├── prometheus.yml
└── docker-compose.monitoring.yml

Also updated:
- school-service/pom.xml — added actuator + micrometer-prometheus
- school-service/application.yml — exposed /actuator/prometheus, made Eureka URL env-var driven

