def call() {
        // step([$class: 'WsCleanup'])
        echo "Checkout groovy"
        checkout scm
        sh "git fetch"
}