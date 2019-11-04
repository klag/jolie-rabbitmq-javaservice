type Queue: void {
    .name: string 
    .binding*: void {
            .routing_key: string
            .exchange_name: string
    }
    .durable: bool 
    .exclusive: bool 
    .autodelete: bool
}

type RabbitMQConnectRequest: void {
    .username: string
    .password: string
    .virtualhost?: string
    .hostname: string 
    .portnumber: int 
    .exchange*: void {
        .name: string 
        .type: string 
        .durable: bool
        .format?: string    // Value | json
    }
    .output_queues*: Queue
    .input_queues*: void {
        .response_api_type: string  // push | pull, default is push
        .max_thread?: int           // used only when apy type is pull, default is 1
        .millis_pull_range?: long   // used only when apy type is pull, default is 1000
        .queue: Queue
        .format?: string
    }
}

type RabbitMQWriteOnExchangeRequest: void {
    .exchange_name: string
    .routing_key: string
    .message: undefined
}

type ReceiveMessageFromRabbitMQ: void {
    .message: undefined 
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

