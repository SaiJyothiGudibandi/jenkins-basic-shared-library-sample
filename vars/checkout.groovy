def call() {
        // step([$class: 'WsCleanup'])
        sh "echo "Executing Checkout""
        checkout scm
        sh "git fetch"
}