/*
 * Copyright 2011-2014 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.wallet.ui;

import javax.annotation.Nonnull;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;

import de.schildbach.wallet.AddressBookProvider;
import de.schildbach.wallet.BitidIntent;
import de.schildbach.wallet.Constants;
import de.schildbach.wallet.PaymentIntent;
import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.ui.InputParser.StringInputParser;
import de.schildbach.wallet_test.R;

/**
 * @author Eric Larchevêque
 */
public final class BitidFragment extends SherlockFragment implements LoaderCallbacks<Cursor>
{
	private AbstractBindServiceActivity activity;
	private WalletApplication application;
	private Wallet wallet;
	private ContentResolver contentResolver;
	private LoaderManager loaderManager;

	private ECKey key;
	private boolean autoCallback;
	
	private final Handler handler = new Handler();

	private TextView hostView;
	private TextView addressView;

	private Button viewConfirm;
	private Button viewCancel;

	private BitidIntent bitidIntent;

	private static final Logger log = LoggerFactory.getLogger(BitidFragment.class);

	@Override
	public void onAttach(final Activity activity)
	{
		super.onAttach(activity);

		this.activity = (AbstractBindServiceActivity) activity;
		this.application = (WalletApplication) activity.getApplication();
		this.wallet = application.getWallet();
		this.contentResolver = activity.getContentResolver();
		this.loaderManager = getLoaderManager();
		
		this.autoCallback = false;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
		setHasOptionsMenu(true);

		if (savedInstanceState != null)
        {
            restoreInstanceState(savedInstanceState);
        }
        else
        {
            final Intent intent = activity.getIntent();
            final String action = intent.getAction();
            final Uri intentUri = intent.getData();
            final String scheme = intentUri != null ? intentUri.getScheme() : null;

            if ((Intent.ACTION_VIEW.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) && intentUri != null
                    && "bitid".equals(scheme))
            {
                initStateFromBitcoinUri(intentUri);
            }
            else if (intent.hasExtra(BitidActivity.INTENT_EXTRA_BITID_INTENT))
            {
                initStateFromIntentExtras(intent.getExtras());
            }
        }
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
	{
		final View view = inflater.inflate(R.layout.bitid_fragment, container);

		hostView = (TextView) view.findViewById(R.id.bitid_host);
		addressView = (TextView) view.findViewById(R.id.bitid_address);

		viewConfirm = (Button) view.findViewById(R.id.send_coins_go);
		viewConfirm.setText(R.string.bitid_fragment_confirm_auth);
        viewConfirm.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(final View v)
            {
                new HttpCallbackTask().execute();
            }
        });

        viewCancel = (Button) view.findViewById(R.id.send_coins_cancel);
        viewCancel.setText(R.string.bitid_fragment_cancel_auth);
        viewCancel.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(final View v)
            {
                activity.finish();
            }
        });
	
		
		return view;
	}

	@Override
	public void onDetach()
	{
		handler.removeCallbacksAndMessages(null);

		super.onDetach();
	}

	@Override
	public void onSaveInstanceState(final Bundle outState)
	{
		super.onSaveInstanceState(outState);

		saveInstanceState(outState);
	}

	private void saveInstanceState(final Bundle outState)
	{
		outState.putParcelable("bitid_intent", bitidIntent);
	}

	private void restoreInstanceState(final Bundle savedInstanceState)
	{
		bitidIntent = (BitidIntent) savedInstanceState.getParcelable("bitid_intent");
	}

	private void showAuthUI()
	{
	    getView().setVisibility(View.VISIBLE);
	    hostView.setText(bitidIntent.getHost());
		addressView.setText(key.toAddress(Constants.NETWORK_PARAMETERS).toString());
	}
	
	private final DialogInterface.OnClickListener activityDismissListener = new DialogInterface.OnClickListener()
    {
	    @Override
        public void onClick(final DialogInterface dialog, final int which)
        {
            activity.finish();
        }
    };

	private void initStateFromIntentExtras(@Nonnull final Bundle extras)
	{
		final BitidIntent bitidIntent = extras.getParcelable(BitidActivity.INTENT_EXTRA_BITID_INTENT);

		updateStateFrom(bitidIntent);
	}

	private void initStateFromBitcoinUri(@Nonnull final Uri bitcoinUri)
	{
		final String input = bitcoinUri.toString();

		new StringInputParser(input)
		{
			@Override
			protected void handlePaymentIntent(final PaymentIntent paymentIntent)
			{
			    throw new UnsupportedOperationException();
			}

			@Override
			protected void handleDirectTransaction(final Transaction transaction)
			{
			    throw new UnsupportedOperationException();
			}

            @Override
            protected void handleBitidIntent(BitidIntent bitidIntent) {
                updateStateFrom(bitidIntent);
            }
            
			@Override
			protected void error(final int messageResId, final Object... messageArgs)
			{
				dialog(activity, activityDismissListener, 0, messageResId, messageArgs);
			}
		}.parse();
	}

	private void updateStateFrom(final @Nonnull BitidIntent bitidIntent)
	{
		log.info("got {}", bitidIntent);

		this.bitidIntent = bitidIntent;

		// delay these actions until fragment is resumed
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
			    log.info("initLoader");
			    loaderManager.initLoader(0, null, BitidFragment.this);
			}
		});
	}

	class HttpCallbackTask extends AsyncTask<Void, Void, HttpResponse> 
	{

	    ProgressDialog progressDialog;
        AndroidHttpClient http = AndroidHttpClient.newInstance(Constants.USER_AGENT);

        @Override
        protected void onPreExecute()
        {
            getView().setVisibility(View.GONE);
            progressDialog = new ProgressDialog(activity);
            progressDialog.setCancelable(false);
            progressDialog.setMessage(getString(R.string.bitid_auth_processing));
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setProgress(0);
            progressDialog.show();
        }
        
        @Override
        protected HttpResponse doInBackground(Void... params) 
        {
            final String signature = key.signMessage(bitidIntent.message);
            log.info("Signed message : " + signature);
            
            try 
            {
                log.info("POSTing callback to " + bitidIntent.callback);
                
                HttpPost post = new HttpPost(bitidIntent.callback);
                
                JSONObject json = new JSONObject();
                json.put(Constants.BITID_PARAM_ADDRESS, key.toAddress(Constants.NETWORK_PARAMETERS).toString());
                json.put(Constants.BITID_PARAM_SIGNATURE, signature);
                json.put(Constants.BITID_PARAM_URI, bitidIntent.message);
                post.setEntity(new StringEntity(json.toString()));
                
                post.setHeader("Accept", "application/json");
                post.setHeader("Content-type", "application/json");
                
                return http.execute(post);
            } 
            catch (Exception e) 
            {
                return null;
            } 
            finally 
            {
                http.close();
            }
        }

        @Override
        protected void onPostExecute(HttpResponse response) 
        {
            progressDialog.dismiss();
            
            if (response != null) 
            {
                log.info("Callback response status : " + response.getStatusLine());
                
                try
                {
                    if (response.getStatusLine().getStatusCode() == 200)
                    {
                        Toast.makeText(activity, R.string.bitid_auth_successfull, Toast.LENGTH_SHORT).show();
                        activity.finish();
                    }
                    else
                    {
                        callbackError(response.getStatusLine().getReasonPhrase());
                    }
                }
                catch (Exception e)
                {
                    callbackError(e.getMessage());
                }
            }
        }
        
    }
	
	private void callbackError(String message)
    {
        final DialogBuilder dialog = new DialogBuilder(activity);
        dialog.setTitle(R.string.bitid_callback_error_title);
        dialog.setMessage(message);
        dialog.singleDismissButton(null);
        dialog.show();
    }
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) 
	{
	    return new CursorLoader(activity, AddressBookProvider.contentUri(activity.getPackageName()), null, 
	            AddressBookProvider.SELECTION_QUERY, new String[] { bitidIntent.getAddressLabel() }, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) 
	{
	    if (data != null && data.moveToFirst()) 
	    {
            final String addressHash = data.getString(data.getColumnIndexOrThrow(AddressBookProvider.KEY_ADDRESS));

            log.info("Found address " + addressHash + " for label " + bitidIntent.getAddressLabel());

            for (ECKey _key : wallet.getKeys())
            {
                if (addressHash.equals(_key.toAddress(Constants.NETWORK_PARAMETERS).toString()))
                {
                    key = _key;
                    autoCallback = true;
                }
            }
            
            if (key == null)
            {
                final Uri uri = AddressBookProvider.contentUri(activity.getPackageName()).buildUpon().appendPath(addressHash).build();
                contentResolver.delete(uri, null, null);
                key = generateNewKeyForHost();
            }            
	    } 
	    else 
	    {
	        key = generateNewKeyForHost();
	    }

        if (autoCallback)
	    {
            new HttpCallbackTask().execute();
	    }
        else
        {
            showAuthUI();            
        }
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) 
	{
	}
	
	private ECKey generateNewKeyForHost()
	{
        final ECKey key = application.addNewKeyToWallet();
        
        final Uri uri = AddressBookProvider.contentUri(activity.getPackageName()).buildUpon().appendPath(key.toAddress(Constants.NETWORK_PARAMETERS).toString()).build();
        
        final ContentValues values = new ContentValues();
        values.put(AddressBookProvider.KEY_LABEL, bitidIntent.getAddressLabel());
        
        loaderManager.destroyLoader(0);
        contentResolver.insert(uri, values);
        
        log.info("created new Address " + key.toAddress(Constants.NETWORK_PARAMETERS).toString() + " for host " + bitidIntent.getAddressLabel());
        
        return key;
	}
}
