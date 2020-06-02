package chess.jiruska.chessrecorder;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class GameDetailActivity extends AppCompatActivity {

    private TextView gameTitle;
    private TextView dateLabel;
    private TextView notesView;
    private TextView pgnView;
    private TextView winnerLabel;
    private GameReader reader;
    private String file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_detail);
        ActionBar bar = getSupportActionBar();
        assert bar != null;
        bar.setDisplayHomeAsUpEnabled(true);
        Bundle bundle = getIntent().getExtras();
        String filename = "";
        if(bundle != null){
            filename = bundle.getString("filename");
        }
        file = filename;

        reader = new GameReader(getApplicationContext());
        reader.readFile(filename);

        gameTitle = findViewById(R.id.textViewDetailTitle);
        gameTitle.setText(reader.getName());

        dateLabel = findViewById(R.id.textViewDetailDate);
        dateLabel.setText(reader.getDate());

        notesView = findViewById(R.id.textViewDetailNotes);
        notesView.setText(reader.getNotes());

        winnerLabel = findViewById(R.id.textViewDetailWinner);
        winnerLabel.setText(getWinner());

        pgnView = findViewById(R.id.textViewDetailPGN);
        pgnView.setText(readFileData(filename));

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public String getWinner(){
        String result = reader.getResult();
        switch(result){
            case "1-0":
                return reader.getWhite();
            case "0-1":
                return reader.getBlack();
            case "1/2-1/2":
                return getString(R.string.draw);
            default:
                return getString(R.string.unknown);
        }
    }

    public String readFileData(String filename){
        String ret = "";
        try {
            FileInputStream f = openFileInput(filename);
            InputStreamReader inputStreamReader = new InputStreamReader(f);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString;
            StringBuilder stringBuilder = new StringBuilder();

            while ( (receiveString = bufferedReader.readLine()) != null ) {
                stringBuilder.append("\n").append(receiveString);
            }

            f.close();
            ret = stringBuilder.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void copyToClipboard(View view){
        String moves = readFileData(file);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("PGN", moves);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getApplicationContext(), R.string.copied, Toast.LENGTH_SHORT).show();
    }
}
