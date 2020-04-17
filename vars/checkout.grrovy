def call() {
    stage("Checkout"){
        // step([$class: 'WsCleanup'])
        sh "echo "Executing Checkout""
        checkout scm
        sh "git fetch"
    }
}