package com.vorsk.tethr;


import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;


public class TetherAccessibilityService extends AccessibilityService {
    private static final String TAG = TetherAccessibilityService.class.getSimpleName();
    private static long enableTime = 0;
    private static final long enableDuration = 10; // seconds
    private static int mode;
    private static int stage = 0;

    public static final int TETHER_USB = 0;
    public static final int TETHER_WIFI = 1;
    public static final int TETHER_BLUETOOTH = 2;

    public static void enableService(int m) {
        mode = m;
        stage = 1;
        TetherAccessibilityService.enableTime = System.currentTimeMillis() / 1000L;
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //int eventType = event.getEventType();
        AccessibilityNodeInfo rootNode = event.getSource();
        if (rootNode == null) {
            return;
        }

        //Log.v(TAG, "event type: " + event.getEventType());
        //Log.v(TAG, "event text: " + getEventText(event));
        //Log.v(TAG, "current mode: " + mode);
        //printViewTree(rootNode);

        // was the service enabled recently?
        if (((System.currentTimeMillis() / 1000L) - enableTime) <= enableDuration) {
            if (stage == 1) {
                performToggle(rootNode);
            } else if (stage == 2) {
                stage = 0;
                // go back to app
                Log.v(TAG, "state changed, pressing back button");
                // TODO actually check for switch status
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                enableTime = 0;
            }
            event.recycle();
        }
    }

    private void performToggle(AccessibilityNodeInfo rootNode) {
        ArrayList<AccessibilityNodeInfo> modeNodes = findNodeByClassName(rootNode, "android.widget.Switch");
        Log.v(TAG, "switch nodes: "+modeNodes.size());
        if (modeNodes.size() == 3) {
            //enableTime = 0;
            AccessibilityNodeInfo modeNode = modeNodes.get(mode);

            // parent node is clickable
            modeNode = modeNode.getParent();

            // toggle
            modeNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);

            // press back button
            // moved to different stage
            //performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);

            stage = 2;
            return;
        }
        Log.e(TAG, "Service active but unable to find toggle");
    }

    private static ArrayList<AccessibilityNodeInfo> findNodeByClassName(AccessibilityNodeInfo rootNode, final String className) {
        class NodeFinder {
            private ArrayList<AccessibilityNodeInfo> nodes = new ArrayList<>();
            private void find(AccessibilityNodeInfo innerRootNode) {
                if (innerRootNode == null || innerRootNode.getClassName() == null) {
                    return;
                }
                int childCount = innerRootNode.getChildCount();
                if (childCount == 0) {
                    if (className.equals(innerRootNode.getClassName().toString())) {
                        nodes.add(innerRootNode);
                    }
                } else {
                    for (int i = 0; i < childCount; i++) {
                        find(innerRootNode.getChild(i));
                    }
                }
            }
        }

        NodeFinder nf = new NodeFinder();
        nf.find(rootNode);

        return nf.nodes;
    }

    private static void printViewTree(AccessibilityNodeInfo parentView) {
        ArrayList<AccessibilityNodeInfo> buttonNodes = new ArrayList<>();
        printViewTreeIteration(parentView, buttonNodes, 0, "0");
    }


    private static void printViewTreeIteration(AccessibilityNodeInfo parentView, ArrayList<AccessibilityNodeInfo> buttonNodes, int d, String pos) {
        if (parentView == null || parentView.getClassName() == null) {
            return;
        }
        Log.i(TAG, "======================");
        Log.i(TAG, "parent id: " + pos);
        Log.i(TAG, "parent depth: " + d);
        Log.i(TAG, "parent class: " + parentView.getClassName());
        Log.i(TAG, "parent viewID: " + parentView.getViewIdResourceName());
        Log.i(TAG, "parent child count: " + parentView.getChildCount());
        Log.i(TAG, "parent string: " + parentView.toString());
        Log.i(TAG, "parent text: " + parentView.getText());
        Log.i(TAG, "======================");
        int childCount = parentView.getChildCount();
        if (childCount == 0) {
            buttonNodes.add(parentView);
        } else {
            for (int i = 0; i < childCount; i++) {
                printViewTreeIteration(parentView.getChild(i), buttonNodes, d + 1, pos + i);
            }
        }
    }

    private static String getEventText(AccessibilityEvent event) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence s : event.getText()) {
            sb.append(s);
        }
        return sb.toString();
    }

    @Override
    public void onServiceConnected() {
        //Log.i(TAG, "onServiceConnected");
        enableTime = 0;
    }

    @Override
    public void onInterrupt() {
        //Log.i(TAG, "onInterrupt");
    }
}