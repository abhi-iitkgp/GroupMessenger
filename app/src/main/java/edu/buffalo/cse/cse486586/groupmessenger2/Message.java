package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by abhinav on 3/7/15.
 */
class Message implements Serializable
{
    int origin_port;
    String message;
    int sequence_number;
    int priority;
    boolean delivered;

    public Message(int origin_port, String message, int sequence_number, int priority, boolean delivered)
    {
        this.origin_port = origin_port;
        this.message = message;
        this.priority = priority;
        this.delivered = delivered;
        this.sequence_number = sequence_number;
    }
}