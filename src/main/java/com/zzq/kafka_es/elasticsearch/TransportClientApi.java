package com.zzq.kafka_es.elasticsearch;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class TransportClientApi {
    public static TransportClientApi transportClient = new TransportClientApi();
    private TransportClientApi() {

    }

    public  TransportClient getTransportClient() {
        try {
            Settings esSettings = Settings.builder()
                    .put("cluster.name", "es_cluster")
                    .put("client.transport.sniff", true).build();
            TransportClient client = new PreBuiltTransportClient(esSettings);

            String[] esHosts = "127.0.0.1".trim().split(",");
            for (String host : esHosts) {
                client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host),
                        9300));
            }
            return client;
        }catch (Exception e){
            return null;
        }

    }


}
