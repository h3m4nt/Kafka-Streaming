package com.example.kafka.main

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}

object KafkaSparkConsumerMain {

  def main(args: Array[String]): Unit = {
    val brokers = "localhost:9092"
    val groupID = "KafkaConsumerGroup"
    val topics = "ddosData"

    //creating spark context
    val sparkConf = new SparkConf().setMaster("local[*]").setAppName("kafkaConsumer")
    val ssc = new StreamingContext(sparkConf, Seconds(10))
    val sc = ssc.sparkContext
    sc.setLogLevel("OFF")

    //setting basic kafka parameters
    val topicSet = topics.split(",").toList
    val kafkaParam = Map[String, Object](
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> brokers,
      ConsumerConfig.GROUP_ID_CONFIG -> groupID,
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer],
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer]
    )

    //reading from kafka topic
    val messages = KafkaUtils.createDirectStream[String, String](
      ssc, LocationStrategies.PreferConsistent, ConsumerStrategies.Subscribe[String, String](topicSet, kafkaParam)
    )

    //fetch data from stream, and filter
    val line = messages.map(_.value)
    val words = line.map(x => (x.split("\\|")(0))).map(x=>(x,1)).reduceByKey((_+_)).filter(v=>v._2 > 50)

    //printing IP Address for further use
    words.foreachRDD{
      rdd =>rdd.sortBy(_._2).collect().foreach(println)
    }

    ssc.start()
    ssc.awaitTermination()
  }


}
