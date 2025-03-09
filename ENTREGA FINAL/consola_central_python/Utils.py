# Utils.py
import stomp
from Consola_central import ConsolaCentral  # Importa la clase listener definida en Consola_central.py

class Utils:
    @staticmethod
    def setup_connection():
        """
        Crea y configura la conexión STOMP, define el listener y se suscribe al topic.
        Retorna:
            conn: la conexión STOMP establecida.
            lecturas_destination: el topic de lectura.
            actuador_destination: el topic de envío.
        """
        conn = stomp.Connection12(host_and_ports=[("localhost", 61613)])
        listener = ConsolaCentral()
        conn.set_listener('', listener)
        conn.connect(wait=True)
        
        lecturas_destination  = "/topic/lecturas_temperaturas_oficina1"
        actuador_destination = "/topic/actuador_temperatura_oficina1"

        conn.subscribe(destination=lecturas_destination, id=1, ack='auto')
        
        print("Waiting for asynchronous messages. Press Ctrl+C to exit...")
        return conn, lecturas_destination, actuador_destination
