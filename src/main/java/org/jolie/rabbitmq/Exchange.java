package org.jolie.rabbitmq;

import java.util.HashMap;
import java.util.Map;

public class Exchange {


    public enum ExchangeType {
        TOPIC("topic"),
        FANOUT("fanout"),
        DIRECT("direct");

        private String value;
        private static final Map<String, ExchangeType> lookup = new HashMap<String, ExchangeType>();

        static {
            for (ExchangeType e : ExchangeType.values()) {
                lookup.put(e.getValue(), e);
            }
        }

        ExchangeType(String value) {
            this.value = value;

        }

        public String getValue() {
            return this.value;
        }

        public void setValue( String value ) {
            this.value = value;
        }

        public static ExchangeType get(String value) {
            return lookup.get(value);
        }

    }

    private String name;
    private FormatValues format;
    private ExchangeType exchangeType;
    private boolean durable;


    public Exchange(String name) {
        this.name = name;
        this.format = FormatValues.VALUE;
        this.exchangeType = ExchangeType.DIRECT;
        this.durable = false;
    }

    public String getName() {
        return name;
    }

    public FormatValues getFormat() {
        return this.format;
    }

    public void setFormat( FormatValues format ) {
        this.format = format;
    }

    public ExchangeType getExchangeType() {
        return this.exchangeType;
    }

    public void setExchangeType( ExchangeType exchangeType ) {
        this.exchangeType = exchangeType;
    }

    public boolean getDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }



}
