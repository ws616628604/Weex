/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.alibaba.weex.yzh;

import android.text.TextUtils;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.http.HttpMethod;
import com.taobao.weex.adapter.IWXHttpAdapter;
import com.taobao.weex.common.WXRequest;
import com.taobao.weex.common.WXResponse;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class OkHttpWXHttpAdapter implements IWXHttpAdapter {
    @Override
    public void sendRequest(WXRequest request, OnHttpListener listener) {
        if (listener == null) {
            return;
        }

        listener.onHttpStart();

        Request.Builder builder = new Request.Builder().url(request.url);

        //添加header
        addHeader(builder, request.paramMap);

        //添加body
        Request okRequest = addBody(builder, request);

        request(okRequest, listener);

    }

    private void request(Request request, final OnHttpListener listener) {
        OkHttpClient okHttpClient = getOkHttpClient();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                if (listener == null) {
                    return;
                }
                WXResponse wxResponse = new WXResponse();
                wxResponse.statusCode = "-1";
                wxResponse.errorCode = "-1";
                wxResponse.errorMsg = e.getMessage();
                listener.onHttpFinish(wxResponse);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (listener == null) {
                    return;
                }
                WXResponse wxResponse = new WXResponse();
                int code = response.code();
                wxResponse.statusCode = String.valueOf(code);
                listener.onHeadersReceived(code, toMultimap(response.headers()));
                if (response.isSuccessful()) {
                    wxResponse.originalData = response.body().bytes();
                } else {
                    wxResponse.errorCode = String.valueOf(code);
                    wxResponse.errorMsg = response.toString();
                }
                listener.onHttpFinish(wxResponse);
            }
        });


    }

    public Map<String, List<String>> toMultimap(Headers headers) {
        Map<String, List<String>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> names = headers.names();
        for (String name : names) {
            result.put(name, headers.values(name));
        }
        return result;
    }

    private Request addBody(Request.Builder builder, WXRequest request) {

        if (TextUtils.isEmpty(request.method)) {
            request.method = "GET";
        }
        request.method = request.method.toUpperCase();

        if (HttpMethod.permitsRequestBody(request.method)) {
            RequestBody requestBody = null;
            if (request.body != null) {
                MediaType mediaType = null;
                if (request.paramMap != null) {
                    String type = request.paramMap.get("Content-Type");
                    if (type != null) {
                        mediaType = MediaType.parse(type);
                    }
                }
                if (mediaType == null) {
                    mediaType = MediaType.parse("application/octet-stream; charset=utf-8");
                }
                requestBody = RequestBody.create(mediaType, request.body);
            }
            return builder.method(request.method, requestBody).build();
        } else {
            return builder.method(request.method, null).build();
        }
    }


    private void addHeader(Request.Builder builder, Map<String, String> paramMap) {
        if (paramMap != null && !paramMap.isEmpty()) {
            Iterator<Map.Entry<String, String>> iterator = paramMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && value != null) {
                    builder.addHeader(key, value);
                }
            }
        }
    }

    private OkHttpClient getOkHttpClient() {
        return Inner.mOkHttpClient;
    }

    private static class Inner {
        private static OkHttpClient mOkHttpClient = new OkHttpClient();
    }
}
