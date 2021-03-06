package org.androidtown.signupdemo;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import Model.Order;
import Model.Restaurant;
import info.guardianproject.netcipher.NetCipher;
import listadpater.CartAdapter;
import Database.Database;
import listadpater.RestaurantListAdapter;

public class ShowCart extends AppCompatActivity {
    TextView txt_expectedTime;
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

//    FirebaseDatabase database;
//    DatabaseReference request;

    TextView txtTotalPrice;
    Button btnOrder;
    Button btnClean;

    List<Order> cart = new ArrayList<>();
    CartAdapter adapter;
    JSONObject jsonObject;
    int expectedTime;
    Order globalOrder;
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity__cart);

        //database = FirebaseDatabase.getInstance();
        //request = database.getReference("Request");
        globalOrder = (Order) getIntent().getSerializableExtra("order");
        recyclerView = (RecyclerView) findViewById(R.id.listCart);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        txtTotalPrice = (TextView) findViewById(R.id.total);
        btnOrder = (Button) findViewById(R.id.btnOrder);
        txt_expectedTime = (TextView) findViewById(R.id.expectedTime);
        btnClean = (Button) findViewById(R.id.btnClean);
        loadListOrder();

        token = MyFirebaseInstanceIDService.getRealtoken();
        //Toast.makeText(getApplicationContext(), token, Toast.LENGTH_LONG).show();


        //txt_expectedTime.setText();
    }
    private void loadListOrder() {
        cart = new Database(this).getCarts();
        adapter = new CartAdapter(cart, this);
        recyclerView.setAdapter(adapter);
        int restaurantDelay=0;
        int menuCookingTime=0;
        expectedTime=0;

        for(Order order : cart){
            int tempDelay = order.getRestaurant_delayTime();
            int tempCooking = order.getMenu_delayTime();
            if(expectedTime < tempDelay+tempCooking) {
                restaurantDelay=tempDelay;
                menuCookingTime = tempCooking;
                expectedTime= tempCooking+tempDelay;}
        }

        int total = 0;
        for (Order order : cart)
            total += (order.getMenu_Price() + order.getSumMenuOptionPrice());
//        Locale locale = new Locale("en", "US");
//        NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

        txtTotalPrice.setText(total+"won");
        txt_expectedTime.setText(expectedTime+"minutes");
        btnClean.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                new Database(getBaseContext()).cleanCart();
                loadListOrder();
            }
        });

        btnOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 jsonObject = new JSONObject();
                 try{
                    JSONArray jArray = new JSONArray();
                    for(int i=0; i<cart.size(); i++) {
                        JSONObject jsonObject1 = new JSONObject();
                        JSONArray jArray2 = new JSONArray();
                        for (int j = 0; j < cart.get(i).getMenuOption_ListNum(); j++) {
                            JSONObject jsonObject2 = new JSONObject();
                            jsonObject2.put("MenuOption_Code", cart.get(i).getMenuOption_List().get(j).getMenuoption_code() + "");
                            jArray2.put(jsonObject2);
                        }
                        jsonObject1.put("Email", cart.get(i).getEmail());
                        jsonObject1.put("Restaurant_Code", cart.get(i).getRestaurant_Code());
                        jsonObject1.put("Menu_Code", cart.get(i).getMenu_Code());
                        jsonObject1.put("ClientToken", token);
                        jsonObject1.put("MenuOption_CodeList", jArray2);

                        jArray.put(jsonObject1);
                    }
                    jsonObject.put("mobileOrders",jArray);

                }catch (JSONException e){}
                JSONTask task = new JSONTask();
                task.execute("https://freeorder1010.herokuapp.com/order/mobile");//AsyncTask 시작시킴
                //task.execute("http://172.16.20.141:3000/auth/yes");//AsyncTask 시작시킴
                //
//                MyFirebaseMessagingService.setOrder(globalOrder,expectedTime);
                Intent i = new Intent(getApplicationContext(), ShowWaiting.class);
                i.putExtra("globalOrder", globalOrder);
                i.putExtra("expectedTime", expectedTime);
               // i.putExtra("order",getIntent().getSerializableExtra("order"));
                startActivity(i);
            }
        });


    }

    public class JSONTask extends AsyncTask<String, String, String> {

        @Override

        protected String doInBackground(String... urls) {

            try {
                //JSONObject를 만들고 key value 형식으로 값을 저장해준다.
                HttpURLConnection con = null;
                BufferedReader reader = null;
                try {
                    URL url = new URL(urls[0]);
                    //연결을 함
                    //con = (HttpURLConnection) url.openConnection();

                    con = NetCipher.getHttpsURLConnection(url);
                    con.setConnectTimeout(40000);
                    con.setReadTimeout(30000);
                    con.setRequestMethod("POST");//POST방식으로 보냄
                    con.setRequestProperty("Cache-Control", "no-cache");//캐시 설정
                    con.setRequestProperty("Content-Type", "application/json");//application JSON 형식으로 전송
                    con.setRequestProperty("Accept", "text/html");//서버에 response 데이터를 html로 받음
                    con.setDoOutput(true);//Outstream으로 post 데이터를 넘겨주겠다는 의미
                    con.setDoInput(true);//Inputstream으로 서버로부터 응답을 받겠다는 의미
                    con.connect();
                    //서버로 보내기위해서 스트림 만듬

                    OutputStream outStream = con.getOutputStream();
                    //버퍼를 생성하고 넣음
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream));
                    writer.write(jsonObject.toString());
                    writer.flush();
                    writer.close();//버퍼를 받아줌
                    //서버로 부터 데이터를 받음
                   InputStream stream = con.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(stream));
                    StringBuffer buffer = new StringBuffer();
                    String line = "";


                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }
                    return buffer.toString();//서버로 부터 받은 값을 리턴해줌 아마 OK!!가 들어올것임
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
//                    Intent cartIntent = new Intent(getApplicationContext(),ShowCart.class);
//                    cartIntent.putExtra("order",globalOrder);
//                    startActivity(cartIntent);
                    //Toast.makeText(getApplicationContext(), "Your Order has been accepted!", Toast.LENGTH_LONG).show();
                    //e.printStackTrace();
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                    try {
                        if (reader != null) {
                            reader.close();//버퍼를 닫아줌
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        protected void onPostExecute(String result) {
            super.onPostExecute(result);


            //Toast.makeText(getApplicationContext(), "gimochi", Toast.LENGTH_LONG).show();
            try{

                    JSONParser jsonParser = new JSONParser();
                    org.json.simple.JSONObject jsonObj = (org.json.simple.JSONObject) jsonParser.parse(result);
                    String a = jsonObj.get("response").toString();
                    //Toast.makeText(getApplicationContext(), a, Toast.LENGTH_LONG).show();
                //Toast.makeText(getApplicationContext(), "Your Order has been accepted!", Toast.LENGTH_LONG).show();


                if (a.equals("cancel")) {
                        Intent cartIntent = new Intent(getApplicationContext(), ShowCart.class);
                        cartIntent.putExtra("order", globalOrder);
                        startActivity(cartIntent);
                        Toast.makeText(getApplicationContext(), "Sorry... Your order has been denyed.", Toast.LENGTH_LONG).show();
                    }
                if (a.equals("accept")) {

                }


            }
            catch(ParseException e){
                e.printStackTrace();
            }
            catch(NullPointerException e){}
        }
    }

}