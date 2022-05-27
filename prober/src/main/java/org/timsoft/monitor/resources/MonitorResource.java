package org.timsoft.monitor.resources;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder;
import org.jboss.resteasy.reactive.RestResponse.StatusCode;
import org.timsoft.monitor.models.Monitor;
import org.timsoft.monitor.services.MonitorService;
import org.timsoft.utils.EntityNotFoundException;

@Path("/monitors")
public class MonitorResource {

    @Inject
    MonitorService monitorService;

    @GET
    public List<Monitor> list() {
        return monitorService.list();
    }

    @GET
    @Path("/{name}")
    public Monitor getByName(@PathParam("name") String name) {
        return monitorService.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Monitor not found: " + name));
    }

    @POST
    public RestResponse<Monitor> add(@Valid Monitor monitor) {
        String id = monitorService.add(monitor);
        monitor.setId(id);
        return ResponseBuilder.ok(monitor)
                .status(StatusCode.CREATED)
                .build();
    }
}