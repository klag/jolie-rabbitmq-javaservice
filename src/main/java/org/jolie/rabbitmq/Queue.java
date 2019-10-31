package org.jolie.rabbitmq;

import java.util.ArrayList;
import java.util.HashMap;

public class Queue {

    public class Binding {
        private Exchange exchange;
        private String routingKey;

        public Binding( Exchange exchange, String routingKey ) {
            this.exchange = exchange;
            this.routingKey = routingKey;
        }

        public Exchange getExchange() {
            return this.exchange;
        }

        public String getRoutingKey() {
            return this.routingKey;
        }
    }


    private String name;
    private boolean durable;
    private boolean exclusive;
    private boolean autodelete;
    private HashMap<String, Binding> bindings = new HashMap<>();

    public Queue( String name ) {
        this.name = name;

        this.durable = false;
        this.autodelete = false;
        this.exclusive = false;

    }

    public String getName() {
        return this.name;
    }


    public void setDurable( boolean durable ) {
        this.durable = durable;
    }

    public boolean getDurable() {
        return this.durable;
    }

    public boolean getExclusive() {
        return this.exclusive;
    }

    public boolean getAutodelete() {
        return this.autodelete;
    }

    public void setExclusive( boolean e ) {
        this.exclusive = e;
    }

    public void setAutodelete( boolean a ) {
        this.autodelete = a;
    }

    public void addBinding( Binding binding ) {
        bindings.put( binding.getExchange().getName(), binding );
    }

    public Binding getBinding(String exchangeName ) {
        return bindings.get(exchangeName);
    }

    public HashMap<String,Binding> getBindings() {
        return this.bindings;
    }
}
