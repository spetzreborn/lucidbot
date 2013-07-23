package web.documentation;

import static api.tools.text.StringUtil.isNotNullOrEmpty;
import static api.tools.text.StringUtil.isNullOrEmpty;

public class ParameterDocumentation {
    private final ParameterType parameterType;
    private final String parameterName;
    private final Class<?> parameterClass;
    private final String description;

    public ParameterDocumentation(final ParameterType parameterType, final String parameterName, final Class<?> parameterClass, final String description) {
        this.parameterType = parameterType;
        this.parameterName = parameterName;
        this.parameterClass = parameterClass;
        this.description = description;
    }

    public ParameterType getParameterType() {
        return parameterType;
    }

    public String getParameterName() {
        return parameterName;
    }

    public Class<?> getParameterClass() {
        return parameterClass;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParameterDocumentation that = (ParameterDocumentation) o;

        if (!description.equals(that.description)) return false;
        if (!parameterClass.equals(that.parameterClass)) return false;
        if (!parameterName.equals(that.parameterName)) return false;
        if (parameterType != that.parameterType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = parameterType.hashCode();
        result = 31 * result + parameterName.hashCode();
        result = 31 * result + parameterClass.hashCode();
        result = 31 * result + description.hashCode();
        return result;
    }

    public static class ParameterDocumentationBuilder {
        private ParameterType parameterType;
        private String parameterName;
        private Class<?> parameterClass;
        private String description;

        public ParameterDocumentationBuilder setParameterType(final ParameterType parameterType) {
            this.parameterType = parameterType;
            return this;
        }

        public ParameterDocumentationBuilder setParameterNameIfEmpty(final String parameterName) {
            if (isNullOrEmpty(this.parameterName)) this.parameterName = parameterName;
            return this;
        }

        public ParameterDocumentationBuilder setParameterName(final String parameterName) {
            this.parameterName = parameterName;
            return this;
        }

        public ParameterDocumentationBuilder setParameterClass(final Class<?> parameterClass) {
            this.parameterClass = parameterClass;
            return this;
        }

        public ParameterDocumentationBuilder setDescription(final String description) {
            this.description = description;
            return this;
        }

        public void clear() {
            this.parameterType = null;
            this.parameterName = null;
            this.parameterClass = null;
            this.description = null;
        }

        public boolean isComplete() {
            return parameterType != null && isNotNullOrEmpty(parameterName) && parameterClass != null && isNotNullOrEmpty(description);
        }

        public ParameterDocumentation build() {
            return new ParameterDocumentation(parameterType, parameterName, parameterClass, description);
        }
    }
}
