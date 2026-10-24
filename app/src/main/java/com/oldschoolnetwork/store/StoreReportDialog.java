package com.oldschoolnetwork.store;
import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import org.json.JSONObject;
public final class StoreReportDialog {
 private StoreReportDialog(){}
 public static void show(Context context,ONAccountSessionManager session,String kind,int id){
  View fields=LayoutInflater.from(context).inflate(R.layout.dialog_store_report,null);
  TextView status=fields.findViewById(R.id.reportStatus);Spinner reason=fields.findViewById(R.id.reportReason);EditText details=fields.findViewById(R.id.reportDetails);
  String[] reasons={"Inappropriate content","Spam or misleading","Malware or virus","Harassment","Other"};reason.setAdapter(new ArrayAdapter<>(context,android.R.layout.simple_spinner_dropdown_item,reasons));
  JSONObject target=new JSONObject();try{target.put("target_type",kind);target.put("target_id",id);}catch(Exception ignored){}
  AlertDialog dialog=new AlertDialog.Builder(context).setTitle("Report "+kind).setView(fields).setNegativeButton("Close",null).setPositiveButton("Send report",null).create();
  dialog.setOnShowListener(ignored->{Button send=dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);send.setEnabled(false);reason.setEnabled(false);details.setEnabled(false);
   StoreRepository.getInstance().requestAccountAction("store_report_status",target,session,(result,error)->{if(!dialog.isShowing())return;if(error!=null){status.setText(error);return;}JSONObject report=result.optJSONObject("report");boolean editable=report==null||report.optBoolean("editable");
    status.setText(report==null?"Describe the problem.":editable?"Your report is pending. You can edit it.":"This report has already been reviewed.");
    if(report!=null){for(int i=0;i<reasons.length;i++)if(reasons[i].equals(report.optString("reason")))reason.setSelection(i);details.setText(report.optString("details"));send.setText("Update report");}
    send.setEnabled(editable);reason.setEnabled(editable);details.setEnabled(editable);
   });
   send.setOnClickListener(v->{send.setEnabled(false);JSONObject request=new JSONObject();try{request.put("target_type",kind);request.put("target_id",id);request.put("reason",reason.getSelectedItem().toString());request.put("details",details.getText().toString().trim());}catch(Exception ignoredJson){}
    StoreRepository.getInstance().requestAccountAction("platform_report",request,session,(result,error)->{if(!dialog.isShowing())return;if(error!=null){status.setText(error);send.setEnabled(true);}else{Toast.makeText(context,result.optString("message","Report saved."),Toast.LENGTH_LONG).show();dialog.dismiss();}});
   });
  });dialog.show();
 }
}
