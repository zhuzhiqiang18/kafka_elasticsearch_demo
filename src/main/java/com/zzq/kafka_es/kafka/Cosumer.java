package com.zzq.kafka_es.kafka;

import com.zzq.kafka_es.bean.Person;
import com.zzq.kafka_es.elasticsearch.ElasticOperationService;
import com.zzq.kafka_es.elasticsearch.JsonUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Arrays;
import java.util.Properties;

public class Cosumer {
    public static void main(String[] args){

        ElasticOperationService elasticOperationService = new ElasticOperationService();
        elasticOperationService.initBulkProcessor();


        Properties properties = new Properties();
        properties.put("bootstrap.servers", "127.0.0.1:9092");
        properties.put("group.id", "group-1");
        properties.put("enable.auto.commit", "false");
        properties.put("auto.commit.interval.ms", "1000");
        properties.put("auto.offset.reset", "earliest");
        properties.put("session.timeout.ms", "30000");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties);
        kafkaConsumer.subscribe(Arrays.asList("HelloWorld"));


        while (true) {
            ConsumerRecords<String, String> records = kafkaConsumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                System.out.printf("offset = %d, value = %s", record.offset(), JsonUtils.str2Obj(record.value(), Person.class));
                System.out.println();
                elasticOperationService.addDocument("person","info",JsonUtils.str2Obj(record.value(), Person.class));
                kafkaConsumer.commitSync();
            }
        }

    }
}
