using System;
using System.Threading;
using System.Threading.Tasks;
using Apache.NMS;
using Apache.NMS.ActiveMQ;
using Newtonsoft.Json.Linq;

namespace mtis
{
    public class Oficina1
    {
        // Variables de temperatura
        private int temperature;
        private bool heatSystemActivated = false;
        private bool coldSystemActivated = false;

        // Variables de iluminación
        private int illuminationIntensity;
        private bool increaseIntensityIlluminationRegulatorActivated = false;
        private bool decreaseIntensityIlluminationRegulatorActivated = false;

        public const int TEMPERATURE_DIFFERENCE = 5;
        public const int HEAT_SYSTEM_STOP_TEMPERATURE = 23;
        public const int COLD_SYSTEM_STOP_TEMPERATURE = 23;

        public const int ILLUMINATION_INTENSITY_DIFFERENCE = 500;
        public const int INCREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY = 2300;
        public const int DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY = 2300;

        public Oficina1()
        {
            // Valor inicial de la temperatura e iluminación
            this.temperature = 0;
            this.illuminationIntensity = 2300;
        }

        // Propiedades para acceder a los campos
        public int Temperature { get => temperature; set => temperature = value; }
        public bool HeatSystemActivated { get => heatSystemActivated; set => heatSystemActivated = value; }
        public bool ColdSystemActivated { get => coldSystemActivated; set => coldSystemActivated = value; }
        public int IlluminationIntensity { get => illuminationIntensity; set => illuminationIntensity = value; }

        public void IncrementTemperature()
        {
            temperature += TEMPERATURE_DIFFERENCE;
        }

        public void DecrementTemperature()
        {
            temperature -= TEMPERATURE_DIFFERENCE;
        }

        // Clase interna para agrupar los componentes JMS
        private class JMSComponents
        {
            public IConnection Connection { get; }
            public ISession Session { get; }
            public IMessageProducer TemperatureProducer { get; }
            public IMessageConsumer TemperatureConsumer { get; }
            public IMessageProducer IlluminationProducer { get; }
            public IMessageConsumer IlluminationConsumer { get; }

            public JMSComponents(IConnection connection, ISession session,
                IMessageProducer temperatureProducer, IMessageConsumer temperatureConsumer,
                IMessageProducer illuminationProducer, IMessageConsumer illuminationConsumer)
            {
                Connection = connection;
                Session = session;
                TemperatureProducer = temperatureProducer;
                TemperatureConsumer = temperatureConsumer;
                IlluminationProducer = illuminationProducer;
                IlluminationConsumer = illuminationConsumer;
            }
        }

        /// <summary>
        /// Configura la conexión JMS, la sesión, los productores y consumidores para temperatura e iluminación.
        /// </summary>
        private JMSComponents SetupJMS()
        {
            Console.WriteLine("Office1 Starting... ");
            string url = "tcp://localhost:61616";

            // Crear la conexión y la sesión
            IConnectionFactory connectionFactory = new ConnectionFactory(url);
            // Se pasan las credenciales "mtis", "mtis"
            IConnection connection = connectionFactory.CreateConnection("mtis", "mtis");
            connection.Start();
            ISession session = connection.CreateSession(AcknowledgementMode.AutoAcknowledge);

            // Crear destinos para temperatura
            IDestination lecturasTempDest = session.GetTopic("lecturas_temperaturas_oficina1");
            IDestination actuadorTempDest = session.GetTopic("actuador_temperatura_oficina1");

            // Crear destinos para iluminación
            IDestination lecturasIllumDest = session.GetTopic("lecturas_iluminacion_oficina1");
            IDestination actuadorIllumDest = session.GetTopic("actuador_iluminacion_oficina1");

            // Crear productor y consumidor para temperatura
            IMessageProducer temperatureProducer = session.CreateProducer(lecturasTempDest);
            IMessageConsumer temperatureConsumer = session.CreateConsumer(actuadorTempDest);
            temperatureConsumer.Listener += OnMessage;

            // Crear productor y consumidor para iluminación
            IMessageProducer illuminationProducer = session.CreateProducer(lecturasIllumDest);
            IMessageConsumer illuminationConsumer = session.CreateConsumer(actuadorIllumDest);
            illuminationConsumer.Listener += OnMessage;

            return new JMSComponents(connection, session, temperatureProducer, temperatureConsumer, illuminationProducer, illuminationConsumer);
        }

        /// <summary>
        /// Envía un mensaje con la lectura de temperatura.
        /// </summary>
        private void SendTemperatureMessage(ISession session, IMessageProducer producer)
        {
            string jsonPayload = "{" +
                "\"office\": \"office1\"," +
                "\"temperature\": " + this.temperature + "," +
                "\"cold_system_activated\": " + this.coldSystemActivated.ToString().ToLower() + "," +
                "\"heat_system_activated\": " + this.heatSystemActivated.ToString().ToLower() +
                "}";
            ITextMessage message = session.CreateTextMessage(jsonPayload);
            producer.Send(message);
        }

        /// <summary>
        /// Gestiona el sistema de enfriamiento: decrementa la temperatura, envía el mensaje y desactiva el sistema si la condición se cumple.
        /// </summary>
        public void ManageColdSystem(ISession session, IMessageProducer producer)
        {
            if (!this.coldSystemActivated)
            {
                this.coldSystemActivated = true;
            }
            this.DecrementTemperature();

            string jsonPayload = "{" +
                "\"office\": \"office1\"," +
                "\"temperature\": " + this.temperature + "," +
                "\"cold_system_activated\": " + this.coldSystemActivated.ToString().ToLower() + "," +
                "\"heat_system_activated\": " + this.heatSystemActivated.ToString().ToLower() +
                "}";
            ITextMessage message = session.CreateTextMessage(jsonPayload);
            producer.Send(message);

            if (this.temperature <= COLD_SYSTEM_STOP_TEMPERATURE)
            {
                this.coldSystemActivated = false;
            }
        }

        /// <summary>
        /// Gestiona el sistema de calefacción: incrementa la temperatura, envía el mensaje y desactiva el sistema si la condición se cumple.
        /// </summary>
        public void ManageHeatSystem(ISession session, IMessageProducer producer)
        {
            if (!this.heatSystemActivated)
            {
                this.heatSystemActivated = true;
            }
            this.IncrementTemperature();

            string jsonPayload = "{" +
                "\"office\": \"office1\"," +
                "\"temperature\": " + this.temperature + "," +
                "\"cold_system_activated\": " + this.coldSystemActivated.ToString().ToLower() + "," +
                "\"heat_system_activated\": " + this.heatSystemActivated.ToString().ToLower() +
                "}";
            ITextMessage message = session.CreateTextMessage(jsonPayload);
            producer.Send(message);

            if (this.temperature >= HEAT_SYSTEM_STOP_TEMPERATURE)
            {
                this.heatSystemActivated = false;
            }
        }

        // Métodos de manejo de flags (temperatura)
        public bool? GetActivateColdSystemFlag(string text)
        {
            JObject json = JObject.Parse(text);
            if (json["activate_cold_system"] == null)
            {
                return null;
            }
            else
            {
                return (bool)json["activate_cold_system"];
            }
        }

        public bool? GetStopColdSystemFlag(string text)
        {
            JObject json = JObject.Parse(text);
            if (json["stop_cold_system"] == null)
            {
                return null;
            }
            else
            {
                return (bool)json["stop_cold_system"];
            }
        }

        public bool? GetActivateHeatSystemFlag(string text)
        {
            JObject json = JObject.Parse(text);
            if (json["activate_heat_system"] == null)
            {
                return null;
            }
            else
            {
                return (bool)json["activate_heat_system"];
            }
        }

        public bool? GetStopHeatSystemFlag(string text)
        {
            JObject json = JObject.Parse(text);
            if (json["stop_heat_system"] == null)
            {
                return null;
            }
            else
            {
                return (bool)json["stop_heat_system"];
            }
        }

        public void ManageTemperatureFlags(string text)
        {
            bool? activateColdSystem = GetActivateColdSystemFlag(text);
            bool? stopColdSystem = GetStopColdSystemFlag(text);
            bool? activateHeatSystem = GetActivateHeatSystemFlag(text);
            bool? stopHeatSystem = GetStopHeatSystemFlag(text);

            if (activateColdSystem.HasValue && activateColdSystem.Value)
            {
                this.coldSystemActivated = true;
            }
            if (stopColdSystem.HasValue && stopColdSystem.Value)
            {
                this.coldSystemActivated = false;
            }
            if (activateHeatSystem.HasValue && activateHeatSystem.Value)
            {
                this.heatSystemActivated = true;
            }
            if (stopHeatSystem.HasValue && stopHeatSystem.Value)
            {
                this.heatSystemActivated = false;
            }
        }

        public void PrintOwnTemperatureInformation()
        {
            Console.WriteLine("Temperature Sensor: " + this.temperature + "ºC");
            if (this.coldSystemActivated)
            {
                Console.WriteLine("Temperature Activator: Cold System activated.");
            }
            if (this.heatSystemActivated)
            {
                Console.WriteLine("Temperature Activator: Heat System activated.");
            }
        }

        // Métodos para iluminación

        private void SendIlluminationMessage(ISession session, IMessageProducer producer)
        {
            string jsonPayload = "{" +
                "\"office\": \"office1\"," +
                "\"illumination_intensity\": " + this.illuminationIntensity + "," +
                "\"increase_intensity_regulator_activated\": " + this.increaseIntensityIlluminationRegulatorActivated.ToString().ToLower() + "," +
                "\"decrease_intensity_regulator_activated\": " + this.decreaseIntensityIlluminationRegulatorActivated.ToString().ToLower() +
                "}";
            ITextMessage message = session.CreateTextMessage(jsonPayload);
            producer.Send(message);
        }

        public void ManageIncreaseIlluminationRegulator(ISession session, IMessageProducer producer)
        {
            if (!this.increaseIntensityIlluminationRegulatorActivated)
            {
                this.increaseIntensityIlluminationRegulatorActivated = true;
            }
            this.illuminationIntensity += ILLUMINATION_INTENSITY_DIFFERENCE;

            string jsonPayload = "{" +
                "\"office\": \"office1\"," +
                "\"illumination_intensity\": " + this.illuminationIntensity + "," +
                "\"increase_intensity_regulator_activated\": " + this.increaseIntensityIlluminationRegulatorActivated.ToString().ToLower() + "," +
                "\"decrease_intensity_regulator_activated\": " + this.decreaseIntensityIlluminationRegulatorActivated.ToString().ToLower() +
                "}";
            ITextMessage message = session.CreateTextMessage(jsonPayload);
            producer.Send(message);

            if (this.illuminationIntensity >= INCREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY)
            {
                this.increaseIntensityIlluminationRegulatorActivated = false;
            }
        }

        public void ManageDecreaseIlluminationRegulator(ISession session, IMessageProducer producer)
        {
            if (!this.decreaseIntensityIlluminationRegulatorActivated)
            {
                this.decreaseIntensityIlluminationRegulatorActivated = true;
            }
            this.illuminationIntensity -= ILLUMINATION_INTENSITY_DIFFERENCE;

            string jsonPayload = "{" +
                "\"office\": \"office1\"," +
                "\"illumination_intensity\": " + this.illuminationIntensity + "," +
                "\"increase_intensity_regulator_activated\": " + this.increaseIntensityIlluminationRegulatorActivated.ToString().ToLower() + "," +
                "\"decrease_intensity_regulator_activated\": " + this.decreaseIntensityIlluminationRegulatorActivated.ToString().ToLower() +
                "}";
            ITextMessage message = session.CreateTextMessage(jsonPayload);
            producer.Send(message);

            if (this.illuminationIntensity <= DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY)
            {
                this.decreaseIntensityIlluminationRegulatorActivated = false;
            }
        }

        public bool? GetActivateIncreaseIntensityFlag(string text)
        {
            JObject json = JObject.Parse(text);
            if (json["activate_increase_intensity_illumination_regulator"] == null)
            {
                return null;
            }
            else
            {
                return (bool)json["activate_increase_intensity_illumination_regulator"];
            }
        }

        public bool? GetStopIncreaseIntensityFlag(string text)
        {
            JObject json = JObject.Parse(text);
            if (json["stop_increase_intensity_illumination_regulator"] == null)
            {
                return null;
            }
            else
            {
                return (bool)json["stop_increase_intensity_illumination_regulator"];
            }
        }

        public bool? GetActivateDecreaseIntensityFlag(string text)
        {
            JObject json = JObject.Parse(text);
            if (json["activate_decrease_intensity_illumination_regulator"] == null)
            {
                return null;
            }
            else
            {
                return (bool)json["activate_decrease_intensity_illumination_regulator"];
            }
        }

        public bool? GetStopDecreaseIntensityFlag(string text)
        {
            JObject json = JObject.Parse(text);
            if (json["stop_decrease_intensity_illumination_regulator"] == null)
            {
                return null;
            }
            else
            {
                return (bool)json["stop_decrease_intensity_illumination_regulator"];
            }
        }

        public void ManageIlluminationFlags(string text)
        {
            bool? activateIncrease = GetActivateIncreaseIntensityFlag(text);
            bool? stopIncrease = GetStopIncreaseIntensityFlag(text);
            bool? activateDecrease = GetActivateDecreaseIntensityFlag(text);
            bool? stopDecrease = GetStopDecreaseIntensityFlag(text);

            if (activateIncrease.HasValue && activateIncrease.Value)
            {
                this.increaseIntensityIlluminationRegulatorActivated = true;
            }
            if (stopIncrease.HasValue && stopIncrease.Value)
            {
                this.increaseIntensityIlluminationRegulatorActivated = false;
            }
            if (activateDecrease.HasValue && activateDecrease.Value)
            {
                this.decreaseIntensityIlluminationRegulatorActivated = true;
            }
            if (stopDecrease.HasValue && stopDecrease.Value)
            {
                this.decreaseIntensityIlluminationRegulatorActivated = false;
            }
        }

        public void PrintOwnIlluminationInformation()
        {
            Console.WriteLine("Illumination Sensor: " + this.illuminationIntensity + " lumens");
            if (this.increaseIntensityIlluminationRegulatorActivated)
            {
                Console.WriteLine("Illumination Activator: Increase Intensity Regulator activated.");
            }
            if (this.decreaseIntensityIlluminationRegulatorActivated)
            {
                Console.WriteLine("Illumination Activator: Decrease Intensity Regulator activated.");
            }
        }

        public void PrintReceivedIlluminationInformation(string text)
        {
            if (GetActivateIncreaseIntensityFlag(text).HasValue && GetActivateIncreaseIntensityFlag(text).Value)
            {
                Console.WriteLine("Illumination Activator: Illumination intensity is below threshold (below 1500 lumens). Activating Increase Intensity Regulator...");
            }
            if (GetStopIncreaseIntensityFlag(text).HasValue && GetStopIncreaseIntensityFlag(text).Value)
            {
                Console.WriteLine("Illumination Activator: Illumination intensity has reached desired level (around 2300 lumens). Stopping Increase Intensity Regulator...");
            }
            if (GetActivateDecreaseIntensityFlag(text).HasValue && GetActivateDecreaseIntensityFlag(text).Value)
            {
                Console.WriteLine("Illumination Activator: Illumination intensity is above threshold (above 3000 lumens). Activating Decrease Intensity Regulator...");
            }
            if (GetStopDecreaseIntensityFlag(text).HasValue && GetStopDecreaseIntensityFlag(text).Value)
            {
                Console.WriteLine("Illumination Activator: Illumination intensity has reached desired level (around 2300 lumens). Stopping Decrease Intensity Regulator...");
            }
        }

        // Implementación del listener de mensajes
        public void OnMessage(IMessage message)
        {
            try
            {
                if (message is ITextMessage textMessage)
                {
                    string text = textMessage.Text;
                    JObject json = JObject.Parse(text);
                    if (json["temperature"] != null)
                    {
                        ManageTemperatureFlags(text);
                        PrintReceivedTemperatureInformation(text);
                    }
                    if (json["illumination_intensity"] != null)
                    {
                        ManageIlluminationFlags(text);
                        PrintReceivedIlluminationInformation(text);
                    }
                }
                else
                {
                    Console.WriteLine("Received message of type: " + message.GetType().Name);
                }
            }
            catch (Exception e)
            {
                Console.WriteLine("Got an Exception in OnMessage: " + e.Message);
            }
        }

        public void PrintReceivedTemperatureInformation(string text)
        {
            if (GetActivateColdSystemFlag(text).HasValue && GetActivateColdSystemFlag(text).Value)
            {
                Console.WriteLine("Temperature Activator: Temperature has exceeded 30ºC. Activating Cold System...");
            }
            if (GetStopColdSystemFlag(text).HasValue && GetStopColdSystemFlag(text).Value)
            {
                Console.WriteLine("Temperature Activator: Temperature has reached 23ºC or less. Stopping Cold System...");
            }
            if (GetActivateHeatSystemFlag(text).HasValue && GetActivateHeatSystemFlag(text).Value)
            {
                Console.WriteLine("Temperature Activator: Temperature is below 15ºC. Activating Heat System...");
            }
            if (GetStopHeatSystemFlag(text).HasValue && GetStopHeatSystemFlag(text).Value)
            {
                Console.WriteLine("Temperature Activator: Temperature has reached 23ºC or more. Stopping Heat System...");
            }
        }

        /// <summary>
        /// Inicia la ejecución de las tareas asíncronas para temperatura e iluminación.
        /// </summary>
        public async void Start()
        {
            JMSComponents jmsComponents = SetupJMS();

            // Tarea para temperatura
            CancellationTokenSource cts = new CancellationTokenSource();
            Task temperatureTask = Task.Run(async () =>
            {
                while (!cts.Token.IsCancellationRequested)
                {
                    try
                    {
                        if (!this.coldSystemActivated && !this.heatSystemActivated)
                        {
                            this.temperature = Utils.ManejarTemperaturaRandomIndicator();
                        }
                        if (this.heatSystemActivated)
                        {
                            ManageHeatSystem(jmsComponents.Session, jmsComponents.TemperatureProducer);
                        }
                        else if (this.coldSystemActivated)
                        {
                            ManageColdSystem(jmsComponents.Session, jmsComponents.TemperatureProducer);
                        }
                        else
                        {
                            SendTemperatureMessage(jmsComponents.Session, jmsComponents.TemperatureProducer);
                        }
                        PrintOwnTemperatureInformation();
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine(ex);
                    }
                    await Task.Delay(5000);
                }
            }, cts.Token);

            // Tarea para iluminación
            Task illuminationTask = Task.Run(async () =>
            {
                while (!cts.Token.IsCancellationRequested)
                {
                    try
                    {
                        if (!this.increaseIntensityIlluminationRegulatorActivated && !this.decreaseIntensityIlluminationRegulatorActivated)
                        {
                            this.illuminationIntensity = Utils.ManejarIlluminationIntensityRandomIndicator();
                        }
                        if (this.increaseIntensityIlluminationRegulatorActivated)
                        {
                            ManageIncreaseIlluminationRegulator(jmsComponents.Session, jmsComponents.IlluminationProducer);
                        }
                        else if (this.decreaseIntensityIlluminationRegulatorActivated)
                        {
                            ManageDecreaseIlluminationRegulator(jmsComponents.Session, jmsComponents.IlluminationProducer);
                        }
                        else
                        {
                            SendIlluminationMessage(jmsComponents.Session, jmsComponents.IlluminationProducer);
                        }
                        PrintOwnIlluminationInformation();
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine(ex);
                    }
                    await Task.Delay(5000);
                }
            }, cts.Token);

            // Se espera a que ambas tareas se ejecuten (en este ejemplo la aplicación se mantiene en ejecución)
            await Task.WhenAll(temperatureTask, illuminationTask);
        }

        public static void Main(string[] args)
        {
            Oficina1 oficina = new Oficina1();
            oficina.Start();
            // Se previene que la aplicación se cierre de inmediato.
            Console.ReadLine();
        }
    }
}
