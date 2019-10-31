package org.jolie.rabbitmq;

import com.rabbitmq.client.*;
import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.embedding.RequestResponse;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

public class RabbitMQJavaService extends JavaService {

    private Connection connection;
    private ConnectionFactory factory;
    private Channel channel;
    final String splitToken="#";
    ArrayList<String> queueNames;
    ArrayList<String> responseQueues;
    ArrayList<String> responseQueuesNames;
    HashMap<String, String> uniqueIds;

    public RabbitMQJavaService() {
        uniqueIds = new HashMap<String, String>();
        responseQueues=new ArrayList<String>();
        responseQueuesNames=new ArrayList<String>();
        queueNames=new ArrayList<String>();
    }


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

            String exchangeName = request.getFirstChild("exchange").getFirstChild("name").strValue();
            String exchangeType = request.getFirstChild("exchange").getFirstChild("type").strValue();  //direct, fanout, topic
            if ( !exchangeType.equals("direct") && !exchangeType.equals("fanout") && !exchangeType.equals("topic") ) {
                throw new FaultException( "ConfigurationFault", "exchange type must be: direct | fanout | topic" );
            }
            boolean exchangeDurable = request.getFirstChild("exchange").getFirstChild("durable").boolValue();

            String apiType = request.getFirstChild("input_queues").getFirstChild("response_api_type").strValue();
            int maxThread = request.getFirstChild("input_queues").getFirstChild("max_thread").intValue();
            long millisPullRange = request.getFirstChild("input_queues").getFirstChild("millis_pull_range").longValue();

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
            channel.exchangeDeclare( exchangeName,exchangeType, exchangeDurable );

            // creating queues
            for( Value q : request.getChildren("output_queues") ) {
                channel.queueDeclare( q.getFirstChild("name").strValue(), q.getFirstChild("durable").boolValue(), q.getFirstChild("exclusive").boolValue(), q.getFirstChild("autodelete").boolValue(), null );
                channel.queueBind( q.getFirstChild("name").strValue(), exchangeName, q.getFirstChild("routingKey").strValue() );
            }

            for( Value q : request.getFirstChild("input_queues").getChildren("queues") ) {
                channel.queueDeclare( q.getFirstChild("name").strValue(), q.getFirstChild("durable").boolValue(), q.getFirstChild("exclusive").boolValue(), q.getFirstChild("autodelete").boolValue(), null );
                channel.queueBind( q.getFirstChild("name").strValue(), exchangeName, q.getFirstChild("routing_key").strValue() );
                responseQueues.add( q.getFirstChild("name").strValue() );
            }

            QueueListeningThread queueThread = new QueueListeningThread( apiType, maxThread, millisPullRange, responseQueues );
            queueThread.start();
        } catch (IOException ex) {
            throw new FaultException("IOException", ex.getMessage() );
        } catch (TimeoutException ex) {
            throw new FaultException("TimeoutException", ex.getMessage() );
        } catch (Exception ex) {
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
            message.setMessage( request.getFirstChild("message") );

            channel.exchangeDeclare( exchangeName,"direct");
            channel.confirmSelect();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(message);
            oos.close();
            String requestString = Base64.getEncoder().encodeToString(baos.toByteArray());
            channel.basicPublish(exchangeName, routingKey,null,requestString.getBytes());
            channel.waitForConfirmsOrDie();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
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
            for(int i=0; i < queues.size(); i++ ) {
                try {
                    final String queueName = queues.get(i);
                    DefaultConsumer consumer = new DefaultConsumer( channel ) {
                        @Override
                        public void handleDelivery (String consumerTag, Envelope envelope,
                                                    AMQP.BasicProperties properties, byte[] body) throws UnsupportedEncodingException
                        {
                            QueueMessage response=null;
                            byte [] data = Base64.getDecoder().decode(new String(body));
                            try{
                                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
                                response = (QueueMessage) ois.readObject();
                                ois.close();
                            } catch(IOException e){
                                e.printStackTrace();
                            } catch (ClassNotFoundException ex) {
                                ex.printStackTrace();
                            }

                            Value operationCallback=Value.create();
                            operationCallback.getFirstChild("message").deepCopy(response.getMessage());
                            operationCallback.getFirstChild("queue_name").setValue( queueName );
                            CommMessage request=CommMessage.createRequest("receiveMessage","/",operationCallback);
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
                                CommMessage request=CommMessage.createRequest("_receiveResponse","/",messageFromQueue);
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
}
