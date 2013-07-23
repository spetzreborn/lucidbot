package web.documentation;

import static api.tools.text.StringUtil.prettifyEnumName;

public enum ParameterType {
    ENTITY, QUERY_PARAM;

    public String prettyName() {
        return prettifyEnumName(this);
    }
}
