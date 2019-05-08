package com.zzq.kafka_es;

import com.zzq.kafka_es.bean.Person;
import com.zzq.kafka_es.elasticsearch.ElasticOperationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        ElasticOperationService elasticOperationService = new ElasticOperationService();
        elasticOperationService.initBulkProcessor();
        Person person= null;
       /* for (int i=0;i<10;i++){
            person= new Person();
            person.setAge(18+i);
            person.setId(i+1);
            person.setName(i+"name"+i);
            elasticOperationService.addDocument("person","info",person);
        }*/
        Map<String,Object> para = new HashMap<String, Object>();
        para.put("name","3name3");
        List<Person> personList= elasticOperationService.queryDocumentByParam("person","info",para,Person.class);
        personList.forEach(t-> System.out.println(t));
    }
}
