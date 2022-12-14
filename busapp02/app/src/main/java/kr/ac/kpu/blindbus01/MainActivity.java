package kr.ac.kpu.blindbus02;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    Intent intent;
    SpeechRecognizer mRecognizer;
    TextToSpeech tts;
    Button sttBtn;
    TextView busNum;
    final int PERMISSION = 1;

    String BusTime_data;
    TextView busTime_text;
    String resultStr;
    String arrive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        busTime_text = (TextView)findViewById(R.id.busTime);

        if ( Build.VERSION.SDK_INT >= 23 ){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET,
                    Manifest.permission.RECORD_AUDIO},PERMISSION);
        }

        busNum = (TextView)findViewById(R.id.busNum);
        sttBtn = (Button) findViewById(R.id.busReservationBtn);

        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");

        tts = new TextToSpeech(MainActivity.this, this);

        sttBtn.setOnClickListener(v -> {
            mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            mRecognizer.setRecognitionListener(listener);
            mRecognizer.startListening(intent);
        });
    }


    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Toast.makeText(getApplicationContext(), "??????????????? ???????????????.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onError(int error) {
            String message;

            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "????????? ??????";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "??????????????? ??????";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "????????? ??????";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "???????????? ??????";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "????????? ????????????";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "?????? ??? ??? ??????????????????";
                    funcVoiceOut(message);
                    // ?????? ????????? ?????? ???????????? ?????? ?????? ??????????????? ??????
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER??? ??????";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "????????? ?????????";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "????????? ????????????";
                    break;
                default:
                    message = "??? ??? ?????? ?????????";
                    break;
            }

            Toast.makeText(getApplicationContext(), "????????? ?????????????????????. : " + message,Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            String[] busList = {"20", "11", "12", "26"};
            int Nonbus = 0;
            resultStr = "";
            final int[] thread_flag = {0};
            final int[] thread_num = {0};


            for(int i = 0; i < matches.size() ; i++){
                // textView.setText(matches.get(i));
                resultStr += matches.get(i);
            }

            if(resultStr.length() < 1) return;
            resultStr = resultStr.replace(" ", "");
            resultStr = resultStr.replace("???", "");
            for(int i=0; i<busList.length; i++){
                if(resultStr.equals(busList[i])){
                    busNum.setText(resultStr + "???");
                    thread_flag[0] = 1;
                }
                else Nonbus++;
            }
            if(Nonbus == 4) {
                funcVoiceOut("???????????? ????????? ?????? ???????????? ???????????? ????????????.");
                busNum.setText(resultStr + "??? ?????? ??????");
                Nonbus=0;
                busTime_text.setText("??? ??? ???");
            }


        // ???????????? ?????? ????????? ?????? ???????????? ?????? ?????? ?????? ?????? ????????? ?????? ?????? ????????? ?????????
            // ???????????? ?????? ????????? ??? ????????? ?????? ?????? ?????? ????????????
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub

                    // ?????? ???????????? ???????????? XML data??? ???????????? String ????????? ????????????
                    // ?????? api
                    while(thread_flag[0] == 1){
                        thread_num[0]++;
                        BusTime_data = getBusXmlData();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // TODO Auto-generated method stub
                                busTime_text.setText(BusTime_data);//TextView??? ?????????  data ??????
                                arrive = busTime_text.getText().toString();
                                if(thread_num[0] == 1){
                                    if(arrive.equals("?????? ?????? ??????")){
                                        funcVoiceOut(resultStr + "??? ????????? ???????????????. ?????? ?????? ????????? ????????????.");
                                    }
                                    else{
                                        funcVoiceOut(resultStr + "??? ????????? ???????????????." + arrive + "??? ??? ???????????????.");
                                    }

                                }
                                else{
                                    if(arrive.equals("2???")){
                                        funcVoiceOut( "2??? ??? ????????? ???????????????. ?????? ????????? ????????????");
                                    }
                                    if(arrive.equals("1???")){
                                        funcVoiceOut( "????????? ????????? ???????????????. ?????? ????????? ????????????");
                                        thread_flag[0] = 0;
                                        thread_num[0] = 0;
                                        busTime_text.setText("?????? ??????");
                                    }

                                }
                            }
                        });
                        try{
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }).start();
        }

        @Override
        public void onPartialResults(Bundle partialResults) {

        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
    };

    public void funcVoiceOut(String OutMsg){
        if(OutMsg.length()<1)return;
        if(!tts.isSpeaking()) {
            tts.speak(OutMsg, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.KOREAN);
            tts.setPitch(1);
        } else {
            Log.e("TTS", "????????? ??????");
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if(mRecognizer!=null){
            mRecognizer.destroy();
            mRecognizer.cancel();
            mRecognizer=null;
        }
        super.onDestroy();
    }

    String real_busnum;

    String getBusXmlData(){
        StringBuffer buffer=new StringBuffer();
        String queryUrl = "";//????????? ??????

        int Non=0;
        int List = 0;

        try {
            URL url= new URL(queryUrl);//???????????? ??? ?????? url??? URL ????????? ??????.
            InputStream is= url.openStream(); //url????????? ??????????????? ??????

            XmlPullParserFactory factory= XmlPullParserFactory.newInstance();
            XmlPullParser xpp= factory.newPullParser();
            xpp.setInput( new InputStreamReader(is, "UTF-8") ); //inputstream ???????????? xml ????????????

            String tag;
            String busTime="1";

            xpp.next();
            int eventType= xpp.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT){
                switch(eventType){
                    case XmlPullParser.START_DOCUMENT:
                        break;

                    case XmlPullParser.START_TAG:
                        tag= xpp.getName();//?????? ?????? ????????????
                        if(tag.equals("predictTime1")) {
                            xpp.next();
                            busTime = xpp.getText();
                        }


                        else if(tag.equals("routeId")) {
                            xpp.next();
                            List++;
                            if(xpp.getText().equals("224000036")){
                                real_busnum = "11"; //11-A
                            } else if(xpp.getText().equals("224000011")){
                                real_busnum = "20"; //20-1
                            } else if(xpp.getText().equals("224000037")){
                                real_busnum = "12"; //11-B
                            } else if(xpp.getText().equals("224000639")) {
                                real_busnum = "26";
                            }
                            else real_busnum = "0";
                            if(real_busnum.equals(resultStr)){
                                buffer.append(busTime);
                                buffer.append("???");
                            }
                            else Non++;

                        }
                        break;
                    case XmlPullParser.TEXT:
                        break;

                    case XmlPullParser.END_TAG:
                        tag= xpp.getName();
                        break;
                }
                eventType= xpp.next();
            }

        } catch (Exception e) {
            // TODO Auto-generated catch blocke.printStackTrace();
        }
        if(List == Non){
            buffer.append("?????? ?????? ??????");
        }
        List = 0;
        Non = 0;
        return buffer.toString();//StringBuffer ????????? ?????? ??????
    }

}