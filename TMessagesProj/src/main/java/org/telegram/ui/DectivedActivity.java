package org.telegram.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ir.radius.iphogram.R;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.Utilities;

public class DectivedActivity extends Activity {

  Button btnDeactived;
  TextView txtDeactived;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_dectived);

    btnDeactived = (Button) findViewById(R.id.btnDeactived);
    txtDeactived = (TextView) findViewById(R.id.txtDeactived);

    txtDeactived.setText(LocaleController.getString("txtDeactive", R.string.txtDeactive));
    txtDeactived.setTextColor(0xff000000);

    btnDeactived.setText(LocaleController.getString("btnDeactive", R.string.btnDeactive));
    btnDeactived.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Utilities.restartApp();
        finish();
      }
    });
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
    homeIntent.addCategory( Intent.CATEGORY_HOME );
    homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(homeIntent);
  }
}
