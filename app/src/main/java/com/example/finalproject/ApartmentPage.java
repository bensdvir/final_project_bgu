package com.example.finalproject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by דביר בן שבת on 19/12/2017.
 */

public class ApartmentPage extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hi);
        Bundle data = getIntent().getExtras();
        final Object pic = data.get("profile_pic");
        final ImageView imageView2 = (ImageView) findViewById(R.id.imageView2);

        new AsyncTask<Void, Void, Void>() {
            Bitmap bmp =  null;
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    InputStream in = new URL(pic.toString()).openStream();
                     bmp = BitmapFactory.decodeStream(in);
                } catch (Exception e) {
                    // log error
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (bmp != null)
                    imageView2.setImageBitmap(bmp);
            }

        }.execute();
        TextView textView2 = (TextView) findViewById(R.id.textView2);
        textView2.setText(data.get("first_name").toString()+" " +data.get("last_name").toString());
    }
}
