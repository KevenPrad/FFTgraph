package com.example.fftgraph;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;


//https://www.developpez.net/forums/d938180/java/general-java/java-mobiles/android/generer/
public class MainActivity extends AppCompatActivity {

    private Button play;
    private Button stop;
    private Button record;
    private Button stopRecord;
    private TextView txt1;
    private Button calcul;
    private Button init;
    private GraphView graph;

    MediaPlayer mediaPlayer;
    MediaRecorder mediaRecorder;
    String pathsave="";

    final int REQUEST_PERMISSION_CODE=1000;

    AudioTrack audioOut = null;
    int sampleRate = 44100;

    int minSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

    byte [] music;
    short[] music2Short;

    InputStream is;

    FileInputStream file1 = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        if(!checkPermissionFromDevice()){
            RequestPermission();
        }
        this.stop= findViewById(R.id.stop);
        this.play= findViewById(R.id.play);
        this.stopRecord= findViewById(R.id.stopRecord);
        this.record= findViewById(R.id.record);
        calcul=findViewById(R.id.calcul);
        graph=findViewById(R.id.graph);
        init=findViewById(R.id.initialise);


        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calcul.setEnabled(true);
                stop.setEnabled(false);
                stopRecord.setEnabled(true);
                play.setEnabled(false);
                pathsave = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"
                        + UUID.randomUUID()+toString()+"Audio_recorder.Jgp";
                setUpMediaRecorder();
                try{
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                }
                catch(IOException e){
                    e.printStackTrace();
                }

                Toast.makeText(MainActivity.this, "Recording", Toast.LENGTH_SHORT).show();
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop.setEnabled(true);
                stopRecord.setEnabled(false);
                record.setEnabled(false);
                calcul.setEnabled(false);

                mediaPlayer = new MediaPlayer();

                try{
                    mediaPlayer.setDataSource(pathsave);
                    mediaPlayer.prepare();
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
                mediaPlayer.start();
                Toast.makeText(MainActivity.this, "Playing", Toast.LENGTH_SHORT).show();
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer != null){
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }
                calcul.setEnabled(false);
                record.setEnabled(true);
                stopRecord.setEnabled(false);
                play.setEnabled(true);
            }
        });

        stopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaRecorder.stop();
                calcul.setEnabled(true);
                record.setEnabled(true);
                stop.setEnabled(false);
                play.setEnabled(true);
            }
        });

        calcul.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playAudio();

                if (is != null){

                    int i;
                    //buffer with the signal
                    try{
                        while (((i = is.read(music)) != -1)) {
                            ByteBuffer.wrap(music).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(music2Short);
                        }
                    } catch (IOException e){
                        e.printStackTrace();
                    }

                    //timeSize should be a power of two.
                    int timeSize= 2^(nearest_power_2(minSize));

                    FFT a = new FFT(1024,sampleRate);

                    a.forward(Tofloat(music2Short));
                    //txt1.setText(Float.toString(a.real[500]));
                    LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(generateData(a,800));

                    graph.addSeries(series);
                }
            }
        });

        init.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                graph.removeAllSeries();
            }
        });


    }

    private void setUpMediaRecorder(){
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(pathsave);
        //mediaRecorder.setOutputFile();
    }

    private boolean checkPermissionFromDevice() {
        int result_from_storage_permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int record_audio_result = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return (result_from_storage_permission == PackageManager.PERMISSION_GRANTED) &&
                (record_audio_result == PackageManager.PERMISSION_GRANTED);
    }

    private void RequestPermission(){
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case REQUEST_PERMISSION_CODE:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(MainActivity.this, "Permission granted", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
            break;
        }

    }

    public void initialize(){

        File initialFile = new File(pathsave);
        try{
            is = new FileInputStream(initialFile);
        }
        catch (IOException e){
            e.printStackTrace();
        }




        audioOut = new AudioTrack(
                AudioManager.STREAM_MUSIC,          // Stream Type
                sampleRate,                         // Initial Sample Rate in Hz
                AudioFormat.CHANNEL_OUT_MONO,       // Channel Configuration
                AudioFormat.ENCODING_PCM_16BIT,     // Audio Format
                minSize,                            // Buffer Size in Bytes
                AudioTrack.MODE_STREAM);            // Streaming static Buffer

    }

    public int nearest_power_2(int x){
        int i=0;
        int p = 1;
        while (p<x){
            p=2*p;
            i++;
        }
        if ((x-p)<(x-p/2))
            return i;
        else
            return i-1;
    }

    public void playAudio() {

        this.initialize();

        if ( (minSize/2) % 2 != 0 ) {
            /*If minSize divided by 2 is odd, then subtract 1 and make it even*/
            music2Short     = new short [((minSize/2) - 1)/2];
            music           = new byte  [(minSize/2) - 1];
        }
        else {
            /* Else it is even already */
            music2Short     = new short [minSize/4];
            music           = new byte  [minSize/2];
        }
    }

    public float[] Tofloat(short[] s){
        int len = s.length;
        float[] f= new float[len];
        for (int i=0;i<len;i++){
            f[i]=s[i];
        }
        return f;
    }

    private DataPoint[] generateData(FFT a, int total) {
        int x;
        double y;
        float c;
        float b;
        float z;

        DataPoint[] values = new DataPoint[total];
        for (int j=0;j<total;j++){
            x=j;
            c=a.real[j]*a.real[j];
            b=a.imag[j]*a.imag[j];
            z=Math.abs(b+c);
            y=20*Math.log(z);
            DataPoint v = new DataPoint(x,y);
            values[j]=v;
        }
        return values;
    }
}