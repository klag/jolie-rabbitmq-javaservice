package org.jolie.rabbitmq;

import jolie.js.JsUtils;
import jolie.runtime.Value;
import jolie.runtime.typing.Type;

import java.io.IOException;
import java.io.Serializable;

public class QueueMessage implements Serializable {

    private String id="";
    private Value message;
    private String sessionToken="";

    public QueueMessage(String id,Value message,String sessionToken){
        this.id=id;
        this.message=message;
        this.sessionToken=sessionToken;
    }

    public QueueMessage(){

    }

    public String getId(){
        return id;
    }

    public Value getMessage(){
        return message;
    }

    public void setId(String id){
        this.id=id;
    }

    public void setMessage(Value message){
        this.message=message;
    }

    public String getSessionToken(){
        return sessionToken;
    }

    public void setSessionToken(String sessionToken){
        this.sessionToken=sessionToken;
    }

    public String getJSONMessage() throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        JsUtils.valueToJsonString( message, true, Type.UNDEFINED, stringBuilder );
        return stringBuilder.toString();
    }

}

