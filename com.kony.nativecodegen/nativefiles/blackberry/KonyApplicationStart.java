package com.konylabs.api.nativecode;

import com.konylabs.midlet.KMIDlet;

public class KonyApplicationStart extends KMIDlet {
       
        public static void main(String[] args) {
            KMIDlet mid = new KMIDlet();
            mid.startApp();
            Globalsinit.initializeApp(null);
            mid.enterEventDispatcher();
        }
 }

