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
import org.json.JSONObject;

public class Oficina1 implements MessageListener {

    // Attribute to store the temperature
    private int temperature;
    
    private boolean coldSystemActivated = false;
    private boolean heatSystemActivated = false;

    public static final int TEMPERATURE_DIFFERENCE = 2;
    public static final int COLD_SYSTEM_STOP_TEMPERATURE = 23;
    public static final int HEAT_SYSTEM_STOP_TEMPERATURE = 23;

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }
    
    public boolean isColdSystemActivated() {
        return coldSystemActivated;
    }

    public void setColdSystemActivated(boolean coldSystemActivated) {
        this.coldSystemActivated = coldSystemActivated;
    }

    public boolean isHeatSystemActivated() {
        return heatSystemActivated;
    }

    public void setHeatSystemActivated(boolean heatSystemActivated) {
        this.heatSystemActivated = heatSystemActivated;
    }

    public void incrementTemperature() {
        this.temperature += TEMPERATURE_DIFFERENCE;
    }

    public void decrementTemperature() {
        this.temperature -= TEMPERATURE_DIFFERENCE;
    }

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
     * Updates the 'temperature' attribute using Utils, builds the JSON payload,
     * creates a TextMessage and sends it via the producer.
     */
    private void sendTemperatureMessage(Session session, MessageProducer producer) throws JMSException {
        // Update the temperature (e.g., using a utility method)
        this.temperature = Utils.manejarTemperaturaRandomIndicator();
        
        // Build the JSON payload with the desired format (no extra comma)
        String jsonPayload = "{"
                + "\"office\": \"office1\","
                + "\"temperature\": " + this.temperature + ","
                + "\"cold_system_activated\": false,"
                + "\"heat_system_activated\": false"
                + "}";
        
        // Create and send the TextMessage
        TextMessage message = session.createTextMessage(jsonPayload);
        producer.send(message);
    }

    /**
     * Manages the cold system: decreases temperature, sends a JSON payload, and resets the cold system flag if necessary.
     */
    public void manageColdSystem(Session session, MessageProducer producer) throws JMSException {
        this.decrementTemperature();
        
        String jsonPayload = "{"
                + "\"office\": \"office1\","
                + "\"temperature\": " + this.getTemperature() + ","
                + "\"cold_system_activated\": true,"
                + "\"heat_system_activated\": false"
                + "}";
        
        TextMessage message = session.createTextMessage(jsonPayload);
        producer.send(message);
        
        if (this.getTemperature() <= COLD_SYSTEM_STOP_TEMPERATURE) {
            this.setColdSystemActivated(false);
        }
    }

    /**
     * Manages the heat system: increases temperature, sends a JSON payload, and resets the heat system flag if necessary.
     */
    public void manageHeatSystem(Session session, MessageProducer producer) throws JMSException {
        this.incrementTemperature();
        
        String jsonPayload = "{"
                + "\"office\": \"office1\","
                + "\"temperature\": " + this.getTemperature() + ","
                + "\"cold_system_activated\": false,"
                + "\"heat_system_activated\": true"
                + "}";
        
        TextMessage message = session.createTextMessage(jsonPayload);
        producer.send(message);
        
        if (this.getTemperature() >= HEAT_SYSTEM_STOP_TEMPERATURE) {
            this.setHeatSystemActivated(false);
        }
    }
    
    /**
     * Initializes the JMS connection and schedules tasks to be executed concurrently based on conditions.
     */
    public void start() throws JMSException {
        final JMSComponents jmsComponents = setupJMS();
        
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
        
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    // Update temperature only if neither system is activated.
                    if (!Oficina1.this.isColdSystemActivated() && !Oficina1.this.isHeatSystemActivated()) {
                        Oficina1.this.setTemperature(Utils.manejarTemperaturaRandomIndicator());
                    }
                    
                    int currentTemperature = Oficina1.this.getTemperature();
                    
                    // Submit tasks based on conditions; each task runs concurrently.
                    if (currentTemperature < 15) {
                        executor.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Oficina1.this.manageColdSystem(jmsComponents.session, jmsComponents.producer);
                                } catch (JMSException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    
                    if (currentTemperature > 30) {
                        executor.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Oficina1.this.manageHeatSystem(jmsComponents.session, jmsComponents.producer);
                                } catch (JMSException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    
                    // Submit the regular task, which always runs concurrently.
                    executor.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Oficina1.this.sendTemperatureMessage(jmsComponents.session, jmsComponents.producer);
                            } catch (JMSException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 2, TimeUnit.SECONDS);
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
