package mtis;

import java.util.concurrent.CountDownLatch;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;

public class Oficina1 implements MessageListener {

    // Atributo privado para almacenar la temperatura
    private int temperature;

    public Oficina1() {
        this.temperature = 0; // Valor inicial
    }

    // M�todo que inicializa la conexi�n JMS y env�a mensajes peri�dicamente
    public void start() throws JMSException {
        System.out.println("ComienzOOOo");
        String url = "tcp://localhost:61616";

        CountDownLatch latch = new CountDownLatch(1);

        // Crear conexi�n y sesi�n
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Crear destinos
        Destination lecturas_dest = session.createTopic("lecturas_temperaturas_oficina1");
        Destination actuador_dest = session.createTopic("actuador_temperatura_oficina1");

        // Crear productor y consumidor
        MessageProducer producer = session.createProducer(lecturas_dest);
        MessageConsumer consumer = session.createConsumer(actuador_dest);
        consumer.setMessageListener(this);

        // Bucle para enviar mensajes peri�dicamente
        while (true) {
            try {
                // Actualiza el atributo 'temperature' con el valor obtenido de Utils
                this.temperature = Utils.manejarTemperaturaRandomIndicator();

                // Se env�a el valor de 'temperature'
                TextMessage message = session.createTextMessage(String.valueOf(this.temperature));
                producer.send(message);

                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                System.out.println("Received message '" + textMessage.getText() + "'");
            } else {
                System.out.println("Received message of type: " + message.getClass().getName());
            }
        } catch (JMSException e) {
            System.out.println("Got a JMS Exception!");
        }
    }

    public static void main(String[] args) throws JMSException {
        Oficina1 oficina = new Oficina1();
        oficina.start();
    }
}
