package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GroupMessengerActivity extends Activity implements View.OnClickListener
{
    String origin_port;
    int sequence_number = 0;
    int sequence_number_delivered = 0;
    LinkedBlockingQueue<Message> msgs_to_send;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        // finding the port used by current AVD
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        origin_port = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        msgs_to_send = new LinkedBlockingQueue<>();

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        // registering onClick of Ptest button
        findViewById(R.id.button1).setOnClickListener(new OnPTestClickListener(tv, getContentResolver()));

        // registering send button onClick
        Button b = (Button)findViewById(R.id.button4);
        b.setOnClickListener(this);

        // creating server
        try
        {
            ServerSocket server_socket = new ServerSocket(10000);
            ServerTask server_task = new ServerTask(origin_port);
            server_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, server_socket);
        }
        catch(Exception e)
        {

        }

        // creating client
        try
        {
            ClientTask client_task = new ClientTask(msgs_to_send);
            client_task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
        catch (Exception e)
        {

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    @Override
    public void onClick(View v)
    {
        // Messages are added to queue
        int priority = -1;
        boolean is_delivered = false;

        if(v.getId() == R.id.button4)
        {
            EditText edt_text = (EditText) findViewById(R.id.editText1);
            Message new_msg = new Message(Integer.parseInt(origin_port), edt_text.getText().toString(), sequence_number, priority, is_delivered);

            try
            {
                msgs_to_send.put(new_msg);
            }
            catch (InterruptedException e)
            {

            }

            edt_text.setText("");
            sequence_number++;
        }
    }

    private  class ClientTask extends AsyncTask<String, String, Void>
    {
        LinkedBlockingQueue<Message> msgs_to_send;
        public ClientTask(LinkedBlockingQueue<Message> msgs_to_send)
        {
            this.msgs_to_send = msgs_to_send;
        }

        @Override
        protected Void doInBackground(String... params)
        {
            while(true)
            {
                if(msgs_to_send.size() > 0)
                {
                    Message new_msg = null;

                    try
                    {
                        new_msg = msgs_to_send.take();
                    }
                    catch (Exception e)
                    {

                    }

                    final int [] ports = {11108, 11112, 11116, 11120, 11124};

                    publishProgress("starting new iteration");
                    for(int i = 0; i < 5; i++)
                    {
                        try
                        {
                            // sending msg to other avd
                            Socket socket = new Socket();
                            socket.setSoTimeout(2000);
                            socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), ports[i]));
                            ObjectOutputStream oo = new ObjectOutputStream(socket.getOutputStream());
                            oo.writeObject(new_msg);
                            oo.flush();

                            // receiving msg from other avd, other avd assigns priority to it.
                            ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
                            Message received_msg = (Message)oin.readObject();

                            if(received_msg.priority > new_msg.priority)
                            {
                                new_msg.priority = received_msg.priority;
                            }

                            oo.close();
                            socket.close();
                        }
                        catch (Exception e)
                        {
                            publishProgress("avd crashed " + ports[i]);
                        }

//                        publishProgress("continuing next iteration");
                    }

                    new_msg.delivered = true;
//                    publishProgress("starting new sending iteration");
                    for(int i = 0; i < 5; i++)
                    {
                        try
                        {
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), ports[i]);
                            socket.setSoTimeout(2000);
                            ObjectOutputStream oo = new ObjectOutputStream(socket.getOutputStream());
                            oo.writeObject(new_msg);
                            oo.close();
                            socket.close();
                        }
                        catch (Exception e)
                        {
                            publishProgress("avd crashed while sending final priority" + ports[i]);
                        }

//                        publishProgress("continuing next iteration with port " + ports[i]);
                    }
                }
                else
                {
                    try
                    {
                        Thread.sleep(200);
                    }
                    catch (Exception e)
                    {

                    }
                }
            }
        }

        @Override
        protected void onProgressUpdate(String... args)
        {
            super.onProgressUpdate(args);
            TextView tv = (TextView)findViewById(R.id.textView1);
            tv.append(args[0] + "\r\n");
        }
    }

    class ServerTask extends AsyncTask<ServerSocket, String, Void>
    {
        int sequence_number = 0;
        public final Uri mUri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");
        int origin_port;

        public ServerTask(String origin_port)
        {
            this.origin_port = Integer.parseInt(origin_port);
        }

        @Override
        protected Void doInBackground(ServerSocket... params)
        {
            int proposed_priority = 0;
            PriorityQueue<Message> received_msgs = new PriorityQueue<Message>(50, new MessageComparator());

            try
            {
                while(true)
                {
                    ServerSocket server_socket = params[0];
                    Log.v("statement", "before accepting server socket");
                    Socket incoming_socket = server_socket.accept();
                    incoming_socket.setSoTimeout(2000);
                    Log.v("statement", "accepted server socket");

                    try
                    {
                        ObjectInputStream oin = new ObjectInputStream(incoming_socket.getInputStream());
                        Message msg_received = (Message)oin.readObject();

                        if(msg_received.message == "testing_if_app_crashed")
                        {
                            incoming_socket.close();
                            continue;
                        }

                        if(msg_received.delivered)
                        {
                            if(msg_received.priority >= proposed_priority)
                            {
                                proposed_priority = msg_received.priority + 1;
                            }

                            Iterator<Message> itr = received_msgs.iterator();
                            Message matching_msg_in_queue = null;
                            while(itr.hasNext())
                            {
                                Message msg = itr.next();
                                if((msg.sequence_number == msg_received.sequence_number) && (msg.origin_port == msg_received.origin_port))
                                {
                                    matching_msg_in_queue = msg;
                                    break;
                                }
                            }

                            if(matching_msg_in_queue != null)
                            {
                                received_msgs.remove(matching_msg_in_queue);
                                received_msgs.add(msg_received);
                            }

                            while((received_msgs.size() > 0))
                            {
                                Message msg = received_msgs.peek();
                                if(msg.delivered)
                                {
                                    publishProgress(msg.message, msg.priority + "  " + msg.origin_port, "add");
                                    received_msgs.poll();
                                }
                                else
                                {
                                    try
                                    {
                                        Message test_msg = new Message(12345, "testing_if_app_crashed", 1, 1, false);
                                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), (msg.origin_port*2));
                                        socket.setSoTimeout(2000);
                                        ObjectOutputStream oo = new ObjectOutputStream(socket.getOutputStream());
                                        oo.writeObject(test_msg);
                                        oo.close();
                                        socket.close();
                                        publishProgress(" ", "message not delivered from " + msg.origin_port, "debug");
                                        break;
                                    }
                                    catch (Exception e)
                                    {
                                        publishProgress(" ", "found app crashed from server", "debug");
                                        received_msgs.poll();
                                    }
                                }
                            }
                        }
                        else
                        {
//                            publishProgress(msg_received.message, " " + msg_received.origin_port + "  " + origin_port + " " + proposed_priority, "debug");
                            msg_received.priority = proposed_priority;
                            received_msgs.add(msg_received);
                            proposed_priority++;

                            // send ur priority
                            ObjectOutputStream oo = new ObjectOutputStream(incoming_socket.getOutputStream());
                            oo.writeObject(msg_received);
                            oo.close();
                        }

                        incoming_socket.close();
                    }
                    catch (Exception e)
                    {
                        Log.e("server_error", e.toString());
                    }
                }
            }
            catch(Exception e)
            {
                Log.e("error in socket creation", e.toString());
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values)
        {
            final String msg = values[0];
            final String priority = values[1];
            final String debug = values[2];
            super.onProgressUpdate(values);

            if(debug.equals("debug"))
            {
                TextView txt_view = (TextView)findViewById(R.id.textView1);
                txt_view.append(" debug: " + values[1] + "\r\n");
                return;
            }

            TextView txt_view = (TextView)findViewById(R.id.textView1);
            txt_view.append(msg + " delivered " + sequence_number_delivered + "\r\n");
            ContentValues key_value_to_insert = new ContentValues();
            key_value_to_insert.put("key", "" + sequence_number_delivered);
            key_value_to_insert.put("value", msg);
            getContentResolver().insert(mUri, key_value_to_insert);

            sequence_number_delivered++;

            Log.v("sequence number is ", "" + sequence_number_delivered);
        }
    }
}