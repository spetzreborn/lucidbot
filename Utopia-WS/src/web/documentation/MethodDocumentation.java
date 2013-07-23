package web.documentation;

import java.util.LinkedList;
import java.util.List;

public class MethodDocumentation {
    private final String relativePath;
    private final String httpMethod;
    private final String description;
    private final List<ParameterDocumentation> parameterDocumentations;

    public MethodDocumentation(final String relativePath,
                               final String httpMethod,
                               final String description,
                               final List<ParameterDocumentation> parameterDocumentations) {
        this.relativePath = relativePath;
        this.httpMethod = httpMethod;
        this.description = description;
        this.parameterDocumentations = parameterDocumentations;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getDescription() {
        return description;
    }

    public List<ParameterDocumentation> getParameterDocumentations() {
        return parameterDocumentations;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodDocumentation that = (MethodDocumentation) o;

        if (!description.equals(that.description)) return false;
        if (!httpMethod.equals(that.httpMethod)) return false;
        if (!parameterDocumentations.equals(that.parameterDocumentations)) return false;
        if (!relativePath.equals(that.relativePath)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = relativePath.hashCode();
        result = 31 * result + httpMethod.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + parameterDocumentations.hashCode();
        return result;
    }

    public static class MethodDocumentationBuilder {
        private String relativePath;
        private String httpMethod;
        private String description;
        private List<ParameterDocumentation> parameterDocumentations = new LinkedList<>();

        public MethodDocumentationBuilder setRelativePath(final String relativePath) {
            this.relativePath = relativePath;
            return this;
        }

        public MethodDocumentationBuilder setHttpMethod(final String httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public MethodDocumentationBuilder setDescription(final String description) {
            this.description = description;
            return this;
        }

        public MethodDocumentationBuilder setParameterDocumentation(final List<ParameterDocumentation> parameterDocumentations) {
            this.parameterDocumentations = parameterDocumentations;
            return this;
        }

        public MethodDocumentation build() {
            return new MethodDocumentation(relativePath, httpMethod, description, parameterDocumentations);
        }
    }
}
