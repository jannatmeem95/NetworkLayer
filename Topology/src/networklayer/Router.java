/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package networklayer;

import sun.nio.ch.Net;

import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author samsung
 */
public class Router {
    private int routerId;
    private int numberOfInterfaces;
    private ArrayList<IPAddress> interfaceAddrs;//list of IP address of all interfaces of the router
    private ArrayList<RoutingTableEntry> routingTable;//used to implement DVR
    private ArrayList<Integer> neighborRouterIds;//Contains both "UP" and "DOWN" state routers
    private Boolean state;//true represents "UP" state and false is for "DOWN" state

    public Router() {
        interfaceAddrs = new ArrayList<>();
        routingTable = new ArrayList<>();
        neighborRouterIds = new ArrayList<>();

        /**
         * 80% Probability that the router is up
         */
        Random random = new Random();
        double p = random.nextDouble();
        if(p<=0.80) state = true;
        else state = false;

        numberOfInterfaces = 0;
    }

    public Router(int routerId, ArrayList<Integer> neighborRouters, ArrayList<IPAddress> interfaceAddrs)
    {
        this.routerId = routerId;
        this.interfaceAddrs = interfaceAddrs;
        this.neighborRouterIds = neighborRouters;
        routingTable = new ArrayList<>();

        /**
         * 80% Probability that the router is up
         */
        Random random = new Random();
        double p = random.nextDouble();
        if(p<=0.80) state = true;
        else state = false;

        numberOfInterfaces = this.interfaceAddrs.size();
    }

    @Override
    public String toString() {
        String temp = "";
        temp+="Router ID: "+routerId+"\n";
        temp+="Intefaces: \n";
        for(int i=0;i<numberOfInterfaces;i++)
        {
            temp+=interfaceAddrs.get(i).getString()+"\t";
        }
        temp+="\n";
        temp+="Neighbors: \n";
        for(int i=0;i<neighborRouterIds.size();i++)
        {
            temp+=neighborRouterIds.get(i)+"\t";
        }
        return temp;
    }


    /**
     * Initialize the distance(hop count) for each router.
     * for itself, distance=0; for any connected router with state=true, distance=1; otherwise distance=Constants.INFTY;
     */
    public void initiateRoutingTable()
    {
        boolean change=false;
        RoutingTableEntry Rt;
        routingTable.clear();

        for(int j=0;j<NetworkLayerServer.routers.size();j++)
        {
            if(state==false){
                /*Rt = new RoutingTableEntry(NetworkLayerServer.routers.get(j).routerId, Constants.INFTY, 0);
                routingTable.add(Rt);
                change=true;*/
            }

            else if(NetworkLayerServer.routers.get(j).getState()==true)
            {
                if(NetworkLayerServer.routers.get(j).routerId==routerId)
                {
                    Rt=new RoutingTableEntry(routerId, 0, routerId);
                    routingTable.add(Rt);
                    change=true;
                }
                else {
                    for (int i = 0; i < neighborRouterIds.size(); i++) {
                        if (NetworkLayerServer.routers.get(j).routerId == neighborRouterIds.get(i)) {
                            Rt = new RoutingTableEntry(neighborRouterIds.get(i), 1, neighborRouterIds.get(i));
                            routingTable.add(Rt);
                            change = true;
                            break;
                        }
                    }
                }
            }
            if(change==false) {
                Rt = new RoutingTableEntry(NetworkLayerServer.routers.get(j).routerId, Constants.INFTY, routerId);
                routingTable.add(Rt);
            }
           // System.out.println("Dest : "+routingTable.get(j).getRouterId()+" Distance : "+routingTable.get(j).getDistance()+"gateway : "+routingTable.get(j).getGatewayRouterId());
            change=false;
        }
    }

    public void printRoutingTable()
    {
        System.out.println("RoutingTable of Router "+routerId+" : ");
        for(int i=0;i<routingTable.size();i++) {
            try {
                System.out.println("Dest : " + routingTable.get(i).getRouterId() + " Distance : " +
                        routingTable.get(i).getDistance() + " Next hop : " + routingTable.get(i).getGatewayRouterId());
            }catch (NullPointerException b)
            {
                System.out.println("Nullpointer in routingtable : "+"Dest : " + routingTable.get(i).getRouterId() + " Distance : " +
                        routingTable.get(i).getDistance() + " Next hop : " + routingTable.get(i).getGatewayRouterId());
            }
        }
        for(int j=0;j<neighborRouterIds.size();j++) {
            if(NetworkLayerServer.routers.get(neighborRouterIds.get(j)-1).getState()==true)
                System.out.println(neighborRouterIds.get(j));
        }
    }

    /**
     * Delete all the routingTableEntry
     */
    public void clearRoutingTable()
    {

        routingTable.clear();

    }

    /**
     * Update the routing table for this router using the entries of Router neighbor
     * @param neighbor
     */

    public boolean updateRoutingTable(Router neighbor)
    {
        ArrayList<RoutingTableEntry> routes=neighbor.routingTable;
        ArrayList<RoutingTableEntry> oldRoutingtable=routingTable;
        //System.out.println("\n\nRouterid : "+routerId+"  neighbor id"+neighbor.getRouterId());
        boolean check=false;
        for(int i=0;i<routes.size();i++)
        {
            double p=routingTable.get(neighbor.getRouterId()-1).getDistance()+routes.get(i).getDistance();
            try {
                if ((neighbor.getRouterId() == routingTable.get(i).getGatewayRouterId()) || (routerId != routes.get(i).getGatewayRouterId()
                        && p < routingTable.get(i).getDistance())) {
                    if (p > Constants.INFTY) p = Constants.INFTY;
                    if (p != routingTable.get(i).getDistance()) check = true;
                    routingTable.get(i).setDistance(p);
                    routingTable.get(i).setGatewayRouterId(neighbor.routerId);

                }
            } catch(IndexOutOfBoundsException a){
                System.out.println("i="+i+" routes.size="+routes.size()+" neighbor="+neighbor.getRouterId());
                NetworkLayerServer.printRoutingTables();
                a.printStackTrace();
                System.exit(0);
            }

        }
        if(check==false) return false;
        else return true;
    }


    public boolean updateRoutingTable2(Router neighbor)
    {
        ArrayList<RoutingTableEntry> routes=neighbor.routingTable;
        ArrayList<RoutingTableEntry> oldRoutingtable=routingTable;
        //System.out.println("\n\nRouterid : "+routerId+"  neighbor id"+neighbor.getRouterId());
        boolean check=false;
        for(int i=0;i<routes.size();i++)
        {
            double p=routingTable.get(neighbor.getRouterId()-1).getDistance()+routes.get(i).getDistance();
            if( p<routingTable.get(i).getDistance())
            {
                if(p>Constants.INFTY) p=Constants.INFTY;
                if(p!=routingTable.get(i).getDistance()) check=true;
                routingTable.get(i).setDistance(p);
                routingTable.get(i).setGatewayRouterId(neighbor.routerId);
            }

        }
        if(check==false) return false;
        else return true;
    }

    /**
     * If the state was up, down it; if state was down, up it
     */

    public void revertState()
    {
        state=!state;
        if(state==true) this.initiateRoutingTable();
        else this.clearRoutingTable();
    }

    public int getRouterId() {
        return routerId;
    }

    public void setRouterId(int routerId) {
        this.routerId = routerId;
    }

    public int getNumberOfInterfaces() {
        return numberOfInterfaces;
    }

    public void setNumberOfInterfaces(int numberOfInterfaces) {
        this.numberOfInterfaces = numberOfInterfaces;
    }

    public ArrayList<IPAddress> getInterfaceAddrs() {
        return interfaceAddrs;
    }

    public void setInterfaceAddrs(ArrayList<IPAddress> interfaceAddrs) {
        this.interfaceAddrs = interfaceAddrs;
        numberOfInterfaces = this.interfaceAddrs.size();
    }

    public ArrayList<RoutingTableEntry> getRoutingTable() {
        return routingTable;
    }

    public void addRoutingTableEntry(RoutingTableEntry entry) {
        this.routingTable.add(entry);
    }

    public ArrayList<Integer> getNeighborRouterIds() {
        return neighborRouterIds;
    }

    public void setNeighborRouterIds(ArrayList<Integer> neighborRouterIds) {
        this.neighborRouterIds = neighborRouterIds;
    }

    public Boolean getState() {
        return state;
    }

    public void setState(Boolean state) {
        this.state = state;
    }


}
