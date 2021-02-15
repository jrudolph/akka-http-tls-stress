import java.nio.ByteBuffer
import java.util.concurrent.ThreadLocalRandom
import javax.net.ssl.{SSLEngine, SSLEngineResult, SSLSession}

class FaultySSLEngine(e: SSLEngine, faultFactor: Int) extends SSLEngine {
  override def wrap(byteBuffers: Array[ByteBuffer], i: Int, i1: Int, byteBuffer: ByteBuffer): SSLEngineResult =
    e.wrap(byteBuffers, i, i1, byteBuffer)

  override def unwrap(byteBuffer: ByteBuffer, byteBuffers: Array[ByteBuffer], i: Int, i1: Int): SSLEngineResult = {
    if (ThreadLocalRandom.current().nextInt(faultFactor) == 0) {
      byteBuffer.clear()
    }

    e.unwrap(byteBuffer, byteBuffers, i, i1)
  }

  override def getDelegatedTask: Runnable = e.getDelegatedTask

  override def closeInbound(): Unit = e.closeInbound()

  override def isInboundDone: Boolean = e.isInboundDone

  override def closeOutbound(): Unit = e.closeOutbound()

  override def isOutboundDone: Boolean = e.isOutboundDone

  override def getSupportedCipherSuites: Array[String] = e.getSupportedCipherSuites

  override def getEnabledCipherSuites: Array[String] = e.getEnabledCipherSuites

  override def setEnabledCipherSuites(strings: Array[String]): Unit = e.setEnabledCipherSuites(strings)

  override def getSupportedProtocols: Array[String] = e.getSupportedProtocols

  override def getEnabledProtocols: Array[String] = e.getEnabledProtocols

  override def setEnabledProtocols(strings: Array[String]): Unit = e.setEnabledProtocols(strings)

  override def getSession: SSLSession = e.getSession

  override def beginHandshake(): Unit = e.beginHandshake()

  override def getHandshakeStatus: SSLEngineResult.HandshakeStatus = e.getHandshakeStatus

  override def setUseClientMode(b: Boolean): Unit = e.setUseClientMode(b)

  override def getUseClientMode: Boolean = e.getUseClientMode

  override def setNeedClientAuth(b: Boolean): Unit = e.setNeedClientAuth(b)

  override def getNeedClientAuth: Boolean = e.getNeedClientAuth

  override def setWantClientAuth(b: Boolean): Unit = e.setWantClientAuth(b)

  override def getWantClientAuth: Boolean = e.getWantClientAuth

  override def setEnableSessionCreation(b: Boolean): Unit = e.setEnableSessionCreation(b)

  override def getEnableSessionCreation: Boolean = e.getEnableSessionCreation
}
