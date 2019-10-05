# memcached4s

A WIP memcached client built on top of `fs2` with `circe` for value serialization. The
current implementation uses only a single memcached node.

# Features

## Protocols
 - [x] Text
 - [ ] Binary

## Invalidation
 - [ ] Expiration
 - [ ] Tags

## Cluster
 - [x] Single Node
 - [ ] _n_ nodes

## Commands
### Storage
 - [x] `set`
 - [ ] `add`
 - [ ] `replace`
 - [ ] `append`
 - [ ] `prepend`
 - [ ] `cas`

### Retrieval
 - [x] `get`
 - [ ] `gets`

### Other
 - [ ] `delete`
 - [ ] `incr`/`decr`
 - [ ] `flush_all`


### Statistics
 - [ ] `stats`
 - [ ] `stats items`
 - [ ] `stats slabs`
 - [ ] `stats sizes`
 
# Usage

```scala

object App extends IOApp {

  // Circe Encoders/Decoders must be available for serialization
  case class Example(data: String)
  implicit val fooDecoder: Decoder[Example] = deriveDecoder[Example]
  implicit val fooEncoder: Encoder[Example] = deriveEncoder[Example]

  override def run(args: List[String]): IO[ExitCode] = {
    val address: InetSocketAddress = new InetSocketAddress("localhost", 11211)

    val cacheEc: ExecutionContext =
      ExecutionContext.fromExecutor(
        Executors.newCachedThreadPool(
          new ThreadFactoryBuilder().setNameFormat("cache-client-%d").build()
        )
      )

    // Blocker must be passed into client for I/O blocking operations
    val blocker = Blocker.liftExecutionContext(cacheEc)

    val tracer = LoggingTracingContext[IO]() _
    Client[IO](address, blocker) use { client =>
      val cachingResult: IO[Example] = client.caching("cool") {
        IO.pure(Example("cat"))
      } 
      val setResult: IO[Unit] = client.set[String]("abc", "xyz")
      (cachingResult, setResult).tupled.map(_ => ExitCode.Success)
    }
  }
}
```

# Benchmarks

While not a focus of this library _yet_ there is a benchmark module to assess performance against `scalacached` powered
by `net.spy.memcached`. Right now, `get` for `String` is about half as performant:

```
Benchmark                          Mode  Cnt      Score       Error  Units
StringGetBenchmark.clientGet      thrpt    5   5860.759 ±  3363.843  ops/s
StringGetBenchmark.scalacacheGet  thrpt    5  10844.443 ± 11653.085  ops/s
```