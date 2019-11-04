package org.jolie.rabbitmq;


import java.util.HashMap;
import java.util.Map;

public enum FormatValues {
    VALUE("Value"),
    JSON("json");

    private String value;
    private static final Map<String, FormatValues> lookup = new HashMap<String, FormatValues>();

    static {
        for (FormatValues f : FormatValues.values()) {
            lookup.put(f.getValue(), f);
        }
    }

    FormatValues(String v) {
        setValue(v);
    }

    public void setValue(String v) {
        this.value = v;
    }

    public String getValue() {
        return value;
    }

    public static FormatValues get(String value) {
        return lookup.get(value);
    }
}