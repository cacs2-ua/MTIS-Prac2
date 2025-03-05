import stomp
import threading

class SyncListener(stomp.ConnectionListener):
    def __init__(self):
        self.received_event = threading.Event()

    def on_message(self, frame):
        print(f"Received message: {frame.body}")
        self.received_event.set()

def main():
    conn = stomp.Connection12(host_and_ports=[("localhost", 61613)])
    listener = SyncListener()
    conn.set_listener('', listener)
    conn.connect(wait=True)
    
    destination = "/topic/historiales"
    conn.subscribe(destination=destination, id=1, ack='auto')
    print("Waiting for message...")
    
    # Espera hasta recibir el mensaje (timeout de 10 segundos por seguridad)
    if listener.received_event.wait(timeout=1000000):
        print("Message received, disconnecting.")
    else:
        print("Timeout waiting for message.")
    
    conn.disconnect()

if __name__ == "__main__":
    main()
