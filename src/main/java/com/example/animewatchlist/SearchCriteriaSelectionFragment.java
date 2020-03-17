package com.example.animewatchlist;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class SearchCriteriaSelectionFragment extends Fragment {
    OnDataPass dataPasser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search_criteria_selection, container, false);
        addRadioButton_listener(view);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        dataPasser = (OnDataPass) context;
    }

    public void passData(String data) {
        dataPasser.onDataPass(data);
    }

    private void addRadioButton_listener(View view){
        // ** Radio button ALL **
        View.OnClickListener radioBtn_listener_name = new View.OnClickListener (){
            public void onClick(View v) {
                RadioButton rbType = view.findViewById(R.id.radioType);
                rbType.setChecked(false); // Ugly

                RadioButton rb = view.findViewById(R.id.radioName);
                passData( rb.getText().toString() );
            }
        };
        RadioButton rb_Name = view.findViewById(R.id.radioName);
        rb_Name.setOnClickListener(radioBtn_listener_name);

        View.OnClickListener radioBtn_listener_type = new View.OnClickListener (){
            public void onClick(View v) {
                RadioButton rbName = view.findViewById(R.id.radioName);
                rbName.setChecked(false); // Ugly

                RadioButton rb = view.findViewById(R.id.radioType);
                passData( rb.getText().toString() );
            }
        };
        RadioButton rb_Type = view.findViewById(R.id.radioType);
        rb_Type.setOnClickListener(radioBtn_listener_type);
    }

}
