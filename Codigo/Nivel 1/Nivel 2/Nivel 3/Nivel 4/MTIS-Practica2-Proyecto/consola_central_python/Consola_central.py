import time
import stomp
import json
import threading

# Variable global para almacenar el último mensaje recibido
latest_message = None
latest_message_lock = threading.Lock()

class ConsolaCentral(stomp.ConnectionListener):
    def on_message(self, frame):
        global latest_message
        if frame.body.strip() != "":
            print(f"Received message: {frame.body}")
            # Almacenar el contenido recibido
            with latest_message_lock:
                latest_message = frame.body

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
            with latest_message_lock:
                received_content = latest_message if latest_message is not None else "N/A"
            
            # Crear el cuerpo JSON incluyendo el contenido recibido
            json_body = json.dumps({
                "frio": True
            })
            
            conn.send(
                destination=actuador_temperatura_oficina1_destination, 
                body=json_body,
                headers={
                    "content-type": "application/json", 
                    "amq-msg-type": "text"
                }
            )
            time.sleep(1)
    except KeyboardInterrupt:
        print("Disconnecting...")
        conn.disconnect()

if __name__ == "__main__":
    main()
