/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package networklayer;

import com.sun.deploy.net.proxy.pac.PACFunctions;

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
public class ServerThread implements Runnable {
    private Thread t;
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    int count;
    EndDevice sender;
    int hopcount;

    public ServerThread(Socket socket,EndDevice client){
        this.socket = socket;
        this.sender=client;
        count=NetworkLayerServer.clientCount;
        hopcount=0;
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());

        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Server Ready for client "+NetworkLayerServer.clientCount);
        NetworkLayerServer.clientCount++;

        t=new Thread(this);
        t.start();
    }

    @Override
    public void run() {
        /**
         * Synchronize actions with client.
         */

        try {
          //  System.out.println("sender.selfIP: "+sender.getIp()+" ,sender.gateway: "+sender.getGateway());
            output.writeObject(sender);
            output.flush();

            //output.writeObject(NetworkLayerServer.activeClients);
            //output.flush();

            for(int i=0;i<100;i++) {
                Packet packet = (Packet) input.readObject();

                Router source = null;
                Router destination = null;
                Random random = new Random();
                int r =Math.abs(random.nextInt(NetworkLayerServer.activeClients.size()));
                EndDevice receiver=NetworkLayerServer.activeClients.get(r);
//                System.out.println("dest gateway : "+receiver.getGateway());

                packet.setDestinationIP(receiver.getGateway());  //gateway ke sourceIP and destIP hishebe set korsi
              //  packet.setSourceIP(new IPAddress("192.168.35.1"));
                //packet.setDestinationIP(new IPAddress("192.168.15.1"));

                System.out.println("packet for client "+count+": "  + packet.getMessage()+" source : "+packet.getSourceIP()+" dest : "+packet.getDestinationIP());
               // System.out.println(packet.getSourceIP().toString()+" = "+packet.getDestinationIP().toString());

                boolean result=deliverPacket(packet);
                if(i==20 && packet.discarded==false && packet.dropped==false){
                    output.writeObject(NetworkLayerServer.routers.size());
                    output.flush();

                    for(int z=0;z<NetworkLayerServer.routers.size();z++)
                    {
                        Router b=NetworkLayerServer.routers.get(z);
                        String table="\nRoutingTable of Router "+b.getRouterId()+" : ";

                        for(int s=0;s<b.getRoutingTable().size();s++) {

                            table=table+"\n"+("Dest : " + b.getRoutingTable().get(s).getRouterId() + " Distance : " +
                                    b.getRoutingTable().get(s).getDistance() + " Next hop : " + b.getRoutingTable().get(s).getGatewayRouterId());

                        }
                        output.writeObject(table);
                        output.flush();

                    }
                }
                //System.out.println("ki jani "+deliverPacket(packet));


            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        /*
        Tasks:
        1. Upon receiving a packet and recipient, call deliverPacket(packet)
        2. If the packet contains "SHOW_ROUTE" request, then fetch the required information
                and send back to client
        3. Either send acknowledgement with number of hops or send failure message back to client
        */
    }

    /**
     * Returns true if successfully delivered
     * Returns false if packet is dropped
     * @param p
     * @return
     */
    public Boolean deliverPacket(Packet p) throws IOException {

        Router source = null;
        Router destination = null;
         /*
        1. Find the router s which has an interface
                such that the interface and source end device have same network address.*/
        for(int j=0;j<NetworkLayerServer.routers.size();j++)
        {
           /* 1. Find the router s which has an interface
        such that the interface and source end device have same network address.*/

            if(NetworkLayerServer.routers.get(j).getInterfaceAddrs().get(0).toString().equals(p.getSourceIP().toString())){
                source=NetworkLayerServer.routers.get(j); //route="-"+source.getRouterId()+"-";
                System.out.println("\nSource router for client "+count+": "+source.getRouterId()+"  state : "+source.getState());
            }
            /* 2. Find the router d which has an interface
                such that the interface and destination end device have same network address. */
            if(NetworkLayerServer.routers.get(j).getInterfaceAddrs().get(0).toString().equals(p.getDestinationIP().toString())){
                destination=NetworkLayerServer.routers.get(j);
                System.out.println("Destination router for client "+count+":  "+destination.getRouterId()+"  state : "+destination.getState());
            }

        }

        output.writeObject(source.getRouterId());
        output.flush();
        output.writeObject(destination.getRouterId());
        output.flush();

        if((source.getRouterId()==destination.getRouterId())||
                (NetworkLayerServer.routers.get(source.getRouterId()-1).getState()==false)){

            p.discarded=true;
            try {
                output.writeObject(p);
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(" Discarded ");

            return false;
        }

         /*
        3. Implement forwarding, i.e., s forwards to its gateway router x considering d as the destination.
                similarly, x forwards to the next gateway router y considering d as the destination,
                and eventually the packet reaches to destination router d.

            3(a) If, while forwarding, any gateway x, found from routingTable of router r is in down state[x.state==FALSE]
                    (i) Drop packet
                    (ii) Update the entry with distance Constants.INFTY
                    (iii) Block NetworkLayerServer.stateChanger.t
                    (iv) Apply DVR starting from router r.
                    (v) Resume NetworkLayerServer.stateChanger.t

            3(b) If, while forwarding, a router x receives the packet from router y,
                    but routingTableEntry shows Constants.INFTY distance from x to y,
                    (i) Update the entry with distance 1
                    (ii) Block NetworkLayerServer.stateChanger.t
                    (iii) Apply DVR starting from router x.
                    (iv) Resume NetworkLayerServer.stateChanger.t*/

        String route="-"+source.getRouterId()+"-";
        while (true){
            if(source.getRouterId()==destination.getRouterId() && source.getState()==true){
                p.setRoute(route);
                p.discarded=false;
                p.dropped=false;
                try {

                    if (p.getSpecialMessage().contains("ShowRoute")) {
                        p.setRoute(route);
                    }
                }catch (NullPointerException s)
                {
                    System.out.println("Shomosha: "+route+" message "+p.getSpecialMessage());
                }
                try {
                    output.writeObject(p);
                    output.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println(" Success ");
                return true;
            }
            ArrayList<RoutingTableEntry> routingTable=source.getRoutingTable();

            for (int k = 0; k < routingTable.size(); k++){
                try {
                    if (routingTable.get(k).getRouterId() == destination.getRouterId()) {
                        if(destination.getState()==false)
                        {
                            p.discarded=true;
                            p.dropped=false;
                            try {
                                output.writeObject(p);
                                output.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            route+= "{dropped in " + destination.getRouterId() + "}";
                            p.setRoute(route);
                            return false;
                        }
                        RoutingTableEntry found_dest = routingTable.get(k);
                        Router gateway = NetworkLayerServer.routers.get(found_dest.getGatewayRouterId() - 1);

                        //for disconnected
                        if(found_dest.getDistance()==Constants.INFTY && found_dest.getGatewayRouterId()==source.getRouterId()){
                            p.dropped=true;
                            route = route + "{dropped in " + gateway.getRouterId() + "}";
                            if(p.getSpecialMessage().contains("ShowRoute")) p.setRoute(route);
                            try {
                                output.writeObject(p);
                                output.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            System.out.println("yoyoyo Dropped ");
                            return false;
                        }

                        //3a

                        if (gateway.getState() == false) {
                            for (int x = 0; x < routingTable.size(); x++) {
                                if (routingTable.get(x).getRouterId() == gateway.getRouterId()) {
                                    RoutingTableEntry found_gateway = routingTable.get(x);
                                    found_gateway.setDistance(Constants.INFTY);

                                    NetworkLayerServer.stateChanger.t.stop();
                                    if (source.getState() == true) {
                                        NetworkLayerServer.DVR(source.getRouterId());
                                    }
                                    System.out.println(" Done DVR  ");
                                    NetworkLayerServer.stateChanger = new RouterStateChanger();

                                    p.dropped = true;
                                    route = route + "{dropped in " + gateway.getRouterId() + "}";
                                    p.setRoute(route);
                                    p.discarded = false;

                                    if(p.getSpecialMessage().contains("ShowRoute")) p.setRoute(route);
                                    try {
                                        output.writeObject(p);
                                        output.flush();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    System.out.println("gogo Dropped ");

                                    return false;


                                }

                            }
                        }
                        else{
                            System.out.println(" yayyyyy");
                            for(int y=0;y<gateway.getRoutingTable().size();y++)
                            {
                                if(gateway.getRoutingTable().get(y).getRouterId()==source.getRouterId())
                                {
                                    RoutingTableEntry old=gateway.getRoutingTable().get(y);

                                    //3b
                                    if(old.getDistance()!=1)
                                    {
                                        old.setDistance(1);
                                        old.setGatewayRouterId(source.getRouterId());
                                        NetworkLayerServer.stateChanger.t.stop();
                                        if (source.getState() == true) {
                                            NetworkLayerServer.DVR(gateway.getRouterId());
                                        }
                                        System.out.println(" Done DVR 3b ");
                                        NetworkLayerServer.stateChanger = new RouterStateChanger();
                                    }
                                }
                                source=gateway;
                                break;
                            }
                            p.hopcount++;
                            route+=gateway.getRouterId()+"-";
                            source=gateway;

                        }
                    }
                }catch (NullPointerException a)
                {
                    System.out.println("Nulpointer "+routingTable.get(k).getRouterId()+"  "+destination.getRouterId());
                }

            }
        }

        /*
        4. If 3(a) occurs at any stage, packet will be dropped,
            otherwise successfully sent to the destination router
        */


    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj); //To change body of generated methods, choose Tools | Templates.
    }

}
