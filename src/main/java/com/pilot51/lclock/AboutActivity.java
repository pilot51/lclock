package com.pilot51.lclock;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

/**
 * Created by jacob_000 on 7/29/2015.
 */
public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        TextView info = (TextView) findViewById(R.id.about_info);
        String text = "• <a href=\"http://pilot51.com/wiki/L-Clock\">L-Clock</a> for original idea and source code<p>"+
                "• <a href=\"spaceflightnow.com/launch-schedule\">SpaceFlightNow</a> for data on launches<p>"+
                "• <a href=\"https://github.com/bumptech/glide\">Glide</a> for an amazing image library<p>"+
                "• <a href=\"http://imgur.com/a/OJGIP\">All images with sources</a>";
        info.setText(Html.fromHtml(text));
        Linkify.addLinks(info, Linkify.ALL);
        info.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
