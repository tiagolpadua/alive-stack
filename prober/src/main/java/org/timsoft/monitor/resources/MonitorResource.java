package org.timsoft.monitor.resources;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
    public Monitor get(@PathParam("name") String name) {
        return monitorService.find(name)
                .orElseThrow(() -> new EntityNotFoundException("Monitor not found: " + name));
    }

    @PUT
    @Path("/{name}")
    public Monitor update(@PathParam("name") String name, @Valid Monitor monitor) {
        monitorService.update(monitor);
        return get(name);
    }

    @DELETE
    @Path("/{name}")
    public RestResponse<Object> delete(@PathParam("name") String name) {
        monitorService.delete(name);
        return ResponseBuilder.ok()
                .status(StatusCode.NO_CONTENT)
                .build();
    }

    @POST
    public Monitor add(@Valid Monitor monitor) {
        monitorService.add(monitor);
        return get(monitor.getName());
    }
}