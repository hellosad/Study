package com.lanyi.study.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * 功能概要描述：
 * 功能详细描述：
 * 作者： Kay
 * 创建日期： 16/9/19
 * 修改人：
 * 修改日期：
 * 版本号：
 * 版权所有：Copyright © 2015-2016 上海览益信息科技有限公司 http://www.lanyife.com
 */
public class MyScrollView extends ScrollView {

    public MyScrollView(Context context) {
        super(context);
    }

    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        Log.d("Kay", "myscroll_____________y=" + ev.getY() + " scroll=" + getScrollY());
        return super.onTouchEvent(ev);
    }
}
