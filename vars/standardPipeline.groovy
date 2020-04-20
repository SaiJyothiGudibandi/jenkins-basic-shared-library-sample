import java.util.regex.Pattern

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
			branch1 = scm.branches[0].name
			sh "echo $branch"
			sh "echo $branch1"
			// helm_chart_url = ${config.helm_artifactory_url} + ${config.helm_chart_name}

			echo config.helm_artifactory_url[-1]
			def helm_chart_name = "dev-my-helm-chart.tgz"
			sh "echo $helm_chart_name"
			def helm_chart_url = ""

			if (config.helm_artifactory_url =~ /\/$/) {
				println "Helm URL has /"
				helm_chart_url = config.helm_artifactory_url + ${helm_chart_url}
				println helm_chart_url
			} else {
				println "string does not have '/' at the end"
				config.helm_artifactory_url = config.helm_artifactory_url + '/';
				println config.helm_artifactory_url;
				helm_chart_url = config.helm_artifactory_url + ${helm_chart_url}
				println helm_chart_url
			}

			if (branch.startsWith("feature") || branch.startsWith("dev")) {
					echo "Starts with Feature* or Dev"
					stage('Checkout') {
						checkout scm
					}
				buildStages()
				scanStages()
				testStages()
			}
			if (branch.startsWith("dev")) {
				echo "Dev Branch"
				publishStages()
			}
			if (branch.startsWith("dev") || branch.startsWith("rel") || branch.startsWith("master")) {
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
		echo "build code"
	}
}
def testStages(){
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
def scanStages(){
	stage("Code-Scan") {
		echo("Code Scan Stage")
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
	publishers["gcr"] = {
		stage("Push Image to GCR") {
			echo "Pushing docker image to GCR"
		}
	}
	publishers["helm-chart"] = {
			//stage("Build Helm Chart") {
			//	echo "Build Helm Chart"
			// }
		//read environment_namespace variable from jenkinsfile and then publish
			stage("Publish Helm Chart") {
				echo ${helm_chart_url}
				echo "Publish Helm Chart ${helm_chart_url}"
				// use helm_chart_url
				// <environment_namespace>-<Helm-chart-name>
			}
	}
	parallel publishers
}
def deployStages() {
	stage("Fetch-Helm-Chart") {
		// get <environment_namespace>-<Helm-chart-name>
		// fetch  helm_chart_url
		//unzip tgz
		echo "Fetching Helm chart $helm_chart_url from Helm Artifactory"
		echo "Unzip $helm_chart_url"
	}
	stage("Deploy-to-GKE") {
		//run helm command
		echo "Deploying Helm chart to GKE cluster"
	}
}