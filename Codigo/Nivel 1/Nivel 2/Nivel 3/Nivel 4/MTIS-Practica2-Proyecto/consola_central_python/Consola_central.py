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

    def __init__(self, conn, lecturas_temp_dest, actuador_temp_dest, lecturas_illum_dest, actuador_illum_dest):
        self.conn = conn
        self.lecturas_temp_dest = lecturas_temp_dest
        self.actuador_temp_dest = actuador_temp_dest
        self.lecturas_illum_dest = lecturas_illum_dest
        self.actuador_illum_dest = actuador_illum_dest

    def getOfficeFromMessage(self, data):
        return data.get("office")    

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
        # Increase regulator conditions (activation and deactivation)
        if (illumination < self.INCREASE_INTENSITY_ILLUMINATION_REGULATOR_ACTIVATION_INTENSITY and 
            not self.getIncreaseIntensityRegulatorActivatedFlagFromMessage(data)):
            print("Illumination in Office1 is below threshold (below 1500 lumens). Requesting Office1 to activate Increase Intensity Regulator...")
        if (illumination < self.DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY and 
            self.getIncreaseIntensityRegulatorActivatedFlagFromMessage(data)):
            print("Increase Intensity Regulator is activated in Office1.")
        if (illumination >= self.INCREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY and 
            self.getIncreaseIntensityRegulatorActivatedFlagFromMessage(data)):
            print("Illumination in Office1 has reached desired level (around 2300 lumens). Requesting Office1 to stop Increase Intensity Regulator...")
        # Decrease regulator conditions (activation and deactivation)
        if (illumination > self.DECREASE_INTENSITY_ILLUMINATION_REGULATOR_ACTIVATION_INTENSITY and 
            not self.getDecreaseIntensityRegulatorActivatedFlagFromMessage(data)):
            print("Illumination in Office1 is above threshold (above 3000 lumens). Requesting Office1 to activate Decrease Intensity Regulator...")
        if (illumination > self.DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY and 
            self.getDecreaseIntensityRegulatorActivatedFlagFromMessage(data)):
            print("Decrease Intensity Regulator is activated in Office1.")
        if (illumination <= self.DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY and 
            self.getDecreaseIntensityRegulatorActivatedFlagFromMessage(data)):
            print("Illumination in Office1 has reached desired level (around 2300 lumens). Requesting Office1 to stop Decrease Intensity Regulator...")

    def on_message(self, frame):
        if frame.body.strip() != "":
            try:
                data = json.loads(frame.body)
            except json.JSONDecodeError:
                print("Error decoding JSON.")
                return

            if "temperature" in data:
                office = data.get("office")
                temperature = data.get("temperature")
                try:
                    temperature_numeric_value = int(temperature) if temperature is not None else None
                except ValueError:
                    temperature_numeric_value = None
                cold_system_activated = data.get("cold_system_activated")
                heat_system_activated = data.get("heat_system_activated")
                if temperature_numeric_value is None:
                    print("Temperature value missing or invalid.")
                    return
                self.manage_temperature_system(office, temperature_numeric_value, cold_system_activated, heat_system_activated)
                self.printOffice1TemperatureInformation(data)
            if "illumination_intensity" in data:
                office = data.get("office")
                illumination = data.get("illumination_intensity")
                try:
                    illumination_numeric_value = int(illumination) if illumination is not None else None
                except ValueError:
                    illumination_numeric_value = None
                increase_regulator_activated = data.get("increase_intensity_regulator_activated")
                decrease_regulator_activated = data.get("decrease_intensity_regulator_activated")
                if illumination_numeric_value is None:
                    print("Illumination value missing or invalid.")
                    return
                self.manage_illumination_system(office, illumination_numeric_value, increase_regulator_activated, decrease_regulator_activated)
                self.printOffice1IlluminationInformation(data)

    def manage_temperature_system(self, office, temperature_numeric_value, cold_system_activated, heat_system_activated):
        if (temperature_numeric_value > self.COLD_SYSTEM_ACTIVATION_TEMPERATURE and not cold_system_activated):
            self.conn.send(
                destination=self.actuador_temp_dest, 
                body=json.dumps({
                    "activate_cold_system": True,
                    "temperature": temperature_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )
        elif (temperature_numeric_value <= self.COLD_SYSTEM_STOP_TEMPERATURE and cold_system_activated):
            self.conn.send(
                destination=self.actuador_temp_dest, 
                body=json.dumps({
                    "stop_cold_system": True,
                    "temperature": temperature_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )
        elif (temperature_numeric_value < self.HEAT_SYSTEM_ACTIVATION_TEMPERATURE and not heat_system_activated):
            self.conn.send(
                destination=self.actuador_temp_dest, 
                body=json.dumps({
                    "activate_heat_system": True,
                    "temperature": temperature_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )
        elif (temperature_numeric_value >= self.HEAT_SYSTEM_STOP_TEMPERATURE and heat_system_activated):
            self.conn.send(
                destination=self.actuador_temp_dest, 
                body=json.dumps({
                    "stop_heat_system": True,
                    "temperature": temperature_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )

    def manage_illumination_system(self, office, illumination_numeric_value, increase_regulator_activated, decrease_regulator_activated):
        if (illumination_numeric_value < self.INCREASE_INTENSITY_ILLUMINATION_REGULATOR_ACTIVATION_INTENSITY and not increase_regulator_activated):
            self.conn.send(
                destination=self.actuador_illum_dest, 
                body=json.dumps({
                    "activate_increase_intensity_illumination_regulator": True,
                    "illumination_intensity": illumination_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )
        elif (illumination_numeric_value >= self.INCREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY and increase_regulator_activated):
            self.conn.send(
                destination=self.actuador_illum_dest, 
                body=json.dumps({
                    "stop_increase_intensity_illumination_regulator": True,
                    "illumination_intensity": illumination_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )
        elif (illumination_numeric_value > self.DECREASE_INTENSITY_ILLUMINATION_REGULATOR_ACTIVATION_INTENSITY and not decrease_regulator_activated):
            self.conn.send(
                destination=self.actuador_illum_dest, 
                body=json.dumps({
                    "activate_decrease_intensity_illumination_regulator": True,
                    "illumination_intensity": illumination_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )
        elif (illumination_numeric_value <= self.DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY and decrease_regulator_activated):
            self.conn.send(
                destination=self.actuador_illum_dest, 
                body=json.dumps({
                    "stop_decrease_intensity_illumination_regulator": True,
                    "illumination_intensity": illumination_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )

    @staticmethod
    def setup_connection():
        conn = stomp.Connection12(host_and_ports=[("localhost", 61613)])
        lecturas_temp_dest = "/topic/lecturas_temperaturas_oficina1"
        actuador_temp_dest = "/topic/actuador_temperatura_oficina1"
        lecturas_illum_dest = "/topic/lecturas_iluminacion_oficina1"
        actuador_illum_dest = "/topic/actuador_iluminacion_oficina1"
        
        conn.connect(wait=True)
        listener = ConsolaCentral(conn, lecturas_temp_dest, actuador_temp_dest, lecturas_illum_dest, actuador_illum_dest)
        conn.set_listener('', listener)
        conn.subscribe(destination=lecturas_temp_dest, id=1, ack='auto')
        conn.subscribe(destination=lecturas_illum_dest, id=2, ack='auto')
        
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
