pipeline {
    agent any
    options {
        disableConcurrentBuilds()
        skipDefaultCheckout(false)
        timestamps()
    }

    tools {
        maven 'M2_HOME'
        jdk 'JAVA_HOME'
    }

    environment {
        SONAR_TOKEN = credentials('sonar-token1')
        SONAR_HOST = 'http://localhost:9000'
        PROJECT_KEY = 'devsecops-demo'
        HOST_PORT  = '8081'
        APP_PORT   = '3000'
    }

    stages {
        stage('🧭 Debug Workspace') {
            steps {
                echo "Workspace path: ${env.WORKSPACE}"
                sh 'pwd'
                sh 'ls -la'
            }
        }

        stage('🔍 Checkout Code') {
            steps {
                echo '📥 Récupération du code source...'
                checkout scm
            }
        }

        stage('🔐 Secrets Scan') {
            steps {
                echo '🔎 Scan des secrets exposés avec Gitleaks...'
                script {
                    try {
                        sh '/usr/local/bin/gitleaks detect --source=. --report-format=json --report-path=gitleaks-report.json || true'
                        archiveArtifacts allowEmptyArchive: true, artifacts: 'gitleaks-report.json'
                    } catch (Exception e) {
                        echo "⚠️ Secrets détectés ! Vérifiez gitleaks-report.json"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }

        stage('🔨 Build & Unit Tests') {
            steps {
                echo '🏗️ Compilation et tests unitaires...'
                sh 'mvn clean compile test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('🛡️ SAST - SonarQube Analysis') {
            steps {
                echo '🔍 Analyse statique du code avec SonarQube...'
                withSonarQubeEnv('SonarQube') {
                    sh "mvn sonar:sonar -Dsonar.projectKey=${env.PROJECT_KEY} -Dsonar.host.url=${env.SONAR_HOST} -Dsonar.login=${env.SONAR_TOKEN}"
                }
            }
        }

        stage('📊 Quality Gate') {
            steps {
                echo '⏳ Vérification du Quality Gate SonarQube...'
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('📦 SCA - Dependency Check') {
            steps {
                echo '🔎 Running Trivy scan...'
                sh '''
                    mkdir -p "${WORKSPACE}/trivy-output"
                    trivy fs --security-checks vuln,config --format json -o "${WORKSPACE}/trivy-output/trivy-report.json" .
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: '**/trivy-output/*.json', allowEmptyArchive: true
                }
            }
        }

        stage('Docker Scan - Image Security') {
              steps {
                echo '🔎 Scan de sécurité de l’image Docker...'
                sh '''
                  docker image ls
                  trivy image ${APP_NAME} --exit-code 0 --format json --output trivy_image_report.json || true
                '''
              }
            }


        stage('📦 Package Application') {
            steps {
                echo '📦 Packaging de l\'application...'
                sh 'mvn package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                }
            }
        }

        stage('🎯 DAST - Dynamic Security Testing') {
            when { expression { env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'master' } }
            steps {
                echo '🔍 Scan DAST avec OWASP ZAP...'
                script {
                    try {
                        sh '''
                            docker run --rm -t owasp/zap2docker-stable zap-baseline.py \
                            -t http://your-staging-url.com -r zap-report.html
                        '''
                    } catch (Exception e) {
                        echo "⚠️ Vulnérabilités détectées par ZAP"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
                publishHTML([
                    allowMissing: true,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: '.',
                    reportFiles: 'zap-report.html',
                    reportName: 'ZAP Security Report',
                    reportTitles: 'OWASP ZAP Security Report'
                ])
            }
        }

        stage('Deploy') {
              steps {
                echo '🚀 Déploiement du conteneur sur le port ${HOST_PORT}...'
                sh """
                  docker ps -q --filter "publish=${HOST_PORT}" | xargs -r docker stop
                  docker ps -q --filter "publish=${HOST_PORT}" | xargs -r docker rm
                  docker stop ${PROJECT_KEY} || true
                  docker rm ${PROJECT_KEY} || true
                  docker run -d --name ${PROJECT_KEY} -p ${HOST_PORT}:${APP_PORT} ${PROJECT_KEY}
                """
              }
            }

    post {
        always {
            echo '🧹 Nettoyage de l\'environnement...'
            cleanWs()
        }
        success {
            echo '✅ Pipeline terminé avec succès !'
            script {
                try {
                    emailext(
                        subject: "✅ Build SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: """
                            Le build a été complété avec succès !

                            Projet: ${env.JOB_NAME}
                            Build: ${env.BUILD_NUMBER}

                            Consultez les rapports de sécurité:
                            ${env.BUILD_URL}
                        """,
                        to: 'votre-email@example.com'
                    )
                } catch (Exception e) {
                    echo "Email notification non configuré: ${e.message}"
                }
            }
        }
        failure {
            echo '❌ Pipeline échoué !'
        }
        unstable {
            echo '⚠️ Build instable - Vulnérabilités détectées'
        }
    }
}
