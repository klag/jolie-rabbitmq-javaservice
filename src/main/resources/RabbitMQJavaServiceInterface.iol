type RabbitMQConnectRequest: void {
    .username: string
    .password: string
    .virtualhost?: string
    .hostname: string 
    .portnumber: int 
    .exchange: void {
        .name: string 
        .type: string 
        .durable: bool
    }
    .output_queues*: void {
        .name: string 
        .routing_key: string
        .durable: bool 
        .exclusive: bool 
        .autodelete: bool
    }
    .input_queues?: void {
        .response_api_type: string 
        .max_thread: int 
        .millis_pull_range: long
        .queues*: void {
            .name: string 
            .durable: bool
            .exclusive: bool
            .autodelete: bool 
            .routing_key: string
        }
    }
}

type RabbitMQWriteOnExchangeRequest: void {
    .exchange_name: string
    .routing_key: string
    .message: undefined
}

type ReceiveMessageFromRabbitMQ: void {
    .message: void 
    .queue_name: string
}

interface RabbitMQJavaServiceInterface {
    RequestResponse:
        connect( RabbitMQConnectRequest )( void ) 
            throws  IOException( string )
                    TimeoutException( string )
                    ConnectionFault( string )
                    ConfigurationFault( string )
    OneWay: 
        disconnect( void ),
        writeOnExchange( RabbitMQWriteOnExchangeRequest )
}

interface RabbitMQJavaServiceListenerInterface {
    OneWay:
        receiveMessage( ReceiveMessageFromRabbitMQ )
}

outputPort RabbitMQ {
    Interfaces: RabbitMQJavaServiceInterface
}

inputPort MySelf {
    Location: "local"
    Protocol: sodep
    Interfaces: RabbitMQJavaServiceListenerInterface
}

embedded {
    Java: 
    "org.jolie.rabbitmq.RabbitMQJavaService" in RabbitMQ
}

