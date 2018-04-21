package com.seojung.hsdproject;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by SeoJung on 16. 5. 19..
 */
public class TabHeart extends Fragment {

    final String TAG = "TabHeart";

    private ImageView mImage;
    private TextView tvBPM;
//    private HeartBeatView heartBeatView;
    private SurfViewHeart heartBeatView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.tab_heart, container, false);
        mImage = (ImageView)view.findViewById(R.id.image);
        Animation pulse = AnimationUtils.loadAnimation(getContext(), R.anim.heartpulse);
        mImage.startAnimation(pulse);
        tvBPM = (TextView)view.findViewById(R.id.bpm);
//        heartBeatView = (HeartBeatView) view.findViewById(R.id.surfViewHeart);
        heartBeatView = (SurfViewHeart) view.findViewById(R.id.surfViewHeart);
        heartBeatView.setZOrderOnTop(true);
        return view;
    }

    public void setY(int Y) {
        if (heartBeatView != null) {
            heartBeatView.setY(Y);
        }
    }
}
