package com.learnerdev.sharenearn;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.DataQueryBuilder;

import java.util.List;
import java.util.Locale;

import static com.learnerdev.sharenearn.Defaults.COLUMN_NAME_ITEM_LOCATION;

public class ViewItem extends AppCompatActivity {
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
}
