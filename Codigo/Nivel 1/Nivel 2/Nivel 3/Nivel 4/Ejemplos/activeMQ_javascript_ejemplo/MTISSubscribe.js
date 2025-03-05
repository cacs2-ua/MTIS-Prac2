// MTISSubscribe.js

const stompit = require('stompit');

const connectOptions = {
  host: 'localhost',
  port: 61613,
};

stompit.connect(connectOptions, (error, client) => {
    if (error) {
        console.error('Error de conexión: ' + error.message);
        return;
    }

    const subscribeHeaders = {
        destination: '/topic/historiales',
        ack: 'auto'
    };

    client.subscribe(subscribeHeaders, (error, message) => {
        if (error) {
            console.error('Error en la suscripción: ' + error.message);
            return;
        }

        let body = '';
        message.on('data', (chunk) => {
            body += chunk;
        });
        message.on('end', () => {
            console.log("Mensaje recibido: " + body);
            // Desconectamos al recibir el mensaje.
            client.disconnect();
        });
    });
});
