package web.tools;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Type;

@Provider
public class WebContextInjectable extends AbstractHttpContextInjectable<WebContext> implements InjectableProvider<Context, Type> {
    private final WebContextFactory webContextFactory;

    @Inject
    public WebContextInjectable(final WebContextFactory webContextFactory) {
        this.webContextFactory = webContextFactory;
    }

    @Override
    public ComponentScope getScope() {
        return ComponentScope.PerRequest;
    }

    @Override
    public Injectable getInjectable(final ComponentContext ic, final Context context, final Type type) {
        return type.equals(WebContext.class) ? this : null;
    }

    @Override
    public WebContext getValue(final HttpContext c) {
        HttpRequestContext request = c.getRequest();
        if (request instanceof SecurityContext) {
            SecurityContext securityContext = request;
            return webContextFactory.createWebContext(securityContext);
        }
        throw new RuntimeException("Coding error");
    }
}
