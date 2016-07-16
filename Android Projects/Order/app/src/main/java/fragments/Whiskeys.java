package fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.order.app.order.R;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import adapters.SpiritComponentAdapter;
import adapters.SpiritsListAdapter;
import dialogs.DialogMessageDisplay;
import functions.Constants;
import functions.StringGenerator;
import lists.SpiritComponentProduct;
import lists.SpiritList;


public class Whiskeys extends AppCompatActivity {

    private Spinner spRef, spDrinks;
    private CheckBox shortGlass, longGlass, yes, no;
    private EditText sxolia, quantity;
    private int quantityNumberFinal;
    private Button cart, plus, minus;
    private StringBuilder jsonResult;
    private SpiritsListAdapter adapterWhiskys;
    private String servitoros_id, magazi_id, table, title, quantityPreference, comment, componentPreference, whiskeyPreference, glassPreference, strollPreference, name, image, price;
    ProgressDialog pDialog;
    ArrayList<SpiritList> customSpinner;
    private Toolbar toolbar;
    private List<SpiritComponentProduct> components;
    private SpiritComponentAdapter componentAdapter;
    private JSONObject jsonResponse, jsonChildNode;
    private JSONArray jsonMainNode;
    private HttpURLConnection urlConnection;
    private URL url;
    private List<NameValuePair> nameValuePairs;
    private BufferedWriter bufferedWriter;
    private OutputStream outputStream;
    private InputStream inputStream;
    private double priceCalculated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.whiskeys);
        populateToolBar();
        spDrinks = (Spinner)findViewById(R.id.flavor_whisky_spinner);
        components = new ArrayList<>();
        checkNetworkInfo();
        findItems();
        setupListeners();
        populateSpiritComponentsList();
        buildRefreshmentsSpinner();
        getSpinnerSelectedPreference();
        checkQuantity();
    }

    private void getSpinnerSelectedPreference() {
        spDrinks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                whiskeyPreference = customSpinner.get(position).getName();
                image = customSpinner.get(position).getImage();
                price = customSpinner.get(position).getPrice();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void checkNetworkInfo() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean network_connected = activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnectedOrConnecting();
        if (!network_connected) {
            DialogMessageDisplay.displayWifiSettingsDialog(Whiskeys.this, Whiskeys.this, getString(R.string.wifi_off_title), getString(R.string.wifi_off_message));
        } else {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI || activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                readDataWebService();
            }
        }
    }

    private void findItems() {
        spRef = (Spinner)findViewById(R.id.refreshment_spinner_whisky);
        shortGlass = (CheckBox)findViewById(R.id.short_glass);
        longGlass = (CheckBox)findViewById(R.id.long_glass);
        yes = (CheckBox)findViewById(R.id.yesCheck);
        no = (CheckBox)findViewById(R.id.noCheck);
    }

    private void setupListeners() {
        shortGlass.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    longGlass.setEnabled(false);
                    glassPreference = shortGlass.getText().toString();
                }else{
                    longGlass.setEnabled(true);
                    glassPreference = null;
                }
            }
        });
        longGlass.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    shortGlass.setEnabled(false);
                    glassPreference = longGlass.getText().toString();
                }else{
                    shortGlass.setEnabled(true);
                    glassPreference = null;
                }
            }
        });
        yes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    no.setEnabled(false);
                    strollPreference = yes.getText().toString().toLowerCase();
                }else{
                    no.setEnabled(true);
                    strollPreference = null;
                }
            }
        });
        no.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    yes.setEnabled(false);
                    strollPreference = no.getText().toString().toLowerCase();
                }else{
                    yes.setEnabled(true);
                    strollPreference = null;
                }
            }
        });
    }

    private void checkQuantity() {
        quantity = (EditText) findViewById(R.id.quantityEditTextWhiskeys);
        plus = (Button) findViewById(R.id.buttonPlusWhiskeys);
        minus = (Button) findViewById(R.id.buttonMinusWhiskeys);
        sxolia = (EditText)findViewById(R.id.editTextWhiskeysComments);
        cart = (Button)findViewById(R.id.cartButtonWhisky);

        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String addTxt = quantity.getText().toString();
                int add = Integer.parseInt(addTxt);
                quantity.setText(String.valueOf(add + 1));
            }
        });
        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String minusTxt = quantity.getText().toString();
                int minus = Integer.parseInt(minusTxt);
                if (minus > 0) {
                    quantity.setText(String.valueOf(minus - 1));
                }
            }
        });



        quantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String quanText = quantity.getText().toString();
                int numberQuant = Integer.parseInt(quanText);
                if (numberQuant > 0) {
                    cart.setEnabled(true);
                    cart.setBackgroundColor(Color.parseColor(Constants.ENABLED_BUTTON_COLOR));
                } else {
                    cart.setEnabled(false);
                    cart.setBackgroundColor(Color.parseColor(Constants.DISABLED_BUTTON_COLOR));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quantityPreference = quantity.getText().toString();
                quantityNumberFinal = Integer.parseInt(quantityPreference);
                priceCalculated = Double.parseDouble(price) * quantityNumberFinal;
                comment = sxolia.getText().toString();
                getSpinnerSelectedPreference();
                if (comment == null) {
                    comment = " ";
                }
                if (glassPreference == null) {
                    Toast.makeText(Whiskeys.this, getString(R.string.glass_required), Toast.LENGTH_LONG).show();
                } else if (strollPreference == null) {
                    Toast.makeText(Whiskeys.this, getString(R.string.stroll_required), Toast.LENGTH_LONG).show();
                } else {
                    writeDataWebService();
                }

            }
        });
    }

    private void populateSpiritComponentsList() {
        components.add(new SpiritComponentProduct(R.mipmap.ic_glass, getString(R.string.sketo)));
        components.add(new SpiritComponentProduct(R.mipmap.ic_ice, getString(R.string.pago)));
        components.add(new SpiritComponentProduct(R.mipmap.ic_coca_cola, getString(R.string.coca)));
        components.add(new SpiritComponentProduct(R.mipmap.ic_coca_light, getString(R.string.coca_light)));
        components.add(new SpiritComponentProduct(R.mipmap.ic_zero, getString(R.string.coca_zero)));
        components.add(new SpiritComponentProduct(R.mipmap.ic_sprite, getString(R.string.sprite)));
        components.add(new SpiritComponentProduct(R.mipmap.ic_fanta_lemon, getString(R.string.fanta_lemon)));
        components.add(new SpiritComponentProduct(R.mipmap.ic_fanta_orange, getString(R.string.fanta_orange)));
        components.add(new SpiritComponentProduct(R.mipmap.ic_soda, getString(R.string.soda)));
        components.add(new SpiritComponentProduct(R.mipmap.ic_tonic, getString(R.string.tonic)));
        components.add(new SpiritComponentProduct(R.mipmap.ic_water, getString(R.string.water)));
        components.add(new SpiritComponentProduct(R.mipmap.ic_redbull, getString(R.string.redbull)));
    }

    private void populateToolBar() {
        title = getIntent().getStringExtra(Constants.SPIRIT_ITEM);
        table = getIntent().getStringExtra(Constants.TABLE_INTENT_ID);
        servitoros_id = getIntent().getStringExtra(Constants.WAITER_INTENT_ID);
        magazi_id = getIntent().getStringExtra(Constants.COMPANY_INTENT_ID);
        toolbar = (Toolbar) findViewById(R.id.toolBar);
        toolbar.setTitle(title);
        toolbar.setSubtitle(getString(R.string.table_id) + table);
        setSupportActionBar(toolbar);
    }

    private void buildRefreshmentsSpinner() {
        componentAdapter = new SpiritComponentAdapter(Whiskeys.this, components);
        spRef.setAdapter(componentAdapter);
        spRef.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                componentPreference = components.get(position).getProductName();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    public class JsonReadTask extends AsyncTask<String , Void, List<SpiritList>> {
        public JsonReadTask() {
            super();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(Whiskeys.this);
            pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pDialog.setIndeterminate(true);
            pDialog.setMessage(getString(R.string.get_stocks));
            pDialog.setCancelable(false);
            pDialog.setInverseBackgroundForced(true);
            pDialog.show();
        }

        @Override
        protected List<SpiritList> doInBackground(String... params) {
            try {
                url = new URL(params[0]);
                urlConnection =(HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("X-API-KEY", "123456");
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                urlConnection.setConnectTimeout(5000);
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                jsonResult = StringGenerator.inputStreamToString(in, Whiskeys.this);
                customSpinner = new ArrayList<>();

                jsonResponse = new JSONObject(jsonResult.toString());
                jsonMainNode = jsonResponse.optJSONArray(Constants.WHISKEYS_JSON_ARRAY);
                for (int i = 0; i < jsonMainNode.length(); i++) {
                    jsonChildNode = jsonMainNode.getJSONObject(i);
                    name = jsonChildNode.optString("name");
                    price = jsonChildNode.optString("price");
                    image = jsonChildNode.optString("image");
                    customSpinner.add(new SpiritList(name, price, image));
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return customSpinner;
        }

        @Override
        protected void onPostExecute(List<SpiritList> customSpinner) {
            if(customSpinner == null){
                Log.e("ERORR", "No result to show.");
                return;
            }
            ListDrawer(customSpinner);
            pDialog.dismiss();
        }
    }

    private class MyInsertDataTask extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(Whiskeys.this);
            pDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pDialog.setIndeterminate(true);
            pDialog.setMessage(getString(R.string.dialog_rate_data_submit));
            pDialog.setCancelable(false);
            pDialog.setInverseBackgroundForced(true);
            pDialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {

            nameValuePairs = new ArrayList<>();
            try {
                url = new URL(params[0]);
                urlConnection =(HttpURLConnection) url.openConnection();
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                setupDataToDB();
                outputStream = urlConnection.getOutputStream();
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                bufferedWriter.write(StringGenerator.queryResults(nameValuePairs));
                bufferedWriter.flush();
                bufferedWriter.close();
                outputStream.close();
                urlConnection.connect();
                inputStream = new BufferedInputStream(urlConnection.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            pDialog.dismiss();
            Toast.makeText(Whiskeys.this, getString(R.string.cart_addition_successfull), Toast.LENGTH_LONG).show();
            Whiskeys.this.finish();
        }
    }

    private void setupDataToDB() {
        nameValuePairs.add(new BasicNameValuePair(Constants.PRODUCT_NAME_VALUE_PAIR, whiskeyPreference));
        nameValuePairs.add(new BasicNameValuePair(Constants.PRODUCT_PRICE_VALUE_PAIR, String.valueOf(priceCalculated)));
        nameValuePairs.add(new BasicNameValuePair(Constants.PRODUCT_IMAGE_VALUE_PAIR, image));
        nameValuePairs.add(new BasicNameValuePair(Constants.PRODUCT_QUANTITY_VALUE_PAIR, String.valueOf(quantityNumberFinal)));
        nameValuePairs.add(new BasicNameValuePair("glass", glassPreference));
        nameValuePairs.add(new BasicNameValuePair("stroll", strollPreference));
        nameValuePairs.add(new BasicNameValuePair(Constants.PRODUCT_COMPONENT_VALUE_PAIR, componentPreference));
        nameValuePairs.add(new BasicNameValuePair(Constants.PRODUCT_COMMENT_VALUE_PAIR, comment));
        nameValuePairs.add(new BasicNameValuePair(Constants.PRODUCT_COMPANY_ID_VALUE_PAIR, magazi_id));
        nameValuePairs.add(new BasicNameValuePair(Constants.PRODUCT_WAITER_ID_VALUE_PAIR, servitoros_id));
        nameValuePairs.add(new BasicNameValuePair(Constants.PRODUCT_TABLE_ID_VALUE_PAIR, table));
    }

    public void readDataWebService() {
        JsonReadTask task = new JsonReadTask();
        task.execute(Constants.WHISKEYS_URL);
    }

    public void writeDataWebService() {
        MyInsertDataTask task = new MyInsertDataTask();
        task.execute(Constants.SPIRIT_ADD_TO_CART_URL);
    }



    public void ListDrawer(List<SpiritList> customSpinner) {
        adapterWhiskys = new SpiritsListAdapter(Whiskeys.this ,customSpinner);
        spDrinks.setAdapter(adapterWhiskys);
    }


}
