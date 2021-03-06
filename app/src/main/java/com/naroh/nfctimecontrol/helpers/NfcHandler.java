/*
 * Copyright (c) 2016. OneOctopus www.oneoctopus.es
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.naroh.nfctimecontrol.helpers;

import android.content.Context;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Parcelable;
import android.widget.Toast;

import org.ndeftools.MimeRecord;
import org.ndeftools.Record;
import org.ndeftools.UnsupportedRecord;
import org.ndeftools.externaltype.AndroidApplicationRecord;

import java.io.IOException;
import java.util.List;

import com.naroh.nfctimecontrol.R;
import com.naroh.nfctimecontrol.other.Constants;
import com.naroh.nfctimecontrol.other.IterableMessage;

public class NfcHandler {
    private Context context;

    public NfcHandler(Context context) {
        this.context = context;
    }

    public Tag eraseTag(Tag tag){
        Ndef ndefTag = Ndef.get(tag);
            try {
                ndefTag.connect();
                ndefTag.writeNdefMessage(new NdefMessage(new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)));
                ndefTag.close();
                return tag;
            } catch (IOException | FormatException e) {
                e.printStackTrace();
                return null;
            }
    }

    /**
     * Write the desired data into the scanned NFC tag.
     * @param tag scanned tag
     * @param name the place name
     * @return true if the operation is successful, false otherwise
     */
    public boolean writeTag(Tag tag, String name){
        Ndef ndefTag = Ndef.get(tag);

        // Check if the tag is formatted. If it is not, format it
        if(ndefTag == null)
            ndefTag = Ndef.get(eraseTag(tag));

        // The format wasn't successful, so we abort the mission
        if(ndefTag == null)
            return false;

        // Check if the NFC tag is writable
        if(!ndefTag.isWritable()){
            Toast.makeText(context, R.string.nfc_tag_not_writable, Toast.LENGTH_LONG).show();
            return false;
        }

        // Create a record so the device knows what application should handle the tag
        NdefRecord appRecord = NdefRecord.createApplicationRecord(context.getPackageName());
        // Create a mimetype with the package name and the place name
        byte[] placeNameBytes = name.getBytes();
        NdefRecord dataRecord = NdefRecord.createMime(Constants.NFC_MIME_TYPE, placeNameBytes);

        /*
            Create the definitive NFC message
            Write the data record before the AAR so Android can read the tag when scanned
            even if the activity is not active.
            Reference: http://stackoverflow.com/a/25510642/1376140
        */

        NdefMessage message = new NdefMessage(new NdefRecord[] {dataRecord, appRecord});

        // Check if there is enough space
        int messageSize = message.toByteArray().length;
        if(ndefTag.getMaxSize() < messageSize){
            Toast.makeText(context, R.string.nfc_tag_no_space, Toast.LENGTH_LONG).show();
            return false;
        }
        // Try to format and write the data in the tag
        try{
            ndefTag.connect();
            ndefTag.writeNdefMessage(message);
            ndefTag.close();
            Toast.makeText(context, R.string.nfc_tag_successful_write, Toast.LENGTH_LONG).show();
            return true;
        } catch (FormatException | IOException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.nfc_tag_error_writing, Toast.LENGTH_LONG).show();
            return false;
        }
    }
    public String readTag(Parcelable[] data){
        if(data == null)
            return null;
        NdefMessage[] messages = new NdefMessage[data.length];
        for(int i=0; i<data.length; i++)
            messages[i] = (NdefMessage) data[i];
        
        if (messages.length > 0) {
            IterableMessage message = new IterableMessage();
            for (NdefMessage m : messages) {
                for (NdefRecord record : m.getRecords()) {
                    try {
                        message.add(Record.parse(record));
                    } catch (FormatException e) {
                        e.printStackTrace();
                        // if record is unsupported or corrupted, keep it.
                        message.add(UnsupportedRecord.parse(record));
                    }
                }
            }
            return parse(message);
        }else
            return null;
    }

    private String parse(IterableMessage message) {
        // Our tags only have 2 records
        if (message.size() != 2)
            return null;

        List<Record> records = message.getAllRecords();
        if(records.get(0) instanceof MimeRecord && records.get(1) instanceof AndroidApplicationRecord ){
            if(((AndroidApplicationRecord) records.get(1)).getPackageName().equals(Constants.PACKAGE_NAME))
                return new String((((MimeRecord) records.get(0)).getData()));
            else return null;
        } else return null;
    }
}
