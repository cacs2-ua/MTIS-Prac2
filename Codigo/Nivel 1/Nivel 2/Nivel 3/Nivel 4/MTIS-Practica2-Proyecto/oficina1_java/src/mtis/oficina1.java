package mtis;

import java.util.concurrent.CountDownLatch;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.apache.activemq.ActiveMQConnectionFactory;

public class Oficina1 implements MessageListener {

    // Private attribute to store the temperature
    private int temperature;

    public Oficina1() {
        this.temperature = 0; // Initial value
    }
    
    /**
     * Helper inner class to hold the JMS components.
     */
    private static class JMSComponents {
        public Connection connection;
        public Session session;
        public MessageProducer producer;
        public MessageConsumer consumer;
        
        public JMSComponents(Connection connection, Session session, MessageProducer producer, MessageConsumer consumer) {
            this.connection = connection;
            this.session = session;
            this.producer = producer;
            this.consumer = consumer;
        }
    }
    
    /**
     * Sets up the JMS connection, session, destinations, producer, and consumer.
     * The consumer's message listener is set to this instance.
     */
    private JMSComponents setupJMS() throws JMSException {
        System.out.println("ComienzOOOo");
        String url = "tcp://localhost:61616";
        
        CountDownLatch latch = new CountDownLatch(1);
        
        // Create connection and session
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        
        // Create destinations
        Destination lecturasDest = session.createTopic("lecturas_temperaturas_oficina1");
        Destination actuadorDest = session.createTopic("actuador_temperatura_oficina1");
        
        // Create producer and consumer
        MessageProducer producer = session.createProducer(lecturasDest);
        MessageConsumer consumer = session.createConsumer(actuadorDest);
        consumer.setMessageListener(this);
        
        return new JMSComponents(connection, session, producer, consumer);
    }
    
    /**
     * This method updates the temperature attribute using Utils,
     * builds the JSON payload, creates a TextMessage, sends it through the producer,
     * and then pauses for 4000 milliseconds.
     */
    private void sendTemperatureMessage(Session session, MessageProducer producer) throws JMSException, InterruptedException {
        // Update the 'temperature' attribute using Utils
        this.temperature = Utils.manejarTemperaturaRandomIndicator();
        
        // Build the JSON payload with the desired format
        String jsonPayload = "{"
                + "\"office\": \"office1\","
                + "\"temperature\": " + this.temperature + ","
                + "\"cold_system_activated\": false,"
                + "\"heat_system_activated\": false"
                + "}";

        
        // Create the TextMessage with the JSON payload and send it
        TextMessage message = session.createTextMessage(jsonPayload);
        producer.send(message);
        
        // Pause for 2000 milliseconds
        Thread.sleep(2000);
    }
    
    /**
     * Initializes the JMS connection and periodically sends messages.
     */
    public void start() throws JMSException {
        JMSComponents jmsComponents = setupJMS();
        
        // Loop to periodically send messages
        while (true) {
            try {
                sendTemperatureMessage(jmsComponents.session, jmsComponents.producer);
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
