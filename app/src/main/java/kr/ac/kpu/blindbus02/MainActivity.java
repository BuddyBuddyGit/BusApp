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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

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

    // 버스 API를 위한 변수들
    String BusTime_data;
    TextView busTime_text;
    String resultStr;
    String arrive;

    final int[] thread_flag = {0};
    final int[] thread_num = {0};

    String ServerIP = "tcp://IP 주소:1883";
    String TOPIC = "test";
    final MqttClient[] mqttClient = {null};
    String cancelStr = "cancel Bus";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        busTime_text = (TextView)findViewById(R.id.busTime);
        LinearLayout minuteLeft = (LinearLayout) findViewById(R.id.minuteLeft);

        // 버스 음성 확인
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
            if(busNum.getText().equals(resultStr + "번")){
                funcVoiceOut("이미 예약하신 버스가 있습니다.");
            } else {
                mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                mRecognizer.setRecognitionListener(listener);
                mRecognizer.startListening(intent);
            }
        });

        minuteLeft.setOnClickListener(v -> {
            if (busTime_text.getText().equals("몇 분 전") || busTime_text.getText().equals("도착 정보 없음")){
                funcVoiceOut("현재 예약된 버스가 없습니다.");
            } else {
                funcVoiceOut(busTime_text.getText() + " 남았습니다.");
            }
        });


        // MQTT 서버에 취소 문장 올리기
        Button cancelBtn = (Button) findViewById(R.id.busCancelBtn);
        cancelBtn.setOnClickListener(v -> {
            if(busTime_text.getText().equals("몇 분 전") || busTime_text.getText().equals("도착 정보 없음")){
                funcVoiceOut("현재 예약된 버스가 없습니다.");
            } else {
                try {
                    mqttClient[0] = new MqttClient(ServerIP, MqttClient.generateClientId(), null);
                    mqttClient[0].connect();

                    try {
                        mqttClient[0].publish(TOPIC, new MqttMessage(cancelStr.getBytes()));
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

                    mqttClient[0].subscribe(TOPIC);
                    mqttClient[0].setCallback(new MqttCallback() {
                        @Override
                        public void connectionLost(Throwable throwable) {
                            Log.d("MQTTService", "Connection Lost");
                        }

                        @Override
                        public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                            Log.d("MQTTService", "Message Arrived : " + mqttMessage.toString());
                        }

                        @Override
                        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                            Log.d("MQTTService", "Delivery Complete");
                        }
                    });
                } catch (MqttException e) {
                    e.printStackTrace();
                }

                thread_flag[0] = 0;
                thread_num[0] = 0;
                busNum.setText("버스 번호");
                busTime_text.setText("몇 분 전");
                funcVoiceOut("예약하신 버스가 취소되었습니다.");
            }
        });
    }


    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Toast.makeText(getApplicationContext(), "음성인식을 시작합니다.", Toast.LENGTH_SHORT).show();
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
                    message = "오디오 에러";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "클라이언트 에러";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "퍼미션 없음";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트웍 타임아웃";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "다시 한 번 말씀해주세요";
                    funcVoiceOut(message);
                    // 말이 들리지 않은 것이기에 다시 한번 말해달라고 음성
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER가 바쁨";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "서버가 이상함";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "말하는 시간초과";
                    break;
                default:
                    message = "알 수 없는 오류임";
                    break;
            }

            Toast.makeText(getApplicationContext(), "에러가 발생하였습니다. : " + message,Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            String[] busList = {"1", "22", "23", "55", "3400", "5200"};

            int Nonbus = 0;
            resultStr = "";

            for(int i = 0; i < matches.size() ; i++){
                // textView.setText(matches.get(i));
                resultStr += matches.get(i);
            }

            if(resultStr.length() < 1) return;
            resultStr = resultStr.replace(" ", "");
            resultStr = resultStr.replace("번", "");
            for(int i=0; i<busList.length; i++){
                if(resultStr.equals(busList[i])){
                    busNum.setText(resultStr + "번");
                    thread_flag[0] = 1;
                }
                else Nonbus++;
            }
            if(Nonbus == 6) {
                funcVoiceOut("예약하신 버스가 현재 정류장에 존재하지 않습니다.");
                busNum.setText(resultStr + "번 버스 없음");
                Nonbus=0;
                busTime_text.setText("몇 분 전");
            }


            // 정류장에 있는 버스들 목록 가져와서 일치 하지 않는 버스 말하면 다시 말해 달라고 활성화
            // 정류장에 있는 버스들 중 하나면 버스 예약 버튼 비활성화 -> 버튼을 누르면 이미 예약한 버스가 있다는 안내 말 나오게 설정함.
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub

                    // 아래 메소드를 호출하여 XML data를 파싱해서 String 객체로 얻어오기
                    // 버스 api
                    while(thread_flag[0] == 1){
                        thread_num[0]++;
                        BusTime_data = getBusXmlData();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // TODO Auto-generated method stub
                                busTime_text.setText(BusTime_data);//TextView에 문자열  data 출력
                                arrive = busTime_text.getText().toString();
                                if(thread_num[0] == 1){
                                    if(arrive.equals("도착 정보 없음")){
                                        thread_num[0] = 0;
                                        funcVoiceOut(resultStr + "번 버스의 도착 정보가 없어 예약되지 않았습니다.");
                                        // 버스 도착정보 없을 시 자동 예약되게 할지, 사용자에게 예약을 선택하게 할지 결정해야함.
                                        // -> 도착정보가 없어 예약되지 않았다고 안내해줌.
                                    }
                                    else{
                                        // thread_num[0] = 0;
                                        funcVoiceOut(resultStr + "번 버스가 예약됩니다.  " + arrive + " 후에 도착합니다.");

                                        // 버스 예약 번호 서버에 올리기
                                        try {
                                            mqttClient[0] = new MqttClient(ServerIP, MqttClient.generateClientId(), null);
                                            mqttClient[0].connect();

                                            try {
                                                mqttClient[0].publish(TOPIC, new MqttMessage(resultStr.getBytes()));
                                            } catch (MqttException e) {
                                                e.printStackTrace();
                                            }

                                            mqttClient[0].subscribe(TOPIC);
                                            mqttClient[0].setCallback(new MqttCallback() {
                                                @Override
                                                public void connectionLost(Throwable throwable) {
                                                    Log.d("MQTTService", "Connection Lost");
                                                }

                                                @Override
                                                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                                                    Log.d("MQTTService", "Message Arrived : " + mqttMessage.toString());
                                                }

                                                @Override
                                                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                                                    Log.d("MQTTService", "Delivery Complete");
                                                }
                                            });
                                        } catch (MqttException e) {
                                            e.printStackTrace();
                                        }

                                        // funcVoiceOut(resultStr + "번 버스가 예약됩니다. 버스 도착 정보가 없습니다.");
                                    }

                                }
                                else{
                                    if(arrive.equals("2분")){
                                        funcVoiceOut( "2분 후 버스가 도착합니다. 탑승 준비를 해주세요");
                                    }
                                    if(arrive.equals("1분")){
                                        funcVoiceOut( "잠시후 버스가 도착합니다. 탑승 준비를 해주세요");
                                        thread_flag[0] = 0;
                                        thread_num[0] = 0;
                                        busTime_text.setText("탑승 준비");
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
            Log.e("TTS", "초기화 실패");
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
        String queryUrl =  ""; // url 복붙

        int Non=0;
        int List = 0;

        try {
            URL url= new URL(queryUrl);// 문자열로 된 요청 url을 URL 객체로 생성.
            InputStream is= url.openStream(); // url 위치로 입력스트림 연결

            XmlPullParserFactory factory= XmlPullParserFactory.newInstance();
            XmlPullParser xpp= factory.newPullParser();
            xpp.setInput( new InputStreamReader(is, "UTF-8") ); //inputstream 으로부터 xml 입력받기

            String tag;
            String busTime="1";

            xpp.next();
            int eventType= xpp.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT){
                switch(eventType){
                    case XmlPullParser.START_DOCUMENT:
                        break;

                    case XmlPullParser.START_TAG:
                        tag= xpp.getName();//태그 이름 얻어오기
                        if(tag.equals("predictTime1")) {
                            xpp.next();
                            busTime = xpp.getText();
                        }


                        else if(tag.equals("routeId")) {
                            xpp.next();
                            List++;

                            if(xpp.getText().equals("213000006")){
                                real_busnum = "1";
                            } else if(xpp.getText().equals("216000004")){
                                real_busnum = "22";
                            } else if(xpp.getText().equals("224000003")) {
                                real_busnum = "23";
                            } else if(xpp.getText().equals("216000011")){
                                real_busnum = "55";
                            } else if(xpp.getText().equals("224000050")){
                                real_busnum = "3400";
                            } else if(xpp.getText().equals("224000052")){
                                real_busnum = "5200";
                            } else real_busnum = "0";

                            if(real_busnum.equals(resultStr)){
                                buffer.append(busTime);
                                buffer.append("분");
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
            buffer.append("도착 정보 없음");
            List = 0;
            Non = 0;
        }
        return buffer.toString();//StringBuffer 문자열 객체 반환
    }

}
