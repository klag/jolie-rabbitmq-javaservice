package org.jolie.rabbitmq;

import java.util.HashMap;
import java.util.Map;

public class InputQueue extends Queue {

    public enum ResponseApiType {
        PUSH("push"),
        PULL("pull");

        private String value;
        private static final Map<String, InputQueue.ResponseApiType> lookup = new HashMap<String, ResponseApiType>();

        static {
            for (ResponseApiType e : ResponseApiType.values()) {
                lookup.put(e.getValue(), e);
            }
        }

        ResponseApiType(String value) {
            this.value = value;

        }

        public String getValue() {
            return this.value;
        }

        public void setValue( String value ) {
            this.value = value;
        }

        public static ResponseApiType get(String value) {
            return lookup.get(value);
        }

    }

    private FormatValues format = FormatValues.JSON;
    private ResponseApiType responseApiType = ResponseApiType.PUSH;
    private int maxThread = 1;
    private long millisPullRange = 1000;

    public InputQueue(String name) {
        super( name );
    }

    public void setFormat(FormatValues format) {
        this.format = format;
    }

    public FormatValues getFormat() {
        return format;
    }

    public void setResponseApiType( ResponseApiType responseApiType ) {
        this.responseApiType = responseApiType;
    }

    public ResponseApiType getResponseApiType() {
        return responseApiType;
    }

    public void setMaxThread(int maxThread) {
        this.maxThread = maxThread;
    }

    public int getMaxThread() {
        return maxThread;
    }

    public void setMillisPullRange(long millisPullRange) {
        this.millisPullRange = millisPullRange;
    }

    public long getMillisPullRange() {
        return millisPullRange;
    }
}
