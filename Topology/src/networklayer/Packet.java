/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package networklayer;

import java.io.Serializable;

/**
 *
 * @author samsung
 */
public class Packet implements Serializable {
    private String message;
    private String specialMessage;  //ex: "SHOW_ROUTE" request
    private IPAddress destinationIP;
    private IPAddress sourceIP;
    private String route;
    public boolean dropped;
    public boolean discarded;

    public  int hopcount;
    //aro jinish add kora lagbe


    public Packet(String message, String specialMessage, IPAddress sourceIP, IPAddress destinationIP) {
        this.message = message;
        this.specialMessage = specialMessage;
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;
        this.hopcount=0;
        route=" ";
        dropped=false;
        discarded=false;

    }

    public IPAddress getSourceIP() {
        return sourceIP;
    }

    public void setSourceIP(IPAddress sourceIP) {
        this.sourceIP = sourceIP;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSpecialMessage() {
        return specialMessage;
    }

    public void setSpecialMessage(String specialMessage) {
        this.specialMessage = specialMessage;
    }

    public IPAddress getDestinationIP() {
        return destinationIP;
    }

    public void setDestinationIP(IPAddress destinationIP) {
        this.destinationIP = destinationIP;
    }

    public void setHopcount(int x) { this.hopcount = x; }
    public int getHopcount() { return this.hopcount; }

    public void setRoute(String a) { this.route = a; }

    public String getRoute() { return this.route; }

}
