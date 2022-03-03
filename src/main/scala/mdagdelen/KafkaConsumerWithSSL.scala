package mdagdelen

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.{ExecutionContext, Future}


object KafkaConsumerWithSSL {
  lazy private val config = ConfigFactory.load()

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("system")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = system.dispatcher

    val control = source.toMat(Sink.ignore)(Keep.both)
      .mapMaterializedValue(r => DrainingControl.apply(r))

    control.run()
  }

  private def source(implicit executionContext: ExecutionContext, system: ActorSystem): Source[Done, Consumer.Control] = {
    val bootstrapServers = config.getString("kafka.bootstrapServersWithSSL")
    val topic = config.getString("kafka.topic")
    val groupId = config.getString("kafka.groupId")

    val consumerSettings =
      ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
        .withBootstrapServers(bootstrapServers)
        .withGroupId(groupId)
        .withProperties(
          Map(
            "security.protocol" -> "SSL",
            "ssl.truststore.location" -> config.getString("ssl.truststore.consumer.location"),
            "ssl.truststore.password" -> config.getString("ssl.truststore.consumer.password"),
            "ssl.keystore.location" -> config.getString("ssl.keystore.consumer.location"),
            "ssl.keystore.password" -> config.getString("ssl.keystore.consumer.password"),
            "ssl.key.password" -> config.getString("ssl.key.password")
          )
        )
        /*
        AUTO_OFFSET_RESET_CONFIG -> Values
          earliest: automatically reset the offset to the smallest offset
          latest: automatically reset the offset to the largest offset
          disable: throw exception to the consumer if no previous offset is found for the consumer's group
          anything else: throw exception to the consumer.
         */
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    Consumer
      .committableSource(consumerSettings, Subscriptions.topics(topic))
      .mapAsync(10) { msg =>
        println(s"[Message Key: ${msg.record.key}], [Message Value: ${msg.record.value}]")
        Future(msg.committableOffset)
      }
      .via(Committer.flow(CommitterSettings(system)))
  }

}
