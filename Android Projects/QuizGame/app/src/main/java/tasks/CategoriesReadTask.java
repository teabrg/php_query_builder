package tasks;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.library.quizgame.QuestionsActivity;
import com.library.quizgame.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import adapters.CategoriesAdapter;
import constants.Constants;
import constants.StringGenerator;
import listeners.RecyclerItemClickListener;
import lists.SingleCategories;

public class CategoriesReadTask extends AsyncTask<String, Void, List<SingleCategories>>{

    /**
     * Αρχικοποίηση των μεταβλητων για να είναι διαθέσιμες σε όλες τις μεθόδους
     */
    private ProgressDialog progressDialog;
    private Context context;
    List<SingleCategories> categories;
    CategoriesAdapter adapter;
    RecyclerView recyclerView;
    private URL url;
    private HttpURLConnection httpURLConnection;
    private InputStream inputStream;
    private StringBuilder jsonResult;
    private JSONObject jsonResponse, jsonChildNode;
    private JSONArray jsonMainNode;
    private String categoryName, locale;
    private String id;
    private String langText;
    private SharedPreferences sharedPreferences;

    public CategoriesReadTask(ProgressDialog progressDialog, Context context, List<SingleCategories> categories,
                              CategoriesAdapter adapter, RecyclerView recyclerView) {
        /**
         * Αρχικοποίηση των Objects στον constructor
         */
        this.progressDialog = progressDialog;
        this.context = context;
        this.categories= categories;
        this.adapter = adapter;
        this.recyclerView = recyclerView;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        loadFromPrefs();
        StringGenerator.setLocale(langText, context);
        /**
         * Όσο ο χρήστης περιμένει η εφαρμογή να φορτώσει δεδομένα απο το Internet
         * του δείχνουμε ένα διάλογο Loading...
         */
        progressDialog.setMessage(this.context.getString(R.string.loading));
        progressDialog.show();
    }

    @Override
    protected List<SingleCategories> doInBackground(String... params) {
        /**
         * εδώ γίνεται η κύρια διεργασία του AsyncTask
         */
        //Αρχικοποίηση του δυναμικού πίνακα που θα φιλοξενίσει τις κατηγορίες
        this.categories = new ArrayList<>();
        try {
            //το URL το παίρνουμε απο την πρώτη παράμετρο του πίνακα String[] params
            url = new URL(params[0]);
            //Συνδεόμαστε με HttpURLConnection στο URL για να πάρουμε τα δεδομένα
            httpURLConnection = (HttpURLConnection) url.openConnection();

            //Κάνουμε set τα timeouts για την σύνδεση
            httpURLConnection.setReadTimeout(10000);
            httpURLConnection.setConnectTimeout(15000);

            //Κάνουμε set τις μεθόδους μας για την σύνδεση
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            //Κάνουμε την σύνδεση. Αν δεν πάει καλά για οποιονδήποτε λόγο μας πετάει Exception
            //γιαυτό και χρησιμοποιούμε την try/catch
            httpURLConnection.connect();
            /**
             *  Χρησιμοποιούμε BufferedInputStream για τον εξής λόγο:
             *  Κάθε network call για να διαβάσουμε συγκεκριμένο μήνυμα απο bytes
             *  μπορεί να είναι χιλιάδες εντολές μηχανής για κάθε syscall.
             *  Ο BufferedInputStream το μειώνει κάνοντας ένα "μεγάλο" διάβασμα ας πούμε 8Κ bytes
             *  για κάθε syscall.
             */
            inputStream = new BufferedInputStream(httpURLConnection.getInputStream());
            /**
             * Θα βρείς την εξήγηση το constants.StringGenerator.java
             */
            jsonResult = StringGenerator.inputStreamToString(inputStream,(Activity) this.context);
            //Αρχικοποιούμε το JSONObject μας
            jsonResponse = new JSONObject(jsonResult.toString());
            //Αρχικοποιούμε το JSONArray μας
            //locale = Locale.getDefault().getLanguage();
            if (langText.equals(context.getString(R.string.ta_to_select))|| langText.equals(null)){
                jsonMainNode = jsonResponse.optJSONArray(Constants.CATEGORIES_ARRAY_EN);
            }else {
                checkDisplayLanguage(langText);
            }
            for (int i = 0; i < jsonMainNode.length(); i++) {
                jsonChildNode = jsonMainNode.getJSONObject(i);
                //Για κάθε κατηγορία επιλέγουμε το όνομα και το id.
                categoryName = jsonChildNode.optString(Constants.CATEGORY_JSON_NAME);
                id = jsonChildNode.optString(Constants.CATEGORY_ID_JSON_NAME);
                //Φτιάχνουμε την λίστα μας χρησιμοποιώντας κάθε φορά ενα αντικείμενο τύπου SingleCategories
                this.categories.add(new SingleCategories(categoryName, id));
            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return this.categories;
    }

    @Override
    protected void onPostExecute(final List<SingleCategories> categoryLists) {
        super.onPostExecute(categoryLists);
        progressDialog.dismiss();
        //Εφόσον έχουμε πάρει τις τιμές απο το URL
        //τις περνάμε στον adapter και φτιάχνουμε το RecyclerView μας.
        this.adapter = new CategoriesAdapter(categoryLists, this.context);
        this.recyclerView.setAdapter(adapter);
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(context, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent intent = new Intent(context, QuestionsActivity.class);
                intent.putExtra(Constants.CATEGORIES_INTENT_NAME, categoryLists.get(position).getCategoryName());
                intent.putExtra(Constants.CATEGORIES_INTENT_ID, categoryLists.get(position).getId());
                context.startActivity(intent);
            }
        }));
    }


    private void checkDisplayLanguage(String locale){
        if (locale.equals(Constants.EN)){
            jsonMainNode = jsonResponse.optJSONArray(Constants.CATEGORIES_ARRAY_EN);
        }else if (locale.equals(Constants.GR)){
            jsonMainNode = jsonResponse.optJSONArray(Constants.CATEGORIES_ARRAY);
        }
    }

    private void loadFromPrefs() {
        sharedPreferences = this.context.getSharedPreferences(Constants.PREFERENCES_FILE, context.MODE_PRIVATE);
        langText = sharedPreferences.getString(Constants.LANGUAGE_PREFS_FILE, context.getString(R.string.ta_to_select));
    }
}
