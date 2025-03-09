import json
import stomp
import threading

class ConsolaCentral(stomp.ConnectionListener):
    HEAT_SYSTEM_ACTIVATION_TEMPERATURE = 15
    HEAT_SYSTEM_STOP_TEMPERATURE = 23

    COLD_SYSTEM_ACTIVATION_TEMPERATURE = 30
    COLD_SYSTEM_STOP_TEMPERATURE = 23

    INCREASE_INTENSITY_ILLUMINATION_REGULATOR_ACTIVATION_INTENSITY = 1500
    INCREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY = 2300
    
    DECREASE_INTENSITY_ILLUMINATION_REGULATOR_ACTIVATION_INTENSITY = 3000
    DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY = 2300

    def __init__(self, conn, 
                 lecturas_temp_dest1, actuador_temp_dest1,
                 lecturas_temp_dest2, actuador_temp_dest2,
                 lecturas_illum_dest1, actuador_illum_dest1,
                 lecturas_illum_dest2, actuador_illum_dest2):
        self.conn = conn
        # Office1 topics
        self.lecturas_temp_dest1 = lecturas_temp_dest1
        self.actuador_temp_dest1 = actuador_temp_dest1
        self.lecturas_illum_dest1 = lecturas_illum_dest1
        self.actuador_illum_dest1 = actuador_illum_dest1
        # Office2 topics
        self.lecturas_temp_dest2 = lecturas_temp_dest2
        self.actuador_temp_dest2 = actuador_temp_dest2
        self.lecturas_illum_dest2 = lecturas_illum_dest2
        self.actuador_illum_dest2 = actuador_illum_dest2

    def getTemperatureFromMessage(self, data):
        return data.get("temperature")

    def getColdSystemActivatedFlagFromMessage(self, data):
        return data.get("cold_system_activated")

    def getHeatSystemActivatedFlagFromMessage(self, data):
        return data.get("heat_system_activated")

    def getIlluminationIntensityFromMessage(self, data):
        return data.get("illumination_intensity")

    def getIncreaseIntensityRegulatorActivatedFlagFromMessage(self, data):
        return data.get("increase_intensity_regulator_activated")

    def getDecreaseIntensityRegulatorActivatedFlagFromMessage(self, data):
        return data.get("decrease_intensity_regulator_activated")

    def printOffice1TemperatureInformation(self, data):
        temperature = self.getTemperatureFromMessage(data)
        print(f"Office 1 temperature: {temperature}°C")
        if (temperature > self.COLD_SYSTEM_ACTIVATION_TEMPERATURE and not self.getColdSystemActivatedFlagFromMessage(data)):
            print("Temperature in Office1 has exceeded 30ºC. Requesting Office1 to activate Cold System...")
        if (temperature > self.COLD_SYSTEM_STOP_TEMPERATURE and self.getColdSystemActivatedFlagFromMessage(data)):
            print("Cold System is activated in Office1.")
        if (temperature <= self.COLD_SYSTEM_STOP_TEMPERATURE and self.getColdSystemActivatedFlagFromMessage(data)):
            print("Temperature in Office1 has reached 23ºC or less. Requesting Office1 to stop Cold System...")
        if (temperature < self.HEAT_SYSTEM_ACTIVATION_TEMPERATURE and not self.getHeatSystemActivatedFlagFromMessage(data)):
            print("Temperature in Office1 is below 15ºC. Requesting Office1 to activate Heat System...")
        if (temperature < self.HEAT_SYSTEM_STOP_TEMPERATURE and self.getHeatSystemActivatedFlagFromMessage(data)):
            print("Heat System is activated in Office1.")
        if (temperature >= self.HEAT_SYSTEM_STOP_TEMPERATURE and self.getHeatSystemActivatedFlagFromMessage(data)):
            print("Temperature in Office1 has reached 23ºC or more. Requesting Office1 to stop Heat System...")

    def printOffice1IlluminationInformation(self, data):
        illumination = self.getIlluminationIntensityFromMessage(data)
        print(f"Office 1 illumination: {illumination} lumens")
        if (illumination < self.INCREASE_INTENSITY_ILLUMINATION_REGULATOR_ACTIVATION_INTENSITY and 
            not self.getIncreaseIntensityRegulatorActivatedFlagFromMessage(data)):
            print("Illumination in Office1 is below threshold (below 1500 lumens). Requesting Office1 to activate Increase Intensity Regulator...")
        if (illumination < self.DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY and 
            self.getIncreaseIntensityRegulatorActivatedFlagFromMessage(data)):
            print("Increase Intensity Regulator is activated in Office1.")
        if (illumination >= self.INCREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY and 
            self.getIncreaseIntensityRegulatorActivatedFlagFromMessage(data)):
            print("Illumination in Office1 has reached desired level (around 2300 lumens). Requesting Office1 to stop Increase Intensity Regulator...")
        if (illumination > self.DECREASE_INTENSITY_ILLUMINATION_REGULATOR_ACTIVATION_INTENSITY and 
            not self.getDecreaseIntensityRegulatorActivatedFlagFromMessage(data)):
            print("Illumination in Office1 is above threshold (above 3000 lumens). Requesting Office1 to activate Decrease Intensity Regulator...")
        if (illumination > self.DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY and 
            self.getDecreaseIntensityRegulatorActivatedFlagFromMessage(data)):
            print("Decrease Intensity Regulator is activated in Office1.")
        if (illumination <= self.DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY and 
            self.getDecreaseIntensityRegulatorActivatedFlagFromMessage(data)):
            print("Illumination in Office1 has reached desired level (around 2300 lumens). Requesting Office1 to stop Decrease Intensity Regulator...")

    def printOffice2TemperatureInformation(self, data):
        temperature = data.get("temperature")
        print(f"Office 2 temperature: {temperature}°C")
        if (temperature > self.COLD_SYSTEM_ACTIVATION_TEMPERATURE and not data.get("cold_system_activated")):
            print("Temperature in Office2 has exceeded 30C. Requesting Office2 to activate Cold System...")
        if (temperature > self.COLD_SYSTEM_STOP_TEMPERATURE and data.get("cold_system_activated")):
            print("Cold System is activated in Office2.")
        if (temperature <= self.COLD_SYSTEM_STOP_TEMPERATURE and data.get("cold_system_activated")):
            print("Temperature in Office2 has reached 23C or less. Requesting Office2 to stop Cold System...")
        if (temperature < self.HEAT_SYSTEM_ACTIVATION_TEMPERATURE and not data.get("heat_system_activated")):
            print("Temperature in Office2 is below 15C. Requesting Office2 to activate Heat System...")
        if (temperature < self.HEAT_SYSTEM_STOP_TEMPERATURE and data.get("heat_system_activated")):
            print("Heat System is activated in Office2.")
        if (temperature >= self.HEAT_SYSTEM_STOP_TEMPERATURE and data.get("heat_system_activated")):
            print("Temperature in Office2 has reached 23C or more. Requesting Office2 to stop Heat System...")

    def printOffice2IlluminationInformation(self, data):
        illumination = data.get("illumination_intensity")
        print(f"Office 2 illumination: {illumination} lumens")
        if (illumination < self.INCREASE_INTENSITY_ILLUMINATION_REGULATOR_ACTIVATION_INTENSITY and not data.get("increase_intensity_regulator_activated")):
            print("Illumination in Office2 is below threshold (below 1500 lumens). Requesting Office2 to activate Increase Intensity Regulator...")
        if (illumination < self.DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY and data.get("increase_intensity_regulator_activated")):
            print("Increase Intensity Regulator is activated in Office2.")
        if (illumination >= self.INCREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY and data.get("increase_intensity_regulator_activated")):
            print("Illumination in Office2 has reached desired level (around 2300 lumens). Requesting Office2 to stop Increase Intensity Regulator...")
        if (illumination > self.DECREASE_INTENSITY_ILLUMINATION_REGULATOR_ACTIVATION_INTENSITY and not data.get("decrease_intensity_regulator_activated")):
            print("Illumination in Office2 is above threshold (above 3000 lumens). Requesting Office2 to activate Decrease Intensity Regulator...")
        if (illumination > self.DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY and data.get("decrease_intensity_regulator_activated")):
            print("Decrease Intensity Regulator is activated in Office2.")
        if (illumination <= self.DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY and data.get("decrease_intensity_regulator_activated")):
            print("Illumination in Office2 has reached desired level (around 2300 lumens). Requesting Office2 to stop Decrease Intensity Regulator...")

    def manage_temperature_system(self, office, temperature_numeric_value, cold_system_activated, heat_system_activated, actuador_dest):
        if (temperature_numeric_value > self.COLD_SYSTEM_ACTIVATION_TEMPERATURE and not cold_system_activated):
            self.conn.send(
                destination=actuador_dest,
                body=json.dumps({
                    "activate_cold_system": True,
                    "temperature": temperature_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )
        elif (temperature_numeric_value <= self.COLD_SYSTEM_STOP_TEMPERATURE and cold_system_activated):
            self.conn.send(
                destination=actuador_dest,
                body=json.dumps({
                    "stop_cold_system": True,
                    "temperature": temperature_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )
        elif (temperature_numeric_value < self.HEAT_SYSTEM_ACTIVATION_TEMPERATURE and not heat_system_activated):
            self.conn.send(
                destination=actuador_dest,
                body=json.dumps({
                    "activate_heat_system": True,
                    "temperature": temperature_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )
        elif (temperature_numeric_value >= self.HEAT_SYSTEM_STOP_TEMPERATURE and heat_system_activated):
            self.conn.send(
                destination=actuador_dest,
                body=json.dumps({
                    "stop_heat_system": True,
                    "temperature": temperature_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )

    def manage_illumination_system(self, office, illumination_numeric_value, increase_regulator_activated, decrease_regulator_activated, actuador_dest):
        if (illumination_numeric_value < self.INCREASE_INTENSITY_ILLUMINATION_REGULATOR_ACTIVATION_INTENSITY and not increase_regulator_activated):
            self.conn.send(
                destination=actuador_dest,
                body=json.dumps({
                    "activate_increase_intensity_illumination_regulator": True,
                    "illumination_intensity": illumination_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )
        elif (illumination_numeric_value >= self.INCREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY and increase_regulator_activated):
            self.conn.send(
                destination=actuador_dest,
                body=json.dumps({
                    "stop_increase_intensity_illumination_regulator": True,
                    "illumination_intensity": illumination_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )
        elif (illumination_numeric_value > self.DECREASE_INTENSITY_ILLUMINATION_REGULATOR_ACTIVATION_INTENSITY and not decrease_regulator_activated):
            self.conn.send(
                destination=actuador_dest,
                body=json.dumps({
                    "activate_decrease_intensity_illumination_regulator": True,
                    "illumination_intensity": illumination_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )
        elif (illumination_numeric_value <= self.DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY and decrease_regulator_activated):
            self.conn.send(
                destination=actuador_dest,
                body=json.dumps({
                    "stop_decrease_intensity_illumination_regulator": True,
                    "illumination_intensity": illumination_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )

    def on_message(self, frame):
        if frame.body.strip() != "":
            try:
                data = json.loads(frame.body)
            except json.JSONDecodeError:
                print("Error decoding JSON.")
                return

            office = data.get("office")
            if office == "office1" or office == "office2":
                # Process temperature messages if present
                if "temperature" in data:
                    try:
                        temperature_numeric_value = int(data.get("temperature"))
                    except (ValueError, TypeError):
                        print("Temperature value missing or invalid.")
                    else:
                        if office == "office1":
                            self.manage_temperature_system(office, temperature_numeric_value,
                                                           data.get("cold_system_activated"),
                                                           data.get("heat_system_activated"),
                                                           self.actuador_temp_dest1)
                            self.printOffice1TemperatureInformation(data)
                        elif office == "office2":
                            self.manage_temperature_system(office, temperature_numeric_value,
                                                           data.get("cold_system_activated"),
                                                           data.get("heat_system_activated"),
                                                           self.actuador_temp_dest2)
                            self.printOffice2TemperatureInformation(data)
                # Process illumination messages if present
                if "illumination_intensity" in data:
                    try:
                        illumination_numeric_value = int(data.get("illumination_intensity"))
                    except (ValueError, TypeError):
                        print("Illumination value missing or invalid.")
                    else:
                        if office == "office1":
                            self.manage_illumination_system(office,
                                                           illumination_numeric_value,
                                                           data.get("increase_intensity_regulator_activated"),
                                                           data.get("decrease_intensity_regulator_activated"),
                                                           self.actuador_illum_dest1)
                            self.printOffice1IlluminationInformation(data)
                        elif office == "office2":
                            self.manage_illumination_system(office,
                                                           illumination_numeric_value,
                                                           data.get("increase_intensity_regulator_activated"),
                                                           data.get("decrease_intensity_regulator_activated"),
                                                           self.actuador_illum_dest2)
                            self.printOffice2IlluminationInformation(data)
            else:
                print("Unknown office value.")
        else:
            print("Received empty message.")

    @staticmethod
    def setup_connection():
        conn = stomp.Connection12(host_and_ports=[("localhost", 61613)])
        # Office1 topics
        lecturas_temp_dest1 = "/topic/lecturas_temperaturas_oficina1"
        actuador_temp_dest1 = "/topic/actuador_temperatura_oficina1"
        lecturas_illum_dest1 = "/topic/lecturas_iluminacion_oficina1"
        actuador_illum_dest1 = "/topic/actuador_iluminacion_oficina1"
        # Office2 topics
        lecturas_temp_dest2 = "/topic/lecturas_temperaturas_oficina2"
        actuador_temp_dest2 = "/topic/actuador_temperatura_oficina2"
        lecturas_illum_dest2 = "/topic/lecturas_iluminacion_oficina2"
        actuador_illum_dest2 = "/topic/actuador_iluminacion_oficina2"
        
        conn.connect('mtis', 'mtis', wait=True)
        listener = ConsolaCentral(conn, lecturas_temp_dest1, actuador_temp_dest1,
                                    lecturas_temp_dest2, actuador_temp_dest2,
                                    lecturas_illum_dest1, actuador_illum_dest1,
                                    lecturas_illum_dest2, actuador_illum_dest2)
        conn.set_listener('', listener)
        # Subscribe to all reading topics
        conn.subscribe(destination=lecturas_temp_dest1, id=1, ack='auto')
        conn.subscribe(destination=lecturas_temp_dest2, id=2, ack='auto')
        conn.subscribe(destination=lecturas_illum_dest1, id=3, ack='auto')
        conn.subscribe(destination=lecturas_illum_dest2, id=4, ack='auto')
        
        print("Waiting for asynchronous messages. Press Ctrl+C to exit...")
        return conn, listener

def main():
    conn, listener = ConsolaCentral.setup_connection()
    try:
        threading.Event().wait()
    except KeyboardInterrupt:
        print("Disconnecting...")
        conn.disconnect()

if __name__ == "__main__":
    main()
