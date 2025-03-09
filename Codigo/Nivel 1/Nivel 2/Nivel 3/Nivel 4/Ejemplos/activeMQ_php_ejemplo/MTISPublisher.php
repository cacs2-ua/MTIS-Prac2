<?php
// MTISPublisher.php

require_once __DIR__ . '/vendor/autoload.php';

use Stomp\Client;
use Stomp\StatefulStomp;
use Stomp\Transport\Message;

// URL del servidor STOMP (ActiveMQ usa por defecto el puerto 61613 para STOMP)
$url = 'tcp://localhost:61613';

// Nombre del tema (para STOMP se antepone "/topic/")
$topic = '/topic/historiales';

// Creamos el cliente STOMP
$client = new Client($url);
// Establecemos las credenciales de ActiveMQ: usuario "mtis" y contraseña "mtis"
$client->setLogin('mtis', 'mtis');

$stomp = new StatefulStomp($client);

// Mensaje en formato XML
$messageBody = '<history><patient><name>Manolo García</name></patient></history>';

// Creamos el objeto Message con el contenido
$message = new Message($messageBody);

// Enviamos el mensaje al tema
$stomp->send($topic, $message);

echo "Sent message: $messageBody" . PHP_EOL;
