include "../src/main/resources/RabbitMQJavaServiceInterface.iol"
include "string_utils.iol"
include "console.iol"

execution{ concurrent }

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
            .response_api_type = "push";
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