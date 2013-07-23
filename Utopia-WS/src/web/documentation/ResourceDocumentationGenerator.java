package web.documentation;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import lombok.extern.log4j.Log4j;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static web.documentation.ParameterDocumentation.ParameterDocumentationBuilder;

@Log4j
public class ResourceDocumentationGenerator {

    public static void main(String[] args) throws IOException {
        List<Class<?>> resourceClasses = new ArrayList<>();
        ImmutableSet<ClassPath.ClassInfo> classInfos = ClassPath.from(Thread.currentThread().getContextClassLoader()).getTopLevelClasses("web.resources");
        for (ClassPath.ClassInfo classInfo : classInfos) {
            Class<?> theClass = classInfo.load();
            if (theClass.isAnnotationPresent(Path.class))
                resourceClasses.add(theClass);
        }

        DocumentationContainer documentationForResources = createDocumentationForResources(resourceClasses);
        System.out.println(documentationForResources.toHtml());
    }

    private static DocumentationContainer createDocumentationForResources(final List<Class<?>> resourceClasses) {
        DocumentationContainer documentationContainer = new DocumentationContainer();
        for (Class<?> resourceClass : resourceClasses) {
            Collection<MethodDocumentation> methodDocumentations = createResourceDocumentation(resourceClass);
            documentationContainer.addResourceDocumentation(resourceClass, methodDocumentations);
        }
        return documentationContainer;
    }

    private static Collection<MethodDocumentation> createResourceDocumentation(final Class<?> resourceClass) {
        Collection<MethodDocumentation> methodDocumentations = new LinkedList<>();

        MethodDocumentation.MethodDocumentationBuilder builder = new MethodDocumentation.MethodDocumentationBuilder();
        for (Method method : resourceClass.getDeclaredMethods()) {
            if (isDocumented(method)) {
                builder.setRelativePath(getPath(resourceClass, method));

                builder.setHttpMethod(getHttpMethod(method));

                builder.setDescription(getDescription(method));

                builder.setParameterDocumentation(getParameterDocumentations(method));

                methodDocumentations.add(builder.build());
            }
        }

        return methodDocumentations;
    }

    private static boolean isDocumented(final Method method) {
        return method.isAnnotationPresent(Documentation.class);
    }

    private static String getPath(final Class<?> resourceClass, final Method method) {
        Path annotation = method.getAnnotation(Path.class);
        String subPath = "";
        if (annotation != null) subPath = annotation.value() + '/';
        return '/' + resourceClass.getAnnotation(Path.class).value() + '/' + subPath;
    }

    private static String getHttpMethod(final Method method) {
        String httpMethod = "?";
        for (Annotation declaredAnnotation : method.getDeclaredAnnotations()) {
            if (declaredAnnotation.annotationType().isAnnotationPresent(HttpMethod.class)) {
                httpMethod = declaredAnnotation.annotationType().getSimpleName();
                break;
            }
        }
        return httpMethod;
    }

    private static String getDescription(final Method method) {
        return method.getAnnotation(Documentation.class).value();
    }

    private static List<ParameterDocumentation> getParameterDocumentations(final Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        List<ParameterDocumentation> parameterDocumentations = new ArrayList<>(parameterTypes.length);

        ParameterDocumentationBuilder builder = new ParameterDocumentationBuilder();
        for (int i = 0; i < parameterTypes.length; i++) {
            builder.setParameterClass(parameterTypes[i]);
            builder.setParameterType(ParameterType.ENTITY); //default assumption since there's no annotation for it like there is for query param
            for (Annotation paramAnnotation : parameterAnnotations[i]) {
                if (paramAnnotation.annotationType().equals(Documentation.class)) {
                    Documentation documentationAnnotation = (Documentation) paramAnnotation;
                    builder.setParameterNameIfEmpty(documentationAnnotation.itemName());
                    builder.setDescription(documentationAnnotation.value());
                } else if (paramAnnotation.annotationType().equals(QueryParam.class)) {
                    QueryParam queryParamAnnotation = (QueryParam) paramAnnotation;
                    builder.setParameterType(ParameterType.QUERY_PARAM);
                    builder.setParameterName(queryParamAnnotation.value());
                }
            }
            if (builder.isComplete()) {
                parameterDocumentations.add(builder.build());
            } else {
                System.err.println("Method '" + method.getName() + "' in class '" + method.getDeclaringClass().getSimpleName() +
                        "' has incomplete documentation");
            }
            builder.clear();
        }

        return parameterDocumentations;
    }

}
