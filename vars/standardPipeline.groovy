def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    node {
	    // Clean workspace before doing anything
	    deleteDir()

	    try {
	        stage ('Checkout') {
				checkout scm
				branch = env.BRANCH_NAME ? "${env.BRANCH_NAME}" : scm.branches[0].name
				sh "echo $branch"
	        }
	        stage ('Build') {
	        	sh "echo 'building ${config.projectName} ...'"
				// archiveArtifacts artifacts: '.zip', onlyIfSuccessful: true
	        }
			if (branch != release) {
				stage('Tests') {
					parallel 'static': {
						sh "echo 'shell scripts to run static tests...'"
					},
							'unit': {
								sh "echo 'shell scripts to run unit tests...'"
							},
							'integration': {
								sh "echo 'shell scripts to run integration tests...'"
							}
				}
			}
	      	stage ('Deploy') {
	            sh "echo 'deploying to server ${config.serverDomain}...'"
	      	}
	    } catch (err) {
	        currentBuild.result = 'FAILED'
	        throw err
	    }
    }
}



