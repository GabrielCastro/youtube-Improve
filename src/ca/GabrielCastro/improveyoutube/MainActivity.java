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


import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;
import ca.GabrielCastro.improveyoutube.R;
import eu.chainfire.libsuperuser.Shell;
import eu.chainfire.libsuperuser.StreamGobbler.OnLineListener;

public class MainActivity extends AnalyzedActivity implements OnCheckedChangeListener {

    private static final String[] ENABLE_CMDS = new String[] { "iptables -A OUTPUT -d 173.194.55.0/24 -j DROP", "iptables -A OUTPUT -d 206.111.0.0/16 -j DROP" };
    private static final String[] DISABLE_CMDS = new String[] { "iptables -D OUTPUT -d 173.194.55.0/24 -j DROP", "iptables -D OUTPUT -d 206.111.0.0/16 -j DROP" };

    private ToggleButton mToggle;
    @SuppressWarnings("unused")
    private boolean mEnabled = false;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToggle = (ToggleButton) findViewById(R.id.toggle_button);
        mToggle.setOnCheckedChangeListener(this);
        mToggle.setEnabled(true);
    }
    
    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        if (buttonView.getId() != R.id.toggle_button || !buttonView.isEnabled()) {
            return;
        }
        SetIPTables task = new SetIPTables(this, false, isChecked ? "Enabling" : "Disabling");
        task.setOnTaskCompleteListener(new SetIPTables.OnTaskComplete() {

            @Override
            public void onTaskComplete(boolean sucess) {
                if (sucess) {
                    mEnabled = isChecked;
                    buttonView.setEnabled(true);
                } else {
                    buttonView.setEnabled(false);
                    buttonView.setChecked(!isChecked);
                    mEnabled = !isChecked;
                    buttonView.setEnabled(true);
                }
            }
        });

        task.execute(isChecked ? ENABLE_CMDS : DISABLE_CMDS);
        buttonView.setEnabled(false);
    }

    private static class SetIPTables extends AsyncTask<String, String, Boolean> {

        private boolean mRooted = false;
        private boolean error = false;
        private final String mTitle;
        private final boolean mResetErrors;
        private final WeakReference<Activity> mActivity;
        private WeakReference<OnTaskComplete> mOnTaskComplete = new WeakReference<OnTaskComplete>(null);

        private ProgressDialog dialog = null;
        private String msg = "Executing ...\n";

        public SetIPTables(Activity activity, boolean resetErrors, String title) {
            mActivity = new WeakReference<Activity>(activity);
            mResetErrors = resetErrors;
            mTitle = title;
        }

        public void setOnTaskCompleteListener(OnTaskComplete cb) {
            mOnTaskComplete = new WeakReference<OnTaskComplete>(cb);
        }

        public interface OnTaskComplete {
            public void onTaskComplete(boolean sucess);
        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(mActivity.get());
            dialog.setTitle(mTitle);
            dialog.setMessage(msg);
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(final String... cmds) {
            mRooted = Shell.SU.available();
            if (!mRooted) {
                error = true;
                publishProgress("no root!");
                return null;
            }
            Shell.Interactive inteactiveShell = new Shell.Builder().useSU().setOnSTDOUTLineListener(new OnLineListener() {

                @Override
                public void onLine(String line) {
                    publishProgress(line);
                }
            }).setOnSTDERRLineListener(new OnLineListener() {

                @Override
                public void onLine(String line) {
                    error = true;
                    publishProgress(line);
                }
            }).open();

            for (String cmd : cmds) {
                publishProgress(cmd);
                inteactiveShell.addCommand(cmd);
                inteactiveShell.waitForIdle();
                SystemClock.sleep(400);
            }

            inteactiveShell.close();
            SystemClock.sleep(400);
            return error;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            Activity activity = mActivity.get();
            if (activity == null) {
                return;
            }
            String txt = !error ? "\n" : "\nerr:\n";
            for (String s : values) {
                txt += (error ? "\t" : "") + s + '\n';
            }
            txt += !error ? "" : "endERR";
            msg += txt;
            dialog.setMessage(msg);
            if (mResetErrors) {
                error = false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            Context context = dialog.getContext();
            dialog.dismiss();
            String ok = context.getResources().getString(android.R.string.ok);

            if (!result) {
                new AlertDialog.Builder(context)
                        .setTitle("Success")
                        .setCancelable(false)
                        .setPositiveButton(ok, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                OnTaskComplete cb = mOnTaskComplete.get();
                                if (cb != null) {
                                    cb.onTaskComplete(true);
                                }
                            }

                        }).create().show();
            } else {
                new AlertDialog.Builder(context)
                        .setTitle("Failure")
                        .setCancelable(false)
                        .setMessage("Something has gone wrong, your phone may be in an unstable state, a reboot is recomended")
                        .setPositiveButton("ignore", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                OnTaskComplete cb = mOnTaskComplete.get();
                                if (cb != null) {
                                    cb.onTaskComplete(false);
                                }
                            }

                        })
                        .setNegativeButton("reboot", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new Thread(new Runnable(){
                                    @Override
                                    public void run() {
                                        Shell.SU.run("halt");                                        
                                    }
                                }).start();
                            }

                        })
                        .create().show();
            }

        }

    }

}
