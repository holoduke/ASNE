/*******************************************************************************
 * Copyright (c) 2014 Evgeny Gorbin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/
package com.github.gorbin.asne.googleplus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.github.gorbin.asne.core.AccessToken;
import com.github.gorbin.asne.core.SocialNetwork;
import com.github.gorbin.asne.core.SocialNetworkException;
import com.github.gorbin.asne.core.listener.OnCheckIsFriendCompleteListener;
import com.github.gorbin.asne.core.listener.OnLoginCompleteListener;
import com.github.gorbin.asne.core.listener.OnLogoutCompleteListener;
import com.github.gorbin.asne.core.listener.OnPostingCompleteListener;
import com.github.gorbin.asne.core.listener.OnRequestAccessTokenCompleteListener;
import com.github.gorbin.asne.core.listener.OnRequestAddFriendCompleteListener;
import com.github.gorbin.asne.core.listener.OnRequestDetailedSocialPersonCompleteListener;
import com.github.gorbin.asne.core.listener.OnRequestGetFriendsCompleteListener;
import com.github.gorbin.asne.core.listener.OnRequestRemoveFriendCompleteListener;
import com.github.gorbin.asne.core.listener.OnRequestSocialPersonCompleteListener;
import com.github.gorbin.asne.core.listener.OnRequestSocialPersonsCompleteListener;
import com.github.gorbin.asne.core.persons.SocialPerson;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.io.File;
import java.nio.channels.AlreadyConnectedException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Class for Google plus social network integration
 *
 * @author Anton Krasov
 * @author Evgeny Gorbin (gorbin.e.o@gmail.com)
 */
public class GooglePlusSocialNetwork extends SocialNetwork implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /*** Social network ID in asne modules, should be unique*/
    public static final int ID = 3;

    private static final int REQUEST_AUTH = UUID.randomUUID().hashCode() & 0xFFFF;
    private static Activity mActivity;
    private FragmentActivity mContext;
    private static final String TAG = "GoogleActivity";
    private static final int RC_SIGN_IN = 9001;
    private GoogleApiClient mGoogleApiClient;
    private String webClientId;

    private GoogleSignInResult signInResult;

    public GooglePlusSocialNetwork(Fragment fragment, Context context, String web_client_id) {
        super(fragment, context);
        webClientId = web_client_id;
        mContext = (FragmentActivity)context;
    }

    /**
     * Check is social network connected
     * @return true if connected to Google Plus social network and false if not
     */
    @Override
    public boolean isConnected() {
        Log.d(TAG,"is connected");
        if (mGoogleApiClient == null){
            Log.d(TAG,"cannot check is connected without api client");
            return false;
        }
        return mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected();
    }

    /**
     * Overrided for Google plus
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle). Note: Otherwise it is null.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG,"on create");

        try {
            mActivity = mSocialNetworkManager.getActivity();

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(mActivity)
                    .enableAutoManage(mContext /* FragmentActivity */, this /* OnConnectionFailedListener */)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();

            Log.d(TAG,"done setting op google login service");
        }
        catch(Exception e){
            Log.e(TAG, "error setting up google login service "+e.getMessage());
        }
    }

    /**
     * Make login request - authorize in Google plus social network
     * @param onLoginCompleteListener listener for login complete
     */
    @Override
    public void requestLogin(OnLoginCompleteListener onLoginCompleteListener) {

        Log.d(TAG,"request login");

        //already logged in scenarios
        if (isConnected() && signInResult != null){
            Log.d(TAG,"already connected and signin result known");
            onLoginCompleteListener.onLoginSuccess(ID);
            return;
        }
        else if (isConnected()){
            Log.d(TAG,"already connected");
            silentLogin(onLoginCompleteListener);
            return;
        }

        forceRegularLogin(onLoginCompleteListener);
    }

    private void forceRegularLogin(OnLoginCompleteListener onLoginCompleteListener){
        if (mGoogleApiClient == null){
            Log.d(TAG,"cannot login in. no google api client");
            onLoginCompleteListener.onError(ID,"500","no_google_api_client_found",null);
            return;
        }

        registerListener(REQUEST_LOGIN, onLoginCompleteListener); //register for later use in activity result

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        mActivity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }


    private void silentLogin(final OnLoginCompleteListener onLoginCompleteListener){
        Log.d(TAG,"silent login");

        try {
            OptionalPendingResult<GoogleSignInResult> pendingResult =
                    Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);

            pendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult googleSignInResult) {

                    Log.d(TAG, " on result called silent shit " + googleSignInResult.getStatus());

                    if (googleSignInResult.isSuccess()) {
                        Log.d(TAG, "jaaa is goed");
                        Log.d(TAG, " on result called silent shit " + googleSignInResult.getSignInAccount().getDisplayName());
                        signInResult = googleSignInResult;
                        onLoginCompleteListener.onLoginSuccess(ID);
                    } else {
                        Log.d(TAG, "Damn, you cant be logged in silently man. you need to really login");
                        forceRegularLogin(onLoginCompleteListener);
                    }
                }
            });
        }
        catch(Exception e){
            Log.e(TAG,"critical error silent loggin "+e.getMessage());
        }
    }


    @Override
    public void onStart() {
    }

    @Override
    public void onStop() {

    }

    private Intent loginIntentData;
    /**
     * Overrided for Google plus
     * @param requestCode The integer request code originally supplied to startActivityForResult(), allowing you to identify who this result came from.
     * @param resultCode The integer result code returned by the child activity through its setResult().
     * @param data An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

        if (result.isSuccess()){

            if (mLocalListeners.get(REQUEST_LOGIN) != null) {
                ((OnLoginCompleteListener) mLocalListeners.get(REQUEST_LOGIN)).onLoginSuccess(getID());
            }
            else{
                Log.e(TAG,"no listeners registered for logging in result");
            }
        }
        else{
            mLocalListeners.get(REQUEST_LOGIN).onError(getID(), REQUEST_LOGIN, "error_logging_in", null);
        }
    }

    /**
     * After calling connect(), this method will be invoked asynchronously when the connect request has successfully completed.
     * @param bundle Bundle of data provided to clients by Google Play services. May be null if no content is provided by the service.
     */
    @Override
    public void onConnected(Bundle bundle) {

    }

    /**
     * Called when the client is temporarily in a disconnected state.
     * @param i The reason for the disconnection. Defined by constants CAUSE_*.
     */
    @Override
    public void onConnectionSuspended(int i) {
        if (mLocalListeners.get(REQUEST_LOGIN) != null) {
            mLocalListeners.get(REQUEST_LOGIN).onError(getID(), REQUEST_LOGIN,
                    "get person == null", null);
        }
    }

    /**
     * Called when the client is disconnected.
     */
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {

    }

    /**
     * Logout from Google plus social network
     */
    @Override
    public void logout(final OnLogoutCompleteListener completeListener) {
        Log.d(TAG,"logout");
        if (mGoogleApiClient == null){
            Log.e(TAG,"cannot sign out without api client");
            return;
        }

        try {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            Log.d(TAG, "signed out");
                            Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                                    new ResultCallback<Status>() {
                                        @Override
                                        public void onResult(Status status) {
                                            if (completeListener != null) {
                                                completeListener.onLogoutSuccess(ID);
                                            }
                                        }
                                    });
                        }
                    });
        }
        catch (Exception e){
            Log.e(TAG,"error signing out "+e.getMessage());
        }
    }

    /**
     * Get id of Google plus social network
     * @return Social network id for Google Plus = 3
     */
    @Override
    public int getID() {
        return ID;
    }

    /**
     * Not supported in Google plus sdk
     */
    @Override
    public AccessToken getAccessToken() {

        throw new SocialNetworkException("requestPostMessage isn't allowed for GooglePlusSocialNetwork");
    }

    /**
     * Request {@link com.github.gorbin.asne.core.AccessToken} of Google plus social network that you can get from onRequestAccessTokenCompleteListener
     * @param onRequestAccessTokenCompleteListener listener for {@link com.github.gorbin.asne.core.AccessToken} request
     */
    @Override
    public void requestAccessToken(final OnRequestAccessTokenCompleteListener onRequestAccessTokenCompleteListener) {
        silentLogin(new OnLoginCompleteListener() {
            @Override
            public void onLoginSuccess(int socialNetworkID) {

                onRequestAccessTokenCompleteListener.onRequestAccessTokenComplete(ID,new AccessToken(signInResult.getSignInAccount().getIdToken(),null));
            }

            @Override
            public void onError(int socialNetworkID, String requestID, String errorMessage, Object data) {
                onRequestAccessTokenCompleteListener.onError(ID,"500","error_fetching_current_person_not_logged_in",null);
            }
        });
    }

    /**
     * Request current user {@link com.github.gorbin.asne.core.persons.SocialPerson}
     * @param onRequestSocialPersonCompleteListener listener for {@link com.github.gorbin.asne.core.persons.SocialPerson} request
     */
    @Override
    public void requestCurrentPerson(final OnRequestSocialPersonCompleteListener onRequestSocialPersonCompleteListener) {

        silentLogin(new OnLoginCompleteListener() {
            @Override
            public void onLoginSuccess(int socialNetworkID) {

                try {
                    SocialPerson socialPerson = new SocialPerson();
                    socialPerson.id = signInResult.getSignInAccount().getId();
                    socialPerson.name = signInResult.getSignInAccount().getDisplayName();
                    socialPerson.avatarURL = signInResult.getSignInAccount().getPhotoUrl().toString();
                    onRequestSocialPersonCompleteListener.onRequestSocialPersonSuccess(ID, socialPerson);
                }
                catch(Exception e){
                    Log.e(TAG,"error creating social person "+e.getMessage());
                    onRequestSocialPersonCompleteListener.onError(ID,"500","error_creating_social_person",null);
                }
            }

            @Override
            public void onError(int socialNetworkID, String requestID, String errorMessage, Object data) {
                onRequestSocialPersonCompleteListener.onError(ID,"500","error_fetching_current_person_not_logged_in",null);
            }
        });
    }

    /**
     * Request {@link com.github.gorbin.asne.core.persons.SocialPerson} by user id
     * @param userID id of Google plus user
     * @param onRequestSocialPersonCompleteListener listener for {@link com.github.gorbin.asne.core.persons.SocialPerson} request
     */
    @Override
    public void requestSocialPerson(String userID, OnRequestSocialPersonCompleteListener onRequestSocialPersonCompleteListener) {
        super.requestSocialPerson(userID, onRequestSocialPersonCompleteListener);
        requestPerson(userID, onRequestSocialPersonCompleteListener);
    }

    /**
     * Request ArrayList of {@link com.github.gorbin.asne.core.persons.SocialPerson} by array of userIds
     * @param userID array of user ids in social network
     * @param onRequestSocialPersonsCompleteListener listener for request ArrayList of {@link com.github.gorbin.asne.core.persons.SocialPerson}
     */
    @Override
    public void requestSocialPersons(final String[] userID, OnRequestSocialPersonsCompleteListener onRequestSocialPersonsCompleteListener) {
        throw new SocialNetworkException("requestPostMessage isn't allowed for GooglePlusSocialNetwork");
    }

    @Override
    public void requestDetailedSocialPerson(final String userId, OnRequestDetailedSocialPersonCompleteListener onRequestDetailedSocialPersonCompleteListener) {
        throw new SocialNetworkException("requestPostMessage isn't allowed for GooglePlusSocialNetwork");
    }

    private void requestPerson(final String userID, OnRequestSocialPersonCompleteListener onRequestSocialPersonCompleteListener){
        requestCurrentPerson(onRequestSocialPersonCompleteListener);
    }

    /**
     * Not supported via Google plus sdk.
     * @throws com.github.gorbin.asne.core.SocialNetworkException
     * @param message  message that should be shared
     * @param onPostingCompleteListener listener for posting request
     */
    @Override
    public void requestPostMessage(String message, OnPostingCompleteListener onPostingCompleteListener) {
        throw new SocialNetworkException("requestPostMessage isn't allowed for GooglePlusSocialNetwork");
    }

    /**
     * Not supported via Google plus sdk.
     * @throws com.github.gorbin.asne.core.SocialNetworkException
     * @param photo photo that should be shared
     * @param message message that should be shared with photo
     * @param onPostingCompleteListener listener for posting request
     */
    @Override
    public void requestPostPhoto(File photo, String message, OnPostingCompleteListener onPostingCompleteListener) {
        throw new SocialNetworkException("requestPostPhoto isn't allowed for GooglePlusSocialNetwork");
    }

    /**
     * Not supported via Google plus sdk.
     * @throws com.github.gorbin.asne.core.SocialNetworkException
     * @param bundle bundle containing information that should be shared(Bundle constants in {@link com.github.gorbin.asne.core.SocialNetwork})
     * @param message message that should be shared with bundle
     * @param onPostingCompleteListener listener for posting request
     */
    @Override
    public void requestPostLink(Bundle bundle, String message, OnPostingCompleteListener onPostingCompleteListener) {
        throw new SocialNetworkException("requestPostLink isn't allowed for GooglePlusSocialNetwork");
    }

    /**
     * Request Google plus share dialog
     * @param bundle bundle containing information that should be shared(Bundle constants in {@link com.github.gorbin.asne.core.SocialNetwork})
     * @param onPostingCompleteListener listener for posting request
     */
    @Override
    public void requestPostDialog(Bundle bundle, OnPostingCompleteListener onPostingCompleteListener) {
        throw new SocialNetworkException("requestPostMessage isn't allowed for GooglePlusSocialNetwork");
    }

    /**
     * Not supported via Google plus sdk.
     * @throws com.github.gorbin.asne.core.SocialNetworkException
     * @param userID user id that should be checked as friend of current user
     * @param onCheckIsFriendCompleteListener listener for checking friend request
     */
    @Override
    public void requestCheckIsFriend(String userID, OnCheckIsFriendCompleteListener onCheckIsFriendCompleteListener) {
        throw new SocialNetworkException("requestCheckIsFriend isn't allowed for GooglePlusSocialNetwork");
    }

    /**
     * Get current user friends list
     * @param onRequestGetFriendsCompleteListener listener for getting list of current user friends
     */
    @Override
    public void requestGetFriends(OnRequestGetFriendsCompleteListener onRequestGetFriendsCompleteListener) {
        throw new SocialNetworkException("requestPostMessage isn't allowed for GooglePlusSocialNetwork");
    }

    private void getAllFriends(String pageToken, final ArrayList<SocialPerson> socialPersons, final ArrayList<String> ids){
        throw new SocialNetworkException("requestPostMessage isn't allowed for GooglePlusSocialNetwork");
    }

    /**
     * Not supported via Google plus sdk.
     * @throws com.github.gorbin.asne.core.SocialNetworkException
     * @param userID id of user that should be invited
     * @param onRequestAddFriendCompleteListener listener for invite result
     */
    @Override
    public void requestAddFriend(String userID, OnRequestAddFriendCompleteListener onRequestAddFriendCompleteListener) {
        throw new SocialNetworkException("requestAddFriend isn't allowed for GooglePlusSocialNetwork");
    }

    /**
     * Not supported via Google plus sdk.
     * @throws com.github.gorbin.asne.core.SocialNetworkException
     * @param userID user id that should be removed from friends
     * @param onRequestRemoveFriendCompleteListener listener to remove friend request response
     */
    @Override
    public void requestRemoveFriend(String userID, OnRequestRemoveFriendCompleteListener onRequestRemoveFriendCompleteListener) {
        throw new SocialNetworkException("requestRemoveFriend isn't allowed for GooglePlusSocialNetwork");
    }


}