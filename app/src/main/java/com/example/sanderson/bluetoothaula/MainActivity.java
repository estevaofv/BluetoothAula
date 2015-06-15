package com.example.sanderson.bluetoothaula;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import java.util.logging.LogRecord;


public class MainActivity extends ActionBarActivity {

    private static final int REQUEST_ENABLE_BT = 100;
    private BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    private AcceptThread acceptThread;
    private ConnectedThread conn;

    private UUID uuid = UUID.fromString("00002415-0000-1000-8000-00805F9B34FB");
    private static final int MESSAGE_READ = 1;


    private EditText input;
    private TextView output;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        input = (EditText)findViewById(R.id.input);
        output = (TextView) findViewById(R.id.output);

        if(adapter==null){
            Toast.makeText(this,"Não tem bluetooth", Toast.LENGTH_LONG).show();
        }else{
            isBluetoothAtivo();
        }
    }


    public void enviar(View v){
        output.setText("txt:"+input.getText().toString());
    }


    public void escreverNaTela(String msg){
        output.setText(msg);
    }

    private void isBluetoothAtivo(){
        if (!adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else{
            Toast.makeText(this,"Bluetooth está ativo",Toast.LENGTH_SHORT).show();
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        isBluetoothAtivo();
    }


    /**
     * CLASSE PRIVADA PARA ESPERAR
     * UMA CONEXÃO CLIENTE
     */
    private class AcceptThread extends Thread{

        private final BluetoothServerSocket serverSocket;

        private AcceptThread() {

            BluetoothServerSocket tmp = null;

            try {
                tmp = adapter.listenUsingRfcommWithServiceRecord("server",uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.serverSocket = tmp;
        }


        @Override
        public void run() {

            BluetoothSocket socket = null;

            while(true){

                try {
                    //método bloqueante de espera de conexão
                    socket  = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                if(socket !=null){
                    // conexão ok
                    // chamar próxima thread
                    gerenciarConexao(socket);
                }

            }

        }
    }


    /**
     * MÉTODO PONTE
     * ENTRE AS DUAS THREADS
     * @param socket
     */
    private void gerenciarConexao(BluetoothSocket socket){
        this.conn = new ConnectedThread(socket);
    }



    /**
     * CLASSE PRIVADA PARA GERENCIAR A CONEXÃO, ENVIO E RECEBIMENTO
     * DE INFORMAÇÕES VIA BluetoothSocket
     */
    private class ConnectedThread extends Thread{

        private BluetoothSocket socket;
        private InputStream input;
        private OutputStream output;

        private ConnectedThread(BluetoothSocket socket){
            this.socket = socket;
            try {
                input = socket.getInputStream();
                output = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

            byte[] buffer = new byte[1024];
            int bytes;
            try {
                bytes = input.read(buffer);
                // Send the obtained bytes to the UI activity
                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    output.setText(readMessage);

                    break;
            }
        }
    };






    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
