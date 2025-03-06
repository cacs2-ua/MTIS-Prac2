package mtis;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

public class Oficina1 {

    public static void main(String []args) throws JMSException {
        // URL of the JMS server.
		String url = "tcp://localhost:61616";

        // Getting JMS connection from the server and starting it
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
        Connection connection = connectionFactory.createConnection();
        connection.start();

        // JMS messages are sent and received using a Session. We will
        // create here a non-transactional session object. If you want
        // to use transactions you should set the first parameter to 'true'
        Session session = connection.createSession(false,
            Session.AUTO_ACKNOWLEDGE);

        Destination lecturas_temperaturas_oficina1_destination = session.createTopic("lecturas_temperaturas_oficina1");

        Destination actuador_temperatura_oficina1_destination = session.createTopic("actuador_temperatura_oficina1");


        MessageProducer lecturas_temperaturas_oficina1_producer = session.createProducer(lecturas_temperaturas_oficina1_destination);

        MessageProducer actuador_temperatura_oficina1_consumer = session.createConsumer(actuador_temperatura_oficina1_destination);
    }
    
}
