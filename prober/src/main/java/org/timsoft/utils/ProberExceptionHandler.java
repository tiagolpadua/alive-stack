package org.timsoft.utils;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ProberExceptionHandler implements ExceptionMapper<ProberException> {
    @Override
    public Response toResponse(ProberException exception) {
        return Response.status(Status.BAD_REQUEST).entity(new ProberError(exception.getMessage())).build();
    }
}
