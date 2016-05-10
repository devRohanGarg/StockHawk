package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {

    private String LOG_TAG = StockTaskService.class.getSimpleName();

    private Context mContext;
    private boolean isUpdate;
    private boolean history;

    public StockTaskService() {
    }

    public StockTaskService(Context context) {
        mContext = context;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int onRunTask(TaskParams params) {
        Map<String, Stock> stocks = new HashMap<>(); // single request
        String PARAM_TAG = params.getTag();

        if (mContext == null) {
            mContext = this;
        }

        switch (PARAM_TAG) {
            case "init":
            case "periodic":
                isUpdate = true;
                Cursor initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                        null, null);
                if (initQueryCursor == null || initQueryCursor.getCount() == 0) {
                    // Init task. Populates DB with quotes for the symbols seen below
                    showToast("Loading...", Toast.LENGTH_SHORT);
                    stocks = fetch(new String[]{"INTC", "BABA", "TSLA", "AIR.PA", "YHOO"});
                } else if (initQueryCursor.getCount() > 0) {
                    DatabaseUtils.dumpCursor(initQueryCursor);
                    initQueryCursor.moveToFirst();
                    ArrayList<String> symbolList = new ArrayList<>();
                    for (int i = 0; i < initQueryCursor.getCount(); i++) {
                        symbolList.add(initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")));
                        initQueryCursor.moveToNext();
                    }
                    stocks = fetch(symbolList.toArray(new String[symbolList.size()]));
                    initQueryCursor.close();
                }
                break;
            case "add":
                isUpdate = false;
                // get symbol from params.getExtra and build query
                stocks = fetch(new String[]{params.getExtras().getString("symbol")});
                break;
            case "graph":
                history = true;
                Stock stock = null;
                int position = params.getExtras().getInt("position");
                Cursor c = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{QuoteColumns.SYMBOL, QuoteColumns.STOCK, QuoteColumns.HISTORICAL_QUOTE}, null,
                        null, null);
                try {
                    if (c != null) {
                        c.moveToPosition(position);
                        stock = Utils.JSONToStock(c.getString(c.getColumnIndex(QuoteColumns.STOCK)));
                        stock.getHistory();
                        c.close();
                    }
                    if (stock != null) {
                        stocks.put(stock.getName(), stock);
                    }
                } catch (SocketTimeoutException e) {
                    showToast("Socket timed out!", Toast.LENGTH_SHORT);
                } catch (FileNotFoundException e) {
                    showToast("Non-existent stock!", Toast.LENGTH_SHORT);
                } catch (NullPointerException | IOException e) {
                    e.printStackTrace();
                }
                break;
        }

        int result = GcmNetworkManager.RESULT_FAILURE;

        if (stocks != null) {
            result = GcmNetworkManager.RESULT_SUCCESS;
            try {
                ContentValues contentValues = new ContentValues();
                // update ISCURRENT to 0 (false) so new data is current
                if (isUpdate) {
                    contentValues.put(QuoteColumns.ISCURRENT, 0);
                    mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                            null, null);
                }
                mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY, Utils.stocksToContentVals(stocks, history));
            } catch (RemoteException | OperationApplicationException e) {
                Log.e(LOG_TAG, "Error applying batch insert", e);
            } catch (SQLiteException e) {
                Log.e(LOG_TAG, e.getMessage());
                showToast("Non-existent stock", Toast.LENGTH_SHORT);
            }
        }
        return result;
    }

    private Map<String, Stock> fetch(String[] symbols) {
        Map<String, Stock> stocks = null;
        try {
            stocks = YahooFinance.get(symbols);
        } catch (SocketTimeoutException e) {
            showToast("Socket timed out!", Toast.LENGTH_SHORT);
        } catch (FileNotFoundException e) {
            showToast("Non-existent stock!", Toast.LENGTH_SHORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stocks;
    }

    private void showToast(final String message, final int length) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext.getApplicationContext(), message, length).show();
            }
        });
    }
}
