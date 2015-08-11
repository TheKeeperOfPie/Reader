/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StrikethroughSpan;
import android.util.Log;

import org.xml.sax.XMLReader;

import java.util.Stack;

/**
 * Created by TheKeeperOfPie on 7/2/2015.
 */
public class TagHandlerReddit implements Html.TagHandler {

    /*

        Some code borrowed from https://bitbucket.org/Kuitsi/android-textview-html-list

        Copyright 2013-2015 Juha Kuitunen

        Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
        except in compliance with the License. You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software distributed under the
        License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
        either express or implied. See the License for the specific language governing permissions
        and limitations under the License.

     */

    private static final String TAG = TagHandlerReddit.class.getCanonicalName();
    private Stack<String> lists = new Stack<>();
    private Stack<Integer> olNextIndex = new Stack<>();
    private static final int indent = 10;
    private static final int listItemIndent = indent * 2;
    private static final BulletSpan bullet = new BulletSpan(indent);

    @Override
    public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {

        if (tag.equalsIgnoreCase("del") || tag.equalsIgnoreCase("strike") || tag.equals("s")) {

            if (opening) {
                start(output, new Strike());

            }
            else {
                end(output, Strike.class, new StrikethroughSpan());
            }
        }
        else if (tag.equalsIgnoreCase("ul")) {
            if (opening) {
                lists.push(tag);
            }
            else {
                lists.pop();
            }
        }
        else if (tag.equalsIgnoreCase("ol")) {
            if (opening) {
                lists.push(tag);
                olNextIndex.push(1);
            }
            else {
                lists.pop();
                olNextIndex.pop();
            }
        }
        else if (tag.equalsIgnoreCase("li")) {
            if (opening) {
                if (output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                    output.append("\n");
                }
                String parentList = lists.peek();
                if (parentList.equalsIgnoreCase("ol")) {
                    start(output, new Ol());
                    output.append(String.valueOf(olNextIndex.peek())).append(". ");
                    olNextIndex.push(olNextIndex.pop() + 1);
                }
                else if (parentList.equalsIgnoreCase("ul")) {
                    start(output, new Ul());
                }
            }
            else {
                if (lists.peek().equalsIgnoreCase("ul")) {
                    if (output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                        output.append("\n");
                    }
                    int bulletMargin = indent;
                    if (lists.size() > 1) {
                        bulletMargin = indent - bullet.getLeadingMargin(true);
                        if (lists.size() > 2) {
                            bulletMargin -= (lists.size() - 2) * listItemIndent;
                        }
                    }
                    BulletSpan newBullet = new BulletSpan(bulletMargin);
                    end(output,
                            Ul.class,
                            new LeadingMarginSpan.Standard(listItemIndent * (lists.size() - 1)),
                            newBullet);
                }
                else if (lists.peek().equalsIgnoreCase("ol")) {
                    if (output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                        output.append("\n");
                    }
                    int numberMargin = listItemIndent * (lists.size() - 1);
                    if (lists.size() > 2) {
                        numberMargin -= (lists.size() - 2) * listItemIndent;
                    }
                    end(output,
                            Ol.class,
                            new LeadingMarginSpan.Standard(numberMargin));
                }
            }
        }
    }

    private static void start(Editable text, Object mark) {
        int len = text.length();
        text.setSpan(mark, len, len, Spannable.SPAN_MARK_MARK);
    }

    private static void end(Editable text, Class<?> kind, Object... replaces) {
        int len = text.length();
        Object obj = getLast(text, kind);
        int where = text.getSpanStart(obj);
        text.removeSpan(obj);
        if (where != len) {
            for (Object replace : replaces) {
                text.setSpan(replace, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private static Object getLast(Spanned text, Class kind) {
        Object[] objs = text.getSpans(0, text.length(), kind);

        if (objs.length == 0) {
            return null;
        }
        else {
            return objs[objs.length - 1];
        }
    }

    private static class Strike {
    }

    private static class Ul {
    }

    private static class Ol {
    }

}
