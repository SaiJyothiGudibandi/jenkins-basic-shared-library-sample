import java.util.regex.Pattern

def call(Map config) {
	def branch
	def helm_chart_url
	def docker_img
	def docker_tag = config.docker_tag
	def helm_docker_img = config.helm_docker_img

	// Setting Helm Chart Url based on the values passed from the config
	if (config.helm_artifactory_url && config.helm_chart_name) {
		if (config.helm_artifactory_url =~ /\/$/) {
			println "Helm URL has /"
			helm_chart_url = config.helm_artifactory_url + config.helm_chart_name
			println helm_chart_url
		} else {
			println "Helm URL does not have '/' at the end. Adding '/' ... "
			config.helm_artifactory_url = config.helm_artifactory_url + '/';
			println config.helm_artifactory_url;
			helm_chart_url = config.helm_artifactory_url + config.helm_chart_name
			println helm_chart_url

		}
	} else {
		println "Helm Chart URl and Name - not defined or null"
		sh "exit 0"
	}

	node {
		// Clean workspace before doing anything
		deleteDir()

		try {
			branch = env.BRANCH_NAME ? "${env.BRANCH_NAME}" : scm.branches[0].name
			//sh "echo $branch"
			//Setting Docker image name based on the values passed from the config
			if(config.docker_tag && config.docker_label){
				if (branch.startsWith("feature")) {
					docker_img=config.docker_label.substring(0,config.docker_label.lastIndexOf('/')+1) + 'fearure-' + config.docker_label.substring(config.docker_label.lastIndexOf('/')+1) + '-' + env.BUILD_NUMBER
					println docker_img
				}
				else{
					docker_img =  config.docker_label + '-' + env.BUILD_NUMBER
					println docker_img
				}
			}else{
				println "Docker vars not defined/null"
				sh "exit 0"
			}
			if (branch.startsWith("feature") || branch.startsWith("dev")) {
				echo "Starts with Feature* or Dev"
				stage('Checkout') {
					checkout scm
				}
				buildStages()
				scanStages()
				testStages()
				publishStages(helm_chart_url, docker_img, docker_tag)
				deployStages(helm_chart_url, helm_docker_img, branch)
			}
			if (branch.startsWith("rel") || branch.startsWith("master")) {
				deployStages(helm_chart_url, helm_docker_img, branch)
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
def publishStages(helm_chart_url, docker_img, docker_tag){
	def publishers = [:]
	publishers["docker"] = {
		stage("Build-Docker-Image") {
			echo "docker build -t ${docker_img}:${docker_tag} ."
		}
		stage("Publish-Docker-Image-to-Artifactory") {
			// sh "docker push ${docker_img}:${docker_tag}"
			// sh "docker stop \$(docker ps -a -q)"
			// sh "docker rm \$(docker ps -a -q)"
			// sh "docker run --name mynginx1 -p 80:80 -d ${docker_img}:${docker_tag}"
			echo "Publish docker image - ${docker_img}:${docker_tag} to artifactory"
		}
	}
	publishers["gcr"] = {
		stage("Publish-Docker-Image-to-GCR") {
			echo "Publishing docker image - ${docker_img}:${docker_tag} to GCR"
		}
	}
	publishers["helm-chart"] = {
		//Publish helm chart to artifact
		stage("Publish-Helm-Chart-to-Artifactory") {
			echo "Publish Helm Chart ${helm_chart_url} "
		}
	}
	parallel publishers
}
def deployStages(helm_chart_url, helm_docker_img, branch) {
	stage("Fetch-Helm-Chart-from-Artifactory") {
		// fetch  helm_chart_url
		echo "Fetching Helm chart ${helm_chart_url} from Helm Artifactory"
		echo "Unzip ${helm_chart_url}"
		//quality gate - read value.yaml file & get the img url, if branch is not feature and the img url is prefix with feature then error out.
		//branch is not feature then error out that u r ref to the feature branch image.
	}
	stage("Deploy-to-GKE") {
		echo "Read values.yaml after unzipping"
		println helm_docker_img
		def helm_docker_img_label = helm_docker_img.substring(helm_docker_img.lastIndexOf("/") + 1)
		helm_docker_img_label = helm_docker_img_label.substring(0, helm_docker_img_label.indexOf('-'))
		println helm_docker_img_label
		if (helm_docker_img_label == "feature"){
			if (branch.startsWith("feature")){
				//Run helm command to deploy
				echo "Deploying Helm chart ${helm_chart_url} to Lower GKE cluster"
			} else {
				println "Can't deploy ${helm_chart_url} to GKE, because you are refering to Feature branch image in Helm Chart values file."
				exit 0
			}
		}
		else {
			//Run helm command to deploy
			echo "Deploying Helm chart ${helm_chart_url} to GKE cluster"
		}
	}
}