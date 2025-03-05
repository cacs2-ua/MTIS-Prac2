import time
import stomp

class AsyncListener(stomp.ConnectionListener):
    def on_message(self, frame):
        print(f"Received message: {frame.body}")

def main():
    conn = stomp.Connection12(host_and_ports=[("localhost", 61613)])
    listener = AsyncListener()
    conn.set_listener('', listener)
    conn.connect(wait=True)
    
    destination = "/topic/historiales"
    conn.subscribe(destination=destination, id=1, ack='auto')
    print("Waiting for asynchronous messages. Press Ctrl+C to exit...")

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("Disconnecting...")
        conn.disconnect()

if __name__ == "__main__":
    main()
