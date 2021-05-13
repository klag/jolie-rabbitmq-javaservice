from  @jolie.RabbitMQ.RabbitMQ import RabbitMQ
from  @jolie.RabbitMQ.RabbitMQ import RabbitMQJavaServiceListenerInterface
from console import Console
from string_utils import StringUtils

execution{ concurrent }
service Reciver{

embed RabbitMQ as RabbitMQ
embed Console as Console
embed StringUtils as StringUtils

inputPort PortName {
    Location: local
    Protocol: sodep
    Interfaces: RabbitMQJavaServiceListenerInterface
}
init {
    with( conf ) {
        .username = "guest";
        .password = "guest";
        .hostname = "localhost";
        .portnumber = 5672;
        with( .exchange ) {
            .name = "test_exchange"; 
            .type = "direct";
            .durable = true;
            .format = "json"
        }
        with( .input_queues ) {
            .response_api_type = "pull";
            .millis_pull_range = 5000;
            with( .queue ) {
                .name = "test_queue2";
                with( .binding ) {
                    .routing_key = "route";
                    .exchange_name = "test_exchange"
                };
                .durable = true;
                .exclusive = false; 
                .autodelete = true
            } 
            .format = "json"
        }
    }
    connect@RabbitMQ( conf )()
}

main {
    receiveMessage( request );
        valueToPrettyString@StringUtils( request )( s )
        println@Console( s )()
    
}

}