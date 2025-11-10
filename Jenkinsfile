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
                archiveArtifacts artifacts: 'gitleaks-report.json', allowEmptyArchive: true
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
                withSonarQubeEnv('SonarQube') { // Vérifier que le nom correspond à Jenkins
                    sh "mvn sonar:sonar -Dsonar.projectKey=${env.PROJECT_KEY} -Dsonar.host.url=${env.SONAR_HOST} -Dsonar.token=${env.SONAR_TOKEN}"
                    echo "Sonar analysis finished"
                }
            }
        }



        stage('📦 SCA - Dependency Check') {
            steps {
                echo '🔍 Scanning project with Trivy...'
                               // Scan the local filesystem (project folder)

                                      // Use Jenkins workspace, no sudo required
                                       sh '''
                                                  mkdir -p ${WORKSPACE}/trivy-output
                                                  trivy fs --security-checks vuln,config --format json -o ${WORKSPACE}/trivy-output/trivy-report.json .
                                                  echo "Trivy scan completed. Report saved to ${WORKSPACE}/trivy-output/trivy-report.json"
                                              '''



                }
            post {
                always {
                    dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
                    archiveArtifacts artifacts: '**/dependency-check-report.html', allowEmptyArchive: true
                }
            }
        }

        stage('🐳 Docker Security Scan') {
            when { expression { fileExists('Dockerfile') } }
            steps {
                echo '🔍 Scan de sécurité de l\'image Docker avec Trivy...'
                script {
                    sh '''
                        docker build -t devsecops-demo:latest .
                        docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                        aquasec/trivy image --format json --output trivy-report.json devsecops-demo:latest
                        docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                        aquasec/trivy image --format template --template "@contrib/html.tpl" \
                        --output trivy-report.html devsecops-demo:latest
                    '''
                }
                archiveArtifacts artifacts: 'trivy-report.*', allowEmptyArchive: true
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

        stage('🚀 Deploy to Staging') {
            when { branch 'main' }
            steps {
                echo '🚀 Déploiement en environnement de staging...'
                sh 'echo "Déploiement simulé vers staging"'
            }
        }

        stage('🎯 DAST - Dynamic Security Testing') {
            when { branch 'main' }
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
        stage('📊 Quality Gate') {
                    steps {
                        echo '⏳ Vérification du Quality Gate SonarQube...'
                        timeout(time: 1, unit: 'MINUTES') { // Timeout augmenté
                            waitForQualityGate abortPipeline: true
                        }
                    }
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
