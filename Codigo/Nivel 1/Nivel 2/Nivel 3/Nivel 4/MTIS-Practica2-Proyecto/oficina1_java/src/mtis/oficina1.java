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

    // Atributo para almacenar la temperatura
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
    
    // getters y setters para coldSystemActivated y heatSystemActivated
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
     * Configura la conexi�n JMS, la sesi�n, los destinos, el productor y el consumidor.
     * Se asigna a este objeto como listener de los mensajes recibidos.
     */
    private JMSComponents setupJMS() throws JMSException {
        System.out.println("ComienzOOOo");
        String url = "tcp://localhost:61616";
        
        CountDownLatch latch = new CountDownLatch(1);
        
        // Crear conexi�n y sesi�n
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
    
    private Integer extractTemperature(TextMessage textMessage) {
        try {
            String jsonText = textMessage.getText();
            JSONObject jsonObject = new JSONObject(jsonText);
            // Check if the "temperature" field exists and is not null
            if (jsonObject.has("temperature") && !jsonObject.isNull("temperature")) {
                return jsonObject.getInt("temperature");
            } else {
                return null; // temperature is missing or null
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Actualiza el atributo 'temperature' usando Utils, construye el payload JSON,
     * crea un TextMessage y lo envia mediante el productor.
     */
    private void sendTemperatureMessage(Session session, MessageProducer producer) throws JMSException {
        // Actualizar la temperatura (por ejemplo, usando un metodo de utilidad)
        this.temperature = Utils.manejarTemperaturaRandomIndicator();
        
        // Construir el payload JSON
        String jsonPayload = "{"
                + "\"office\": \"office1\","
                + "\"temperature\": " + this.temperature + ","
                + "\"cold_system_activated\": " + false + ","
                + "\"heat_system_activated\": " + false + ","
                + "}";
        
        // Crear y enviar el TextMessage
        TextMessage message = session.createTextMessage(jsonPayload);
        producer.send(message);
    }

    public void manageColdSystem(Session session, MessageProducer producer) throws JMSException {
        this.decrementTemperature();

        String jsonPayload = "{"
        + "\"office\": \"office1\","
        + "\"temperature\": " + this.getTemperature() + ","
        + "\"cold_system_activated\": " + true + ","
        + "\"heat_system_activated\": " + false + 
        "}";


        TextMessage message = session.createTextMessage(jsonPayload);
        producer.send(message);

        if (this.getTemperature() <= Oficina1.COLD_SYSTEM_STOP_TEMPERATURE) {
            this.setColdSystemActivated(false);
        }
    }

    
    public void manageHeatSystem(Session session, MessageProducer producer) throws JMSException {
        this.incrementTemperature();

        String jsonPayload = "{"
        + "\"office\": \"office1\","
        + "\"temperature\": " + this.getTemperature() + ","
        + "\"cold_system_activated\": " + false + ","
        + "\"heat_system_activated\": " + true + "}"
        + "}";

        TextMessage message = session.createTextMessage(jsonPayload);
        producer.send(message);

        if (this.getTemperature() >= Oficina1.HEAT_SYSTEM_STOP_TEMPERATURE) {
            this.setColdSystemActivated(false);
        }
    }

    
    /**
     * Inicializa la conexion JMS y programa el envio periodico de mensajes
     * utilizando un ScheduledExecutorService para mantener la asincronna.
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

    public void startTemperatureSystem(int temperature) {
        if (temperature < 15) {
            this.setColdSystemActivated(false);
            this.setHeatSystemActivated(true);
        } else if (temperature > 30) {
            this.setColdSystemActivated(true);
            this.setHeatSystemActivated(false);
        } else {
            this.setColdSystemActivated(false);
            this.setHeatSystemActivated(false);
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                System.out.println("Received message '" + textMessage.getText() + "'");
                
                Integer temperatureValue = extractTemperature(textMessage);
                if (temperatureValue != null) {
                    System.out.println("Extracted temperature: " + temperatureValue);
                    this.setTemperature(temperatureValue);
                } else {
                    System.out.println("Temperature value is null or not provided.");
                }
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
