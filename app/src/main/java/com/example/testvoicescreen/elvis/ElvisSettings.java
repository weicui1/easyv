/*
 * Copyright (C) 2013 Motorola Mobility LLC.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 */

package com.example.testvoicescreen.elvis;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.util.Log;

import com.example.testvoicescreen.AppConstants;
import com.nuance.dragon.toolkit.elvis.ElvisLanguage;
import com.nuance.dragon.toolkit.elvis.ElvisRecognizer; 

public class ElvisSettings {
    private static final String TAG = AppConstants.TAG + "."
            + ElvisSettings.class.getSimpleName();
    private static final boolean DEBUG = AppConstants.DEBUG;

    private static final Map<String, ElvisLanguage> sLanguageMap =
            new HashMap<String, ElvisLanguage>();
    private static final ElvisLanguage DEFAULT_LANGUAGE =
            ElvisRecognizer.Languages.UNITED_STATES_ENGLISH;

    static {
        // Language requires corresponding language file in
        // assets/elvis folder.
        // Require lvr_enus16k.bin.jpg
        sLanguageMap.put("en_US", ElvisRecognizer.Languages.UNITED_STATES_ENGLISH);
        // Require lvr_esus16k.bin.jpg
        sLanguageMap.put("es_US", ElvisRecognizer.Languages.US_SPANISH);
        // Require lvr_ptbr16k.bin.jpg
        sLanguageMap.put("pt_BR", ElvisRecognizer.Languages.BRAZILIAN_PORTUGUESE);
        // Require lvr_engb16k.bin.jpg
        sLanguageMap.put("en_GB", ElvisRecognizer.Languages.BRITISH_ENGLISH);
        // Require lvr_frfr16k.bin.jpg
        sLanguageMap.put("fr_FR", ElvisRecognizer.Languages.EUROPEAN_FRENCH);
        // Require lvr_dede16k.bin.jpg
        sLanguageMap.put("de_DE", ElvisRecognizer.Languages.GERMAN);
    }

    public static ElvisLanguage getLanguage() {
        ElvisLanguage language = sLanguageMap.get(AppConstants.language);
        if (language == null) {
            language = DEFAULT_LANGUAGE;
            if (DEBUG) {
                Log.d(TAG, "Default language is selected " + language);
            }
        }
        return language;
    }

    public static Locale getPreferredLocale(final Locale locale) {
        String strLocale = locale.toString();
        for (String localeString : sLanguageMap.keySet()) {
            if (strLocale.equalsIgnoreCase(localeString))
                return locale;
        }
        // Fallback to matching language-only if language-country does not have
        // an exact match.
        return new Locale("en_US");
    }
}
