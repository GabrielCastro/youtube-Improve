/*******************************************************************************
 * THE BEER-WARE LICENSE (Revision 42):
 * 
 * <dev@GabrielCastro.ca> wrote this app.
 * As long as you retain this notice you can do whatever
 * you want with this stuff. If we meet some day, and you
 * think this stuff is worth it, you can buy me a beer in return
 * -  Gab 2013
 ******************************************************************************/
package ca.GabrielCastro.improveyoutube;


import android.app.Activity;

import com.google.analytics.tracking.android.EasyTracker;

public abstract class AnalyzedActivity extends Activity {

    @Override
    protected void onStart() {
        super.onStart();
        if (!BuildConfig.DEBUG) {
            EasyTracker.getInstance().activityStart(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!BuildConfig.DEBUG) {
            EasyTracker.getInstance().activityStop(this);
            EasyTracker.getInstance().dispatch();
        }
    }

}
