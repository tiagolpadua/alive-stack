package org.prober.task;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.HttpMethod;

import org.prober.monitor.models.Monitor;
import org.prober.monitor.services.MonitorService;
import org.prober.utils.NetworkService;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class TaskService {
    private static final String PROBER_JOB = "ProberJob-";
    private static final String PROBER_TRIGGER = "ProberTrigger-";
    private static final String PROBER_GROUP = "ProberGroup";
    public static final String MONITOR_JOB_KEY = "monitor";

    @Inject
    org.quartz.Scheduler quartz;

    @Inject
    MonitorService monitorService;

    public void scheduleJob(Monitor monitor) throws SchedulerException {
        Log.info(("Scheduling job for: " + monitor.getName()));
        JobDetail job = JobBuilder.newJob(ProberJob.class)
                .withIdentity(PROBER_JOB + monitor.getName().toUpperCase(), PROBER_GROUP)
                .build();

        job.getJobDataMap().put(MONITOR_JOB_KEY, monitor);

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(PROBER_TRIGGER + monitor.getName().toUpperCase(), PROBER_GROUP)
                .startNow()
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInSeconds(monitor.getIntervalSeconds())
                                .repeatForever())
                .build();
        quartz.scheduleJob(job, trigger);
    }

    void onStart(@Observes StartupEvent event) throws SchedulerException {
        monitorService.list().stream().forEach(monitor -> {
            try {
                if (monitor.getActive() == true) {
                    scheduleJob(monitor);
                } else {
                    Log.info("Monitor is not active: " + monitor.getName());
                }
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // A new instance of ProberJob is created by Quartz for every job execution
    public static class ProberJob implements Job {
        private static final String PROBER_METRIC = "prober";

        @Inject
        MeterRegistry registry;

        @Inject
        TaskService taskBean;

        @Inject
        NetworkService networkService;

        public void execute(JobExecutionContext context) throws JobExecutionException {
            var monitor = (Monitor) context.getJobDetail().getJobDataMap().get(MONITOR_JOB_KEY);
            Log.info("Execute Job: " + monitor.getName());

            Builder builder = HttpRequest.newBuilder().timeout(Duration.ofSeconds(monitor.getTimeoutSeconds()));

            switch (monitor.getHttpMethod()) {
                case HttpMethod.GET:
                    builder = builder.GET();
                    break;
                case HttpMethod.HEAD:
                    builder = builder.method(HttpMethod.HEAD, HttpRequest.BodyPublishers.noBody());
                    break;
                default:
                    throw new RuntimeException("Method not supported: " + monitor.getHttpMethod());
            }

            // https://www.baeldung.com/java-9-http-client
            for (var url : monitor.getUrls()) {
                Log.info("Probing:" + monitor.getName() + ":" + url);

                Tags tags = Tags.of("monitor", monitor.getName(), "url", url);

                networkService.getHttpClient()
                        .sendAsync(
                                builder.uri(URI.create(url)).build(),
                                HttpResponse.BodyHandlers.ofString())
                        .whenCompleteAsync((response, error) -> {
                            if (error != null) {
                                registry.gauge(PROBER_METRIC, tags, 1);
                                Log.error(url + ":" + error);
                            } else {
                                if (response.statusCode() < 200 || response.statusCode() >= 400) {
                                    registry.gauge(PROBER_METRIC, tags, 1);
                                    Log.error(response);
                                } else {
                                    registry.gauge(PROBER_METRIC, tags, 0);
                                    Log.info(response);
                                }
                            }
                        });
            }
        }
    }
}
