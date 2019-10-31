include "../src/main/resources/RabbitMQJavaServiceInterface.iol"

main {
    with( conf ) {
        .username = "guest";
        .password = "guest";
        .hostname = "localhost";
        .portnumber = 5672;
        with( .exchange ) {
            .name = "test_exchange"; 
            .type = "direct";
            .durable = true
        }
        with( .output_queues ) {
            .name = "test_queue";
            .routing_key = "route";
            .format?: string // default Value, other  possibilities: json
            .durable = true;
            .exclusive = false; 
            .autodelete = true
        }
    }
    connect@RabbitMQ( conf )()

    with( message ) {
        .exchange_name = "test_exchange";
        .routing_key = "route";
        .message = "ciao"
    }
    writeOnExchange@RabbitMQ( message )
}