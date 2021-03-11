package com.example.myapplication.manager;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.example.myapplication.BaseApplication;
import com.example.myapplication.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class BillingManager implements PurchasesUpdatedListener, BillingClientStateListener, SkuDetailsResponseListener  {

    private static final String LOG_TAG = "iabv3";

    private static BillingClient billingClient;
    private Activity activityy;

    private static final List<String> LIST_OF_SKUS = Collections.unmodifiableList(
            new ArrayList<String>() {{
                add(Constants.BASIC_SKU);
                add(Constants.PREMIUM_SKU);
            }});

    private static BillingManager manager;
    List<SkuDetails> skuDetailsList = new ArrayList<>();

    private Context context;

    private BillingManager(Activity activity) {
        activityy = activity;
        context = BaseApplication.getInstance();
        billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(this);
    }

    public static BillingManager getInstance(Activity activity) {
        if (manager == null) {
            manager = new BillingManager(activity);
        }
        return manager;
    }

    /**
     * In order to make purchases, you need the {@link SkuDetails} for the item or subscription.
     * This is an asynchronous call that will receive a result in {@link #onSkuDetailsResponse}.
     */
    public void querySkuDetails() {
        Log.d(LOG_TAG, "querySkuDetails");
        List<String> skuList = new ArrayList<> ();
        skuList.add("premium_upgrade");
        skuList.add("gas");
        SkuDetailsParams params = SkuDetailsParams.newBuilder()
                .setType(BillingClient.SkuType.INAPP)
                .setSkusList(skuList)
                .build();
        Log.i(LOG_TAG, "querySkuDetailsAsync");
        billingClient.querySkuDetailsAsync(params, this);
    }

    /**
     * Launching the billing flow.
     * <p>
     * Launching the UI to make a purchase requires a reference to the Activity.
     */
    public void launchBillingFlow(BillingFlowParams params) {
        String sku = params.getSku();
        String oldSku = params.getOldSku();
        Log.i(LOG_TAG, "launchBillingFlow: sku: " + sku + ", oldSku: " + oldSku);
        if (!billingClient.isReady()) {
            Log.e(LOG_TAG, "launchBillingFlow: BillingClient is not ready");
        }
        BillingResult billingResult = billingClient.launchBillingFlow(activityy, params);
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(LOG_TAG, "launchBillingFlow: BillingResponse " + responseCode + " " + debugMessage);
    }

    void handlePurchase(Purchase purchase) {
        // Purchase retrieved from BillingClient#queryPurchases or your PurchasesUpdatedListener.
        Purchase purchasee = purchase;

        // Verify the purchase.
        // Ensure entitlement was not already granted for this purchaseToken.
        // Grant entitlement to the user.

        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

        ConsumeResponseListener listener = (billingResult, purchaseToken) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                // Handle the success of the consume operation.
            }
        };

        billingClient.consumeAsync(consumeParams, listener);
    }

    // LISTENERS

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Log.d(LOG_TAG,"an error caused by a user cancelling the purchase flow");
        } else {
            // Handle any other error codes.
            Log.d(LOG_TAG,"an error occurred in purchasing flow");
        }
    }

    @Override
    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
        if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
            // The BillingClient is ready. You can query purchases here.
            Log.d(LOG_TAG,"The BillingClient is ready. You can query purchases here");
            querySkuDetails();
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        Log.d(LOG_TAG,"Billing Service Disconnected. Try to restart the connection by calling the startConnection() method");
    }

    @Override
    public void onSkuDetailsResponse(@NonNull BillingResult billingResult, @Nullable List<SkuDetails> list) {

        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        switch (responseCode) {
            case BillingClient.BillingResponseCode.OK:
                Log.i(LOG_TAG, "onSkuDetailsResponse: " + responseCode + " " + debugMessage);
                final int expectedSkuDetailsCount = LIST_OF_SKUS.size();
                Log.i(LOG_TAG, "expectedSkuDetailsCount: " + expectedSkuDetailsCount);
                // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
                assert list != null;
                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(list.get(0))
                        .build();
                launchBillingFlow(billingFlowParams);
                break;
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
            case BillingClient.BillingResponseCode.ERROR:
                Log.e(LOG_TAG, "onSkuDetailsResponse: " + responseCode + " " + debugMessage);
                break;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                Log.i(LOG_TAG, "onSkuDetailsResponse: " + responseCode + " " + debugMessage);
                break;
            // These response codes are not expected.
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
            default:
                Log.wtf(LOG_TAG, "onSkuDetailsResponse: " + responseCode + " " + debugMessage);
        }
    }
}