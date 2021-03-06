package com.yeumkyuseok.mathtest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class Test extends AppCompatActivity {

    private static final String TAG = "Test.java";
    public static final String BASE_URL = "https://10.0.2.2:8000/random/question";
    // public static final String BASE_URL = "https://192.168.219.101:8000/random/question";


    Student studentTaking;
    int markScored = 0, questionNumber = 0,  currPage, totalPage, totalAnsButton, currBtnNum, answer;
    long timeLeftInMillis;
    LocalDateTime dateTime, endTime;
    JSONObject jsonObject;
    JSONArray options;
    CountDownTimer countDownTimer;
    Data data;

    // R.layout.ready_state.xml
    Button btnStart, btnDelete, btnSendEmail;
    TextView txtFirstName, txtLastName, txtMark;
    ImageView imageView;
    RecyclerView emailRecyclerView, phoneRecyclerView;

    // R.layout.question_normal_layout.xml
    Button btnPrev, btnNext, btnAns1, btnAns2, btnAns3, btnAns4, btnNormalQuit, btnNormalPass;
    TextView txtNormQuestionNum, txtNormQuestionQuestion, txtNormMark, txtNormPage, txtNormTime;

    // R.layout.question_blank_layout.xml
    Button btnBlankAnswer, btnBlankQuit, btnBlankPass;
    TextView txtBlankQuestionNum, txtBlankQuestionQuestion, txtBlankMark, txtBlankAnswer, txtBlankTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ready_state);

        data = new Data();
        data.load(this);

        txtFirstName = (TextView) findViewById(R.id.textReadyFirstName);
        txtLastName = (TextView) findViewById(R.id.textReadyLastName);
        txtMark = (TextView) findViewById(R.id.textReadyMark);
        btnStart = (Button) findViewById(R.id.buttonStartFromReady);
        btnDelete = (Button) findViewById(R.id.buttonDeleteFromReady);
        imageView = (ImageView) findViewById(R.id.imgReadyState);
        emailRecyclerView = (RecyclerView) findViewById(R.id.recyclerViewEmail);
        phoneRecyclerView = (RecyclerView) findViewById(R.id.recyclerViewPhone);
        btnSendEmail = (Button) findViewById(R.id.buttonSendEmail);

        Intent intent = getIntent();
        studentTaking = (Student) intent.getExtras().get("student");

        txtFirstName.setText("First Name : " + studentTaking.getFirstName());
        txtLastName.setText("Last Name : " + studentTaking.getLastName());
        txtMark.setText("Mark : " + studentTaking.getMark());

        data.loadPersonal(this, studentTaking);
        EmailListAdapter emailListAdapter = new EmailListAdapter(this, data.tempEmails);
        emailRecyclerView.setAdapter(emailListAdapter);
        emailRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        EmailListAdapter phoneListAdapter = new EmailListAdapter(this, data.tempPhones);
        phoneRecyclerView.setAdapter(phoneListAdapter);
        phoneRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (studentTaking.getPhoto().contains("https")) {
            Picasso.get().load(studentTaking.getPhoto()).into(imageView);
        } else {
            Bitmap bitmap = BitmapFactory.decodeFile(studentTaking.getPhoto());

            imageView.setImageBitmap(bitmap);
        }

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateTime = LocalDateTime.now();

                doTest();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data.deleteStudent(studentTaking);
                Intent intent = new Intent(Test.this, MainActivity.class);
                startActivity(intent);
            }
        });

        btnSendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                List<String> emailList = data.getEmail(studentTaking);

                String[] emails = new String[emailList.size()];
                for (int i = 0; i < emailList.size(); i++) {
                    emails[i] =emailList.get(i);
                }

                String message = data.getResultMessage(studentTaking);

                Intent email= new Intent(Intent.ACTION_SENDTO);
                //email.setData(Uri.parse("mailto:your.email@gmail.com"));
                email.setData(Uri.parse("mailto:"));
                email.putExtra(Intent.EXTRA_EMAIL, emails);
                email.putExtra(Intent.EXTRA_SUBJECT, "result");
                email.putExtra(Intent.EXTRA_TEXT, message);
                startActivity(email);
            }
        });
    }

    private void doTest() {
        questionNumber++;
        new DownloaderTask().execute();
    }

    private class DownloaderTask extends AsyncTask<Void, Integer, String> {
        private int totalBytes;

        @Override
        protected String doInBackground(Void... voids) {
            String text;
            try {
                String urlString = Uri.parse(BASE_URL)
                        .buildUpon()
                        .appendQueryParameter("method", "thedata.getit")
                        .appendQueryParameter("format", "json")
                        .appendQueryParameter("api_key", "01189998819991197253")
                        .build().toString();
                URL url = new URL(urlString);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                DownloadUtils.addCertificate(Test.this, (HttpsURLConnection) conn);

                totalBytes = conn.getContentLength();

                try {
                    Log.d(TAG, "Connecting");

                    int responseCode = conn.getResponseCode();
                    String responseMSG = conn.getResponseMessage();
                    if (responseCode != HttpsURLConnection.HTTP_OK) {
                        String msg = String.format("Error from Server: %d - %s", responseCode, responseMSG);
                        Log.d(TAG, msg);
                        throw new IOException(msg);
                    }
                    InputStream is = conn.getInputStream();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    int totalBytesRead = 0;
                    int bytesRead = 0;
                    byte[] buffer = new byte[1024];
                    while ((bytesRead = is.read(buffer)) > 0) {
                        baos.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        publishProgress(totalBytesRead);
                    }
                    baos.close();

                    jsonObject = new JSONObject(new String(baos.toByteArray()));
                    return new String(baos.toByteArray());
                } finally {
                    Log.d(TAG, "Disconnect");
                    conn.disconnect();
                }

            } catch (GeneralSecurityException | IOException | JSONException e) {
                return e.getMessage();
            }

        }

        @Override
        protected void onPostExecute(String result) {
            currPage = 1;
            getNextQuestion();
        }
    }

    private void getNextQuestion() {
        String question;
        JSONArray options;
        int givenTime;

        try {
            // do things here
            Log.d(TAG, "check JsonObject question: " + jsonObject.get("question"));
            Log.d(TAG, "check JsonObject result: " + jsonObject.get("result"));
            Log.d(TAG, "check JsonObject options: " + jsonObject.get("options"));
            Log.d(TAG, "check JsonObject timetosolve: " + jsonObject.get("timetosolve"));

            options = jsonObject.getJSONArray("options");
            answer = jsonObject.getInt("result");

            if (options.length() > 0)  {
                if (options.length() % 4 == 0) {
                    totalPage = options.length() / 4;
                } else {
                    totalPage = (options.length() / 4 ) + 1;
                }
                Log.d(TAG, "getNextQuestion: options.length = " + options.length());
                Log.d(TAG, "getNextQuestion: total page = " + totalPage);
                totalAnsButton = options.length();
                loadNormalQuestion(jsonObject);
            } else {
                loadBlankQuestion(jsonObject);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void loadNormalQuestion(JSONObject jsonObject) {
        setContentView(R.layout.question_normal_layout);

        String questionContent;
        int timeToSolve, remainingTime;

        btnPrev = (Button) findViewById(R.id.buttonPrevAns);
        btnNext = (Button) findViewById(R.id.buttonNextAns);
        btnAns1 = (Button) findViewById(R.id.buttonAns1);
        btnAns2 = (Button) findViewById(R.id.buttonAns2);
        btnAns3 = (Button) findViewById(R.id.buttonAns3);
        btnAns4 = (Button) findViewById(R.id.buttonAns4);
        btnNext = (Button) findViewById(R.id.buttonNextAns);
        btnPrev = (Button) findViewById(R.id.buttonPrevAns);
        btnNormalQuit = (Button) findViewById(R.id.buttonNormalQuit);
        btnNormalPass = (Button) findViewById(R.id.buttonNormalPass);
        txtNormQuestionNum = (TextView) findViewById(R.id.textNormalQuestionNumber);
        txtNormQuestionQuestion = (TextView) findViewById(R.id.textNormalQuestionQuestion);
        txtNormMark = (TextView) findViewById(R.id.textNormalQuestionMark);
        txtNormPage = (TextView) findViewById(R.id.textNormalQuestionPage);
        txtNormTime = (TextView) findViewById(R.id.textNormalTime);

        try {
            questionContent = (String) jsonObject.get("question");
            options = jsonObject.getJSONArray("options");
            timeToSolve = jsonObject.getInt("timetosolve");
            remainingTime = timeToSolve;
            timeLeftInMillis = timeToSolve * 1000;

            setInformation(txtNormQuestionNum, txtNormQuestionQuestion, txtNormMark);
            startTimer(txtNormTime);
            setPage();

            if (options.length() <= 4) {
                currBtnNum = totalAnsButton;
            } else {
                if (currPage < totalPage) {
                    currBtnNum = 4;
                } else {
                    currBtnNum = totalAnsButton - (currPage * 4);
                }
            }
            setButton();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currPage == totalPage) {
                    Toast.makeText(Test.this, "Max Page", Toast.LENGTH_SHORT).show();
                } else {
                    currPage++;
                    setButton();
                    setPage();
                }
            }
        });

        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currPage == 1) {
                    Toast.makeText(Test.this, "Min Page", Toast.LENGTH_SHORT).show();
                } else {
                    currPage--;
                    setButton();
                    setPage();
                }
            }
        });

        btnNormalPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countDownTimer.cancel();
                doTest();
            }
        });

        btnNormalQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countDownTimer.cancel();
                // todo : implement resutl intent
                endTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                Log.d(TAG, "onClick: " + dateTime.format(formatter));
                Log.d(TAG, "onClick: endTime.getSecond() : " + endTime.getSecond());
                Log.d(TAG, "onClick: dateTime.getSecond() : " + dateTime.getSecond());
                long minutes = ChronoUnit.MINUTES.between(dateTime, endTime);
                long seconds = ChronoUnit.SECONDS.between(dateTime, endTime);
                String timeTaken = minutes + "min " + seconds + " sec";
                Log.d(TAG, "onClick: timeTaken : " + timeTaken);
                data.addResult(studentTaking.getFullName(), markScored, dateTime.format(formatter), timeTaken);
                data.addMark(studentTaking, markScored);
                Intent intent = new Intent(Test.this, MainActivity.class);
                startActivity(intent);

            }
        });

        btnAns1.setOnClickListener(ansBtnOnClickListener);
        btnAns2.setOnClickListener(ansBtnOnClickListener);
        btnAns3.setOnClickListener(ansBtnOnClickListener);
        btnAns4.setOnClickListener(ansBtnOnClickListener);
    }

    private void loadBlankQuestion(JSONObject jsonObject) {
        setContentView(R.layout.question_blank_layout);
        btnBlankQuit = (Button) findViewById(R.id.buttonBlankQuestionQuit);
        btnBlankPass = (Button) findViewById(R.id.buttonBlankQuestionPass);
        btnBlankAnswer = (Button) findViewById(R.id.buttonBlankQuestionAnswer);
        txtBlankQuestionNum = (TextView) findViewById(R.id.textBlankQuestionNumber);
        txtBlankQuestionQuestion = (TextView) findViewById(R.id.textBlankQuestionQuestion);
        txtBlankMark = (TextView) findViewById(R.id.textBlankQuestionMark);
        txtBlankTime = (TextView) findViewById(R.id.textBlankTime);
        txtBlankAnswer = (TextView) findViewById(R.id.editTextBlankQuestionAnswer);

        try {
            int timeToSolve;
            timeToSolve = jsonObject.getInt("timetosolve");
            timeLeftInMillis = timeToSolve * 1000;

            setInformation(txtBlankQuestionNum, txtBlankQuestionQuestion, txtBlankMark);
            startTimer(txtBlankTime);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        btnBlankAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countDownTimer.cancel();
                try {
                    int input = Integer.parseInt(txtBlankAnswer.getText().toString());
                    if (input == answer) {
                        Toast.makeText(Test.this, "correct", Toast.LENGTH_SHORT).show();
                        markScored += 10;
                    } else {
                        Toast.makeText(Test.this, "wrong", Toast.LENGTH_SHORT).show();
                        markScored -= 5;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(Test.this, "wrong", Toast.LENGTH_SHORT).show();
                    markScored -= 5;
                }

                doTest();
            }
        });

        btnBlankPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countDownTimer.cancel();
                doTest();
            }
        });

        btnBlankQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countDownTimer.cancel();
                endTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                Log.d(TAG, "onClick: " + dateTime.format(formatter));
                Log.d(TAG, "onClick: endTime.getSecond() : " + endTime.getSecond());
                Log.d(TAG, "onClick: dateTime.getSecond() : " + dateTime.getSecond());
                long minutes = ChronoUnit.MINUTES.between(dateTime, endTime);
                long seconds = ChronoUnit.SECONDS.between(dateTime, endTime);
                String timeTaken = minutes + "min " + seconds + " sec";
                Log.d(TAG, "onClick: timeTaken : " + timeTaken);
                data.addResult(studentTaking.getFullName(), markScored, dateTime.format(formatter), timeTaken);
                data.addMark(studentTaking, markScored);
                Intent intent = new Intent(Test.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }

    private View.OnClickListener ansBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button button = (Button) v;
            countDownTimer.cancel();
            int chosen = Integer.parseInt(button.getText().toString());
            Log.d(TAG, "onClick: " + chosen + " clicked");
            if (answer == chosen){
                Toast.makeText(Test.this, "correct", Toast.LENGTH_SHORT).show();
                markScored += 10;
            } else {
                Toast.makeText(Test.this, "wrong", Toast.LENGTH_SHORT).show();
                markScored -= 5;
            }
            doTest();
        }
    };

    private void setPage() {
        txtNormPage.setText(currPage + " / " + totalPage);
    }

    private void setButton() {
        int startIndex = ((currPage - 1) * 4);
        if (currPage == totalPage) {
            int numberOfButton = totalAnsButton - startIndex;

            try {
                if (numberOfButton == 1) {
                    btnAns1.setText(options.getString(startIndex + 0));
                    btnAns2.setVisibility(View.GONE);
                    btnAns3.setVisibility(View.GONE);
                    btnAns4.setVisibility(View.GONE);
                } else if (numberOfButton == 2) {
                    btnAns1.setText(options.getString(startIndex + 0));
                    btnAns2.setText(options.getString(startIndex + 1));
                    btnAns3.setVisibility(View.GONE);
                    btnAns4.setVisibility(View.GONE);
                } else if (numberOfButton == 3) {
                    btnAns1.setText(options.getString(startIndex + 0));
                    btnAns2.setText(options.getString(startIndex + 1));
                    btnAns3.setText(options.getString(startIndex + 2));
                    btnAns4.setVisibility(View.GONE);
                } else if (numberOfButton == 4) {
                    btnAns1.setText(options.getString(startIndex + 0));
                    btnAns2.setText(options.getString(startIndex + 1));
                    btnAns3.setText(options.getString(startIndex + 2));
                    btnAns4.setText(options.getString(startIndex + 3));
                }
            } catch (JSONException e) {
                Log.d(TAG, "setButton: error");
            }
        } else {
            btnAns1.setVisibility(View.VISIBLE);
            btnAns2.setVisibility(View.VISIBLE);
            btnAns3.setVisibility(View.VISIBLE);
            btnAns4.setVisibility(View.VISIBLE);
            try {
                btnAns1.setText(options.getString(startIndex + 0));
                btnAns2.setText(options.getString(startIndex + 1));
                btnAns3.setText(options.getString(startIndex + 2));
                btnAns4.setText(options.getString(startIndex + 3));
            } catch (JSONException e) {
                Log.d(TAG, "setButton: error");
            }
        }
    }

    private void setInformation(TextView textQuestionNumber, TextView textQuestion, TextView textMark) {
        textQuestionNumber.setText("Question Number " + questionNumber);
        try {
            textQuestion.setText("Question: " + jsonObject.get("question"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        textMark.setText("Mark : " + markScored);
    }



    private void startTimer(TextView textView) {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownTest(textView);
            }

            @Override
            public void onFinish() {
                doTest();
            }
        }.start();
    }

    private void updateCountDownTest(TextView textView) {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(),"%02d:%02d", minutes, seconds);
        textView.setText(timeLeftFormatted);
    }

}