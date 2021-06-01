from  @jolie.RabbitMQ.RabbitMQ import RabbitMQ

service Client {
 embed RabbitMQ as RabbitMQ
main {
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
        with( .output_queues ) {
            .name = "test_queue2";
            with( .binding ) {
                .routing_key = "route";
                .exchange_name = "test_exchange"
            };
            .durable = true;
            .exclusive = false;
            .autodelete = true
        }
    }
    connect@RabbitMQ( conf )()

    with( message ) {
        .exchange_name = "test_exchange";
        .routing_key = "route";
        .message.pippo.pluto = args[ 0 ]
    }
    writeOnExchange@RabbitMQ( message )
}


}

