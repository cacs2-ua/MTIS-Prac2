<?php
// MTISSubscribeAsync.php

require_once __DIR__ . '/vendor/autoload.php';

use Stomp\Client;
use Stomp\StatefulStomp;

$url = 'tcp://localhost:61613';
$topic = '/topic/historiales';

// Creamos el cliente STOMP
$client = new Client($url);
$client->setLogin('mtis', 'mtis');

$stomp = new StatefulStomp($client);

// Nos suscribimos al tema (modo AUTO_ACKNOWLEDGE por defecto)
$stomp->subscribe($topic);

echo "Waiting for messages asynchronously..." . PHP_EOL;

// Bucle infinito para esperar mensajes de forma continua
while (true) {
    // read() es bloqueante y esperará hasta recibir un mensaje
    $frame = $stomp->read();
    if ($frame) {
        echo "Received message: " . $frame->body . PHP_EOL;
        // No es necesario llamar a ack() en modo AUTO_ACKNOWLEDGE
    }
    sleep(1);
}
