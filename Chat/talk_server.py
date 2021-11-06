import select
from socket import *
import sys
import argparse

parser = argparse.ArgumentParser(description="A prattle server")

parser.add_argument("-p", "--port", dest="port", type=int, default=12345,
                    help="TCP port the server is listening on (default 12345)")
parser.add_argument("-v", "--verbose", action="store_true", dest="verbose",
                    help="turn verbose output on")
args = parser.parse_args()

#Setting the Default Server IP and the Port Number
Server_IP = "127.127.0.0.1"
Server_Port = 12345

#Opens the Socket and set it to be able to resuse the same packets
#Bind the server to the IP and the Port
#Set the Server to Listen 
#append the Server to a list of servers that we will read from
Server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
Server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
Server.bind((Server_IP, Server_Port))
Server.listen(1)
Socks = [Server]

#Forever Loop
while True:
    #Select() that allows us to read and write to a list of Sockets
    readable, writeable, exceptable = select.select(Socks, [], Socks)
    for s in readable:
        #client connecting to the server
        #Send the client a message upon connecting.
        if s == Server:
            client_Socket, address = Server.accept()
            client_Socket.send("Thank You For Connecting")
            Socks.append[client_Socket]
        else:
            try:
                data = client_Socket.recv(1024)
                if not data:
                    if s in Socks:
                        Socks.remove(s)
                        send_message(s, Socks, "<" + address[0] + "> is Offline")
                    else:
                        send_message(s, Socks, "<" + address[0] + "> " + data)
            except:
                send_message(s, Socks, "<" + address[0] + "> is Offline")
    def send_message(Socks,connection, message):
        for sock in Socks:
            if sock!=connection:
                try:
                    sock.send(message)
                except:
                    sock.close
                    Socks.remove(sock)
Server.close()


