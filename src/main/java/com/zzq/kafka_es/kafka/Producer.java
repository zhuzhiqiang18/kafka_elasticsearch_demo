package com.zzq.kafka_es.kafka;

import com.zzq.kafka_es.bean.Person;
import com.zzq.kafka_es.elasticsearch.JsonUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class Producer {
    public static void main(String[] args){
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "127.0.0.1:9092");
        properties.put("acks", "all");
        properties.put("retries", 0);
        properties.put("batch.size", 16384);
        properties.put("linger.ms", 1);
        properties.put("buffer.memory", 33554432);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        //org.apache.kafka.common.serialization.ByteArraySerializer
        KafkaProducer<String, String> producer = null;
        Person person= null;
        try {
            producer = new KafkaProducer<String, String>(properties);
            for (int i = 0; i < 10; i++) {
                person= new Person();
                person.setAge(18+i);
                person.setId(i+1);
                person.setName(i+"name"+i);
                producer.send(new ProducerRecord<String, String>("HelloWorld", JsonUtils.obj2Str(person)));
                System.out.println("Sent:" + person.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            producer.close();
        }

    }
}
