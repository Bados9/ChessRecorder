package chess.jiruska.chessrecorder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import chess.jiruska.chessrecorder.classification.Classifier;
import chess.jiruska.chessrecorder.classification.TensorFlowClassifier;

public class MainActivity extends AppCompatActivity {

    private ListView listView;
    private String[] files;
    private ArrayList<String> filesArray;
    private GameListAdapter2 gameListAdapter;
    private AlertDialog deleteGameAlert;
    static{ System.loadLibrary("opencv_java4"); }
    private String gameToBeDeleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.listView);

        files = fileList();

        files = Arrays.stream(files).filter(s -> s.contains("game")).toArray(String[]::new);
        filesArray = new ArrayList<>(Arrays.asList(files));
        gameListAdapter = new GameListAdapter2(this, filesArray);

        listView.setAdapter(gameListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showGameDetail(gameListAdapter.titles.get(position));
            }
        });
        createDeleteAlert();
    }

    private void createDeleteAlert(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.app_name);
        builder.setMessage(R.string.delete_game_prompt); //TODO vsude vylepsit texty a preklady
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                deleteGame();
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        deleteGameAlert = builder.create();
    }

    public void deleteGame(){
        File f = new File(getApplicationContext().getFilesDir().getAbsolutePath() + "/" + gameToBeDeleted);
        if (f.exists()){
            if (f.delete()){
                Toast.makeText(getApplicationContext(), R.string.game_deleted, Toast.LENGTH_SHORT).show();
                gameListAdapter.remove(gameToBeDeleted);
                gameListAdapter.notifyDataSetChanged();
                return;
            }
        }
        Toast.makeText(getApplicationContext(), R.string.game_delete_error, Toast.LENGTH_SHORT).show();
    }

    public void showGameDetail(String filename){
        Intent intent = new Intent(this, GameDetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("filename", filename);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public static Context getContext(){
        return getContext();
    }

    public void startRecordingActivity(View view){
        boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                .getBoolean("isFirstRun", true);

        if (isFirstRun) {
            startActivity(new Intent(MainActivity.this, HelpActivity.class));
        } else {
            Intent intent = new Intent(this, RecordingActivity.class);
            startActivity(intent);
        }


        getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit()
                .putBoolean("isFirstRun", false).apply();

    }

    public void startHelpActivity(View view){
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    class GameListAdapter2 extends ArrayAdapter<String>{
        Context context;
        ArrayList<String> mTitle;
        ArrayList<String> mDescription;
        ArrayList<String> titles;

        GameListAdapter2 (Context c, ArrayList<String> titles){
            super(c, R.layout.row, R.id.textViewMain, titles);

            ArrayList<String> names = new ArrayList<>();
            ArrayList<String> dates = new ArrayList<>();
            for (String title: titles) {
                GameReader reader = new GameReader(getApplicationContext());
                reader.readBasicInfo(title);
                names.add(reader.getName());
                dates.add(reader.getDate());
            }
            this.titles = titles;
            this.context = c;
            this.mTitle = names;
            this.mDescription = dates;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater layoutInflater = (LayoutInflater)getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = layoutInflater.inflate(R.layout.row, parent, false);
            TextView mainTitle = row.findViewById(R.id.textViewMain);
            TextView description = row.findViewById(R.id.textViewSub);
            ImageButton deleteBtn = row.findViewById(R.id.deleteGameBtn);
            mainTitle.setText(mTitle.get(position));
            description.setText(mDescription.get(position));
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteGameAlert.show();
                    gameToBeDeleted = titles.get(position);
                }
            });
            return row;
        }

        @Override
        public void remove(@Nullable String object) {
            int removeIndex = this.titles.indexOf(object);
            this.titles.remove(object);
            this.mTitle.remove(removeIndex);
            this.mDescription.remove(removeIndex);
        }

        @Override
        public void notifyDataSetChanged() {
            ArrayList<String> names = new ArrayList<>();
            ArrayList<String> dates = new ArrayList<>();
            for (String title: titles) {
                GameReader reader = new GameReader(getApplicationContext());
                reader.readBasicInfo(title);
                names.add(reader.getName());
                dates.add(reader.getDate());
            }
            this.mTitle = names;
            this.mDescription = dates;
            super.notifyDataSetChanged();
        }
    }
}
