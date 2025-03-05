// MTISPublisher.js

const stompit = require('stompit');

const connectOptions = {
  host: 'localhost',
  port: 61613,  // Nota: STOMP usa 61613 por defecto. Si deseas usar otro puerto, cámbialo aquí.
};

stompit.connect(connectOptions, (error, client) => {
    if (error) {
        console.error('Error de conexión: ' + error.message);
        return;
    }

    const destination = '/topic/historiales';
    const message = "<history><patient><name>Manolo García</name></patient></history>";

    // Enviar mensaje
    const frame = client.send({
        destination: destination,
        'content-type': 'text/plain'
    });
    frame.write(message);
    frame.end();

    console.log(`Mensaje enviado: ${message}`);

    // Se desconecta después de enviar el mensaje.
    client.disconnect();
});
