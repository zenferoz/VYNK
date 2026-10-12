package com.vynk.connection.dto;

import java.util.List;

public class PendingConnectionsResponse {

    private List<ConnectionResponse> incoming;
    private List<ConnectionResponse> outgoing;

    public PendingConnectionsResponse() {
    }

    public PendingConnectionsResponse(List<ConnectionResponse> incoming, List<ConnectionResponse> outgoing) {
        this.incoming = incoming;
        this.outgoing = outgoing;
    }

    public List<ConnectionResponse> getIncoming() {
        return incoming;
    }

    public void setIncoming(List<ConnectionResponse> incoming) {
        this.incoming = incoming;
    }

    public List<ConnectionResponse> getOutgoing() {
        return outgoing;
    }

    public void setOutgoing(List<ConnectionResponse> outgoing) {
        this.outgoing = outgoing;
    }
}
