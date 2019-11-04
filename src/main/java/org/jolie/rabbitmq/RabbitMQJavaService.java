package org.jolie.rabbitmq;

import com.rabbitmq.client.*;
import jolie.js.JsUtils;
import jolie.net.CommMessage;
import jolie.runtime.AndJarDeps;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.embedding.RequestResponse;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@AndJarDeps( { "jolie-js.jar", "json_simple.jar" } )
public class RabbitMQJavaService extends JavaService {

    private final String receivingOperation = "receiveMessage";
    private Connection connection;
    private ConnectionFactory factory;
    private Channel channel;
    HashMap<String, Exchange> exchanges = new HashMap<String, Exchange>();
    ArrayList<Queue> queues = new ArrayList<Queue>();
    ArrayList<InputQueue> inputQueues = new ArrayList<InputQueue>();
    String apiType  = "push";
    int maxThread = 1;
    long millisPullRange = 1000;



    @RequestResponse
    public Value connect (Value request) throws FaultException {
        try {
            String userName = request.getFirstChild("username").strValue();
            String password = request.getFirstChild("password").strValue();
            String virtualHost = null;
            if ( request.getChildren("virtualhost").size() > 0 ) {
                virtualHost = request.getFirstChild("virtualhost").strValue();
            }
            String hostName = request.getFirstChild("hostname").strValue();
            int portNumber = request.getFirstChild("portnumber").intValue();

            for ( Value exch : request.getChildren("exchange") ) {
                String exchangeName = exch.getFirstChild("name").strValue();
                Exchange exchange = new Exchange(exchangeName);

                exchange.setExchangeType( Exchange.ExchangeType.get(exch.getFirstChild("type").strValue()));
                if (exch.getChildren("format").size() > 0) {
                    exchange.setFormat(FormatValues.get(exch.getFirstChild("format").strValue()));
                }
                exchange.setDurable(exch.getFirstChild("durable").boolValue());
                exchanges.put(exchangeName, exchange);
            }

            factory = new ConnectionFactory();

            factory.setUsername(userName);
            factory.setPassword(password);
            if ( virtualHost != null ) {
                factory.setVirtualHost(virtualHost);
            }
            factory.setHost(hostName);
            factory.setPort(portNumber);

            connection = factory.newConnection();
            channel = connection.createChannel();
            for( Map.Entry<String,Exchange> exch : exchanges.entrySet() ) {
                channel.exchangeDeclare( exch.getKey(), exch.getValue().getExchangeType().getValue(), exch.getValue().getDurable() );
            }


            // creating queues
            for( Value q : request.getChildren("output_queues") ) {
                Queue queue = new Queue( q.getFirstChild("name").strValue() );
                fillQueueParameter(q, queue);
                queues.add(queue);
                declareQueue(queue);
            }

            if ( request.getChildren("input_queues").size() > 0 ) {
                for( Value q : request.getChildren("input_queues") ) {
                    InputQueue inputQueue = new InputQueue(q.getFirstChild("queue").getFirstChild("name").strValue());
                    fillQueueParameter(q.getFirstChild("queue"),inputQueue);
                    if ( q.getChildren("format").size() > 0 ) {
                        inputQueue.setFormat(FormatValues.get( q.getFirstChild("format").strValue()));
                    }
                    if ( q.getChildren("response_api_type").size() > 0 ) {
                        inputQueue.setResponseApiType(InputQueue.ResponseApiType.get(q.getFirstChild("response_api_type").strValue()));
                    }
                    if ( q.getChildren("millis_pull_range").size() > 0 ) {
                        inputQueue.setMillisPullRange( q.getFirstChild("millis_pull_range").longValue() );
                    }
                    if ( q.getChildren("max_thread").size() > 0 ) {
                        inputQueue.setMaxThread(q.getFirstChild("max_thread").intValue());
                    }

                    inputQueues.add(inputQueue);
                    declareQueue(inputQueue);
                }
                QueueListeningThread queueThread = new QueueListeningThread( apiType, maxThread, millisPullRange, getInputQueueNames() );
                queueThread.start();
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            throw new FaultException("IOException", ex.getMessage() );
        } catch (TimeoutException ex) {
            throw new FaultException("TimeoutException", ex.getMessage() );
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new FaultException("ConnectionFault", ex.getMessage() );
        }

        return Value.create();
    }

    public void disconnect(Value request ) {
        try {
            channel.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public void writeOnExchange( Value request )  {
        try {
            String exchangeName = request.getFirstChild("exchange_name").strValue();
            String routingKey = request.getFirstChild("routing_key").strValue();
            QueueMessage message = new QueueMessage();
            message.setMessage( request.getFirstChild("message")  );

            channel.confirmSelect();
            Exchange exchange = exchanges.get(exchangeName);
            String requestString = "";
            switch( exchange.getFormat() ) {
                case VALUE:
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(message);
                    oos.close();
                    requestString = Base64.getEncoder().encodeToString(baos.toByteArray());
                    break;
                case JSON:
                    requestString = message.getJSONMessage();
                    break;
            }
            channel.basicPublish(exchangeName, routingKey,null,requestString.getBytes());
            channel.waitForConfirms( 2000 );
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fillQueueParameter(Value q, Queue queue) {
        if ( q.getFirstChild("durable").boolValue() ) {
            queue.setDurable( q.getFirstChild("durable").boolValue() );
        }
        if ( q.getFirstChild("exclusive").boolValue() ) {
            queue.setExclusive(q.getFirstChild("exclusive").boolValue());
        }
        if ( q.getFirstChild("autodelete").boolValue() ) {
            queue.setAutodelete(q.getFirstChild("autodelete").boolValue());
        }

        for( Value bindingValue : q.getChildren("binding") ) {
            Queue.Binding binding = queue.new Binding( exchanges.get(bindingValue.getFirstChild("exchange_name").strValue()), bindingValue.getFirstChild("routing_key").strValue());
            queue.addBinding(binding);
        }
    }

    private void declareQueue(Queue queue) throws IOException {
        channel.queueDeclare(queue.getName(), queue.getDurable(), queue.getExclusive(), queue.getAutodelete(), null );
        for( Map.Entry<String,Queue.Binding> binding : queue.getBindings().entrySet() ) {
            channel.queueBind( queue.getName(), binding.getValue().getExchange().getName(),  binding.getValue().getRoutingKey() );
        }
    }

    private class QueueListeningThread extends Thread{
        private String apiType="";
        private int maxThread;
        private long pullRangeMillis;
        private ArrayList<String> queues;
        ArrayList<DefaultConsumer> consumers;

        public QueueListeningThread(String apiType,int maxThread,long pullRangeMillis,ArrayList<String> queues){
            this.apiType = apiType;
            this.maxThread = maxThread;
            this.pullRangeMillis = pullRangeMillis;
            this.queues = queues;
        }

        @Override
        public void run(){
            if(this.apiType.equalsIgnoreCase("pull")){
                startPull( this.maxThread, this.pullRangeMillis );
            }else if( this.apiType.equalsIgnoreCase("push" ) ){
                startPush();
            }
        }

        private void startPush(){
            consumers = new ArrayList<DefaultConsumer>();
            for(int i = 0; i < inputQueues.size(); i++ ) {
                try {
                    final String queueName = inputQueues.get(i).getName();
                    final FormatValues messageFormat = inputQueues.get(i).getFormat();
                    DefaultConsumer consumer = new DefaultConsumer( channel ) {
                        @Override
                        public void handleDelivery (String consumerTag, Envelope envelope,
                                                    AMQP.BasicProperties properties, byte[] body) throws UnsupportedEncodingException
                        {
                            Value responseValue = Value.create();

                            switch (messageFormat) {
                                case JSON:
                                    try {
                                        JsUtils.parseJsonIntoValue(new StringReader( new String(body) ), responseValue, true );
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                case VALUE:
                                    QueueMessage response = null;
                                    byte [] data = Base64.getDecoder().decode(new String(body));
                                    try{
                                        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
                                        response = (QueueMessage) ois.readObject();
                                        ois.close();
                                        responseValue = response.getMessage();
                                    } catch(IOException e){
                                        e.printStackTrace();
                                    } catch (ClassNotFoundException ex) {
                                        ex.printStackTrace();
                                    }
                                    break;
                            }

                            Value operationCallback = Value.create();
                            operationCallback.getFirstChild("message").deepCopy( responseValue ) ;
                            operationCallback.getFirstChild("queue_name").setValue( queueName );
                            CommMessage request=CommMessage.createRequest(receivingOperation,"/",operationCallback);
                            sendMessage(request);
                        }
                    };
                    consumers.add( consumer );
                    channel.basicConsume( queueName, true, consumer);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }


        }

        private void startPull(int maxThread,long pullRangeMillis){

            while( true ){
                if(ManagementFactory.getThreadMXBean().getThreadCount() < maxThread){
                    getMessage( maxThread );
                }
                try {
                    Thread.sleep(pullRangeMillis);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }

        private void getMessage(int maxThread){

            boolean message = true;
            while( message ) {
                int count = 0;
                for(int i = 0; i < queues.size(); i++ ) {
                    if(ManagementFactory.getThreadMXBean().getThreadCount() < maxThread)
                    {
                        try {
                            boolean autoAck=false;
                            GetResponse response = null;
                            response = channel.basicGet( queues.get(i), autoAck );
                            if( response == null ) {
                                count++;
                            } else{
                                byte[] body = response.getBody();
                                long deliveryTag = response.getEnvelope().getDeliveryTag();
                                QueueMessage callback = null;
                                byte [] data = Base64.getDecoder().decode(new String(body));
                                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
                                callback = (QueueMessage) ois.readObject();
                                ois.close();
                                Value messageFromQueue=Value.create();
                                messageFromQueue.getFirstChild("message").deepCopy(callback.getMessage());
                                CommMessage request=CommMessage.createRequest(receivingOperation,"/",messageFromQueue);
                                sendMessage(request);

                                channel.basicAck(deliveryTag, false);
                            }
                        } catch (IOException ex) {
                           ex.printStackTrace();
                        } catch (ClassNotFoundException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                if( count == queues.size() ) {
                    message=false;
                }
            }
        }
    }

    private ArrayList<String> getInputQueueNames() {
        ArrayList<String> list = new ArrayList<>();
        inputQueues.stream().forEach(
                q -> {
                    list.add(q.getName());
                }
        );
        return list;
    }
}
