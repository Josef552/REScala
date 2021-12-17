object ConfigString {
  val value = """akka {
  loggers = ["akka.event.slf4j.Slf4jLogger"]
  loglevel = "WARNING"
  logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
  actor {
    provider = "akka.cluster.ClusterActorRefProvider"
    warn-about-java-serializer-usage = off
  }
  remote {
    log-remote-lifecycle-events = off
    netty.tcp {
      hostname = "127.0.0.1"
      port = 0
    }
  }

  cluster {
    seed-nodes = [
      "akka.tcp://ClusterSystem@127.0.0.1:2500",
      "akka.tcp://ClusterSystem@127.0.0.1:2501",
      "akka.tcp://ClusterSystem@127.0.0.1:2502",
      "akka.tcp://ClusterSystem@127.0.0.1:2503"]

    # auto downing is NOT safe for production deployments.
    # you may want to use it during development, read more about it in the docs.
    # auto-down-unreachable-after = 10s
  }
}

# Disable legacy metrics in akka-cluster.
akka.cluster.metrics.enabled = off

# Enable metrics extension in akka-cluster-metrics.
# akka.extensions = ["akka.cluster.metrics.ClusterMetricsExtension"]

# Sigar native library extract location during tests.
# Note: use per-jvm-instance folder when running multiple jvm on one host.
# akka.cluster.metrics.native-library-extract-folder = ${user.dir}/target/native
"""
}
