package notthatbreezy.memcached4s.example

import java.util.concurrent.Executors

import cats.effect._
import pureconfig.generic.auto._
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import notthatbreezy.memcached4s.client.Client
import pureconfig.ConfigSource

object App extends IOApp {

  // Testing out encoding/decoding
  case class Example(data: String)
  implicit val fooDecoder: Decoder[Example] = deriveDecoder[Example]
  implicit val fooEncoder: Encoder[Example] = deriveEncoder[Example]

  override def run(args: List[String]): IO[ExitCode] = {

    val clientResource = for {
      blocker <- Blocker[IO]
      memcachedConfig <- Resource.liftF(
        ConfigSource.default.load[MemcachedConfig] match {
          case Left(e)  => IO.raiseError(throw new Exception(e.toString))
          case Right(v) => IO.pure(v)
        }
      )
      client <- Client[IO](memcachedConfig.address, blocker)
    } yield client

    clientResource
      .use { client =>
        client.caching("cool") {
          IO.pure(Example("cat"))
        }
      }
      .as(ExitCode.Success)
  }
}
