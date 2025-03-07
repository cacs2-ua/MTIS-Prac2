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
        this.temperature = 0; // Valor inicial
    }

    /**
     * Clase interna auxiliar para almacenar los componentes JMS.
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
     * Se asigna este objeto como listener del consumidor.
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
     * Actualiza la temperatura utilizando Utils, construye el payload JSON con el estado actual
     * y envía un TextMessage a través del productor.
     */
    private void sendTemperatureMessage(Session session, MessageProducer producer) throws JMSException {
        // Actualizar la temperatura solo si ningún sistema está activado
        if (!this.isColdSystemActivated() && !this.isHeatSystemActivated()) {
            this.temperature = Utils.manejarTemperaturaRandomIndicator();
        }
        
        // Construir el payload JSON usando los flags actuales
        String jsonPayload = "{"
                + "\"office\": \"office1\","
                + "\"temperature\": " + this.temperature + ","
                + "\"cold_system_activated\": " + this.coldSystemActivated + ","
                + "\"heat_system_activated\": " + this.heatSystemActivated
                + "}";
        
        // Crear y enviar el TextMessage
        TextMessage message = session.createTextMessage(jsonPayload);
        producer.send(message);
    }

    /**
     * Gestiona el sistema de frío: disminuye la temperatura, envía el payload JSON y actualiza el flag.
     */
    public void manageColdSystem(Session session, MessageProducer producer) throws JMSException {
        // Activar el sistema de frío si no está ya activado
        if (!this.coldSystemActivated) {
            this.setColdSystemActivated(true);
        }
        this.decrementTemperature();
        
        String jsonPayload = "{"
                + "\"office\": \"office1\","
                + "\"temperature\": " + this.getTemperature() + ","
                + "\"cold_system_activated\": " + this.coldSystemActivated + ","
                + "\"heat_system_activated\": " + this.heatSystemActivated
                + "}";
        
        TextMessage message = session.createTextMessage(jsonPayload);
        producer.send(message);
        
        // Desactivar el sistema de frío si se cumple la condición de parada
        if (this.getTemperature() <= COLD_SYSTEM_STOP_TEMPERATURE) {
            this.setColdSystemActivated(false);
        }
    }

    /**
     * Gestiona el sistema de calor: aumenta la temperatura, envía el payload JSON y actualiza el flag.
     */
    public void manageHeatSystem(Session session, MessageProducer producer) throws JMSException {
        // Activar el sistema de calor si no está ya activado
        if (!this.heatSystemActivated) {
            this.setHeatSystemActivated(true);
        }
        this.incrementTemperature();
        
        String jsonPayload = "{"
                + "\"office\": \"office1\","
                + "\"temperature\": " + this.getTemperature() + ","
                + "\"cold_system_activated\": " + this.coldSystemActivated + ","
                + "\"heat_system_activated\": " + this.heatSystemActivated
                + "}";
        
        TextMessage message = session.createTextMessage(jsonPayload);
        producer.send(message);
        
        // Desactivar el sistema de calor si se cumple la condición de parada
        if (this.getTemperature() >= HEAT_SYSTEM_STOP_TEMPERATURE) {
            this.setHeatSystemActivated(false);
        }
    }
    
    /**
     * Inicializa la conexión JMS y programa tareas que se ejecutan concurrentemente según las condiciones.
     */
    public void start() throws JMSException {
        final JMSComponents jmsComponents = setupJMS();
        
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
        
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    // Actualizar la temperatura solo si ningún sistema está activado
                    if (!Oficina1.this.isColdSystemActivated() && !Oficina1.this.isHeatSystemActivated()) {
                        Oficina1.this.setTemperature(Utils.manejarTemperaturaRandomIndicator());
                    }
                    
                    int currentTemperature = Oficina1.this.getTemperature();
                    
                    // Ejecutar tareas basadas en condiciones (cada tarea se ejecuta concurrentemente)
                    if (currentTemperature < 15) {
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
                    
                    if (currentTemperature > 30) {
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
                    
                    // Tarea regular que se ejecuta siempre
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
