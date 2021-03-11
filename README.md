# TLS NEED_WRAP issue reproducer

## Build

There's a script to build the reproducer:

```
./build.sh
```

It uses sbt and will download quite a lot of dependencies.

It will create a fat jar at `target/scala-2.13/akka-http-tls-stress-assembly-0.1.jar`.

## Run

Run 

```
java -jar target/scala-2.13/akka-http-tls-stress-assembly-0.1.jar
```

This will run TLS connections continuously and randomly drops some packets from server to client. If the right packets are dropped,
the server SSLEngine will get into the state where it doesn't produce any data any more but still returns another `NEED_WRAP`.

When that happens no more log messages are shown but a thread keeps spinning trying to `wrap` in a cycle.