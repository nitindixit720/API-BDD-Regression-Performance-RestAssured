pipeline {
    agent any

    parameters {
        choice(name: 'TEST_TYPE', choices: ['Regression', 'Performance'], description: 'Select test type to execute')
        choice(name: 'FUNCTIONAL_TEST_SCOPE', choices: ['Regression', 'Smoke', 'BAT'], description: 'Functional test scope (used when TEST_TYPE = Regression)')
        choice(name: 'ENVIRONMENT', choices: ['DEV', 'QA'], description: 'Target environment (-Denv value, must match config-<ENV>.properties)')
        choice(name: 'THREADS', choices: ['10', '15', '20', '25', '30', '40', '50'], description: 'Concurrent virtual users (Performance)')
        choice(name: 'RAMP_UP', choices: ['10', '30', '60', '100', '200'], description: 'Ramp-up time in seconds (Performance)')
        choice(name: 'DURATION', choices: ['120', '300', '600', '1800', '3600'], description: 'Test duration in seconds (Performance)')
        string(name: 'API_SELECTION', defaultValue: 'ALL', description: 'Comma-separated API names to load test, or ALL')
    }

    stages {
        stage('Validate Parameters') {
            steps {
                echo "TEST_TYPE=${params.TEST_TYPE} | ENVIRONMENT=${params.ENVIRONMENT}"
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Run Regression Tests') {
            when { expression { params.TEST_TYPE == 'Regression' } }
            steps {
                sh """
                    mvn clean test -Denv=${params.ENVIRONMENT} \
                    "-Dcucumber.filter.tags=@${params.FUNCTIONAL_TEST_SCOPE}"
                """
            }
        }

        stage('Run Performance Tests') {
            when { expression { params.TEST_TYPE == 'Performance' } }
            steps {
                sh """
                    mvn clean test -Denv=${params.ENVIRONMENT} \
                    -Dthreads=${params.THREADS} -DrampUp=${params.RAMP_UP} -Dduration=${params.DURATION} \
                    -Dapis=${params.API_SELECTION} "-Dcucumber.filter.tags=@PerformanceTest"
                """
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            archiveArtifacts allowEmptyArchive: true, artifacts: 'src/test/resources/reports/**, logs/**'
        }
    }
}
