package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;

/**
 * Created by abhinav on 3/7/15.
 */
public class MessageComparator implements Comparator<Message>
{

    @Override
    public int compare(Message lhs, Message rhs)
    {
        int return_value;

        if(lhs.priority > rhs.priority)
        {
            return_value = 1;
        }
        else if(lhs.priority == rhs.priority)
        {
            if(lhs.origin_port > rhs.origin_port)
            {
                return_value = 1;
            }
            else
            {
                return_value = -1;
            }
        }
        else
        {
            return_value = -1;
        }

        return return_value;
    }
}
