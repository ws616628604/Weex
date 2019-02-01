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

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.bridge.WXBridgeManager;
import com.taobao.weex.ui.action.BasicComponentData;
import com.taobao.weex.ui.component.WXVContainer;

import java.util.HashMap;
import java.util.Map;

public class MyMask extends WXVContainer {

    private int mHeight;
    private View.OnLayoutChangeListener mOnLayoutChangeListener;
    private ViewGroup mParent;
    private FrameLayout mFrameLayout;
    private Activity mActivity;

    public MyMask(WXSDKInstance instance, WXVContainer parent, BasicComponentData basicComponentData) {
        super(instance, parent, basicComponentData);
    }

    @Override
    protected View initComponentHostView(@NonNull Context context) {
        if (context instanceof Activity) {
            fireVisibleChangedEvent(true);
            mActivity = (Activity) context;
            mParent = (ViewGroup) mActivity.findViewById(android.R.id.content);
            mFrameLayout = new FrameLayout(context);

            mParent.addView(mFrameLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mFrameLayout.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

                        fireVisibleChangedEvent(false);
                        return true;
                    }
                    return false;
                }
            });
            mFrameLayout.setFocusable(true);
            mFrameLayout.setFocusableInTouchMode(true);
            mFrameLayout.requestFocus();
            mOnLayoutChangeListener = new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    int height = bottom - top;
                    if (height != mHeight) {
                        mHeight = height;

                        WXBridgeManager.getInstance().post(new Runnable() {
                            @Override
                            public void run() {
                                WXBridgeManager.getInstance().setStyleHeight(getInstanceId(), getRef(), mHeight);
                            }
                        });
                    }
                }
            };
            mParent.addOnLayoutChangeListener(mOnLayoutChangeListener);

            return mFrameLayout;
        }

        return super.initComponentHostView(context);
    }

    @Override
    public boolean isVirtualComponent() {
        return true;
    }

    @Override
    public void removeVirtualComponent() {
        fireVisibleChangedEvent(false);
        mParent.removeView(mFrameLayout);
        mParent.removeOnLayoutChangeListener(mOnLayoutChangeListener);
    }

    private void fireVisibleChangedEvent(boolean visible) {
        Map<String, Object> event = new HashMap<>(1);
        event.put("visible", visible);
        fireEvent("visiblechanged", event);
    }
}
