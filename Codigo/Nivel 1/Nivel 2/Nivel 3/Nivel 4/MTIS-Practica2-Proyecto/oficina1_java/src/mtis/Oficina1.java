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

    // Temperature variables 
    private int temperature;
    private boolean heatSystemActivated = false;
    private boolean coldSystemActivated = false;
    
    // Illumination variables
    private int illuminationIntensity;
    private boolean increaseIntensityIlluminationRegulatorActivated = false;
    private boolean decreaseIntensityIlluminationRegulatorActivated = false;

    public static final int TEMPERATURE_DIFFERENCE = 2;
    public static final int HEAT_SYSTEM_STOP_TEMPERATURE = 23;
    public static final int COLD_SYSTEM_STOP_TEMPERATURE = 23;
    
    public static final int ILLUMINATION_INTENSITY_DIFFERENCE = 200;
    public static final int INCREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY = 2300;
    public static final int DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY = 2300;

    // Constructor: initialize temperature and illumination intensity
    public Oficina1() {
        this.temperature = 0; // initial temperature value
        this.illuminationIntensity = 2300; // initial illumination intensity (equilibrium)
    }

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
    
    public int getIlluminationIntensity() {
        return illuminationIntensity;
    }
    
    public void setIlluminationIntensity(int illuminationIntensity) {
        this.illuminationIntensity = illuminationIntensity;
    }

    public void incrementTemperature() {
        this.temperature += TEMPERATURE_DIFFERENCE;
    }

    public void decrementTemperature() {
        this.temperature -= TEMPERATURE_DIFFERENCE;
    }

    /**
     * Internal class to hold JMS components for both temperature and illumination.
     */
    private static class JMSComponents {
        public Connection connection;
        public Session session;
        public MessageProducer temperatureProducer;
        public MessageConsumer temperatureConsumer;
        public MessageProducer illuminationProducer;
        public MessageConsumer illuminationConsumer;
        
        public JMSComponents(Connection connection, Session session,
                             MessageProducer temperatureProducer, MessageConsumer temperatureConsumer,
                             MessageProducer illuminationProducer, MessageConsumer illuminationConsumer) {
            this.connection = connection;
            this.session = session;
            this.temperatureProducer = temperatureProducer;
            this.temperatureConsumer = temperatureConsumer;
            this.illuminationProducer = illuminationProducer;
            this.illuminationConsumer = illuminationConsumer;
        }
    }
    
    /**
     * Setup JMS connection, session, and destinations for both temperature and illumination.
     */
    private JMSComponents setupJMS() throws JMSException {
        System.out.println("Office1 Starting... ");
        String url = "tcp://localhost:61616";
        
        CountDownLatch latch = new CountDownLatch(1);
        
        // Create connection and session  
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("mtis", "mtis", url);
        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        
        // Create destinations for temperature
        Destination lecturasTempDest = session.createTopic("lecturas_temperaturas_oficina1");
        Destination actuadorTempDest = session.createTopic("actuador_temperatura_oficina1");
        
        // Create destinations for illumination
        Destination lecturasIllumDest = session.createTopic("lecturas_iluminacion_oficina1");
        Destination actuadorIllumDest = session.createTopic("actuador_iluminacion_oficina1");
        
        // Create producer and consumer for temperature
        MessageProducer temperatureProducer = session.createProducer(lecturasTempDest);
        MessageConsumer temperatureConsumer = session.createConsumer(actuadorTempDest);
        temperatureConsumer.setMessageListener(this);
        
        // Create producer and consumer for illumination
        MessageProducer illuminationProducer = session.createProducer(lecturasIllumDest);
        MessageConsumer illuminationConsumer = session.createConsumer(actuadorIllumDest);
        illuminationConsumer.setMessageListener(this);
        
        return new JMSComponents(connection, session, temperatureProducer, temperatureConsumer, illuminationProducer, illuminationConsumer);
    }
    
    /**
     * Sends a temperature reading message.
     */
    private void sendTemperatureMessage(Session session, MessageProducer producer) throws JMSException {
        String jsonPayload = "{"
                + "\"office\": \"office1\","
                + "\"temperature\": " + this.temperature + ","
                + "\"cold_system_activated\": " + this.coldSystemActivated + ","
                + "\"heat_system_activated\": " + this.heatSystemActivated
                + "}";
        TextMessage message = session.createTextMessage(jsonPayload);
        producer.send(message);
    }

    /**
     * Manages the cold system: decrements temperature, sends message, and deactivates if condition met.
     */
    public void manageColdSystem(Session session, MessageProducer producer) throws JMSException {
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
        
        if (this.getTemperature() <= COLD_SYSTEM_STOP_TEMPERATURE) {
            this.setColdSystemActivated(false);
        }
    }

    /**
     * Manages the heat system: increments temperature, sends message, and deactivates if condition met.
     */
    public void manageHeatSystem(Session session, MessageProducer producer) throws JMSException {
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
        
        if (this.getTemperature() >= HEAT_SYSTEM_STOP_TEMPERATURE) {
            this.setHeatSystemActivated(false);
        }
    }

    // Temperature flag management methods (unchanged)
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
            System.out.println("Temperature Activator: Cold System activated.");
        }
        if (this.isHeatSystemActivated()) {
            System.out.println("Temperature Activator: Heat System activated.");
        }
    }

    // New methods for illumination

    private void sendIlluminationMessage(Session session, MessageProducer producer) throws JMSException {
        String jsonPayload = "{"
                + "\"office\": \"office1\","
                + "\"illumination_intensity\": " + this.illuminationIntensity + ","
                + "\"increase_intensity_regulator_activated\": " + this.increaseIntensityIlluminationRegulatorActivated + ","
                + "\"decrease_intensity_regulator_activated\": " + this.decreaseIntensityIlluminationRegulatorActivated
                + "}";
        TextMessage message = session.createTextMessage(jsonPayload);
        producer.send(message);
    }

    public void manageIncreaseIlluminationRegulator(Session session, MessageProducer producer) throws JMSException {
        if (!this.increaseIntensityIlluminationRegulatorActivated) {
            this.increaseIntensityIlluminationRegulatorActivated = true;
        }
        this.illuminationIntensity += ILLUMINATION_INTENSITY_DIFFERENCE;
        
        String jsonPayload = "{"
                + "\"office\": \"office1\","
                + "\"illumination_intensity\": " + this.illuminationIntensity + ","
                + "\"increase_intensity_regulator_activated\": " + this.increaseIntensityIlluminationRegulatorActivated + ","
                + "\"decrease_intensity_regulator_activated\": " + this.decreaseIntensityIlluminationRegulatorActivated
                + "}";
        TextMessage message = session.createTextMessage(jsonPayload);
        producer.send(message);
        
        if (this.illuminationIntensity >= INCREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY) {
            this.increaseIntensityIlluminationRegulatorActivated = false;
        }
    }

    public void manageDecreaseIlluminationRegulator(Session session, MessageProducer producer) throws JMSException {
        if (!this.decreaseIntensityIlluminationRegulatorActivated) {
            this.decreaseIntensityIlluminationRegulatorActivated = true;
        }
        this.illuminationIntensity -= ILLUMINATION_INTENSITY_DIFFERENCE;
        
        String jsonPayload = "{"
                + "\"office\": \"office1\","
                + "\"illumination_intensity\": " + this.illuminationIntensity + ","
                + "\"increase_intensity_regulator_activated\": " + this.increaseIntensityIlluminationRegulatorActivated + ","
                + "\"decrease_intensity_regulator_activated\": " + this.decreaseIntensityIlluminationRegulatorActivated
                + "}";
        TextMessage message = session.createTextMessage(jsonPayload);
        producer.send(message);
        
        if (this.illuminationIntensity <= DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY) {
            this.decreaseIntensityIlluminationRegulatorActivated = false;
        }
    }

    public Boolean getActivateIncreaseIntensityFlag(String text) {
        JSONObject json = new JSONObject(text);
        if (json.isNull("activate_increase_intensity_illumination_regulator")) {
            return null;
        } else {
            return json.getBoolean("activate_increase_intensity_illumination_regulator");
        }
    }

    public Boolean getStopIncreaseIntensityFlag(String text) {
        JSONObject json = new JSONObject(text);
        if (json.isNull("stop_increase_intensity_illumination_regulator")) {
            return null;
        } else {
            return json.getBoolean("stop_increase_intensity_illumination_regulator");
        }
    }

    public Boolean getActivateDecreaseIntensityFlag(String text) {
        JSONObject json = new JSONObject(text);
        if (json.isNull("activate_decrease_intensity_illumination_regulator")) {
            return null;
        } else {
            return json.getBoolean("activate_decrease_intensity_illumination_regulator");
        }
    }

    public Boolean getStopDecreaseIntensityFlag(String text) {
        JSONObject json = new JSONObject(text);
        if (json.isNull("stop_decrease_intensity_illumination_regulator")) {
            return null;
        } else {
            return json.getBoolean("stop_decrease_intensity_illumination_regulator");
        }
    }

    public void manageIlluminationFlags(String text) {
        Boolean activateIncrease = getActivateIncreaseIntensityFlag(text);
        Boolean stopIncrease = getStopIncreaseIntensityFlag(text);
        Boolean activateDecrease = getActivateDecreaseIntensityFlag(text);
        Boolean stopDecrease = getStopDecreaseIntensityFlag(text);

        if (activateIncrease != null && activateIncrease) {
            this.increaseIntensityIlluminationRegulatorActivated = true;
        }
        if (stopIncrease != null && stopIncrease) {
            this.increaseIntensityIlluminationRegulatorActivated = false;
        }
        if (activateDecrease != null && activateDecrease) {
            this.decreaseIntensityIlluminationRegulatorActivated = true;
        }
        if (stopDecrease != null && stopDecrease) {
            this.decreaseIntensityIlluminationRegulatorActivated = false;
        }
    }

    public void printOwnIlluminationInformation() {
        System.out.println("Illumination Sensor: " + this.getIlluminationIntensity() + " lumens");
        if (this.increaseIntensityIlluminationRegulatorActivated) {
            System.out.println("Illumination Activator: Increase Intensity Regulator activated.");
        }
        if (this.decreaseIntensityIlluminationRegulatorActivated) {
            System.out.println("Illumination Activator: Decrease Intensity Regulator activated.");
        }
    }

    public void printReceivedIlluminationInformation(String text) {
        if (getActivateIncreaseIntensityFlag(text) != null && getActivateIncreaseIntensityFlag(text)) {
            System.out.println("Illumination Activator: Illumination intensity is below threshold (below 1500 lumens). Activating Increase Intensity Regulator...");
        }
        if (getStopIncreaseIntensityFlag(text) != null && getStopIncreaseIntensityFlag(text)) {
            System.out.println("Illumination Activator: Illumination intensity has reached desired level (around 2300 lumens). Stopping Increase Intensity Regulator...");
        }
        if (getActivateDecreaseIntensityFlag(text) != null && getActivateDecreaseIntensityFlag(text)) {
            System.out.println("Illumination Activator: Illumination intensity is above threshold (above 3000 lumens). Activating Decrease Intensity Regulator...");
        }
        if (getStopDecreaseIntensityFlag(text) != null && getStopDecreaseIntensityFlag(text)) {
            System.out.println("Illumination Activator: Illumination intensity has reached desired level (around 2300 lumens). Stopping Decrease Intensity Regulator...");
        }
    }
    
    /**
     * Modified onMessage to process both temperature and illumination messages.
     */
    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                String text = textMessage.getText();
                JSONObject json = new JSONObject(text);
                if (json.has("temperature")) {
                    manageTemperatureFlags(text);
                    printReceivedTemperatureInformation(text);
                }
                if (json.has("illumination_intensity")) {
                    manageIlluminationFlags(text);
                    printReceivedIlluminationInformation(text);
                }
            } else {
                System.out.println("Received message of type: " + message.getClass().getName());
            }
        } catch (JMSException e) {
            System.out.println("Got a JMS Exception!");
        }
    }
    
    /**
     * The start method now schedules two tasks: one for temperature and one for illumination.
     */
    public void start() throws JMSException {
        final JMSComponents jmsComponents = setupJMS();
        
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
        
        // Temperature task
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!Oficina1.this.isColdSystemActivated() && !Oficina1.this.isHeatSystemActivated()) {
                        Oficina1.this.setTemperature(Utils.manejarTemperaturaRandomIndicator());
                    }
                    if (Oficina1.this.isHeatSystemActivated()) {
                        Oficina1.this.manageHeatSystem(jmsComponents.session, jmsComponents.temperatureProducer);
                    } else if (Oficina1.this.isColdSystemActivated()) {
                        Oficina1.this.manageColdSystem(jmsComponents.session, jmsComponents.temperatureProducer);
                    } else {
                        Oficina1.this.sendTemperatureMessage(jmsComponents.session, jmsComponents.temperatureProducer);
                    }
                    Oficina1.this.printOwnTemperatureInformation();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 2, TimeUnit.SECONDS);
        
        // Illumination task
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!Oficina1.this.increaseIntensityIlluminationRegulatorActivated && !Oficina1.this.decreaseIntensityIlluminationRegulatorActivated) {
                        Oficina1.this.setIlluminationIntensity(Utils.manejarIlluminationIntensityRandomIndicator());
                    }
                    if (Oficina1.this.increaseIntensityIlluminationRegulatorActivated) {
                        Oficina1.this.manageIncreaseIlluminationRegulator(jmsComponents.session, jmsComponents.illuminationProducer);
                    } else if (Oficina1.this.decreaseIntensityIlluminationRegulatorActivated) {
                        Oficina1.this.manageDecreaseIlluminationRegulator(jmsComponents.session, jmsComponents.illuminationProducer);
                    } else {
                        Oficina1.this.sendIlluminationMessage(jmsComponents.session, jmsComponents.illuminationProducer);
                    }
                    Oficina1.this.printOwnIlluminationInformation();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 2, TimeUnit.SECONDS);
    }
    
    public void printReceivedTemperatureInformation(String text) {
        if (this.getActivateColdSystemFlag(text) != null && this.getActivateColdSystemFlag(text)) {
            System.out.println("Temperature Activator: Temperature has exceeded 30C. Activating Cold System...");
        }
        if (this.getStopColdSystemFlag(text) != null && this.getStopColdSystemFlag(text)) {
            System.out.println("Temperature Activator: Temperature has reached 23C or less. Stopping Cold System...");
        }
        if (this.getActivateHeatSystemFlag(text) != null && this.getActivateHeatSystemFlag(text)) {
            System.out.println("Temperature Activator: Temperature is below 15C. Activating Heat System...");
        }
        if (this.getStopHeatSystemFlag(text) != null && this.getStopHeatSystemFlag(text)) {
            System.out.println("Temperature Activator: Temperature has reached 23C or more. Stopping Heat System...");
        }
    }
    
    public static void main(String[] args) throws JMSException {
        Oficina1 oficina = new Oficina1();
        oficina.start();
    }
}
