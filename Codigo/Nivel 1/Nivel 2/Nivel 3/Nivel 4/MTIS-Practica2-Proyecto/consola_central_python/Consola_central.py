# Consola_central.py
import time
import stomp
import json  # <-- Añadir import

class ConsolaCentral(stomp.ConnectionListener):
    def on_message(self, frame):
        if frame.body.strip() != "":
            print(f"Received message: {frame.body}")

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
            # Crear cuerpo JSON
            json_body = json.dumps({"frio": True})  # <-- Nuevo cuerpo
            
            conn.send(
                destination=actuador_temperatura_oficina1_destination, 
                body=json_body,  # <-- Usar JSON
                headers={
                    "content-type": "application/json",  # <-- Header modificado
                    "amq-msg-type": "text"
                }
            )
            time.sleep(1)
    except KeyboardInterrupt:
        print("Disconnecting...")
        conn.disconnect()

if __name__ == "__main__":
    main()