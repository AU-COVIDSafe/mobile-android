package com.atlassian.mobilekit.module.feedback.commands;


import android.os.Looper;

import com.atlassian.mobilekit.module.core.Command;
import com.atlassian.mobilekit.module.core.Receiver;
import com.atlassian.mobilekit.module.core.UiNotifier;
import com.atlassian.mobilekit.module.core.UiReceiver;

abstract class AbstractCommand<T> implements Command {

    private final Receiver<T> receiver;
    private final UiNotifier uiNotifier;

    AbstractCommand(Receiver<T> receiver, UiNotifier uiNotifier) {
        this.receiver = receiver;
        this.uiNotifier = uiNotifier;
    }

    void updateReceiver(final T data) {

        if (receiver == null) {
            return;
        }

        if (!(receiver instanceof UiReceiver) || isMainThread()) {
            receiver.receive(data);
        } else {
            // Post runnable
            uiNotifier.post(new Runnable() {
                @Override
                public void run() {
                    receiver.receive(data);
                }
            });
        }
    }

    private boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

}

