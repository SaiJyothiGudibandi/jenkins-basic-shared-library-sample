import java.util.regex.Pattern

def call(Map config) {
	def branch
	def helm_chart_url
	def docker_img
	def docker_tag = config.docker_tag

	// def build_info = readYaml file: "values.yaml"

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
		// error('Helm Chart URl and Name - not defined or null')
		sh "exit 0"
		// sh "exit 0"
		// error('Docker Vars not defined')
	}

	//Setting Docker image name based on the values passed from the config
	if(config.docker_id && config.docker_label){
			docker_img = config.docker_id + '/' + config.docker_label + '-' + env.BUILD_NUMBER
			println docker_img
		}else{
		println "Docker vars not defined/null"
		sh "exit 0"
	}

    node {
	    // Clean workspace before doing anything
	    deleteDir()

		//def build_info = readYaml file: "./resources/values.yaml"
		sh "find . -name '*.yaml'"

	    try {
			branch = env.BRANCH_NAME ? "${env.BRANCH_NAME}" : scm.branches[0].name
			sh "echo $branch"
			if (branch.startsWith("feature")){
				docker_img = config.docker_id + '/' + 'feature-' + config.docker_label + '-' + env.BUILD_NUMBER
				// println docker_img
			}
			if (branch.startsWith("feature") || branch.startsWith("dev") || branch.startsWith("rel") || branch.startsWith("master")) {
					echo "Starts with Feature* or Dev"
					stage('Checkout') {
						checkout scm
					}
				buildStages()
				scanStages()
				testStages()
				publishStages(helm_chart_url, docker_img, docker_tag)
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
def publishStages(helm_chart_url, docker_img, docker_tag){
	def publishers = [:]
	publishers["docker"] = {
			stage("Build Docker Image") {
				sh "docker build -t ${docker_img}:${docker_tag} ."
			}
			stage("Publish Docker Image") {
				sh "docker push ${docker_img}:${docker_tag}"
				// sh "docker stop \$(docker ps -a -q)"
				// sh "docker rm \$(docker ps -a -q)"
				// sh "docker run --name mynginx1 -p 80:80 -d ${docker_img}:${docker_tag}"
				echo "Published docker image - ${docker_img}:${docker_tag} to artifactory"
			}
	}
	publishers["gcr"] = {
		stage("Publish Image to GCR") {
			echo "Publishing docker image - ${docker_img}:${docker_tag} to GCR"
		}
	}
	publishers["helm-chart"] = {
		//Publish helm chart to artifact
			stage("Publish Helm Chart") {
				 echo "Publish Helm Chart ${helm_chart_url} "
			}
	}
	parallel publishers
}
def deployStages(helm_chart_url) {
	stage("Fetch-Helm-Chart") {
		// fetch  helm_chart_url
		echo "Fetching Helm chart ${helm_chart_url} from Helm Artifactory"
		echo "Unzip ${helm_chart_url}"
		//quality gate - read value.yaml file & get the img url, if branch is not feature and the img url is prefix with feature then error out.
		//branch is not feature then error out that u r ref to the feature branch image.
		echo "Read values.yaml after unzipping"

	}
	stage("Deploy-to-GKE") {
		//Run helm command to deploy
		echo "Deploying Helm chart ${helm_chart_url} to GKE cluster"
	}
}