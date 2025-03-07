package mtis;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    // Atributo para almacenar la temperatura
    private int temperature;

    public Oficina1() {
        this.temperature = 0; // Valor inicial
    }
    
    /**
     * Clase interna de ayuda para almacenar los componentes JMS.
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
     * Configura la conexión JMS, la sesión, los destinos, el productor y el consumidor.
     * Se asigna a este objeto como listener de los mensajes recibidos.
     */
    private JMSComponents setupJMS() throws JMSException {
        System.out.println("ComienzOOOo");
        String url = "tcp://localhost:61616";
        
        CountDownLatch latch = new CountDownLatch(1);
        
        // Crear conexión y sesión
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        
        // Crear destinos
        Destination lecturasDest = session.createTopic("lecturas_temperaturas_oficina1");
        Destination actuadorDest = session.createTopic("actuador_temperatura_oficina1");
        
        // Crear productor y consumidor
        MessageProducer producer = session.createProducer(lecturasDest);
        MessageConsumer consumer = session.createConsumer(actuadorDest);
        consumer.setMessageListener(this);
        
        return new JMSComponents(connection, session, producer, consumer);
    }
    
    /**
     * Actualiza el atributo 'temperature' usando Utils, construye el payload JSON,
     * crea un TextMessage y lo envía mediante el productor.
     */
    private void sendTemperatureMessage(Session session, MessageProducer producer) throws JMSException {
        // Actualizar la temperatura (por ejemplo, usando un método de utilidad)
        this.temperature = Utils.manejarTemperaturaRandomIndicator();
        
        // Construir el payload JSON
        String jsonPayload = "{"
                + "\"office\": \"office1\","
                + "\"temperature\": " + this.temperature + ","
                + "\"cold_system_activated\": false,"
                + "\"heat_system_activated\": false"
                + "}";
        
        // Crear y enviar el TextMessage
        TextMessage message = session.createTextMessage(jsonPayload);
        producer.send(message);
    }
    
    /**
     * Inicializa la conexión JMS y programa el envío periódico de mensajes
     * utilizando un ScheduledExecutorService para mantener la asincronía.
     */
    public void start() throws JMSException {
        JMSComponents jmsComponents = setupJMS();
        
        // Usar ScheduledExecutorService para enviar mensajes cada 2 segundos.
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Runnable sendTask = () -> {
            try {
                sendTemperatureMessage(jmsComponents.session, jmsComponents.producer);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        };
        executor.scheduleAtFixedRate(sendTask, 0, 2, TimeUnit.SECONDS);
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
