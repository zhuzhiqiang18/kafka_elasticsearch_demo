package com.zzq.kafka_es.elasticsearch;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ElasticOperationService {

    private final Logger logger = LoggerFactory.getLogger(ElasticOperationService.class);

    private Client client=null;

    private BulkProcessor bulkProcessor;


    public void initBulkProcessor() {

        client=TransportClientApi.transportClient.getTransportClient();
        bulkProcessor = BulkProcessor.builder(client, new BulkProcessor.Listener() {

            @Override
            public void beforeBulk(long executionId, BulkRequest request) {
                logger.info("序号：{} 开始执行{} 条记录保存",executionId,request.numberOfActions());
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                logger.error(String.format("序号：%s 执行失败; 总记录数：%s",executionId,request.numberOfActions()),failure);
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                logger.info("序号：{} 执行{}条记录保存成功,耗时：{}毫秒,",executionId,request.numberOfActions(),response.getTookInMillis());
            }
        }).setBulkActions(1000)
                .setBulkSize(new ByteSizeValue(10, ByteSizeUnit.MB))
                .setConcurrentRequests(4)
                .setFlushInterval(TimeValue.timeValueSeconds(5))
                .setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(500),3))  //失败后等待多久及重试次数
                .build();
    }


    public void closeBulk() {
        if(bulkProcessor != null) {
            try {
                bulkProcessor.close();
            }catch (Exception e) {

            }
        }
    }


    /**
     * 批量添加,性能最好
     *
     */
    public void addDocumentToBulkProcessor(String indices, String type, Object object) {
        bulkProcessor.add(client.prepareIndex(indices, type).setSource(JsonUtils.obj2Str(object)).request());
    }


    public void addDocument(String indices, String type, Object object) {
        IndexResponse resp = client.prepareIndex(indices, type).setSource(JsonUtils.obj2Str(object)).get();
        logger.info("添加结果：{}",resp.toString());
    }

    /**
     * 按id删除
     *
     */
    public void deleteDocumentById(String index, String type, String id) {
        // new DeleteByQueryRequest(search);
        DeleteResponse resp = client.prepareDelete(index, type, id).get();
        logger.info("删除结果：{}",resp.toString());
    }

    /**
     * 按条件删除
     *
     */
    public void deleteDocumentByQuery(String index, String type, Map param) {

        //DeleteByQueryRequestBuilder builder = new DeleteByQueryRequestBuilder(client,DeleteByQueryAction.INSTANCE);
        DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE.newRequestBuilder(client);

        //builder.filter(convertParam(param));
        builder.source().setIndices(index).setTypes(type).setQuery(convertParam(param));
        BulkByScrollResponse resp = builder.get();
        logger.info("删除结果：{}",resp.toString());
    }

    /**
     * 按ID更新
     *
     */
    public void updateDocument(String indices, String type,String id,Object object) {
        UpdateResponse resp = client.prepareUpdate(indices, type, id).setDoc(JsonUtils.obj2Str(object)).get();
        logger.info("更新结果：{}",resp.toString());
    }


    /**
     * 按条件更新
     *
     */
    public void updateDocumentByQuery(String indices, String type, Object object,Map param) {
        //UpdateByQueryRequestBuilder builder = new UpdateByQueryRequestBuilder(client,UpdateByQueryAction.INSTANCE);
        UpdateByQueryRequestBuilder builder = UpdateByQueryAction.INSTANCE.newRequestBuilder(client);
        builder.source().setIndices(indices).setTypes(type).setQuery(convertParam(param));
    }


    public <T> List<T> queryDocumentByParam(String indices, String type, Map param, Class<T> clazz) {
        SearchRequestBuilder builder = buildRequest(indices,type);
        builder.setQuery(convertParam(param));
        builder.setFrom(0).setSize(10);
        SearchResponse resp = builder.get();
        return convertResponse(resp,clazz);
    }

    private BoolQueryBuilder convertParam(Map<String,Object> param) {

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        Iterator<Map.Entry<String, Object>> iterator=  param.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String,Object> entry= iterator.next();
            //nested
            if(entry.getValue() instanceof Map){
              //  boolQueryBuilder.must(QueryBuilders.nestedQuery("roles", QueryBuilders.termQuery("roles.name", param.getRoleName()), ScoreMode.None));
            }else {
                boolQueryBuilder.must(QueryBuilders.termQuery(entry.getKey(), entry.getValue()));
            }
        }

        System.out.println(boolQueryBuilder.toString());

        return boolQueryBuilder;
    }


    /**
     * 通用的装换返回结果
     *
     */
    public <T> List<T> convertResponse(SearchResponse response,Class<T> clazz) {
        List<T> list = new ArrayList();
        if(response != null && response.getHits() != null) {
            String result = "";
            T e = null;
           /* Field idField = ReflectionUtils.findField(clazz, "id");
            if (idField != null) {
                ReflectionUtils.makeAccessible(idField);
            }*/
            for(SearchHit hit : response.getHits()) {
                result = hit.getSourceAsString();
                if (StringUtils.hasText(result)) {
                    e = JsonUtils.str2Obj(result, clazz);
                }
                list.add(e);
                /*if (e != null) {
                    if (idField != null) {
                        ReflectionUtils.setField(idField, e, hit.getId());
                    }
                    list.add(e);
                }*/
            }
        }
        return list;
    }

    public SearchRequestBuilder buildRequest(String indices, String type) {
        return client.prepareSearch(indices).setTypes(type);
    }

    /**
     * 不存在就创建索引
     *
     */
    public boolean createIndexIfNotExist(String index, String type) {
        IndicesAdminClient adminClient = client.admin().indices();
        IndicesExistsRequest request = new IndicesExistsRequest(index);
        IndicesExistsResponse response = adminClient.exists(request).actionGet();
        if (!response.isExists()) {
            return createIndex(index, type);
        }
        return true;
    }

    /**
     * 创建索引
     *
     */
    public boolean createIndex(String index, String type) {
        XContentBuilder mappingBuilder;
        try {
            mappingBuilder = this.getMapping(type);
        } catch (Exception e) {
            logger.error(String.format("创建Mapping 异常；index:%s type:%s,", index, type), e);
            return false;
        }
        Settings settings = Settings.builder().put("index.number_of_shards", 2)
                .put("index.number_of_replicas", 1)
                .put("index.refresh_interval", "5s").build();
        IndicesAdminClient adminClient = client.admin().indices();
        CreateIndexRequestBuilder builder = adminClient.prepareCreate(index);
        builder.setSettings(settings);
        CreateIndexResponse response = builder.addMapping(type, mappingBuilder).get();
        logger.info("创建索引：{} 类型:{} 是否成功：{}", index, type, response.isAcknowledged());
        return response.isAcknowledged();
    }

    /***
     * 创建索引的Mapping信息  注意声明的roles为nested类型
     */
    private XContentBuilder getMapping(String type) throws Exception {
        XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().startObject().startObject(type)
                .startObject("_all").field("enabled", false).endObject()
                .startObject("properties")
                .startObject("userName").field("type", "keyword").endObject()
                .startObject("age").field("type", "integer").endObject()
                .startObject("birthday").field("type", "date").endObject()
                .startObject("description").field("type", "text").field("analyzer", "ik_smart").endObject()
                .startObject("roles").field("type", "nested")
                .startObject("properties")
                .startObject("createTime").field("type","date").endObject()
                .startObject("name").field("type","keyword").endObject()
                .startObject("description").field("type","text").field("analyzer", "ik_smart").endObject()
                .endObject()
                .endObject()
                .endObject()
                .endObject().endObject();
        return mappingBuilder;
    }
}
