package com.seojung.hsdproject;

import android.content.Context;
import android.util.Log;
import android.widget.MediaController;

/**
 * Created by SeoJung on 16. 6. 7..
 */
public class MusicController extends MediaController {

    public MusicController(Context context, boolean useFastForward) {
        super(context, useFastForward);
    }

    @Override
    public void hide(){
        Log.d("MusicController", "hide!");
        super.hide();
    }

    @Override
    public void show() {
        Log.d("MusicController", "show!");
        super.show(0);
    }

}
