package org.timsoft.monitor.services;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import org.bson.Document;
import org.timsoft.monitor.models.Monitor;

@ApplicationScoped
public class MonitorService {

    @Inject
    MongoClient mongoClient;

    public List<Monitor> list() {
        List<Monitor> list = new ArrayList<>();
        MongoCursor<Document> cursor = getCollection().find().iterator();

        try {
            while (cursor.hasNext()) {
                Document document = cursor.next();
                Monitor monitor = new Monitor();
                monitor.setName(document.getString("name"));
                monitor.setIntervalSeconds(document.getLong("intervalSeconds"));
                monitor.setTimeoutSeconds(document.getLong("timeoutSeconds"));

                // private List<String> urls;
                // private HttpMethod httpMethod;

                list.add(monitor);
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    public void add(Monitor monitor) {
        Document document = new Document()
                .append("name", monitor.getName())
                .append("intervalSeconds", monitor.getIntervalSeconds())
                .append("timeoutSeconds", monitor.getTimeoutSeconds());

        getCollection().insertOne(document);
    }

    private MongoCollection getCollection() {
        return mongoClient.getDatabase("monitor").getCollection("monitor");
    }

}
