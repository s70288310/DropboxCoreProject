package tw.com.stanley.homeworkapptest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Created by stanley on 2015/4/16.
 */
public class FileInfoDialogFragment extends DialogFragment {

    public interface DialogClickListener{
        public void onPositiveClick(DialogFragment dialog);
        public void onNegtiveClick(DialogFragment dialog);
    }

    DialogClickListener listener;

    public static FileInfoDialogFragment newInstance(String name,String mime
            ,String size,String path){
        FileInfoDialogFragment fragment =new FileInfoDialogFragment();
        Bundle bundle=new Bundle();
        bundle.putString("NAME",name);
        bundle.putString("MIME",mime);
        bundle.putString("SIZE", size);
        bundle.putString("PATH",path);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try{
            listener=(DialogClickListener)activity;
        }catch (ClassCastException e){
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String message="MimeType: "+getArguments().get("MIME")+"\n"+
                "Size: "+getArguments().getString("SIZE");

        builder.setTitle(getArguments().getString("NAME"))
                .setMessage(message)
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        listener.onPositiveClick(FileInfoDialogFragment.this);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        listener.onNegtiveClick(FileInfoDialogFragment.this);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

}
