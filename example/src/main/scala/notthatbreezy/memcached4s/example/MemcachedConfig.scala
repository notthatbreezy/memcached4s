package notthatbreezy.memcached4s.example

import java.net.InetSocketAddress

case class MemcachedConfig(memcached: Memcached) {
  val address: InetSocketAddress =
    new InetSocketAddress(memcached.host, memcached.port)
}

case class Memcached(host: String, port: Int)
