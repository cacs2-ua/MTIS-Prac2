import json
import stomp
import threading

class ConsolaCentral(stomp.ConnectionListener):
    COLD_SYSTEM_ACTIVATION_TEMPERATURE = 30
    COLD_SYSTEM_STOP_TEMPERATURE = 23

    HEAT_SYSTEM_ACTIVATION_TEMPERATURE = 15
    HEAT_SYSTEM_STOP_TEMPERATURE = 23

    def __init__(self, conn, lecturas_dest, actuador_dest):
        self.conn = conn
        self.lecturas_dest = lecturas_dest
        self.actuador_dest = actuador_dest
        

    def getOfficeFromMessage(self, data):
        return data.get("office")    

    def getTemperatureFromMessage(self, data):
        return data.get("temperature")
    
    def getColdSystemActivatedFlagFromMessage(self, data):
        return data.get("cold_system_activated")

    def getHeatSystemActivatedFlagFromMessage(self, data):
        return data.get("heat_system_activated")
    

    def printOffice1TemperatureInformation(self, data):
        temperature = self.getTemperatureFromMessage(data)

        print(f"Office 1 temperature: {temperature}°C")

        if (temperature > self.COLD_SYSTEM_ACTIVATION_TEMPERATURE and not self.getColdSystemActivatedFlagFromMessage(data)):
            print("Temperature in Office1 has exceeded 30ºC. Requesting Office1 to activate Cold System...")
        
        if (temperature > self.COLD_SYSTEM_STOP_TEMPERATURE and self.getColdSystemActivatedFlagFromMessage(data)):
            print("Cold System is activated in Office1. ")
        
        if (temperature <= self.COLD_SYSTEM_STOP_TEMPERATURE and self.getColdSystemActivatedFlagFromMessage(data)):
            print("Temperature in Office1 has reached 23ºC or less. Requesting Office1 to stop Cold System...")
        
        if (temperature < self.HEAT_SYSTEM_ACTIVATION_TEMPERATURE and not self.getHeatSystemActivatedFlagFromMessage(data)):
            print("Temperature in Office1 is below 15ºC. Requesting Office1 to activate Heat System...")
        
        if (temperature < self.HEAT_SYSTEM_STOP_TEMPERATURE and self.getHeatSystemActivatedFlagFromMessage(data)):
            print("Heat System is activated in Office1. ")
        
        if (temperature >= self.HEAT_SYSTEM_STOP_TEMPERATURE and self.getHeatSystemActivatedFlagFromMessage(data)):
            print("Temperature in Office1 has reached 23ºC or more. Requesting Office1 to stop Heat System...")

    def on_message(self, frame):
        if frame.body.strip() != "":
            try:
                data = json.loads(frame.body)
            except json.JSONDecodeError:
                print("Error decoding JSON.")
                return

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

            # Llamar directamente al método para gestionar el sistema de temperatura
            self.manage_temperature_system(
                office,
                temperature_numeric_value,
                cold_system_activated,
                heat_system_activated
            )

            self.printOffice1TemperatureInformation(data)

    def manage_temperature_system(self, office, temperature_numeric_value, cold_system_activated, heat_system_activated):
        # Basado en la temperatura recibida, enviar comandos según corresponda
        if (temperature_numeric_value > self.COLD_SYSTEM_ACTIVATION_TEMPERATURE and not cold_system_activated):
            self.conn.send(
                destination=self.actuador_dest, 
                body=json.dumps({
                    "activate_cold_system": True,
                    "temperature": temperature_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )
        
        elif (temperature_numeric_value <= self.COLD_SYSTEM_STOP_TEMPERATURE and cold_system_activated):
            self.conn.send(
                destination=self.actuador_dest, 
                body=json.dumps({
                    "stop_cold_system": True,
                    "temperature": temperature_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )
        
        elif (temperature_numeric_value < self.HEAT_SYSTEM_ACTIVATION_TEMPERATURE and not heat_system_activated):
            self.conn.send(
                destination=self.actuador_dest, 
                body=json.dumps({
                    "activate_heat_system": True,
                    "temperature": temperature_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )

        elif (temperature_numeric_value >= self.HEAT_SYSTEM_STOP_TEMPERATURE and heat_system_activated):
            self.conn.send(
                destination=self.actuador_dest, 
                body=json.dumps({
                    "stop_heat_system": True,
                    "temperature": temperature_numeric_value
                }),
                headers={"content-type": "application/json", "amq-msg-type": "text"}
            )

    @staticmethod
    def setup_connection():
        conn = stomp.Connection12(host_and_ports=[("localhost", 61613)])
        lecturas_dest = "/topic/lecturas_temperaturas_oficina1"
        actuador_dest = "/topic/actuador_temperatura_oficina1"
        
        # Conectar y configurar el listener asíncrono
        conn.connect(wait=True)
        listener = ConsolaCentral(conn, lecturas_dest, actuador_dest)
        conn.set_listener('', listener)
        conn.subscribe(destination=lecturas_dest, id=1, ack='auto')
        
        print("Waiting for asynchronous messages. Press Ctrl+C to exit...")
        return conn, listener

def main():
    conn, listener = ConsolaCentral.setup_connection()
    try:
        # En lugar de un bucle de sondeo con time.sleep, se espera de forma indefinida sin bloquear con pausas fijas.
        threading.Event().wait()
    except KeyboardInterrupt:
        print("Disconnecting...")
        conn.disconnect()

if __name__ == "__main__":
    main()
