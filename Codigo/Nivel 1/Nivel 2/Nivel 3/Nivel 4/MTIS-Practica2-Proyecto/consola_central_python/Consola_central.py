import time
import stomp
import json
import threading

# Global variable to store the last received message 
latest_temperature_message = None
latest_temperature_message_lock = threading.Lock()

class ConsolaCentral(stomp.ConnectionListener):
    def on_message(self, frame):
        global latest_temperature_message
        if frame.body.strip() != "":
            print(f"Received message: {frame.body}")
            # Store the received content
            with latest_temperature_message_lock:
                latest_temperature_message = frame.body
    
    def get_latest_temperature_as_int(self):
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
            return int(temperature) if temperature is not None else None
        except (ValueError, json.JSONDecodeError):
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
            temperature_numeric_value = listener.get_latest_temperature_as_int()
            
            # Create the JSON payload including the received content
            json_body = json.dumps({
                "frio": True,
                "temperature_numeric_value": temperature_numeric_value
            })
            
            conn.send(
                destination=actuador_dest, 
                body=json_body,
                headers={
                    "content-type": "application/json", 
                    "amq-msg-type": "text"
                }
            )
            time.sleep(4)
    except KeyboardInterrupt:
        print("Disconnecting...")
        conn.disconnect()

if __name__ == "__main__":
    main()
