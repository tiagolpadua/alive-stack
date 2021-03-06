package org.prober.monitor.services;

import static com.mongodb.client.model.Filters.eq;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;

import org.bson.Document;
import org.prober.job.services.JobService;
import org.prober.monitor.models.Monitor;
import org.prober.utils.ProberException;

import io.quarkus.logging.Log;

@ApplicationScoped
public class MonitorService {
    private static final String MONITORS_COLLECTION = "monitors";
    private static final String PROBER_DB = "prober";

    @Inject
    MongoClient mongoClient;

    @Inject
    JobService jobService;

    public List<Monitor> list() {
        var list = new ArrayList<Monitor>();
        var cursor = getCollection().find().iterator();

        try {
            while (cursor.hasNext()) {
                var document = cursor.next();
                var monitor = new Monitor(document);
                list.add(monitor);
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    // https://mongodb.github.io/mongo-java-driver/3.4/driver/getting-started/quick-start/
    public Optional<Monitor> find(String name) {
        var whereQuery = new BasicDBObject();
        whereQuery.put(Monitor.FIELD_NAME, name);

        var document = getCollection().find(whereQuery).first();

        Monitor monitor = null;

        if (document != null) {
            monitor = new Monitor(document);
        }

        return Optional.ofNullable(monitor);
    }

    public void delete(String name) {
        var res = getCollection().deleteOne(eq(Monitor.FIELD_NAME, name));
        if (res.getDeletedCount() == 0) {
            throw new ProberException("Monitor not found: " + name);
        }

        if (jobService.isJobForMonitorRunning(name)) {
            jobService.unscheduleJob(name);
        }
    }

    public String add(Monitor monitor) {
        // Check all URLs
        monitor.getUrls().stream().filter(url -> !isValidURL(url)).findAny().ifPresent(url -> {
            throw new ProberException("URL is invalid: " + url);
        });

        // Check if name is already exists
        find(monitor.getName()).ifPresent(foundMonitor -> {
            throw new ProberException("Monitor already exists: " + foundMonitor.getName());
        });

        monitor.setId(null);

        var document = monitor.toDocument();
        getCollection().insertOne(document);
        var id = document.getObjectId(Monitor.FIELD_ID);

        if (monitor.getActive() != null && monitor.getActive() == true) {
            jobService.scheduleJob(monitor.getName());
        }

        return id.toString();
    }

    public void update(Monitor monitor) {
        // Check all URLs
        monitor.getUrls().stream().filter(url -> !isValidURL(url)).findAny().ifPresent(url -> {
            throw new ProberException("URL is invalid: " + url);
        });

        var document = monitor.toDocument();

        document.remove(Monitor.FIELD_ID);
        document.remove(Monitor.FIELD_NAME);

        find(monitor.getName())
                .orElseThrow(() -> new ProberException("Monitor not found: " + monitor.getName()));

        var res = getCollection().updateOne(eq(Monitor.FIELD_NAME, monitor.getName()),
                new Document("$set", document));

        if (monitor.getActive() != null && monitor.getActive() == true) {
            if (!jobService.isJobForMonitorRunning(monitor.getName())) {
                Log.info("No job running for: " + monitor.getName());
                jobService.scheduleJob(monitor.getName());
            } else {
                Log.info("Job already running for: " + monitor.getName());
            }
        } else {
            if (jobService.isJobForMonitorRunning(monitor.getName())) {
                Log.info("Job running for: " + monitor.getName());
                jobService.unscheduleJob(monitor.getName());
            } else {
                Log.info("No job running for: " + monitor.getName());
            }
        }

        if (res.getModifiedCount() == 0) {
            throw new ProberException("Monitor not changed: " + monitor.getName());
        }
    }

    private MongoCollection<Document> getCollection() {
        return mongoClient.getDatabase(PROBER_DB).getCollection(MONITORS_COLLECTION);
    }

    private boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (URISyntaxException exception) {
            return false;
        } catch (MalformedURLException exception) {
            return false;
        }
    }

}
