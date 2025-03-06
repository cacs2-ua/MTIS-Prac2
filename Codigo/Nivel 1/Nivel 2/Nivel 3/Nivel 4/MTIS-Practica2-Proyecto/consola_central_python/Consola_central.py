import time
import stomp
import json
import threading

# Variable global para almacenar el último mensaje recibido
latest_temperature_message = None
latest_temperature_message_lock = threading.Lock()

class ConsolaCentral(stomp.ConnectionListener):
    def on_message(self, frame):
        global latest_temperature_message
        if frame.body.strip() != "":
            print(f"Received message: {frame.body}")
            # Almacenar el contenido recibido
            with latest_temperature_message_lock:
                latest_temperature_message = frame.body
    
    def get_latest_temperature_as_int(self):
        """
        Recupera el contenido del último mensaje recibido, lo interpreta como JSON y extrae
        el valor entero asociado a la clave "temperature".
        Si no se ha recibido ningún mensaje o la conversión falla, devuelve None.
        """
        with latest_temperature_message_lock:
            received_content = latest_temperature_message if latest_temperature_message is not None else None
        if received_content is None:
            return None
        try:
            # Interpretar el contenido como JSON
            data = json.loads(received_content)
            # Extraer el valor de "temperature"
            temperature = data.get("temperature")
            return int(temperature) if temperature is not None else None
        except (ValueError, json.JSONDecodeError):
            return None


def main():
    conn = stomp.Connection12(host_and_ports=[("localhost", 61613)])
    listener = ConsolaCentral()
    conn.set_listener('', listener)
    conn.connect(wait=True)
    
    lecturas_temperaturas_oficina1_destination  = "/topic/lecturas_temperaturas_oficina1"
    actuador_temperatura_oficina1_destination = "/topic/actuador_temperatura_oficina1"

    conn.subscribe(destination=lecturas_temperaturas_oficina1_destination, id=1, ack='auto')

    print("Waiting for asynchronous messages. Press Ctrl+C to exit...")

    try:
        while True:
            # Recuperar el contenido del último mensaje recibido (si no hay, se usa "N/A")
            temperature_numeric_value = listener.get_latest_temperature_as_int()
            
            # Crear el cuerpo JSON incluyendo el contenido recibido
            json_body = json.dumps({
                "frio": True,
                "temperature_numeric_value": temperature_numeric_value
            })
            
            conn.send(
                destination=actuador_temperatura_oficina1_destination, 
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
