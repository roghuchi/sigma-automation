pipeline {
    agent any

    environment {
        GIT_REPO_URL = 'https://github.com/SigmaHQ/sigma'
    }

    stages {
        stage('Clean Workspace') {
            steps {
                cleanWs()
                echo "Workspace cleaned."
            }
        }

        stage('Clone Repository') {
            steps {
                git branch: 'master', url: "$GIT_REPO_URL"
                echo "Repository cloned: $GIT_REPO_URL"
            }
        }

        stage('Process YAML Files') {
            steps {
                script {
                    // Find all .yml files, convert them to .aql
                    try {
                        sh '''
                            find "${WORKSPACE}" -name '*.yml' | while read file; do
                                # Set output .aql file path
                                aql_file="${file%.yml}.aql"
                                
                                # Run the sigma conversion command for each .yml file
                                sigma convert -t q_radar_aql -p qradar-aql-payload "$file" -o "$aql_file" 2>&1 || true
                                
                                # Check if the .aql file was successfully created
                                if [ -f "$aql_file" ]; then
                                    echo "Conversion successful: $file -> $aql_file"
                                else
                                    echo "Conversion failed for: $file"
                                fi
                            done
                        '''
                    } catch (e) {
                        echo "Warnings encountered during conversion, but continuing."
                    }
                }
            }
        }

    }

    post {
        always {
            echo "Pipeline completed."
        }
        success {
            echo "Pipeline executed successfully."
        }
        failure {
            echo "Pipeline execution failed. Please check the logs for details."
        }
    }
}
