package com.chumarin.stanislav.sample_app_dagger;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import vit.khudenko.android.sessiontracker.ISessionTrackerStorage;
import vit.khudenko.android.sessiontracker.SessionRecord;

@Singleton
public class SessionTrackerStorage implements ISessionTrackerStorage<Session, Session.State> {

    private static final String PREFS_FILENAME = "session_tracker_storage";
    private static final String KEY_SESSION_RECORDS = "session_records";
    private static final String KEY_SESSION_ID = "id";
    private static final String KEY_SESSION_STATE = "state";

    @NonNull
    private final SharedPreferences prefs;

    @Inject
    public SessionTrackerStorage(@NonNull Application appContext) {
        prefs = appContext.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE);
    }

    private static JSONObject sessionRecordToJson(SessionRecord<Session, Session.State> sessionRecord) {
        return new JSONObject(
                new HashMap<String, Object>() {{
                    put(KEY_SESSION_ID, sessionRecord.getSession().getSessionId());
                    put(KEY_SESSION_STATE, sessionRecord.getState().ordinal());
                }}
        );
    }

    private static SessionRecord<Session, Session.State> jsonToSessionRecord(JSONObject json) throws JSONException {
        return new SessionRecord<>(
                new Session(json.getString(KEY_SESSION_ID)),
                Session.State.values()[json.getInt(KEY_SESSION_STATE)]
        );
    }


    @Override
    public void createSessionRecord(@NotNull SessionRecord<Session, Session.State> sessionRecord) {
        List<SessionRecord<Session, Session.State>> sessionRecords = readAllSessionRecords();
        sessionRecords.add(sessionRecord);
        saveSessionRecords(sessionRecords);
    }

    @NotNull
    @Override
    public List<SessionRecord<Session, Session.State>> readAllSessionRecords() {
        List<SessionRecord<Session, Session.State>> sessions = new ArrayList<>();
        try {
            JSONArray sessionsJsonArray = new JSONArray(prefs.getString(KEY_SESSION_RECORDS, "[]"));
            for (int i = 0; i < sessionsJsonArray.length(); i++) {
                SessionRecord<Session, Session.State> session = jsonToSessionRecord(sessionsJsonArray.getJSONObject(i));
                sessions.add(session);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return sessions;
    }

    @Override
    public void updateSessionRecord(@NotNull SessionRecord<Session, Session.State> sessionRecord) {

    }

    @Override
    public void deleteSessionRecord(@NotNull String sessionId) {
        saveSessionRecords(
                readAllSessionRecords().stream()
                        .filter(sessionRecord -> !sessionRecord.getSession().getSessionId().equals(sessionId))
                        .collect(Collectors.toList())
        );
    }

    @Override
    public void deleteAllSessionRecords() {
        saveSessionRecords(Collections.emptyList());
    }

    @SuppressLint("ApplySharedPref")
    private void saveSessionRecords(List<SessionRecord<Session, Session.State>> sessionRecords) {
        prefs.edit()
                .putString(
                        KEY_SESSION_RECORDS,
                        new JSONArray(
                                sessionRecords.stream()
                                        .map(SessionTrackerStorage::sessionRecordToJson)
                                        .collect(Collectors.toList())
                        ).toString()
                )
                .commit();
    }
}