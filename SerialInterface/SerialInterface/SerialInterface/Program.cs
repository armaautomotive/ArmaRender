/**
 * Arma Automotive Inc.
 * Serial Communication Services
 * Development Environment: MS Visual Studio 2019
 * 
 * Arma Design Studio java application does not have easy access to serial 
 * ports, This C# application provides serial comminucation for ADS.
 * 
 * Author: Jon Taylor
 * Date: Jan 26 2022
 */

using System;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.IO;
//using System.Reflection;
using System.IO.Ports;

namespace SerialInterface
{
    class Program
    {
        // Serial port
        private static SerialPort port;
        static bool waitingForController = false;
        static string serialBuffer = "";
        static string devicePort = "";
        static Socket clientSocket;

        static string socketSendBuffer = "";

        public static string GetLocalIPAddress()
        {
            var host = Dns.GetHostEntry(Dns.GetHostName());
            foreach (var ip in host.AddressList)
            {
                if (ip.AddressFamily == AddressFamily.InterNetwork)
                {
                    return ip.ToString();
                }
            }
            throw new Exception("No network adapters with an IPv4 address in the system!");
        }

        /**
         * SerialDataReceivedHandler
         * 
         * Description: Handler for input data from the arduino serial port.
         */
        public static void SerialDataReceivedHandler(object sender, SerialDataReceivedEventArgs e)
        {
            Console.WriteLine(@" SerialDataReceivedHandler ");
            String serialInput = port.ReadExisting();
            serialBuffer += serialInput;


            Console.WriteLine(@" SerialDataReceivedHandler: " + serialInput);

            // Parse input

            // Trigger ready state.
            if (serialBuffer.Length > 0 ) // && serialBuffer.ToLower().Contains(@"done")
            {
                if (serialBuffer.ToLower().Contains(@"done")) {
                    Console.WriteLine(@" <- DONE ");
                }
                //Console.WriteLine(@" <- "+ "\n" + serialBuffer + " <- \n");

                waitingForController = false;

                socketSendBuffer += serialBuffer; // temp
                serialBuffer = "";

                int index = serialBuffer.LastIndexOf('\n');
                if (index > -1) // If charrage return, print buffer, and erase it.
                {
                    serialBuffer.Replace('\n', ' ');
                    Console.WriteLine(@" <- " + "\n" + serialBuffer + " <- \n");
                    serialBuffer = serialBuffer.Substring(index);


                    socketSendBuffer += serialBuffer;
                    serialBuffer = "";
                }
            }
        }

        /**
         * waitForControllerResponse
         * 
         * Description: Wait for the controller (Arduino) to reply.
         */
        public static void waitForControllerResponse()
        {
            int counter = 0;
            while (counter < 1000)
            {
                Thread.Sleep(25); // Delay
                if (waitingForController == false)
                {
                    break;
                }

                counter++;
            }
        }

        /**
         * sendSerialCommand
         * 
         * Description: Send serial command. Pauses other tasks until a response is received.
         */
        public static void sendSerialCommand(String command)
        {
            try
            {
                Console.WriteLine("sendSerialCommand: " + command);

                waitingForController = true;        // Global flag indicates waiting for response

                //if (port.isOpen() == false) {


                //}


                port.WriteLine(command + "\n");
                //Console.WriteLine("sent serial");


                waitForControllerResponse();
                //Console.WriteLine(" send serial done ");
            }
            catch (Exception e)
            {
                System.Console.WriteLine(@"Error opening port." + e);
            }
        }

        /**
         * initalize
         * 
         * Description: Connect to the Arduino via a serial port.
         */
        public static void initalizeSerial()
        {
            string[] ports = SerialPort.GetPortNames();
            foreach (string currPort in ports)
            {
                if (currPort.IndexOf("cu.usbmodem") > -1 ||
                    currPort.IndexOf("COM") > -1)
                {
                    try
                    {
                        Console.WriteLine(@"Connecting to serial port: " + currPort);
                        port = new SerialPort(currPort, 9600, Parity.None, 8, StopBits.One);
                        port.Open();
                        port.DataReceived += new SerialDataReceivedEventHandler(SerialDataReceivedHandler);

                        devicePort = currPort;

                        break;
                    }
                    catch (Exception e)
                    {
                        System.Console.WriteLine(@"Error opening serial port." + e);
                    }
                }
            }
        }

        

        static void Main(string[] args)
        {
            initalizeSerial();


            String localIP = GetLocalIPAddress();

            Console.WriteLine("Arma Serial Client: " + localIP);
            IPEndPoint serverAddress = new IPEndPoint(IPAddress.Parse(localIP), 4343);
            clientSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);


            Boolean running = true;
            while (running)
            {

                if (clientSocket.Connected == false)
                {
                    try
                    {
                        clientSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);

                        clientSocket.Connect(serverAddress);
                    }
                    catch (Exception e)
                    {
                        Console.WriteLine("Exception: " + e);
                    }
                }

                if (clientSocket.Connected == true) {
                    // Sending
                    try
                    {
                        string toSend = "noop";
                        if (socketSendBuffer.Length > 0)
                        {
                            toSend = socketSendBuffer;
                            socketSendBuffer = "";
                        }

                        int toSendLen = System.Text.Encoding.ASCII.GetByteCount(toSend);
                        byte[] toSendBytes = System.Text.Encoding.ASCII.GetBytes(toSend);
                        byte[] toSendLenBytes = System.BitConverter.GetBytes(toSendLen);
                        clientSocket.Send(toSendLenBytes);
                        clientSocket.Send(toSendBytes);




                        // Receiving
                        byte[] rcvLenBytes = new byte[4];
                        clientSocket.Receive(rcvLenBytes);
                        int rcvLen = System.BitConverter.ToInt32(rcvLenBytes, 0);
                        byte[] rcvBytes = new byte[rcvLen];
                        clientSocket.Receive(rcvBytes);
                        String rcv = System.Text.Encoding.ASCII.GetString(rcvBytes);

                        Console.WriteLine("Client received: " + rcv);

                        if (rcv.Equals("noop") == false && rcv != null) {

                            sendSerialCommand(rcv);

                        }
                        if (rcv.Equals("isconnected"))
                        {
                            //devicePort
                            sendToSocket(devicePort);
                        }



                    } catch  (Exception e) {
                        Console.WriteLine("Exception: " + e);
                    }

                }


                Thread.Sleep(1000);
            }

            clientSocket.Close();
        }


        /**
         * 
         */
        public static void sendToSocket(String text)
        {
            try
            {
                int toSendLen = System.Text.Encoding.ASCII.GetByteCount(text);
                byte[] toSendBytes = System.Text.Encoding.ASCII.GetBytes(text);
                byte[] toSendLenBytes = System.BitConverter.GetBytes(toSendLen);
                clientSocket.Send(toSendLenBytes);
                clientSocket.Send(toSendBytes);
            }
            catch (Exception e)
            {

            }
        }


    }
}
