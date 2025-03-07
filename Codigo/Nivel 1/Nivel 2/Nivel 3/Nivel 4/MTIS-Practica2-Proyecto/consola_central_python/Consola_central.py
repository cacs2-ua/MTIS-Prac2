import time
import stomp
import json
import threading

# Global variable to store the last received message 
latest_temperature_message = None
latest_temperature_message_lock = threading.Lock()

class ConsolaCentral(stomp.ConnectionListener):
    COLD_SYSTEM_ATIVATION_TEMPERATURE = 15
    COLD_SYSTEM_STOP_TEMPERATURE = 23

    HEAT_SYSTEM_ATIVATION_TEMPERATURE = 30
    HEAT_SYSTEM_STOP_TEMPERATURE = 23


    def on_message(self, frame):
        global latest_temperature_message
        if frame.body.strip() != "":
            print(f"Received message: {frame.body}")
            # Store the received content
            with latest_temperature_message_lock:
                latest_temperature_message = frame.body
    
    def get_latest_temperature_data(self):
        """
        Retrieves the content of the last received message, interprets it as JSON,
        and extracts the integer value associated with the key "temperature".
        Returns None if no message has been received or if conversion fails.
        """
        with latest_temperature_message_lock:
            received_content = latest_temperature_message if latest_temperature_message is not None else None
        if received_content is None:
            return None
        try:
            data = json.loads(received_content)
            temperature = data.get("temperature")
            temperature_numeric_value = int(temperature) if temperature is not None else None
            cold_system_activated = data.get("cold_system_activated")
            heat_system_activated = data.get("heat_system_activated")
            return temperature_numeric_value, cold_system_activated, heat_system_activated
        except (ValueError, json.JSONDecodeError):
            return None
        
    def manage_temperature_system(self, 
                                  conn, 
                                  listener, 
                                  lecturas_dest, 
                                  actuador_des,
                                  temperature_numeric_value, 
                                  cold_system_activated, 
                                  heat_system_activated):
        if temperature_numeric_value
        return None

    @staticmethod
    def setup_connection():
        """
        Sets up the STOMP connection, listener, and subscriptions.
        Returns a tuple containing:
          - the connection,
          - the listener,
          - the 'lecturas_temperaturas_oficina1' destination,
          - the 'actuador_temperatura_oficina1' destination.
        """
        conn = stomp.Connection12(host_and_ports=[("localhost", 61613)])
        listener = ConsolaCentral()
        conn.set_listener('', listener)
        conn.connect(wait=True)
        
        lecturas_dest = "/topic/lecturas_temperaturas_oficina1"
        actuador_dest = "/topic/actuador_temperatura_oficina1"
        
        conn.subscribe(destination=lecturas_dest, id=1, ack='auto')
        
        print("Waiting for asynchronous messages. Press Ctrl+C to exit...")
        return conn, listener, lecturas_dest, actuador_dest

def main():
    conn, listener, lecturas_dest, actuador_dest = ConsolaCentral.setup_connection()

    try:
        while True:
            # Retrieve the content of the last received message (or "N/A" if none)
           temperature_data = listener.get_latest_temperature_data()

           if temperature_data is not None:
                temperature_numeric_value, cold_system_activated, heat_system_activated = temperature_data
            
                # Create the JSON payload including the received content
                json_body = json.dumps({
                    "temperature_numeric_value": temperature_numeric_value,
                    "cold_system_activated": cold_system_activated,
                    "heat_system_activated": heat_system_activated
                })
                
                conn.send(
                    destination=actuador_dest, 
                    body=json_body,
                    headers={
                        "content-type": "application/json", 
                        "amq-msg-type": "text"
                    }
                )

           time.sleep(2)
    except KeyboardInterrupt:
        print("Disconnecting...")
        conn.disconnect()

if __name__ == "__main__":
    main()
