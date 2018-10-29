package com.example.tomha.elevatorPitch;

import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

/**
 * Created by tomha on 4-4-2018.
 */

public class SlidingDoor {

    private ImageView view;
    private int side;

    private TranslateAnimation open;
    private TranslateAnimation close;

    SlidingDoor (ImageView view, int side){
        this.view = view;
        this.side = side;
        float moveDistance = (float) 0.0;
        if(side == 0){
            moveDistance = (float) 0.5;
        } else if (side == 1) {
            moveDistance = (float) -0.5;
        }
        open = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, (float) 0, Animation.RELATIVE_TO_PARENT, moveDistance, Animation.RELATIVE_TO_PARENT, (float) 0, Animation.RELATIVE_TO_PARENT, (float) 0);
        open.setDuration(3000);
        open.setFillAfter(true);
        close = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, moveDistance, Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, (float) 0, Animation.RELATIVE_TO_PARENT, (float) 0);
        close.setDuration(3000);
        close.setFillAfter(true);
    }

    public void open(){
        view.startAnimation(open);
    }

    public void close(){
        view.startAnimation(close);
    }
}
