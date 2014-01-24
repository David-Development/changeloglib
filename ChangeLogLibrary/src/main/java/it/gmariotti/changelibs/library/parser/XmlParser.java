/*******************************************************************************
 * Copyright (c) 2013 Gabriele Mariotti.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package it.gmariotti.changelibs.library.parser;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.gmariotti.changelibs.library.Constants;
import it.gmariotti.changelibs.library.internal.ChangeLog;
import it.gmariotti.changelibs.library.internal.ChangeLogException;
import it.gmariotti.changelibs.library.internal.ChangeLogRow;
import it.gmariotti.changelibs.library.internal.ChangeLogRowHeader;

/**
 * Read and parse res/raw/changelog.xml.
 * Example:
 *
 * <pre>
 *    XmlParser parse = new XmlParser(this);
 *    ChangeLog log=parse.readChangeLogFile();
 * </pre>
 *
 * If you want to use a custom xml file, you can use:
 * <pre>
 *    XmlParser parse = new XmlParser(this,R.raw.mycustomfile);
 *    ChangeLog log=parse.readChangeLogFile();
 * </pre>
 *
 * It is a example for changelog.xml
 * <pre>
 *  <?xml version="1.0" encoding="utf-8"?>
 *       <changelog bulletedList=false>
 *            <changelogversion versionName="1.2" changeDate="20/01/2013">
 *                 <changelogtext>new feature to share data</changelogtext>
 *                 <changelogtext>performance improvement</changelogtext>
 *            </changelogversion>
 *            <changelogversion versionName="1.1" changeDate="13/01/2013">
 *                 <changelogtext>issue on wifi connection</changelogtext>*
 *            </changelogversion>*
 *       </changelog>
 * </pre>
 *
 * @author Gabriele Mariotti (gabri.mariotti@gmail.com)
 *
 */
public class XmlParser extends BaseParser {

    /** TAG for logging **/
    private static String TAG="XmlParser";
    private int mChangeLogFileResourceId= Constants.mChangeLogFileResourceId;

    //--------------------------------------------------------------------------------
    //TAGs and ATTRIBUTEs in xml file
    //--------------------------------------------------------------------------------

    private static final String TAG_CHANGELOG="changelog";
    private static final String TAG_CHANGELOGVERSION="changelogversion";
    private static final String TAG_CHANGELOGTEXT="changelogtext";

    private static final String ATTRIBUTE_BULLETEDLIST="bulletedList";
    private static final String ATTRIBUTE_VERSIONNAME="versionName";
    private static final String ATTRIBUTE_CHANGEDATE="changeDate";
    private static final String ATTRIBUTE_CHANGETEXT="changeText";
    private static final String ATTRIBUTE_CHANGETEXTTITLE= "changeTextTitle";

    //--------------------------------------------------------------------------------
    //Constructors
    //--------------------------------------------------------------------------------

    /**
     * Create a new instance for a context.
     *
     * @param context  current Context
     */
    public XmlParser(Context context){
        super(context);
    }

    /**
     * Create a new instance for a context and for a custom changelogfile.
     *
     * You have to use file in res/raw folder.
     *
     * @param context  current Context
     * @param changeLogFileResourceId  reference for a custom xml file
     */
    public XmlParser(Context context,int changeLogFileResourceId){
        super(context);
        this.mChangeLogFileResourceId=changeLogFileResourceId;
    }
    //--------------------------------------------------------------------------------


    /**
     * Read and parse res/raw/changelog.xml or custom file
     *
     * @throws Exception if changelog.xml or custom file is not found or if there are errors on parsing
     *
     * @return {@link ChangeLog} obj with all data
     */
    @Override
    public ChangeLog readChangeLogFile() throws Exception{

        ChangeLog chg=null;

        ArrayList<String> changelogArr = new ArrayList<String>();

        try {
            //InputStream is = mContext.getResources().openRawResource(mChangeLogFileResourceId);
            URL url = new URL("https://raw.github.com/owncloud/News-Android-App/master/README.md");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                InputStream isTemp = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(isTemp));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if(inputLine.startsWith("- "))
                        inputLine = inputLine.substring(2);

                    changelogArr.add(inputLine.replace("<", "[").replace(">", "]"));
                }
                    //changelog += inputLine + "<br>";
                in.close();
                //readStream(in);
            } finally {
                urlConnection.disconnect();
            }

            for(int i = 0; i < changelogArr.size(); i++) {
                if(changelogArr.get(i).equals("Updates")) {
                    if(changelogArr.get(i + 1).equals("==================================")) {
                        changelogArr.subList(0, i+1).clear();
                        break;
                    }
                }
            }

            //String regex = "((.|\\s)*?)(?=Updates)";
            //changelog = changelog.substring(changelog.indexOf("Updates<br>=================================="));
            //changelog = changelog.replaceAll(regex, "");
            //changelog = changelog.replace("Updates<br>==================================", "").trim();
            //changelog = changelog.replaceAll("\n", "<br>");


            //String[] changelogArr = changelog.split("<br>");

            for(int i = 0; i < changelogArr.size() - 1; i++) {
                if(i < changelogArr.size()) {
                    if(changelogArr.get(i + 1).contains("---------------------")) {
                        changelogArr.set(i, "<changelogversion versionName=\"" + changelogArr.get(i) + "\">");
                        changelogArr.set(i + 1, "");
                    } else if(!changelogArr.get(i).equals("==================================")) {
                        changelogArr.set(i, "<changelogtext>" + changelogArr.get(i).trim() +  "</changelogtext>");
                    } else {
                        changelogArr.remove(i);
                        i--;
                    }

                }
            }

            boolean firstOccurence = true;
            for(int i = 0; i < changelogArr.size(); i++) {
                if(changelogArr.get(i).equals("<changelogtext></changelogtext>")) {
                    changelogArr.remove(i);
                    i--;
                } else if(changelogArr.get(i).contains("<changelogversion")) {
                    if(!firstOccurence) {
                        changelogArr.add(i, "</changelogversion>");
                        i++;
                    }

                    firstOccurence = false;
                }
            }
            changelogArr.add("</changelogversion>");


            /*
            Pattern pattern = Pattern.compile("dasad===");
            Matcher matcher_version = pattern.matcher(changelog);

            StringBuilder sb = new StringBuilder(changelog);
            while(matcher_version.find()) {
                String versionNumber = "1";
                changelog = sb.replace(matcher_version.start(), matcher_version.end(), "<changelogversion versionName=\"" + versionNumber + "\" >").toString();
                //matcher_version = pattern.matcher(changelog);
                matcher_version.reset();
            }*/

            String changelog = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<changelog bulletedList=\"true\">";

            StringBuilder builder = new StringBuilder(changelog);
            for (String value : changelogArr) {
                builder.append(value);
            }
            changelog = builder.toString();
            changelog += "</changelog>";



            //StringReader sr = new StringReader(changelog);
            InputStream is = new ByteArrayInputStream(changelog.getBytes());

            //InputStream is = mContext.getResources().openRawResource(mChangeLogFileResourceId);

            if (is!=null){

                // Create a new XML Pull Parser.
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(is, null);
                parser.nextTag();

                // Create changelog obj that will contain all data
                chg=new ChangeLog();
                // Parse file
                readChangeLogNode(parser, chg);

                // Close inputstream
                is.close();
            }else{
                Log.d(TAG,"Changelog.xml not found");
                throw new ChangeLogException("Changelog.xml not found");
            }
        } catch (XmlPullParserException xpe) {
            Log.d(TAG,"XmlPullParseException while parsing changelog file",xpe);
            throw  xpe;
        } catch (IOException ioe){
            Log.d(TAG,"Error i/o with changelog.xml",ioe);
            throw ioe;
        }

        if (chg!=null)
            Log.d(TAG,"Process ended. ChangeLog:"+chg.toString());

        return chg;
    }


    /**
     * Parse changelog node
     *
     * @param parser
     * @param changeLog
     */
    protected void readChangeLogNode(XmlPullParser parser,ChangeLog changeLog) throws Exception{

        if (parser==null || changeLog==null) return;

        // Parse changelog node
        parser.require(XmlPullParser.START_TAG, null,TAG_CHANGELOG);
        Log.d(TAG,"Processing main tag=");

        // Read attributes
        String bulletedList = parser.getAttributeValue(null, ATTRIBUTE_BULLETEDLIST);
        if (bulletedList==null || bulletedList.equals("true")){
            changeLog.setBulletedList(true);
            super.bulletedList=true;
        }else{
            changeLog.setBulletedList(false);
            super.bulletedList=false;
        }

        //Parse nested nodes
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String tag = parser.getName();
            Log.d(TAG,"Processing tag="+tag);

            if (tag.equals(TAG_CHANGELOGVERSION)) {
                readChangeLogVersionNode(parser, changeLog);
            }
        }
    }

    /**
     * Parse changeLogVersion node
     *
     * @param parser
     * @param changeLog
     * @throws Exception
     */
    protected void readChangeLogVersionNode(XmlPullParser parser, ChangeLog changeLog) throws  Exception{

        if (parser==null) return;

        parser.require(XmlPullParser.START_TAG, null,TAG_CHANGELOGVERSION);

        // Read attributes
        String versionName = parser.getAttributeValue(null, ATTRIBUTE_VERSIONNAME);
        String changeDate= parser.getAttributeValue(null, ATTRIBUTE_CHANGEDATE);
        if (versionName==null)
            throw new ChangeLogException("VersionName required in changeLogVersion node");

        ChangeLogRowHeader row=new ChangeLogRowHeader();
        row.setVersionName(versionName);
        row.setChangeDate(changeDate);
        changeLog.addRow(row);

        Log.d(TAG,"Added rowHeader:"+row.toString());

        // Parse nested nodes
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tag = parser.getName();
            Log.d(TAG,"Processing tag="+tag);

            if (tag.equals(TAG_CHANGELOGTEXT)){
                readChangeLogRowNode(parser, changeLog,versionName);
            }
        }
    }

    /**
     *  Parse changeLogText node
     *
     * @param parser
     * @param changeLog
     * @throws Exception
     */
    private void readChangeLogRowNode(XmlPullParser parser, ChangeLog changeLog,String versionName) throws  Exception{

        if (parser==null) return;

        parser.require(XmlPullParser.START_TAG, null,TAG_CHANGELOGTEXT);

        String tag = parser.getName();
        if (tag.equals(TAG_CHANGELOGTEXT)){
            ChangeLogRow row=new ChangeLogRow();
            row.setVersionName(versionName);

            // Read attributes
            String changeLogTextTitle=parser.getAttributeValue(null,ATTRIBUTE_CHANGETEXTTITLE);
            if (changeLogTextTitle!=null)
                row.setChangeTextTitle(changeLogTextTitle);

            // It is possible to force bulleted List
            String bulletedList = parser.getAttributeValue(null, ATTRIBUTE_BULLETEDLIST);
            if (bulletedList!=null){
                if (bulletedList.equals("true")){
                    row.setBulletedList(true);
                }else{
                    row.setBulletedList(false);
                }
            }else{
                row.setBulletedList(super.bulletedList);
            }

            // Read text
            if (parser.next() == XmlPullParser.TEXT) {
                String changeLogText=parser.getText();
                if (changeLogText==null)
                    throw new ChangeLogException("ChangeLogText required in changeLogText node");
                row.parseChangeText(changeLogText);
                parser.nextTag();
            }
            changeLog.addRow(row);

            Log.d(TAG,"Added row:"+row.toString());
        }
        parser.require(XmlPullParser.END_TAG, null,TAG_CHANGELOGTEXT);
    }

}
