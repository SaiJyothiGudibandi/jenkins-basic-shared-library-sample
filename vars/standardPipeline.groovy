def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    node {
	    // Clean workspace before doing anything
	    deleteDir()

	    try {
			branch = env.BRANCH_NAME ? "${env.BRANCH_NAME}" : scm.branches[0].name
			sh "echo $branch"
			if (branch.startsWith("feature"))
			{
				echo "Starts with Feature*"
				stage('Checkout') {
					checkout scm
				}
				buildStages()
				testScanStages()
			}
			if (branch == config.release_branch) {
				stage('Deploy') {
					sh "echo 'deploying to server ...'"
					deployStages()

				}
			}
		}catch (err) {
	        currentBuild.result = 'FAILED'
	        throw err
	    }
    }
}

def buildStages(){
	def stages = [:]
	stages["build-stage"] = {
		stage("Build") {
			echo "building..."
		}
	}
	stages["publish-stage"] = {
		stage("Publish Artifact") {
			echo("Upload To Artifactory")
		}
	}
	parallel stages
}

def testScanStages(){
		stage('Test') {
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
	sage("Code-Scan") {
			echo("---- Scan Stage -----")
	}

}

def deployStages(){
	def publishers = [:]
	publishers["docker"] = {
			stage("Build Docker Image") {
				echo "Build Docker"
			}
			stage("Publish Docker Image") {
				echo "Publish Docker"
			}

	}
	publishers["helm-chart"] = {
			stage("Build Helm Chart") {
				echo "Build Helm Chart"
			}
			stage("Publish Helm Chart") {
				echo "Publish Helm Chart"
			}
	}
	parallel publishers
}



