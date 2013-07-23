package web.documentation;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.hp.gagawa.java.elements.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

public class DocumentationContainer {
    private final Multimap<Class<?>, MethodDocumentation> resourceDocumentations = TreeMultimap.create(
            new Comparator<Class<?>>() {
                @Override
                public int compare(final Class<?> o1,
                                   final Class<?> o2) {
                    return o1.getSimpleName().compareTo(o2.getSimpleName());
                }
            }, new Comparator<MethodDocumentation>() {
                @Override
                public int compare(final MethodDocumentation o1,
                                   final MethodDocumentation o2) {
                    if (o1.getRelativePath().equals(o2.getRelativePath())) return o1.getHttpMethod().compareTo(o2.getHttpMethod());
                    return o1.getRelativePath().compareTo(o2.getRelativePath());
                }
            }
    );

    public void addResourceDocumentation(final Class<?> resourceClass, final Collection<MethodDocumentation> methodDocumentation) {
        resourceDocumentations.putAll(resourceClass, methodDocumentation);
    }

    public String toHtml() {
        Html html = new Html();
        Head head = new Head();
        Title title = new Title();
        title.appendText("LucidBot WebService reference");
        head.appendChild(title);
        Style style = new Style("text/css");
        style.appendText("h2 {margin: 1em 0 0.5em 0;font-weight: 600;font-family: 'Titillium Web', sans-serif;position: relative;text-shadow: 0 -1px 1px rgba(0,0,0,0.4);font-size: 22px;line-height: 40px;color: #355681;text-transform: uppercase;border-bottom: 1px solid rgba(53,86,129, 0.3);} .datagrid table { border-collapse: collapse; text-align: left; width: 100%; } .datagrid {font: normal 12px/150% Arial, Helvetica, sans-serif; background: #fff; overflow: hidden; border: 1px solid #006699; -webkit-border-radius: 3px; -moz-border-radius: 3px; border-radius: 3px; }.datagrid table td, .datagrid table th { padding: 3px 10px; }.datagrid table thead th {background:-webkit-gradient( linear, left top, left bottom, color-stop(0.05, #006699), color-stop(1, #00557F) );background:-moz-linear-gradient( center top, #006699 5%, #00557F 100% );filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#006699', endColorstr='#00557F');background-color:#006699; color:#ffffff; font-size: 15px; font-weight: bold; border-left: 1px solid #0070A8; } .datagrid table thead th:first-child { border: none; }.datagrid table tbody td { color: #00496B; border-left: 1px solid #E1EEF4;font-size: 12px;font-weight: normal; }.datagrid table tbody .alt td { background: #E1EEF4; color: #00496B; }.datagrid table tbody td:first-child { border-left: none; }.datagrid table tbody tr:last-child td { border-bottom: none; }.datagrid table tfoot td div { border-top: 1px solid #006699;background: #E1EEF4;} .datagrid table tfoot td { padding: 0; font-size: 12px } .datagrid table tfoot td div{ padding: 2px; }.datagrid table tfoot td ul { margin: 0; padding:0; list-style: none; text-align: right; }.datagrid table tfoot  li { display: inline; }.datagrid table tfoot li a { text-decoration: none; display: inline-block;  padding: 2px 8px; margin: 1px;color: #FFFFFF;border: 1px solid #006699;-webkit-border-radius: 3px; -moz-border-radius: 3px; border-radius: 3px; background:-webkit-gradient( linear, left top, left bottom, color-stop(0.05, #006699), color-stop(1, #00557F) );background:-moz-linear-gradient( center top, #006699 5%, #00557F 100% );filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#006699', endColorstr='#00557F');background-color:#006699; }.datagrid table tfoot ul.active, .datagrid table tfoot ul a:hover { text-decoration: none;border-color: #006699; color: #FFFFFF; background: none; background-color:#00557F;}");
        head.appendChild(style);
        html.appendChild(head);

        Body body = new Body();

        for (Map.Entry<Class<?>, Collection<MethodDocumentation>> entry : resourceDocumentations.asMap().entrySet()) {
            H2 h2 = new H2();
            h2.appendText(entry.getKey().getSimpleName());

            Div div = new Div();
            div.setCSSClass("datagrid");

            Table docTable = new Table();
            docTable.setWidth("100%");

            Thead headerRow = new Thead();

            Th pathHeader = new Th();
            pathHeader.appendText("Relative path");
            pathHeader.setWidth("17%");

            Th methodHeader = new Th();
            methodHeader.appendText("HTTP method");
            methodHeader.setWidth("9%");

            Th descriptionHeader = new Th();
            descriptionHeader.appendText("Description");
            descriptionHeader.setWidth("38%");

            Th paramsHeader = new Th();
            paramsHeader.appendText("Parameters/In-Data");
            paramsHeader.setWidth("36%");

            headerRow.appendChild(pathHeader);
            headerRow.appendChild(methodHeader);
            headerRow.appendChild(descriptionHeader);
            headerRow.appendChild(paramsHeader);

            Tbody tableBody = new Tbody();
            int counter = 0;
            for (MethodDocumentation methodDocumentation : entry.getValue()) {
                Tr row = new Tr();
                if (counter % 2 == 1) row.setCSSClass("alt");

                Td path = new Td();
                path.appendText(methodDocumentation.getRelativePath());

                Td method = new Td();
                method.appendText(methodDocumentation.getHttpMethod());

                Td description = new Td();
                description.appendText(methodDocumentation.getDescription());

                Td params = new Td();
                Ul paramList = new Ul();
                for (ParameterDocumentation doc : methodDocumentation.getParameterDocumentations()) {
                    Li item = new Li();
                    item.appendText('[' + doc.getParameterType().prettyName() + "] " + doc.getParameterName() + " (" + doc.getParameterClass().getSimpleName() + ") - " +
                            doc.getDescription()); //TODO do this better than just a String
                    paramList.appendChild(item);
                }
                params.appendChild(paramList);

                row.appendChild(path);
                row.appendChild(method);
                row.appendChild(description);
                row.appendChild(params);

                tableBody.appendChild(row);
                ++counter;
            }

            docTable.appendChild(headerRow);
            docTable.appendChild(tableBody);
            body.appendChild(h2);
            div.appendChild(docTable);
            body.appendChild(div);
        }

        html.appendChild(body);

        return html.write();
    }
}
