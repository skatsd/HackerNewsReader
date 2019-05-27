package com.saikat.skat_sd.hackernewsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.valueOf;

public class MainActivity extends AppCompatActivity {

    Map<Integer,String> articleURLs=new HashMap<Integer, String>();
    Map<Integer,String> articleTitles=new HashMap<Integer, String>();
    ArrayList<Integer> articleIDs=new ArrayList<Integer>();

    SQLiteDatabase articlesDB;

    ArrayList<String> titles=new ArrayList<String>();
    ArrayAdapter arrayAdapter;

    ArrayList<String> urls=new ArrayList<String>();
    ArrayList<String> content=new ArrayList<String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView=(ListView) findViewById(R.id.listview);
        arrayAdapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent i=new Intent(getApplicationContext(),ArticleActivity.class);
                i.putExtra("articleURL",urls.get(position));
                //i.putExtra("content",content.get(position));
                startActivity(i);

                Log.i("ArticleURL",urls.get(position));


            }
        });



        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);


        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY, articleID INTEGER, url VARCHAR, title VARCHAR, content VARCHAR)");

        updateListView();

        DownloadTask task=new DownloadTask();

        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");


           // Log.i("Result",result);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void updateListView(){

        try {

            Log.i("UI updated","DONE");
            Cursor c = articlesDB.rawQuery("SELECT * FROM articles ORDER BY articleID DESC", null);

            int contentIndex = c.getColumnIndex("content");
            int urlIndex = c.getColumnIndex("url");
            int titleIndex = c.getColumnIndex("title");


            c.moveToFirst();

            titles.clear();
            urls.clear();

            while (c != null) {

                titles.add(c.getString(titleIndex));
                urls.add(c.getString(urlIndex));
                content.add(c.getString(contentIndex));

//                Log.i("articleID",Integer.toString((c.getInt(articleIDindex))));
//                Log.i("articleURL",c.getString(urlIndex));
//                Log.i("articleTitle",c.getString(titleIndex));

                c.moveToNext();


            }

            arrayAdapter.notifyDataSetChanged();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public class DownloadTask extends AsyncTask<String,Void,String >{

        @Override
        protected String doInBackground(String... urls) {
            String result="";
            URL url;
            HttpURLConnection urlConnection=null;

            try{
                url=new URL(urls[0]);
                urlConnection=(HttpURLConnection) url.openConnection();

                InputStream in=urlConnection.getInputStream();
                InputStreamReader reader=new InputStreamReader(in);

                int data=reader.read();

                while (data!=-1){
                    char current=(char) data;
                    result+=current;
                    data=reader.read();
                }


                JSONArray jsonArray=new JSONArray(result);
                articlesDB.execSQL("DELETE FROM articles");

                for(int i=0;i<50/*jsonArray.length()*/;i++){//50 articles for now
                    // Log.i("ArticleID",jsonArray.getString(i));

                    String articleID=jsonArray.getString((i));

                    url=new URL("https://hacker-news.firebaseio.com/v0/item/"+jsonArray.getString(i) +".json?print=pretty");

                    urlConnection=(HttpURLConnection) url.openConnection();

                    in=urlConnection.getInputStream();
                    reader=new InputStreamReader(in);

                    data=reader.read();

                    String articleInfo="";

                    while(data!=-1){
                        char current=(char) data;
                        articleInfo+=current;
                        data=reader.read();
                    }

                    JSONObject jsonObject=new JSONObject(articleInfo);

                    String articletitle=jsonObject.getString("title");
                    String articleURL=jsonObject.getString("url");

                    String articleContent="";

                    //getting the content
                    //FROM HERE
                    /*
                    url=new URL(articleURL);

                    urlConnection=(HttpURLConnection) url.openConnection();

                    in=urlConnection.getInputStream();
                    reader=new InputStreamReader(in);

                    data=reader.read();

                   //String articleContent="";
                    //look here
                    while(data!=-1){
                        char current=(char) data;
                        articleContent+=current;
                        data=reader.read();
                    }
                    //TO HERE
            */
                    articleIDs.add(Integer.valueOf(articleID));
                    articleTitles.put(Integer.valueOf(articleID),articletitle);
                    articleURLs.put(Integer.valueOf(articleID),articleURL);

                    String sql="INSERT INTO articles (articleID,url,title,content) VALUES(? , ? , ? , ?)";

                    SQLiteStatement statement= articlesDB.compileStatement(sql);

                    statement.bindString(1,articleID);
                    statement.bindString(2,articleURL);
                    statement.bindString(3,articletitle);
                    statement.bindString(4,articleContent);

                    statement.execute();
//                articlesDB.execSQL();

                    //Log.i("Title",articletitle);
                    //Log.i("url",articleURL);
//
//                Log.i("articleIDs",articleIDs.toString());
//                Log.i("articleTitles",articleTitles.toString());
//                Log.i("articleURLs",articleURLs.toString());

                }
//            Log.i("articleIDs",articleIDs.toString());
//            Log.i("articleTitles",articleTitles.toString());
//            Log.i("articleURLs",articleURLs.toString());


            }catch (Exception e){
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }
}
