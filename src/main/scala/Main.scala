import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model._
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.stream.KillSwitches
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, TimeoutException}
import scala.util.Try

object Main extends App {
  val Parallelism = 128
  val RunTime = 60.seconds

  val sslContext = {
    val password: Array[Char] = "password".toCharArray

    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("keystore.p12")

    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
    sslContext
  }

  private val handshakeCount = new AtomicLong(0)

  def startServer() = {
    // separate system for client and server to separate problems
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "server")

    val helloString = Source.single(ByteString("<h1>Say hello to akka-http</h1>"))

    def route(rq: HttpRequest) = {
      // use close delimited responses to kill HTTP keep-alive and make more handshakes
      Future.successful(HttpResponse(entity = HttpEntity.CloseDelimited(ContentTypes.`text/html(UTF-8)`, helloString)))
    }

    val serverContext = ConnectionContext.httpsServer(() => {
      val engine = sslContext.createSSLEngine()
      engine.setUseClientMode(false)
      engine.setNeedClientAuth(true)

      handshakeCount.incrementAndGet()

      engine
    })

    Http()
      .newServerAt("localhost", 8080)
      .enableHttps(serverContext)
      .bind(route)
  }

  println("Starting")

  Await.result(startServer(), 1.minute)

  println(s"Server is up, testing for $RunTime")

  val clientContext = ConnectionContext.httpsClient((host, port) => {
    val engine = new FaultySSLEngine(sslContext.createSSLEngine(host, port), 32)
    engine.setUseClientMode(true)

    engine.setSSLParameters({
      val params = engine.getSSLParameters
      params.setEndpointIdentificationAlgorithm("https")
      params
    })

    engine
  })

  {
    implicit val clientSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "client")
    implicit val executionContext: ExecutionContext = clientSystem.executionContext

    val (killSwitch, done) = Source.repeat(()).mapAsync(Parallelism) { _ =>
      Http().singleRequest(HttpRequest(uri = "https://localhost:8080/"), connectionContext = clientContext).map { r =>
        r.discardEntityBytes()
        1
      }.recover { case e =>
        println(e.toString)
        0
      }
    }.viaMat(KillSwitches.single)(Keep.right)
      .toMat(Sink.fold(0)(_ + _))(Keep.both)
      .run()

    clientSystem.scheduler.scheduleOnce(RunTime, () => killSwitch.shutdown())

    val requests = Try(Await.result(done, RunTime + 30.seconds).toString).getOrElse("timedout")

    try {
      Await.result(Http().shutdownAllConnectionPools(), 1.minute)
    } catch {
      case _: TimeoutException =>
        println("Timeout out while shutting HTTP client")
    }

    clientSystem.terminate()
    Await.result(clientSystem.whenTerminated, 1.minute)

    println(s"Completed $requests requests out of ${handshakeCount.get()}; " +
      "now inspect thread dump for stuck threads and terminate with Ctrl-C to exit")
  }
}
