package web.documentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import static com.google.common.base.Preconditions.checkNotNull;

@Path("/")
public class DocumentationResource {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getWebServiceDocumentation() {
        try {
            return ResourceDocumentationGenerator.generateResourceDocumentation();
        } catch (IOException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Path("docs/{file:.*html}")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getJavaDoc(@PathParam("file") final List<PathSegment> segments) {
        return getFileContent(segments);
    }

    @Path("docs/{file:.*css}")
    @GET
    @Produces("text/css")
    public String getJavaDocCss(@PathParam("file") final List<PathSegment> segments) {
        return getFileContent(segments);
    }

    @Path("docs/{file:.*gif}")
    @GET
    @Produces("image/gif")
    public byte[] getJavaDocGifs(@PathParam("file") final List<PathSegment> segments) {
        try {
            return getFileByteContent(segments);
        } catch (IOException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private String getFileContent(final List<PathSegment> segments) {
        StringBuilder pathBuilder = new StringBuilder(100);
        for (PathSegment segment : segments) {
            pathBuilder.append('/').append(segment.getPath());
        }
        InputStream input = getClass().getResourceAsStream("/docs" + pathBuilder.toString());
        checkNotNull(input, "No such file");
        Scanner scanner = new Scanner(input).useDelimiter("\\A");
        return scanner.next();
    }

    private byte[] getFileByteContent(final List<PathSegment> segments) throws IOException {
        StringBuilder pathBuilder = new StringBuilder(100);
        for (PathSegment segment : segments) {
            pathBuilder.append('/').append(segment.getPath());
        }
        InputStream input = getClass().getResourceAsStream("/docs" + pathBuilder.toString());
        checkNotNull(input, "No such file");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }


}
