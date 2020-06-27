package edu.umbc.covid19;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {

    // Shared Preferences
    SharedPreferences pref;

    // Editor for Shared preferences
    SharedPreferences.Editor editor;

    // Context
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "botopia";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "name";
    private static final String KEY_SESSION_KEY = "sessionKey";

    private static final String KEY_IS_ONBOARDING_COMPLETED = "isOnboarded";
    private static final String KEY_IS_TRACING_ON = "isTracingOn";


    public PrefManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }


    public boolean isLoggedIn(){
        return pref.getBoolean(KEY_IS_LOGGED_IN,false);
    }

    public void setLoggedIn(boolean isLogged) {
        editor.putBoolean(KEY_IS_LOGGED_IN,isLogged);
        editor.commit();
    }

    public void setEmail(String email){
        editor.putString(KEY_EMAIL,email);
        editor.commit();
    }

    public String getEmail(){
        return pref.getString(KEY_EMAIL,"");
    }

    public void setName(String name){
        editor.putString(KEY_NAME,name);
        editor.commit();
    }

    public String getName(){
        return pref.getString(KEY_NAME,"");
    }

    public void setSessionKey(String key){
        editor.putString(KEY_SESSION_KEY,key);
        editor.commit();
    }

    public String getSessionKey(){
        return pref.getString(KEY_IS_LOGGED_IN,"");
    }

       public void setIsOnboardingCompleted(boolean key){
        editor.putBoolean(KEY_IS_ONBOARDING_COMPLETED,key);
        editor.commit();
    }

    public boolean getIsOnboardingCompleted(){
        return pref.getBoolean(KEY_IS_ONBOARDING_COMPLETED,false);
    }


    public void setIsTracingOn(boolean key){
        editor.putBoolean(KEY_IS_TRACING_ON,key);
        editor.commit();
    }

    public boolean getIsTracingOn(){
        return pref.getBoolean(KEY_IS_TRACING_ON,false);
    }


}
