import stomp

class MTISPublisher:
    def __init__(self, host='localhost', port=61613, topic='historiales'):
        # Nota: Si deseas usar el puerto 61616, cámbialo aquí (pero asegúrate de que el conector STOMP esté configurado para ese puerto)
        self.conn = stomp.Connection12(host_and_ports=[(host, port)])
        self.topic = topic

    def connect(self):
        self.conn.connect(wait=True)

    def send_message(self, message):
        destination = f"/topic/{self.topic}"
        self.conn.send(destination=destination, body=message)
        print(f"Sent message: {message}")

    def disconnect(self):
        self.conn.disconnect()

if __name__ == "__main__":
    publisher = MTISPublisher()
    publisher.connect()
    xml_message = "<history><patient><name>Manolo García</name></patient></history>"
    publisher.send_message(xml_message)
    publisher.disconnect()
