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
			if (branch.startsWith("feature") || branch.startsWith("dev")) {
					echo "Starts with Feature* or Dev"
					stage('Checkout') {
						checkout scm
					}
					buildStages()
					testScanStages()
				}
			if (branch.startsWith("dev")) {
				echo "Dev Branch"
				publishStages()
			}
			if (branch == config.release_branch) {
				echo "Release branch or Master"
				deployStages()
			}
		}catch (err) {
	        currentBuild.result = 'FAILED'
	        throw err
	    }
    }
}

def buildStages() {
	stage("Build") {
		echo "building..."
	}
}
def testScanStages(){
	stage("Code-Scan") {
		echo("---- Scan Stage -----")
	}
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
}
def publishStages(){
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
	publishers["gcr"] = {
		stage("Push Image to GCR") {
			echo "Pushing image to GCR"
		}
	}
	parallel publishers
}
def deployStages() {
	stage("Fetch-Helm-Chart") {
		echo "Fetching Helm chart from Helm Artifactory"
	}
	stage("Deploy-to-GKE") {
		echo "Deploying Helm chart to GKE cluster"
	}
}



