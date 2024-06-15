pipeline {
    agent any

    environment {
        JAVA_HOME = "/usr/lib/jvm/java-17-openjdk-amd64"
    }

    stages {

        stage('Checkout') {
            steps {
                sh 'git submodule update --init --recursive'
            }
        }

        stage('prepare') {
            steps {
	    sh 'mkdir -p out'
                dir('base/sounds') {
                    sh './make'
		    sh 'cp *.wav ../../luwrain/src/main/resources/org/luwrain/core/sound/'
            }
        }
}

        stage('Build') {
            steps {
                sh 'mvn install'
                dir('base/scripts') {
                    sh './lwr-ant-gen-all'
                    sh './lwr-build'
                }
            }
        }

    stage ('snapshot') {
        steps {
            dir 'base/scripts' {
                sh './lwr-snapshot ../../out'
            }
        }
    }
    }
}
