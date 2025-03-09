<?php
// MTISSubscribe.php

require_once __DIR__ . '/vendor/autoload.php';

use Stomp\Client;
use Stomp\StatefulStomp;

$url = 'tcp://localhost:61613';
$topic = '/topic/historiales';

// Creamos el cliente STOMP
$client = new Client($url);
// Establecemos las credenciales de ActiveMQ
$client->setLogin('mtis', 'mtis');

$stomp = new StatefulStomp($client);

// Nos suscribimos al tema
$stomp->subscribe($topic);

echo "Waiting for message..." . PHP_EOL;

// Esperamos a recibir el mensaje (la función read() es bloqueante)
$frame = $stomp->read();
if ($frame) {
    echo "Received message: " . $frame->body . PHP_EOL;
    // Confirmamos (ACK) la recepción del mensaje
    $stomp->ack($frame);
} else {
    echo "No message received." . PHP_EOL;
}
