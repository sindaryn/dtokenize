package org.sindaryn.dtokenize.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.lang.reflect.Method;
import java.util.Map;

@Data
@NoArgsConstructor
public class Token {
    @NonNull
    private Boolean isStaticExpression;
    private String expression;
    public void setMappedValue(Map<String, String> resultsMap, String value){
        resultsMap.put(expression, value);
    }
}
