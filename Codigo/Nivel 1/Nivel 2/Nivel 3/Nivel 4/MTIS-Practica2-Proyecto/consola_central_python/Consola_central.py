import json
import stomp
import threading

class ConsolaCentral(stomp.ConnectionListener):
    COLD_SYSTEM_ATIVATION_TEMPERATURE = 30
    COLD_SYSTEM_STOP_TEMPERATURE = 23

    HEAT_SYSTEM_ATIVATION_TEMPERATURE = 15
    HEAT_SYSTEM_STOP_TEMPERATURE = 23

    def __init__(self, conn, lecturas_dest, actuador_dest):
        self.conn = conn
        self.lecturas_dest = lecturas_dest
        self.actuador_dest = actuador_dest

    def on_message(self, frame):
        if frame.body.strip() != "":
            print(f"Received message: {frame.body}")
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

    def manage_temperature_system(self, office, temperature_numeric_value, cold_system_activated, heat_system_activated):
        # Basado en la temperatura recibida, enviar comandos según corresponda
        if (temperature_numeric_value > self.COLD_SYSTEM_ATIVATION_TEMPERATURE and not cold_system_activated):
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
        
        elif (temperature_numeric_value < self.HEAT_SYSTEM_ATIVATION_TEMPERATURE and not heat_system_activated):
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
