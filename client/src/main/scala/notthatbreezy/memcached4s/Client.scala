package notthatbreezy.memcached4s.client

import java.net.InetSocketAddress
import java.util.concurrent.Executors

import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import com.colisweb.tracing.LoggingTracingContext
import com.colisweb.tracing.implicits._
import com.google.common.util.concurrent.ThreadFactoryBuilder
import fs2.io.tcp._
import fs2.Stream
import fs2.{Chunk, text}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class Client[F[_]: Concurrent](client: Socket[F])(
  implicit cs: ContextShift[F]
) {
  def set[T: Encoder](key: String, value: T): F[Unit] = {
    val valueJson = value.asJson.noSpaces
    val command =
      s"set $key 0 0 ${valueJson.getBytes.length}\r\n$valueJson\r\n"
    for {
      logger <- Slf4jLogger.create[F]
      _ <- logger.debug(s"Writing $key")
      _ <- client.write(Chunk.bytes(command.getBytes))
    } yield ()
  }

  def get[T: Decoder](key: String): F[Option[T]] = {

    val getString: Array[Byte] = s"get $key\r\n".getBytes
    for {
      logger <- Slf4jLogger.create[F]
      _ <- logger.debug("Before requesting response")
      _ <- client.write(Chunk.bytes(getString), Some(10 seconds))
      response <- client
        .reads(256 * 1024, Some(10 seconds))
        .through(text.utf8Decode)
        .through(text.lines)
        .takeThrough(_ != "END")
        .compile
        .toVector
      _ <- logger.debug("After Response")
    } yield {
      response.lift(1) match {
        case Some(v) => {
          parse(v) match {
            case Left(e) => {
              throw e
            }
            case Right(s) => {
              s.as[T] match {
                case Left(e)  => throw e
                case Right(t) => Some(t)
              }
            }
          }
        }
        case _ => None
      }
    }
  }

  def caching[T: Encoder: Decoder](key: String)(f: => F[T]): F[T] = {
    get[T](key) flatMap {
      case Some(t) => t.pure[F]
      case _ =>
        for {
          value <- f
          _ <- set[T](key, value)
        } yield value
    }
  }
}

object Client {
  def apply[F[_]: Concurrent](address: InetSocketAddress, blocker: Blocker)(
    implicit cs: ContextShift[F]
  ): Resource[F, Client[F]] = {
    for {
      socketGroup <- SocketGroup[F](blocker)
      socket <- socketGroup.client[F](address)
    } yield new Client[F](socket)
  }
}
