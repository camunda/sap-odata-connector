package io.camunda.connector.sap.odata.model.batchType;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Options {
  private String format;
  private String top;
  private String filter;
  private String orderby;
  private String expand;
  private String select;
  private String inlinecount;

  public Map<String, String> asMap() {
    Map<String, String> params = new HashMap<>();
    if (format != null && !format.isEmpty()) params.put("$format", format);
    if (top != null && !top.isEmpty()) params.put("$top", top);
    if (filter != null && !filter.isEmpty()) params.put("$filter", filter);
    if (orderby != null && !orderby.isEmpty()) params.put("$orderby", orderby);
    if (expand != null && !expand.isEmpty()) params.put("$expand", expand);
    if (select != null && !select.isEmpty()) params.put("$select", select);
    if (inlinecount != null && !inlinecount.isEmpty()) params.put("$inlinecount", inlinecount);
    return params;
  }
}
