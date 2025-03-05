import stomp
import threading

class AsyncListener(stomp.ConnectionListener):
    def __init__(self, event):
        self.event = event

    def on_message(self, frame):
        print(f"Received message: {frame.body}")
        self.event.set()

def main():
    # Utilizamos un threading.Event para esperar un mensaje.
    event = threading.Event()
    
    conn = stomp.Connection12(host_and_ports=[("localhost", 61613)])
    listener = AsyncListener(event)
    conn.set_listener('', listener)
    conn.connect(wait=True)
    
    destination = "/topic/historiales"
    conn.subscribe(destination=destination, id=1, ack='auto')
    print("Waiting for asynchronous message...")
    
    # Espera hasta recibir el mensaje (timeout de 10 segundos)
    if event.wait(timeout=10000):
        print("Message received, disconnecting.")
    else:
        print("Timeout waiting for message.")
    
    conn.disconnect()

if __name__ == "__main__":
    main()
