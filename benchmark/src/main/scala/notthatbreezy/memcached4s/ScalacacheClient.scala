package notthatbreezy.memcached4s.benchmark

import java.net.InetSocketAddress

import net.spy.memcached.DefaultConnectionFactory

import scala.collection.JavaConverters._

object ScalacacheClient {

  class BacksplashConnectionFactory extends DefaultConnectionFactory()

  val address: InetSocketAddress = new InetSocketAddress("172.19.0.5", 11211)

}
