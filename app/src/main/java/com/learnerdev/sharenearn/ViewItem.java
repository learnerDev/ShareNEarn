package com.learnerdev.sharenearn;

import android.app.ActionBar;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.DataQueryBuilder;

import java.util.List;
import java.util.Locale;

import static com.learnerdev.sharenearn.Defaults.COLUMN_NAME_ITEM_LOCATION;

public class ViewItem extends AppCompatActivity  {
    private final static String TAG="SNE-ViewItem";
    private static String OBJECT_ID;
    private String whereClause;
    private TextView tvItemName;
    private TextView tvItemDetails;
    private Item item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_item);
        //To enable up navigation
        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        OBJECT_ID=getIntent().getStringExtra("EXTRA_OBJECT_ID");
        tvItemName=findViewById(R.id.vi_display_item_name);
        tvItemDetails=findViewById(R.id.vi_display_item_details);

        whereClause=String.format(Locale.getDefault(),"objectId='%s'",OBJECT_ID);
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause( whereClause );
        Backendless.Data.of(Item.class).find(queryBuilder, new AsyncCallback<List<Item>>() {
            @Override
            public void handleResponse(List<Item> response) {
                if(response.size()>0){
                    tvItemName.setText(response.get(0).getItemName());
                    tvItemDetails.setText(response.get(0).getItemDetails());
                }
            }

            @Override
            public void handleFault(BackendlessFault fault) {

            }
        });





    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed in the action bar.
                // Create a simple intent that starts the hierarchical parent activity and
                // use NavUtils in the Support Package to ensure proper handling of Up.
                Intent upIntent = new Intent(this, SearchItems.class);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is not part of the application's task, so create a new task
                    // with a synthesized back stack.
                    TaskStackBuilder.from(this)
                            // If there are ancestor activities, they should be added here.
                            .addNextIntent(upIntent)
                            .startActivities();
                    finish();
                } else {
                    // This activity is part of the application's task, so simply
                    // navigate up to the hierarchical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
