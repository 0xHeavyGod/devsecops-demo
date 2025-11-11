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
        SONAR_TOKEN = credentials('sonarqube-token')
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
                  trivy image ${PROJECT_KEY} --exit-code 0 --format json --output trivy_image_report.json || true
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
                    steps {
                        echo '🔍 Scan DAST avec OWASP ZAP...'
                        echo '⚠️ Note: Configurez une URL cible réelle pour un scan complet'
                        script {
                            try {
                                // Option 1: Scanner une URL publique de test
                                sh '''
                                    docker run --rm -v $(pwd):/zap/wrk/:rw \
                                    owasp/zap2docker-stable zap-baseline.py \
                                    -t https://www.example.com \
                                    -g gen.conf \
                                    -r zap-report.html \
                                    -J zap-report.json \
                                    || true
                                '''

                                echo "✅ Scan DAST terminé - Vérifiez le rapport"
                            } catch (Exception e) {
                                echo "⚠️ DAST scan completed with warnings: ${e.message}"
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



    } // <-- fin stages

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
                        to: 'realdhia07@gmail.com'
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
