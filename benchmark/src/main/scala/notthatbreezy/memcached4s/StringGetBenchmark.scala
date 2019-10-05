package notthatbreezy.memcached4s.benchmark

import java.net.InetSocketAddress
import java.util.concurrent.{Executors, TimeUnit}

import cats.effect.{ContextShift, IO, _}
import cats.implicits._
import com.google.common.util.concurrent.ThreadFactoryBuilder
import net.spy.memcached.MemcachedClient
import notthatbreezy.memcached4s.benchmark.ScalacacheClient.BacksplashConnectionFactory
import notthatbreezy.memcached4s.client.Client
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import scalacache._
import scalacache.memcached.MemcachedCache
import scalacache.modes.sync._
import scalacache.serialization.circe._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

trait BenchmarkSettings {
  val address: InetSocketAddress = new InetSocketAddress("172.19.0.5", 11211)
}

@State(Scope.Benchmark)
object BenchmarkClient extends BenchmarkSettings {

  val cacheEc: ExecutionContext =
    ExecutionContext.fromExecutor(
      Executors.newCachedThreadPool(
        new ThreadFactoryBuilder().setNameFormat("cache-client-%d").build()
      )
    )
  val blocker: Blocker = Blocker.liftExecutionContext(cacheEc)
  implicit val cs: ContextShift[IO] = IO.contextShift(cacheEc)

  val clientResource: Resource[IO, Client[IO]] =
    Client[IO](address, blocker)
}

@State(Scope.Benchmark)
object MemcachedClient extends BenchmarkSettings {
  lazy val memcached: MemcachedClient =
    new MemcachedClient(new BacksplashConnectionFactory, List(address).asJava)

}

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class StringGetBenchmark extends BenchmarkSettings {

  val clientKey: String = "client"

  val scalacacheKey: String = "scalacache"
  implicit val cs: ContextShift[IO] = BenchmarkClient.cs

  @Setup
  def populateCache() = {

    implicit val mosaicDefinitionCache: MemcachedCache[String] =
      MemcachedCache(MemcachedClient.memcached)

    put(scalacacheKey)("scalacache", None)

    BenchmarkClient.clientResource use { client =>
      client
        .set[String](clientKey, "client")
    } unsafeRunSync ()

  }

  @Benchmark
  @OperationsPerInvocation(100)
  def clientGet(blackhole: Blackhole): Unit = {
    val command = BenchmarkClient.clientResource use { client =>
      (1 to 100).toList.traverse { _ =>
        client
          .get[String](clientKey)
          .map(blackhole.consume)
      }
    }
    command.unsafeRunSync()
  }

  @Benchmark
  @OperationsPerInvocation(100)
  def scalacacheGet(blackhole: Blackhole): Unit = {

    implicit val mosaicDefinitionCache: MemcachedCache[String] =
      MemcachedCache(MemcachedClient.memcached)
    for (_ <- 1 to 100) {
      val result = sync.get[String](scalacacheKey)
      blackhole.consume(result)
    }
    ()
  }
}
