package chess.jiruska.chessrecorder;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class GameSaveActivity extends AppCompatActivity {

    private GameWriter writer;
    private EditText name;
    private EditText white;
    private EditText black;
    private Spinner winner;
    private EditText notes;
    ArrayAdapter<String> spinnerAdapter;
    String oldSpinnerTextWhite;
    String oldSpinnerTextBlack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_save);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            writer = new GameWriter(getApplicationContext(), bundle.getString("moves"));
        }
        Log.d("gameSave", writer.getMoves());
        name = findViewById(R.id.editTextGameName);
        white = findViewById(R.id.editTextWhitePlayer);
        black = findViewById(R.id.editTextBlackPlayer);
        winner = findViewById(R.id.spinnerWinner);
        notes = findViewById(R.id.editTextGameNotes);

        List<String> list = new ArrayList<>();
        list.add(getString(R.string.white));
        oldSpinnerTextWhite = getString(R.string.white);
        list.add(getString(R.string.black));
        oldSpinnerTextBlack = getString(R.string.black);
        list.add(getString(R.string.draw));

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        winner.setAdapter(spinnerAdapter);

        white.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                oldSpinnerTextWhite = white.getText().toString();
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                spinnerAdapter.remove(oldSpinnerTextWhite);
                spinnerAdapter.add(white.getText().toString());
                spinnerAdapter.notifyDataSetChanged();
            }
        });

        black.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                oldSpinnerTextBlack = black.getText().toString();
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                spinnerAdapter.remove(oldSpinnerTextBlack);
                spinnerAdapter.add(black.getText().toString());
                spinnerAdapter.notifyDataSetChanged();
            }
        });
    }

    public String getResult(){
        String ret = "*";
        String nameOfWinner = winner.getSelectedItem().toString();
        if (nameOfWinner.equals(white.getText().toString())){
            ret = "1-0";
        } else if (nameOfWinner.equals(black.getText().toString())){
            ret = "0-1";
        } else if (nameOfWinner.equals("Draw")){
            ret = "1/2-1/2";
        }
        return ret;
    }

    public void saveGame(View view){
        writer.saveGame(name.getText().toString(),
                white.getText().toString(),
                black.getText().toString(),
                getResult(),
                notes.getText().toString());

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void cancelSaving(View view){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
