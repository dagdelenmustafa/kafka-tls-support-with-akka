package mdagdelen

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
object KafkaProducerWithoutSSL {
  lazy private val config = ConfigFactory.load()

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("system")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = system.dispatcher

    val done: Future[Done] = source.runWith(sink(system))

    done.onComplete(_ => system.terminate())
  }

  private def source: Source[ProducerRecord[String, String], NotUsed] = {
    val topic = config.getString("kafka.topic")

    Source(1 to 100000)
      .map(_.toString)
      .map { i =>
        Thread.sleep(500)
        new ProducerRecord[String, String](topic, s"[Secured Message: false], [message_id: ${UUID.randomUUID()}], [message: $i]")
      }
  }

  private def sink(system: ActorSystem): Sink[ProducerRecord[String, String], Future[Done]] = {
    val bootstrapServers = config.getString("kafka.bootstrapServersWithoutSSL")

    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers)

    Producer.plainSink(producerSettings)
  }
}
