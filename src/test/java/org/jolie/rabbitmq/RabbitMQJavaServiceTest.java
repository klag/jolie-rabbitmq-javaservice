package org.jolie.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQJavaServiceTest {

    private final String username = "guest";
    private final String password = "guest";
    private final String hostname = "localhost";
    private final int portnumber = 5672;
    private final String testExchange = "test_exchange";
    private final String testQueue = "test_queue";

    @After
    public void cancel() {
        ConnectionFactory factory = new ConnectionFactory();

        factory.setUsername( username );
        factory.setPassword( password );
        factory.setHost(hostname);
        factory.setPort(portnumber);

        Connection connection = null;
        try {
            connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDelete(testQueue);
            channel.exchangeDelete(testExchange );
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

    }


    @Test
    public void testConnection() throws FaultException {
        RabbitMQJavaService rabbitMQJavaService = new RabbitMQJavaService();

        Value request = Value.create();
        request.getFirstChild("username").setValue(username);
        request.getFirstChild("password").setValue(password);
        request.getFirstChild("virtualHost").setValue("/");
        request.getFirstChild("hostname").setValue(hostname);
        request.getFirstChild("portnumber").setValue(portnumber);
        request.getFirstChild("exchange").getFirstChild("name").setValue(testExchange);
        request.getFirstChild("exchange").getFirstChild("type").setValue("direct");
        request.getFirstChild("exchange").getFirstChild("durable").setValue(true);
        request.getFirstChild("output_queues").getFirstChild("name").setValue(testQueue);
        request.getFirstChild("output_queues").getFirstChild("routing_key").setValue("route");
        request.getFirstChild("output_queues").getFirstChild("durable").setValue(true);
        request.getFirstChild("output_queues").getFirstChild("exclusive").setValue(false);
        request.getFirstChild("output_queues").getFirstChild("autodelete").setValue(true);

        rabbitMQJavaService.connect(request);
    }
}

