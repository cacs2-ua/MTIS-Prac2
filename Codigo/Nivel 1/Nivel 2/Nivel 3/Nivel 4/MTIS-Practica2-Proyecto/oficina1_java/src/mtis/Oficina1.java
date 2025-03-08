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
     * Configura la conexi�n JMS, la sesi�n, los destinos, el productor y el consumidor. 
     * Se asigna este objeto como listener del consumidor.
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
    
    /**
     * Actualiza la temperatura utilizando Utils, construye el payload JSON con el estado actual
     * y env�a un TextMessage a trav�s del productor.
     */
    private void sendTemperatureMessage(Session session, MessageProducer producer) throws JMSException {
        // Si ning�n sistema est� activado se actualiza la temperatura;
        // de lo contrario se conserva el valor modificado por la gesti�n.
        
        // Construir el payload JSON usando los flags actuales
        String jsonPayload = "{"
                + "\"office\": \"office1\","
                + "\"temperature\": " + this.temperature + ","
                + "\"cold_system_activated\": " + this.coldSystemActivated + ","
                + "\"heat_system_activated\": " + this.heatSystemActivated
                + "}";
        
        // Crear y enviar el TextMessage
        TextMessage message = session.createTextMessage(jsonPayload);
        
        int temp = this.temperature;
        temp = this.temperature;
        
        producer.send(message);
    }

    /**
     * Gestiona el sistema de fr�o: disminuye la temperatura, env�a el payload JSON y actualiza el flag.
     */
    public void manageColdSystem(Session session, MessageProducer producer) throws JMSException {
        // Activar el sistema de fr�o si a�n no est� activado
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
        
        // Si se cumple la condici�n de parada, desactivar el sistema
        if (this.getTemperature() <= COLD_SYSTEM_STOP_TEMPERATURE) {
            this.setColdSystemActivated(false);
        }
    }

    /**
     * Gestiona el sistema de calor: aumenta la temperatura, env�a el payload JSON y actualiza el flag.
     */
    public void manageHeatSystem(Session session, MessageProducer producer) throws JMSException {
        // Activar el sistema de calor si a�n no est� activado
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
        
        // Si se cumple la condici�n de parada, desactivar el sistema
        if (this.getTemperature() >= HEAT_SYSTEM_STOP_TEMPERATURE) {
            this.setHeatSystemActivated(false);
        }
    }

    public Boolean getActivateColdSystemFlag(String text) {
        JSONObject json = new JSONObject(text);
        if (json.isNull("activate_cold_system")) {
            return null;
        }
        else {
            return json.getBoolean("activate_cold_system");
        }
    }

    public Boolean getStopColdSystemFlag(String text) {
        JSONObject json = new JSONObject(text);
        if (json.isNull("stop_cold_system")) {
            return null;
        } else {
            return json.getBoolean("stop_cold_system");
        }
    }

    public Boolean getActivateHeatSystemFlag(String text) {
        JSONObject json = new JSONObject(text);
        if (json.isNull("activate_heat_system")) {
            return null;
        } else {
            return json.getBoolean("activate_heat_system");
        }
    }

    public Boolean getStopHeatSystemFlag(String text) {
        JSONObject json = new JSONObject(text);
        if (json.isNull("stop_heat_system")) {
            return null;
        } else {
            return json.getBoolean("stop_heat_system");
        }
    }

    public void manageTemperatureFlags(String text) {
        Boolean activateColdSystem = getActivateColdSystemFlag(text);
        Boolean stopColdSystem = getStopColdSystemFlag(text);
        Boolean activateHeatSystem = getActivateHeatSystemFlag(text);
        Boolean stopHeatSystem = getStopHeatSystemFlag(text);

        if (activateColdSystem != null && activateColdSystem) {
            this.setColdSystemActivated(true);
        }
        if (stopColdSystem != null && stopColdSystem) {
            this.setColdSystemActivated(false);
        }
        if (activateHeatSystem != null && activateHeatSystem) {
            this.setHeatSystemActivated(true);
        }
        if (stopHeatSystem != null && stopHeatSystem) {
            this.setHeatSystemActivated(false);
        }
    }

    public void printOwnTemperatureInformation() {
        System.out.println("Temperature Sensor: " + this.getTemperature() + "C");

        if (this.isColdSystemActivated()) {
            System.out.println("Temperature Activator: Cold System activated. ");
        }

        if (this.isHeatSystemActivated()) {
            System.out.println("Temperature Activator: Heat System activated. ");
        }
    }

    public void printReceivedTemperatureInformation(String text) {
        if (this.getActivateColdSystemFlag(text) != null && this.getActivateColdSystemFlag(text)) {
            System.out.println("Temperature Activator: Temperature has exceeded 30C. Activating Cold System... ");
        }

        if (this.getStopColdSystemFlag(text) != null && this.getStopColdSystemFlag(text)) {
            System.out.println("Temperature Activator: Temperature has reached 23C or less. Stopping Cold System... ");
        }


        if (this.getActivateHeatSystemFlag(text) != null && this.getActivateHeatSystemFlag(text)) {
            System.out.println("Temperature Activator: Temperature is below 15C. Activating Heat System... ");
        }

        if (this.getStopHeatSystemFlag(text) != null && this.getStopHeatSystemFlag(text)) {
            System.out.println("Temperature Activator: Temperature has reached 23C or more. Stopping Heat System... ");
        }
    }
        
    
    /**
     * Inicializa la conexi�n JMS y programa tareas que se ejecutan concurrentemente seg�n las condiciones.
     * Se reestructura la l�gica para enviar UN SOLO mensaje por ciclo (sin anidar executor.submit),
     * de modo que el mismo hilo del task programado ejecute directamente la acci�n correspondiente:
     * - Si la temperatura es menor que 15 se ejecuta la gesti�n del calor.
     * - Si la temperatura es mayor que 30 se ejecuta la gesti�n del fr�o.
     * - En caso contrario se env�a solo el mensaje de lectura.
     */
    public void start() throws JMSException {
        final JMSComponents jmsComponents = setupJMS();
        
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
        
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    // Si ning�n sistema est� activado, actualizar la temperatura de forma aleatoria.
                    if (!Oficina1.this.isColdSystemActivated() && !Oficina1.this.isHeatSystemActivated()) {
                        Oficina1.this.setTemperature(Utils.manejarTemperaturaRandomIndicator());
                    }
                    
                    // Seg�n la condici�n, ejecutar la acci�n de gesti�n o enviar el mensaje de lectura
                    if (Oficina1.this.isHeatSystemActivated()) {
                        // Gesti�n del calor (llamada directa para facilitar la depuraci�n)
                        Oficina1.this.manageHeatSystem(jmsComponents.session, jmsComponents.producer);
                    } else if (Oficina1.this.isColdSystemActivated()) {
                        // Gesti�n del fr�o
                        Oficina1.this.manageColdSystem(jmsComponents.session, jmsComponents.producer);
                    } else {
                        // Mensaje de lectura normal
                        Oficina1.this.sendTemperatureMessage(jmsComponents.session, jmsComponents.producer);
                    }
                    
                    Oficina1.this.printOwnTemperatureInformation();
                    
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
                String text = textMessage.getText();
                manageTemperatureFlags(text);

                this.printReceivedTemperatureInformation(text);
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
