package web.tools;

import api.tools.time.DateFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.util.Date;

public class DateParameter {
    private Date date;

    public DateParameter(final String dateAsString) {
        try {
            this.date = DateFactory.getISODateTimeWithTimeZoneFormat().parse(dateAsString);
        } catch (ParseException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    public Date asDate() {
        return date;
    }

    @Override
    public String toString() {
        return date.toString();
    }
}
