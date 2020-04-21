import java.util.regex.Pattern

def call(Map config) {
    // def config = [:]
	def branch
	def helm_chart_url
	def docker_img

	echo config.helm_artifactory_url
	echo config.helm_chart_name

	if (config.helm_artifactory_url && config.helm_chart_name) {
		if (config.helm_artifactory_url =~ /\/$/) {
			println "Helm URL has /"
			helm_chart_url = config.helm_artifactory_url + config.helm_chart_name
			println helm_chart_url
		} else {
			println "string does not have '/' at the end"
			config.helm_artifactory_url = config.helm_artifactory_url + '/';
			println config.helm_artifactory_url;
			helm_chart_url = config.helm_artifactory_url + config.helm_chart_name
			println helm_chart_url

		}
	} else {
		println "Helm Chart URl and Name - not defined or null"
		// error('Helm Chart URl and Name - not defined or null')
		sh "exit 1"
	}

	if(config.docker_id && config.docker_label){
		if (branch.startsWith("feature")){
			docker_img = 'feature' + config.docker_id + '/' + config.docker_label + env.BUILD_NUMBER
			println docker_img
		}else{
			docker_img = config.docker_id + '/' + config.docker_label + env.BUILD_NUMBER
			println docker_img
		}
	}else {
		println "Docker vars not defined/null"
		sh "exit 1"
	}

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
				scanStages()
				testStages()
				publishStages(helm_chart_url, docker_img)
			}
			if (branch.startsWith("dev") || branch.startsWith("rel") || branch.startsWith("master")) {
				echo "Release branch or Master"
				deployStages(helm_chart_url)
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
def publishStages(helm_chart_url, docker_img){
	def publishers = [:]
	publishers["docker"] = {
			stage("Build Docker Image") {
				echo "Build Docker"
				sh "docker build -t ${docker_img} ."
			}
			stage("Publish Docker Image") {
				echo "Publish Docker"
				sh "docker push ${docker_img}"
				sh "docker stop \$(docker ps -a -q)"
				sh "docker rm \$(docker ps -a -q)"
				sh "docker run --name mynginx1 -p 80:80 -d ${docker_img}"
				echo "Published docker image"
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
				 echo "Publish Helm Chart ${helm_chart_url} "
//				 use helm_chart_url
//				 <environment_namespace>-<Helm-chart-name>
			}
	}
	parallel publishers
}
def deployStages(helm_chart_url) {
	stage("Fetch-Helm-Chart") {
		// get <environment_namespace>-<Helm-chart-name>
		// fetch  helm_chart_url
		//unzip tgz
		echo "Fetching Helm chart ${helm_chart_url} from Helm Artifactory"
		echo "Unzip ${helm_chart_url}"
	}
	stage("Deploy-to-GKE") {
		//run helm command
		echo "Deploying Helm chart ${helm_chart_url} to GKE cluster"
	}
}