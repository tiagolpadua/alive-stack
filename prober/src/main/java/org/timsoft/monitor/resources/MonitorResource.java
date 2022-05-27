package org.timsoft.monitor.resources;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.timsoft.monitor.models.Monitor;
import org.timsoft.monitor.services.MonitorService;

@Path("/monitors")
public class MonitorResource {

    @Inject
    MonitorService monitorService;

    @GET
    public List<Monitor> list() {
        return monitorService.list();
    }

    @POST
    public Monitor add(Monitor monitor) {
        monitorService.add(monitor);
        return monitor;
    }
}