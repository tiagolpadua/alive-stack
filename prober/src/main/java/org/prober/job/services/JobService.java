package org.prober.job.services;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.HttpMethod;

import org.prober.job.models.URLStatus;
import org.prober.monitor.models.Monitor;
import org.prober.monitor.services.MonitorService;
import org.prober.utils.NetworkService;
import org.prober.utils.ProberException;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class JobService {
    private static final String PROBER_JOB = "ProberJob-";
    private static final String PROBER_TRIGGER = "ProberTrigger-";
    private static final String PROBER_GROUP = "ProberGroup";
    public static final String MONITOR_JOB_KEY = "monitor";
    private static final String PROBER_METRIC = "prober";

    @Inject
    org.quartz.Scheduler quartz;

    @Inject
    MonitorService monitorService;

    @Inject
    MeterRegistry registry;

    void onStart(@Observes StartupEvent event) throws SchedulerException {
        monitorService.list().stream().forEach(monitor -> {
            if (monitor.getActive() == true) {
                scheduleJob(monitor);
            } else {
                Log.info("Monitor is not active: " + monitor.getName());
            }
        });
    }

    public void unscheduleAllJobs() {
        Log.info("unscheduleAllJobs");

        GroupMatcher<JobKey> fullmatcher = GroupMatcher.anyGroup();

        try {
            quartz.getJobKeys(fullmatcher)
                    .stream()
                    .map(jobKey -> {
                        try {
                            return quartz.getTriggersOfJob(jobKey);
                        } catch (SchedulerException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .flatMap(List::stream)
                    .forEach(trigger -> {
                        try {
                            quartz.unscheduleJob(trigger.getKey());
                        } catch (SchedulerException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

    }

    public void unscheduleJob(String monitorName) {
        Log.info("unscheduleJob: " + monitorName);

        getJobDetailForMonitor(monitorName)
                .orElseThrow(() -> new ProberException("Monitor not found: " + monitorName));

        try {
            var res = quartz.unscheduleJob(getTriggerKeyForMonitorName(monitorName));
            if (!res) {
                throw new RuntimeException("Fail to unschedule job: " + monitorName);
            }
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isJobForMonitorRunning(String monitorName) {
        return getJobDetailForMonitor(monitorName).isPresent();
    }

    private TriggerKey getTriggerKeyForMonitorName(String monitorName) {
        return new TriggerKey(PROBER_TRIGGER + monitorName, PROBER_GROUP);
    }

    private JobKey getJobKeyForMonitorName(String monitorName) {
        return new JobKey(PROBER_JOB + monitorName, PROBER_GROUP);
    }

    private Optional<JobDetail> getJobDetailForMonitor(String monitorName) {
        try {
            return Optional.ofNullable(quartz.getJobDetail(getJobKeyForMonitorName(monitorName)));
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    public void scheduleJob(String monitorName) {
        Log.info("scheduleJob: " + monitorName);
        getJobDetailForMonitor(monitorName).ifPresent(s -> {
            throw new ProberException("Monitor already has a job: " + monitorName);
        });

        var monitor = monitorService.find(monitorName)
                .orElseThrow(() -> new ProberException("Monitor not found: " + monitorName));

        if (monitor.getActive()) {
            scheduleJob(monitor);
        } else {
            throw new ProberException("Cant schedule inactive monitor: " + monitorName);
        }
    }

    private void scheduleJob(Monitor monitor) {
        JobDetail job = JobBuilder.newJob(ProberJob.class)
                .withIdentity(getJobKeyForMonitorName(monitor.getName()))
                .build();

        var mapURLStatus = new HashMap<String, URLStatus>();

        job.getJobDataMap().put(MONITOR_JOB_KEY, monitor);
        job.getJobDataMap().put(MONITOR_JOB_KEY + ":MAP", mapURLStatus);

        monitor.getUrls().stream().forEach(url -> {
            Tags tags = Tags.of("monitor", monitor.getName(), "url", url);
            var urlStatus = new URLStatus();
            urlStatus.setValue(1);
            mapURLStatus.put(monitor.getName() + ":" + url, urlStatus);
            registry.gauge(PROBER_METRIC, tags, urlStatus, URLStatus::getValue);
        });

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(getTriggerKeyForMonitorName(monitor.getName()))
                .startNow()
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInSeconds(monitor.getIntervalSeconds())
                                .repeatForever())
                .build();
        try {
            quartz.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    // A new instance of ProberJob is created by Quartz for every job execution
    public static class ProberJob implements Job {
        @Inject
        MeterRegistry registry;

        @Inject
        JobService taskBean;

        @Inject
        NetworkService networkService;

        @SuppressWarnings("unchecked")
        public void execute(JobExecutionContext context) throws JobExecutionException {
            var monitor = (Monitor) context.getJobDetail().getJobDataMap().get(MONITOR_JOB_KEY);
            var mapURLStatus = (Map<String, URLStatus>) context.getJobDetail().getJobDataMap()
                    .get(MONITOR_JOB_KEY + ":MAP");

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

                var urlStatus = mapURLStatus.get(monitor.getName() + ":" + url);

                networkService.getHttpClient()
                        .sendAsync(
                                builder.uri(URI.create(url)).build(),
                                HttpResponse.BodyHandlers.ofString())
                        .whenCompleteAsync((response, error) -> {

                            if (error != null) {
                                urlStatus.setValue(1);
                                Log.error(url + ":" + error);
                            } else {
                                if (response.statusCode() < 200 || response.statusCode() >= 400) {
                                    urlStatus.setValue(1);
                                    Log.error(response);
                                } else {
                                    urlStatus.setValue(0);
                                    Log.info(response);
                                }
                            }
                        });
            }
        }
    }
}
