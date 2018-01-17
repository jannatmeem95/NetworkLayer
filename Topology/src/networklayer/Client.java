/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package networklayer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author samsung
 */
public class Client {
    public static void main(String[] args)
    {
        Socket socket;
        ObjectInputStream input;
        ObjectOutputStream output;
        ArrayList<EndDevice> activeClients=new ArrayList<>();
        int discard_count=0;
        int droppedcount=0;
        int successCount=0;
        int hops=0;

        try {
            socket = new Socket("localhost", 1234);
            input = new ObjectInputStream(socket.getInputStream());
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();

            try {
                System.out.println("Connected to server");
                EndDevice dev= (EndDevice) input.readObject();
                Thread.sleep(7000);
                System.out.println("Self IP:  "+dev.getIp()+" ,Gateway: "+dev.getGateway());

                //activeClients=(ArrayList<EndDevice>) input.readObject();
                //System.out.println("Active Clients: "+activeClients);

                for(int i=0;i<100;i++) {
                    //generate random message

                    byte[] b = new byte[20];
                    new Random().nextBytes(b);
                    String s = b.toString();
                    Packet packet = new Packet(s, null, dev.getGateway(), null);
                    if(i==20)
                    {
                        packet.setSpecialMessage("ShowRoute");
                    }
                    else{
                        packet.setSpecialMessage(null);
                    }

                    output.writeObject(packet);
                    output.flush();

                    int senderID=(int) input.readObject();
                    int destID=(int) input.readObject() ;
                    System.out.println("Source : "+senderID+" , Dest : "+destID);

                    Packet ack=(Packet) input.readObject();

                    if(ack.discarded==true){
                        System.out.println("Packet no: "+(i+1)+" was discarded.");
                        discard_count++;

                    }
                    else if(ack.dropped==true){
                        System.out.println("Packet no: "+(i+1)+" was dropped.");
                        droppedcount+=1;
                    }
                    else{
                        successCount+=1;
                        hops+=packet.hopcount;
                        System.out.println("Packet no: "+(i+1)+" was delivered successfully.\nHopcount: "+packet.hopcount);

                    }

                    if(i==20){
                        if(packet.discarded==false||packet.dropped==false) {
                            System.out.println("Route: " + packet.getRoute());
                            int size = (int) input.readObject();
                            for (int h = 0; h < size; h++) {
                                String table = (String) input.readObject();
                                System.out.println(table);
                            }
                        }
                    }

                }

                int avg_droprate=droppedcount/(100-discard_count);
                int avg_hop=hops/successCount;

                System.out.println("avg_droprate: "+avg_droprate+"\n avg_hops: "+avg_hop);

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }


        /**
         * Tasks
         */



        /*
        1. Receive EndDevice configuration from server
        2. Receive active client list from server
        3. for(int i=0;i<100;i++)
        4. {
        5.      Generate a random message
        6.      Assign a random receiver from active client list
        7.      if(i==20)
        8.      {
        9.            Send the message and recipient IP address to server and a special request "SHOW_ROUTE"
        10.           Display routing path, hop count and routing table of each router [You need to receive
                            all the required info from the server in response to "SHOW_ROUTE" request]
        11.     }
        12.     else
        13.     {
        14.           Simply send the message and recipient IP address to server.
        15.     }
        16.     If server can successfully send the message, client will get an acknowledgement along with hop count
                    Otherwise, client will get a failure message [dropped packet]
        17. }
        18. Report average number of hops and drop rate
        */
        while(true)
        {

        }
    }
}
