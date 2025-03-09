<?php
// MTISSubscribeAsync.php

require_once __DIR__ . '/vendor/autoload.php';

use React\EventLoop\Factory;
use Stomp\Client;
use Stomp\StatefulStomp;

$url = 'tcp://localhost:61613';
$topic = '/topic/historiales';

// Creamos el cliente STOMP y configuramos las credenciales
$client = new Client($url);
$client->setLogin('mtis', 'mtis');
$stomp = new StatefulStomp($client);

// Nos suscribimos al tema (modo AUTO_ACKNOWLEDGE por defecto)
$stomp->subscribe($topic);

echo "Listening for messages asynchronously...\n";

// Creamos el event loop de ReactPHP
$loop = Factory::create();

// Agregamos un temporizador periódico que se ejecute cada 100ms
$loop->addPeriodicTimer(0.1, function () use ($stomp) {
    // Intentamos leer un mensaje sin bloquear la ejecución
    $frame = $stomp->read();
    if ($frame) {
        echo "Received message: " . $frame->body . PHP_EOL;
    }
});

// Ejecutamos el loop (esto mantiene el script corriendo de forma asíncrona)
$loop->run();
