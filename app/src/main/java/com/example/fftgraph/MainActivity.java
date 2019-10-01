package com.example.fftgraph;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
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
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;


//https://www.developpez.net/forums/d938180/java/general-java/java-mobiles/android/generer/
public class MainActivity extends AppCompatActivity {

    //Activity layout statement
    private Button play;
    private Button stop;
    private Button record;
    private Button stopRecord;
   //private TextView txt1;
    private Button calcul;
    private Button init;
    private GraphView graph;
    private TextView detect;
    private TextView problem;

    int recording=0;
    int stop_recording=0;

    //file error statement
    File extStore = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    //final String pathsave= extStore.getAbsolutePath()+"/"+ UUID.randomUUID()+toString()+"Audio_recorder.txt";
    //final File fiche = new File(pathsave);

    //record statement
    MediaPlayer mediaPlayer;
    MediaRecorder mediaRecorder;

    final int REQUEST_PERMISSION_CODE=1000;

    AudioTrack audioOut = null;
    int sampleRate = 44100;
    int bufferSizes=1024; //1024
    int frequency=4948;
    int deltaF=sampleRate/bufferSizes;


    int minSize = AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);

    //fft statement
    byte [] music;
    short[] music2Short;

    InputStream is;

    //FileInputStream file1 = null;


    //wave recorder statement
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bufferSize = minSize;

        setButtonHandlers();


        if(!checkPermissionFromDevice()){
            RequestPermission();
        }

        if(checkPermissionFromDevice()){
            problem.setText("bonne permission");
            //yo
        }

        if(isExternalStorageWritable()){
            problem.setText("external storage writable ");
        }

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calcul.setEnabled(true);
                stop.setEnabled(false);
                stopRecord.setEnabled(true);
                play.setEnabled(false);
                record.setEnabled(false);

                recording++;
                //PrintFile("record n°"+recording,fiche);

                if (isExternalStorageWritable()) {

                    AppLog.logString("Start Recording");

                    startRecording();


                    Toast.makeText(MainActivity.this, "Recording", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MainActivity.this, "Disque externe non disponible,ou non diponible en écriture", Toast.LENGTH_SHORT).show();
                }
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop.setEnabled(true);
                stopRecord.setEnabled(false);
                record.setEnabled(false);
                calcul.setEnabled(false);
                play.setEnabled(false);

                mediaPlayer = new MediaPlayer();

                try{
                    mediaPlayer.setDataSource(getFilename());
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
                stop.setEnabled(false);
            }
        });

        stopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calcul.setEnabled(true);
                record.setEnabled(true);
                stop.setEnabled(false);
                play.setEnabled(true);
                stopRecord.setEnabled(false);

                stop_recording++;
                //PrintFile("stop record n°"+stop_recording,fiche);

                AppLog.logString("Start Recording");

                try {
                    stopRecording();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        calcul.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //PrintFile("calucl start",fiche);
                try {
                    playAudio();
                    //PrintFile("palyAudio done",fiche);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (is != null){

                    //buffer with the signal
                    try{
                        while (((is.read(music)) != -1)) {
                            ByteBuffer.wrap(music).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(music2Short);
                            //PrintFile("ByteBuffer in progress",fiche);
                        }
                    } catch (IOException e){
                        e.printStackTrace();
                        detect.setText("calcul" + e);
                    }

                    //PrintFile("ByteBuffer all done",fiche);
                    //timeSize should be a power of two.
                    //int timeSize= 2^(nearest_power_2(minSize));

                    FFT a = new FFT(bufferSizes,sampleRate);
                    //PrintFile("fft initialisation done",fiche);

                    //int [] values={645,860,1290};

                    a.forward(Tofloat(music2Short));
                    //PrintFile("analyse of the buffer done",fiche);
                    //txt1.setText(Float.toString(a.real[500]));
                    //PointsGraphSeries<DataPoint> series = new PointsGraphSeries<DataPoint>(generateSelectedDataSpike(a,values));
                    //PointsGraphSeries<DataPoint> series = new PointsGraphSeries<DataPoint>(generateData(a,200));

                    LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(generateData(a,200));

                    graph.addSeries(series);
                    if (calculSeuil(frequency ,a) && calculSeuil(860,a)){
                        //detect.setText("detected" +frequency +"Hz"+a.real[Math.round(frequency/43)]+"\n");
                    }
                    else {
                       // detect.setText("not detected"+frequency +"Hz"+a.real[Math.round(frequency/43)]+"\n");
                    }
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

    private void setButtonHandlers(){
        this.stop= findViewById(R.id.stop);
        this.play= findViewById(R.id.play);
        this.stopRecord= findViewById(R.id.stopRecord);
        this.record= findViewById(R.id.record);
        calcul=findViewById(R.id.calcul);
        graph=findViewById(R.id.graph);
        init=findViewById(R.id.initialise);
        detect=findViewById(R.id.detection);
        problem=findViewById(R.id.problem);
    }

    private String getFilename() throws IOException {

        String path = "/storage/emulated/legacy/Download";
        File extStore = new File(path);
                //Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        //return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/Audio_recorder.wav";
        return (extStore.getAbsolutePath() + "/"+ "AudioRecorder" + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private void setUpMediaRecorder() throws IOException {

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //wav
        //mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.


        //mp3
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(getFilename());
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        //mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        //mediaRecorder.setOutputFile(pathsave);
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

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            Toast.makeText(this, "memoire externe disponible en écriture", Toast.LENGTH_SHORT).show();
            return true;

        }
        return false;
    }

    public void initialize() throws IOException {

        /*
        File initialFile = new File(getFilename());

        problem.setText("jusqu'ici tout va bien");
        try{
            is = new FileInputStream(getFilename());
            detect.setText("victoire is ");
        }
        catch (IOException e){
            e.printStackTrace();
            detect.setText("d" + e);
        }*/

        File initialFile;
        try {
            initialFile = new File(getFilename());
            problem.setText("init file succeed" );
        } catch (IOException e) {
            e.printStackTrace();
            //problem.setText("init fft:");
        }
        try{
            is = new FileInputStream(getFilename());
            problem.setText("init is succeed");
        }
        catch (IOException e) {
            e.printStackTrace();
            //problem.setText(getFilename() + "problem is : " + e);
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

    public void playAudio() throws IOException {

        detect.setText("playAudio begin ");
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
        //float z;

        DataPoint[] values = new DataPoint[total];
        for (int j=0;j<total;j++){
            x=j*deltaF;
            c=a.real[j]*a.real[j];
            b=a.imag[j]*a.imag[j];
            y=Math.abs(b+c)/1000000000;
            DataPoint v = new DataPoint(x,y);
            values[j]=v;
        }
        return values;
    }

    private DataPoint[] generateSelectedDataSpike(FFT a, int[] values) {
        double y;
        int x;
        float c;

        int len=values.length;
        DataPoint[] point = new DataPoint[len];
        for (int j=0;j<len;j++){

            x=values[j];
            //frequency conversion to point
            float b= x/43;
            int d = Math.round(b);
            c=a.real[d]*a.real[d];
            b=a.imag[d]*a.imag[d];
            y=Math.abs(b+c)/1000000000;
            DataPoint v = new DataPoint(x,y);
            point[j]=v;
        }
        return point;
    }

    public boolean calculSeuil(int frequency, FFT a){
        float x= frequency/43;
        int j = Math.round(x);
        float seuil=Math.abs(a.real[j]*a.real[j]+a.imag[j]*a.imag[j])/1000000000;
        return seuil>=1000;
    }

    private String getTempFilename() throws IOException {
        String filepath = "/storage/emulated/legacy/Download";
                //Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                //Environment.getDownloadCacheDirectory().getPath();
                //Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath);

        if (!file.exists()) {

            file.createNewFile();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private void writeAudioDataToFile() throws IOException {
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
// TODO Auto-generated catch block
            e.printStackTrace();
            //problem.setText(getTempFilename() + "problem writeAUdio : " + e);
        }
        int read = 0;

        if (null != os) {
            while (isRecording) {
                read = recorder.read(data, 0, bufferSizes);

                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startRecording(){
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

        int i = recorder.getState();
        if(i==1)
            recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    writeAudioDataToFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        },"AudioRecorder Thread");

        recordingThread.start();
    }


    private void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            AppLog.logString("File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    private void deleteTempFile() throws IOException {
        File file = new File(getTempFilename());

        file.delete();
    }

    private void stopRecording() throws IOException {
        if(null != recorder){
            isRecording = false;

            int i = recorder.getState();
            if(i==1)
                recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }

        copyWaveFile(getTempFilename(),getFilename());
        deleteTempFile();
    }


    private void PrintFile(String text, File fiche){

        try {
            final FileWriter writer = new FileWriter(fiche);
            try {
                writer.write(text +"\n");
            } finally {
                // quoiqu'il arrive, on ferme le fichier
                writer.close();
            }
        } catch (Exception e) {
            System.out.println("Impossible de creer le FileWriter");
        }
    }
}