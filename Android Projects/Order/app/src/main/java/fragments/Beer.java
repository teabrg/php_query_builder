package fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.order.app.order.R;

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

import adapters.ProductsAdapter;
import cart.BeersLayoutActivity;
import functions.Constants;
import functions.StringGenerator;
import listeners.RecyclerItemClickListener;
import lists.ProductList;


public class Beer extends Fragment {

    private View rootView;
    private RecyclerView recyclerView;
    private ProductsAdapter productsAdapter;
    private StringBuilder jsonResult;
    ProgressDialog pDialog;
    private JsonReadTask task;
    private ConnectivityManager cm;
    private NetworkInfo activeNetwork;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    List<ProductList> customList;
    private boolean network_connected;
    private JSONArray jsonMainNode;
    private JSONObject jsonResponse, jsonChildNode;
    private GridLayoutManager layoutManager;
    private HttpURLConnection urlConnection;
    private URL url;
    private String name, image, price, table, servitoros_id, magazi_id;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.beer_fragment, container, false);
        recyclerView = (RecyclerView)rootView.findViewById(R.id.beerRecyclerView);
        checkOrientation();
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setNestedScrollingEnabled(true);
        table = getActivity().getIntent().getStringExtra(Constants.TABLE_INTENT_ID);
        servitoros_id = getActivity().getIntent().getStringExtra(Constants.WAITER_INTENT_ID);
        magazi_id = getActivity().getIntent().getStringExtra(Constants.COMPANY_INTENT_ID);
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.activity_main_swipe_refresh_layout);
        cm = (ConnectivityManager) getActivity().getSystemService(getActivity().getApplicationContext().CONNECTIVITY_SERVICE);
        activeNetwork = cm.getActiveNetworkInfo();
        network_connected = activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnectedOrConnecting();

        if (!network_connected) {
            onDetectNetworkState().show();
        } else {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI || activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                accessWebService();
                setRetainInstance(true);
                registerCallClickBack();
                mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        accessWebService();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        }
        return rootView;
    }

    private void checkOrientation() {
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            layoutManager = new GridLayoutManager(getActivity().getApplicationContext(), 2);
        }else{
            layoutManager = new GridLayoutManager(getActivity().getApplicationContext(), 1);

        }
    }

    private AlertDialog onDetectNetworkState() {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(getActivity().getApplicationContext());
        builder1.setMessage(R.string.wifi_off_message)
                .setTitle(R.string.wifi_off_title)
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                getActivity().finish();
                            }
                        })
                .setPositiveButton(R.string.action_settings,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                startActivityForResult((new Intent(
                                        Settings.ACTION_WIFI_SETTINGS)), 1);
                                getActivity().finish();
                            }
                        });
        return builder1.create();
    }



    private void registerCallClickBack() {
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity().getApplicationContext(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent intent = new Intent(getActivity(), BeersLayoutActivity.class);
                intent.putExtra(Constants.BEER_NAME, customList.get(position).getName());
                intent.putExtra(Constants.BEER_PRICE, customList.get(position).getPrice());
                intent.putExtra(Constants.BEER_IMAGE, customList.get(position).getImage());
                intent.putExtra(Constants.TABLE_INTENT_ID, table);
                intent.putExtra(Constants.WAITER_INTENT_ID, servitoros_id);
                intent.putExtra(Constants.COMPANY_INTENT_ID, magazi_id);
                startActivity(intent);
            }
        }));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (pDialog.isShowing()) {
                pDialog.show();
            } else {
                pDialog.dismiss();
            }
            if (onDetectNetworkState().isShowing()
                    && onDetectNetworkState() != null) {
                onDetectNetworkState().show();
            } else {
                onDetectNetworkState().dismiss();
            }
        }
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

            if (pDialog.isShowing()) {
                pDialog.show();
            } else {
                pDialog.dismiss();
            }
            if (onDetectNetworkState().isShowing()) {
                onDetectNetworkState().show();
            } else {
                onDetectNetworkState().dismiss();
            }
        }
    }


    public class JsonReadTask extends AsyncTask<String , Void, List<ProductList>> {
        public JsonReadTask() {
            super();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(getActivity());
            pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pDialog.setIndeterminate(true);
            pDialog.setMessage(getString(R.string.get_stocks));
            pDialog.setCancelable(false);
            pDialog.setInverseBackgroundForced(true);
            pDialog.show();
        }

        @Override
        protected List<ProductList> doInBackground(String... params) {
            try {
                url = new URL(params[0]);
                urlConnection =(HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty(Constants.CUSTOM_HEADER, Constants.API_KEY);
                urlConnection.setRequestMethod(Constants.METHOD_GET);
                urlConnection.connect();
                urlConnection.setConnectTimeout(5000);
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                jsonResult = StringGenerator.inputStreamToString(in, getActivity());
                customList = new ArrayList<>();

                jsonResponse = new JSONObject(jsonResult.toString());
                jsonMainNode = jsonResponse.optJSONArray(Constants.BEERS_JSON_ARRAY);
                for (int i = 0; i < jsonMainNode.length(); i++) {
                    jsonChildNode = jsonMainNode.getJSONObject(i);
                    name = jsonChildNode.optString("name");
                    price = jsonChildNode.optString("price");
                    image = jsonChildNode.optString("image");
                    customList.add(new ProductList(image, name, price));

                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return customList;
        }

        @Override
        protected void onPostExecute(List<ProductList> customList) {
            if(customList == null){
                Log.d("ERORR", "No result to show.");
                return;
            }
            ListDrawer(customList);
            pDialog.dismiss();
        }
    }// end async task

    public void accessWebService() {
        task = new JsonReadTask();
        task.execute(Constants.BEERS_URL);
    }

    public void ListDrawer(List<ProductList> customList) {
        productsAdapter = new ProductsAdapter(customList, getActivity().getApplicationContext());
        productsAdapter.notifyDataSetChanged();
        recyclerView.setAdapter(productsAdapter);
    }

}
